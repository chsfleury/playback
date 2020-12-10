package com.github.playback.core.strategies

import com.github.playback.api.*
import kotlinx.datetime.Clock

class CompactStrategy (
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
    val lastCompleteTimestamp = documentRepository.getLastCompleteDocumentTimestamp(collection, documentId, timestampStart)
    return documentRepository.getUpdates(collection, documentId, lastCompleteTimestamp, timestampEnd)
      .mapTry({ aggregateUpdates(it, timestampStart, timestampEnd) }, { WebError(404, it.message) })
  }

  private fun aggregateUpdates(updates: List<Document>, timestampStart: Long?, timestampEnd: Long?): List<Document> {
    return if (updates.isEmpty()) {
      error("no document found")
    } else {
      val mutableFields: MutableMap<String, Any?> = mutableMapOf()
      updates.map { update ->
        if (!update.patch) {
          mutableFields.clear()
        }
        mutableFields.putAll(update.fields)
        Document(update.id, update.timestamp, false, mutableFields.toMutableMap())
      }.filter { (timestampStart == null || it.timestamp >= timestampStart) && (timestampEnd == null || it.timestamp <= timestampEnd) }
    }
  }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> {
    val lastCompleteTimestamp = documentRepository.getLastCompleteDocumentTimestamp(collection, documentId, timestamp)
    return documentRepository.getUpdates(collection, documentId, lastCompleteTimestamp, timestamp)
      .mapTry({ aggregateDocument(it) }, { WebError(404, it.message) })
  }

  private fun aggregateDocument(documents: List<Document>): Document {
    return if (documents.isEmpty()) {
      error("no document found")
    } else {
      val completeDocument = documents.first()
      val mostRecentUpdate = documents.last()
      val mutableFields: MutableMap<String, Any?> = mutableMapOf()
      documents.forEach { mutableFields.putAll(it.fields) }
      Document(completeDocument.id, mostRecentUpdate.timestamp, false, mutableFields)
    }
  }

  override fun saveDocument(collection: String, document: Document): WebError? {
    when(val lastUpdatesResult = documentRepository.getLastUpdates(collection, document.id, patchInterval)) {
      is Failure -> return lastUpdatesResult.error
      is Success -> {
        val lastUpdates = lastUpdatesResult.value
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return if (lastUpdates.any { !it.patch }) {
          val currentState = aggregateDocument(lastUpdates)
          val data= computeMapDelta(currentState.fields, document.fields)
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
            is List<*> -> when (val d = computeListDelta(originValue, value)) {
              emptyList -> NOOP
              else -> delta[key] = d
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
            is Map<*, *> -> when (val d = computeMapDelta(originValue as Map<String, Any?>, value as Map<String, Any?>)) {
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
    fun computeListDelta(origin: List<Any?>, current: List<Any?>): List<Any?> {
      val delta = mutableListOf<Any?>()
      val originIter = origin.iterator()
      val currentIter = current.iterator()


      while (originIter.hasNext() && currentIter.hasNext()) {
        val originItem = originIter.next()
        when (val currentItem = currentIter.next()) {

          /*
           * Current list item is null, do nothinh
           */
          null -> NOOP

          /*
           * Current list item is a list (listception :) )
           * If origin item is a map or null (to preserve indices), add it
           * else if origin item is a list, add the computed delta
           */
          is List<*> -> when (originItem) {
            null -> delta.add(currentItem)
            is Map<*, *> -> delta.add(currentItem)
            is List<*> -> delta.add(computeListDelta(originItem, currentItem))
          }

          /*
           * Current list item is a map
           * If origin item is a list or null (to preserve indices), add it
           * else if origin item is a map, add the computed delta
           */
          is Map<*, *> -> when (originItem) {
            null -> delta.add(currentItem)
            is Map<*, *> -> delta.add(computeMapDelta(originItem as Map<String, Any?>, currentItem as Map<String, Any?>))
            is List<*> -> delta.add(currentItem)
          }

          /*
           * Current list item is a value, add it
           */
          else -> delta.add(currentItem)
        }
      }

      /*
       * Continue adding current index non-null values
       * Omit origin list index not present in current list
       */
      while (currentIter.hasNext()) {
        when (val currentItem = currentIter.next()) {
          null -> NOOP
          else -> delta.add(currentItem)
        }
      }

      return delta
    }

    /**
     * complete + n * delta = complete (up to date)
     */
    fun aggregate(documentsToAggregate: List<Document>): Document? {
      val documents = fromLastCompleteDocument(documentsToAggregate)
      return when(documents.size) {
        0 -> null
        1 -> documents[0]
        else -> null
      }
    }

    fun aggregate(documentsToAggregate: List<Document>, destination: Document): Document {

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
  }
}
