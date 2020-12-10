package com.github.playback.api

interface DocumentRepository {

  fun listCollections(): Result<WebError, List<String>>
  fun createCollection(collection: String): WebError?
  fun clearCollection(collection: String): WebError?
  fun deleteCollection(collection: String): WebError?

  fun getLastCompleteDocumentTimestamp(collection: String, documentId: String, timestamp: Long? = null): Long?
  fun getDocument(collection: String, documentId: String, timestamp: Long? = null): Result<WebError, Document>
  fun saveDocument(collection: String, document: Document): WebError?
  fun deleteDocument(collection: String, documentId: String): WebError?

  fun getLastUpdates(collection: String, documentId: String, n: Int): Result<WebError, List<Document>>
  fun getUpdates(collection: String, documentId: String, timestampStart: Long? = null, timestampEnd: Long? = null): Result<WebError, List<Document>>

}
