package com.github.playback.ext.elasticsearch

import com.github.playback.api.*
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.GetIndexResponse
import java.io.IOException


class ElasticsearchDocumentRepository(
  private val elastic: RestHighLevelClient,
  private val indexTemplates: Map<String, Map<String, *>>
): DocumentRepository {

  override fun listCollections(): Result<WebError, List<String>> = try {
    val request = GetIndexRequest("*")
    val response: GetIndexResponse = elastic.indices().get(request, RequestOptions.DEFAULT)
    success(response.indices.toList())
  } catch (e: IOException) {
    failure(indexError(e))
  }

  override fun createCollection(collection: String): WebError? = try {
    val template = indexTemplates[collection]
    val request = CreateIndexRequest(collection).apply {
      if (template != null) {
        mapping(template)
      }
    }
    elastic.indices().create(request, RequestOptions.DEFAULT)
    null
  } catch (e: IOException) {
    indexError(e, collection)
  }

  override fun clearCollection(collection: String): WebError? = try {
    deleteCollection(collection)
    createCollection(collection)
  } catch (e: IOException) {
    indexError(e, collection)
  }

  override fun deleteCollection(collection: String): WebError? = try {
    val request = DeleteIndexRequest(collection)
    elastic.indices().delete(request, RequestOptions.DEFAULT)
    null
  } catch (e: IOException) {
    indexError(e, collection)
  }

  override fun getLastCompleteDocumentTimestamp(collection: String, documentId: String, timestamp: Long?): Long? {
    TODO("Not yet implemented")
  }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> {
    TODO("Not yet implemented")
  }

  override fun saveDocument(collection: String, document: Document): WebError? {
    TODO("Not yet implemented")
  }

  override fun deleteDocument(collection: String, documentId: String): WebError? {
    TODO("Not yet implemented")
  }

  override fun getLastUpdates(collection: String, documentId: String, n: Int): Result<WebError, List<Document>> {
    TODO("Not yet implemented")
  }

  override fun getUpdates(
    collection: String,
    documentId: String,
    timestampStart: Long?,
    timestampEnd: Long?
  ): Result<WebError, List<Document>> {
    TODO("Not yet implemented")
  }

  private fun indexError(e: Exception, collection: String? = null): WebError {
    return WebError(500, "index error", e.message)
  }
}
