package com.github.playback.core.strategies

import com.github.playback.api.*
import kotlinx.datetime.Clock

@Suppress("UNCHECKED_CAST")
class CompactStrategy(
  override val documentRepository: DocumentRepository,
  override val jsonMapper: JsonMapper,
  private val patchInterval: Int
) : PlaybackStrategy {

  override fun getUpdates(
    collection: String,
    documentId: String,
    timestampStart: Long?,
    timestampEnd: Long?
  ): Result<WebError, List<Document>> {
    val lastCompleteTimestamp =
      documentRepository.getLastCompleteDocumentTimestamp(collection, documentId, timestampStart)
    return documentRepository.getUpdates(collection, documentId, lastCompleteTimestamp, timestampEnd)
      .mapTry({ aggregate(it, timestampStart, timestampEnd) }, { WebError(404, it.message) })
  }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> {
    val lastCompleteTimestamp = documentRepository.getLastCompleteDocumentTimestamp(collection, documentId, timestamp)
    return documentRepository.getUpdates(collection, documentId, lastCompleteTimestamp, timestamp)
      .mapTry({ aggregate(it) ?: error("no document found") }, { WebError(404, it.message) })
  }

  override fun saveDocument(collection: String, document: Document): WebError? {
    when (val lastUpdatesResult = documentRepository.getLastUpdates(collection, document.id, patchInterval)) {
      is Failure -> return lastUpdatesResult.error
      is Success -> {
        val lastUpdates = lastUpdatesResult.value
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return if (lastUpdates.any { !it.patch }) {
          val currentState = aggregate(lastUpdates) ?: error("unexpected state")
          val data = computeMapDelta(currentState.fields, document.fields)
          documentRepository.saveDocument(
            collection,
            Document(document.id, timestamp, true, data)
          )
        } else {
          documentRepository.saveDocument(
            collection,
            Document(document.id, timestamp, false, document.fields)
          )
        }
      }
    }
  }

  companion object {
    private val emptyList = emptyList<Any?>()
    private val emptyMap = emptyMap<String, Any?>()
    private val NOOP = Unit

    /**
     * Compute delta between two maps
     */
    fun computeMapDelta(origin: Map<String, Any?>, current: Map<String, Any?>): Map<String, Any?> {
      val delta = mutableMapOf<String, Any?>()
      current.forEach { (key, value) ->
        val originValue = origin[key]
        when (value) {

          /*
           * Current value is null,
           * if originValue is null, nothing to do
           * else remove the value with null (signal for the aggregation, later)
           */
          null -> when (originValue) {
            null -> NOOP
            else -> delta[key] = null
          }

          /*
           * Current value is a list
           * if origin value is a list too, go compute the delta,
           *    if the delta is empty, the lists are equal, nothing to add to map delta
           *    else add the delta to map delta
           * else the origin value is not a list, simply overwrite
           */
          is List<*> -> when (originValue) {
            is List<*> -> {
              val (subListAreEqual, subDelta) = computeListDelta(originValue, value)
              if (!subListAreEqual) {
                when (subDelta) {
                  emptyList -> NOOP
                  else -> delta[key] = subDelta
                }
              }
            }
            else -> delta[key] = value
          }

          /*
           * Current value is a map
           * if origin value is a map too, go compute the delta
           *    if the delta is empty, the maps are equal, nothing to add to parent map delta
           *    else add the delta to parent map delta
           * else the origin value is not a map, simply overwrite
           */
          is Map<*, *> -> when (originValue) {
            is Map<*, *> -> when (val d =
              computeMapDelta(originValue as Map<String, Any?>, value as Map<String, Any?>)) {
              emptyMap -> NOOP
              else -> delta[key] = d
            }
            else -> delta[key] = value
          }

          /*
           * Current value is a simple value
           * if origin value is equal, do nothing
           * else overwrite
           */
          else -> when (originValue) {
            value -> NOOP
            else -> delta[key] = value
          }

        }
      }

      /*
       * The origin map probably have fields that don't exists anymore on current map,
       * removing value explicity with null
       */
      origin.keys.filterNot { current.containsKey(it) }.forEach { delta[it] = null }

      return delta
    }

    /**
     * Compute delta between two lists
     */
    fun computeListDelta(origin: List<Any?>, current: List<Any?>): Pair<Boolean, List<Any?>> {
      val delta = mutableListOf<Any?>()
      val originIter = origin.iterator()
      val currentIter = current.iterator()

      var listAreEquals = true

      while (originIter.hasNext() && currentIter.hasNext()) {
        val originItem = originIter.next()
        when (val currentItem = currentIter.next()) {

          /*
           * Current list item is null, do nothing
           */
          null -> NOOP

          /*
           * Current list item is a list (listception :) )
           * If origin item is a map or null (to preserve indices), add it
           * else if origin item is a list, add the computed delta
           */
          is List<*> -> when (originItem) {
            null -> {
              listAreEquals = false
              delta.add(currentItem)
            }
            is Map<*, *> -> {
              listAreEquals = false
              delta.add(currentItem)
            }
            is List<*> -> {
              val (subListsAreEqual, subDelta) = computeListDelta(originItem, currentItem)
              if (!subListsAreEqual) {
                listAreEquals = false
              }
              delta.add(subDelta)
            }
          }

          /*
           * Current list item is a map
           * If origin item is a list or null (to preserve indices), add it
           * else if origin item is a map, add the computed delta
           */
          is Map<*, *> -> when (originItem) {
            null -> {
              listAreEquals = false
              delta.add(currentItem)
            }
            is Map<*, *> -> {
              val subDelta = computeMapDelta(
                originItem as Map<String, Any?>,
                currentItem as Map<String, Any?>
              )
              delta.add(subDelta)
              if (subDelta.isNotEmpty()) {
                listAreEquals = false
              }
            }
            is List<*> -> {
              listAreEquals = false
              delta.add(currentItem)
            }
          }

          /*
           * Current list item is a value, add it
           */
          else -> {
            if (currentItem != originItem) {
              listAreEquals = false
            }
            delta.add(currentItem)
          }
        }
      }

      /*
       * Continue adding current index non-null values
       * Omit origin list index not present in current list
       */
      while (currentIter.hasNext()) {
        listAreEquals = false
        when (val currentItem = currentIter.next()) {
          null -> NOOP
          else -> delta.add(currentItem)
        }
      }

      if (originIter.hasNext()) {
        listAreEquals = false
      }

      return listAreEquals to delta
    }

    /**
     * complete + n * delta = complete (up to date)
     */
    fun aggregate(documentsToAggregate: List<Document>): Document? {
      val documents = fromLastCompleteDocument(documentsToAggregate)
      return when (documents.size) {
        0 -> null
        1 -> documents[0]
        else -> aggregate(documents, mutableMapOf())
      }
    }

    fun aggregate(documents: List<Document>, mutableFields: MutableMap<String, Any?>): Document {
      val completeDocument = documents.first()
      val mostRecentUpdate = documents.last()
      documents.forEach { applyDelta(it.fields, mutableFields) }
      return Document(completeDocument.id, mostRecentUpdate.timestamp, false, mutableFields)
    }

    fun aggregate(updates: List<Document>, timestampStart: Long?, timestampEnd: Long?): List<Document> {
      return if (updates.isEmpty()) {
        error("no document found")
      } else {
        val mutableFields: MutableMap<String, Any?> = mutableMapOf()
        updates.map { update ->
          if (update.patch) {
            applyDelta(update.fields, mutableFields)
          } else {
            mutableFields.clear()
            mutableFields.putAll(update.fields)
          }
          Document(update.id, update.timestamp, false, deepCopy(mutableFields))
        }
          .filter { (timestampStart == null || it.timestamp >= timestampStart) && (timestampEnd == null || it.timestamp <= timestampEnd) }
      }
    }

    fun applyDelta(delta: Map<String, Any?>, destination: MutableMap<String, Any?>) {
      delta.forEach { (key, deltaValue) ->
        val destValue = destination[key]
        when (deltaValue) {
          null -> destination.remove(key)
          is Map<*, *> -> when (destValue) {
            is Map<*, *> -> applyDelta(deltaValue as Map<String, Any?>, destValue as MutableMap<String, Any?>)
            else -> destination[key] = deltaValue
          }
          is List<*> -> when (destValue) {
            is List<*> -> applyDelta(deltaValue, destValue as MutableList<Any?>)
            else -> destination[key] = deltaValue
          }
          else -> destination[key] = deltaValue
        }
      }
    }

    fun applyDelta(delta: List<Any?>, destination: MutableList<Any?>) {
      val deltaIterator = delta.iterator()
      val destIterator = destination.iterator()
      var i = 0
      while (deltaIterator.hasNext() && destIterator.hasNext()) {
        val deltaValue = deltaIterator.next()
        val destValue = destIterator.next()
        when (deltaValue) {
          is Map<*, *> -> when (destValue) {
            is Map<*, *> -> applyDelta(deltaValue as Map<String, Any?>, destValue as MutableMap<String, Any?>)
            else -> destination[i] = deltaValue
          }
          is List<*> -> when (destValue) {
            is List<*> -> applyDelta(deltaValue, destValue as MutableList<Any?>)
            else -> destination[i] = deltaValue
          }
          else -> destination[i] = deltaValue
        }
        i++
      }

      repeat(destination.size - delta.size) {
        destination.removeLast()
      }

      while (deltaIterator.hasNext()) {
        destination.add(deltaIterator.next())
      }

    }

    fun fromLastCompleteDocument(documentsToAggregate: List<Document>): List<Document> {
      var completeDocumentFound = false
      return documentsToAggregate.takeLastWhile {
        when {
          completeDocumentFound -> false
          it.patch -> true
          else -> {
            completeDocumentFound = true
            true
          }
        }
      }
    }

    fun deepCopy(map: Map<String, Any?>): Map<String, Any?> {
      val copy: MutableMap<String, Any?> = mutableMapOf()
      map.forEach { (k, v) ->
        when (v) {
          is Map<*, *> -> copy[k] = deepCopy(v as Map<String, Any?>)
          is List<*> -> copy[k] = deepCopy(v)
          else -> copy[k] = v
        }
      }
      return copy
    }

    fun deepCopy(list: List<Any?>): List<Any?> {
      val copy: MutableList<Any?> = mutableListOf()
      list.forEach { v ->
        when (v) {
          is Map<*, *> -> copy.add(deepCopy(v as Map<String, Any?>))
          is List<*> -> copy.add(deepCopy(v))
          else -> copy.add(v)
        }
      }
      return copy
    }
  }
}
