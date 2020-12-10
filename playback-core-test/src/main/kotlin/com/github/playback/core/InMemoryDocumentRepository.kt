package com.github.playback.core

import com.github.playback.api.*

class InMemoryDocumentRepository : DocumentRepository {

  companion object {
    val NOT_FOUND_ERROR = WebError(404, "not found")
  }

  private val collections: MutableMap<String, MutableList<Document>> = mutableMapOf()

  override fun listCollections(): Result<WebError, List<String>> = success(collections.keys.toList())

  override fun createCollection(collection: String): WebError? = if (!collections.containsKey(collection)) {
    collections[collection] = mutableListOf()
    null
  } else {
    NOT_FOUND_ERROR
  }

  override fun clearCollection(collection: String): WebError? = if (collections.containsKey(collection)) {
    collections[collection]?.clear()
    null
  } else {
    NOT_FOUND_ERROR
  }

  override fun deleteCollection(collection: String): WebError? {
    collections.remove(collection)
    return null
  }

  override fun getLastCompleteDocumentTimestamp(collection: String, documentId: String, timestamp: Long?): Long? =
    if (collections.containsKey(collection)) {
      collections[collection]!!.asSequence()
        .filter { it.id == documentId && (timestamp == null || it.timestamp <= timestamp) && !it.patch }
        .sortedByDescending { it.timestamp }
        .firstOrNull()
        ?.timestamp
    } else {
      null
    }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> {
    return collections[collection]!!.asSequence()
      .filter { it.id == documentId && (timestamp == null || it.timestamp <= timestamp) }
      .sortedByDescending { it.timestamp }
      .firstOrNull()
      ?.let { success(it) }
      ?: failure(NOT_FOUND_ERROR)
  }

  override fun saveDocument(collection: String, document: Document): WebError? =
    if (collections.containsKey(collection)) {
      collections[collection]!!.add(document)
      null
    } else {
      NOT_FOUND_ERROR
    }

  override fun deleteDocument(collection: String, documentId: String): WebError? =
    if (collections.containsKey(collection)) {
      collections[collection]!!.removeIf { it.id == documentId }
      null
    } else {
      NOT_FOUND_ERROR
    }

  override fun getLastUpdates(collection: String, documentId: String, n: Int): Result<WebError, List<Document>> =
    if (collections.containsKey(collection)) {
      success(
        collections[collection]!!.asSequence()
          .filter { it.id == documentId }
          .sortedByDescending { it.timestamp }
          .take(n)
          .toList()
      )
    } else {
      failure(NOT_FOUND_ERROR)
    }

  override fun getUpdates(
    collection: String,
    documentId: String,
    timestampStart: Long?,
    timestampEnd: Long?
  ): Result<WebError, List<Document>> = if (collections.containsKey(collection)) {
    success(
      collections[collection]!!.asSequence()
        .filter {
          it.id == documentId
                  && (timestampStart == null || timestampStart <= it.timestamp)
                  && (timestampEnd == null || timestampEnd >= it.timestamp)
        }
        .sortedBy { it.timestamp }
        .toList()
    )
  } else {
    failure(NOT_FOUND_ERROR)
  }

}
