package com.github.playback.core

import com.github.playback.api.*
import com.github.playback.core.strategies.CompactStrategy
import com.github.playback.core.strategies.WriteFirstStrategy
import kotlinx.datetime.Clock

object Playback {

  private const val ROOT = "/"
  private const val COLLECTION_PATH = "/{collection}"
  private const val DOCUMENT_PATH = "/{collection}/doc/{documentId}"

  private const val MISSING_COLLECTION = "missing collection"
  private const val MISSING_DATA = "missing data"
  private const val MISSING_DOCUMENT_ID = "missing documentId"

  fun initialize(webServer: WebServer, json: JsonMapper, documentRepository: DocumentRepository, patchInterval: Int = 20) {
    val strategy = if (patchInterval > 0) {
      CompactStrategy(documentRepository, json, patchInterval)
    } else {
      WriteFirstStrategy(documentRepository, json)
    }

    webServer.get(ROOT) { _, res ->
      when (val result = documentRepository.listCollections()) {
        is Success -> res.send(200, json(mapOf("collections" to result.value)))
        is Failure -> res.send(result.error, json)
      }
    }

    webServer.post(ROOT) { req, res ->
      try {
        val body = req.body()?.let { json(it) } ?: error(MISSING_DATA)
        val collection = body["name"] ?: error("missing collection name")
        documentRepository
          .createCollection(collection as String)
          ?.run { res.send(this, json) }
          ?: res.send(201)
      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

    webServer.put(COLLECTION_PATH) { req, res ->
      try {
        val pathParams = req.pathParams()
        val collection = pathParams["collection"] ?: error(MISSING_COLLECTION)
        documentRepository
          .clearCollection(collection)
          ?.run { res.send(this, json) }
          ?: res.send(204)
      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

    webServer.delete(COLLECTION_PATH) { req, res ->
      try {
        val pathParams = req.pathParams()
        val collection = pathParams["collection"] ?: error(MISSING_COLLECTION)
        documentRepository
          .deleteCollection(collection)
          ?.run { res.send(this, json) }
          ?: res.send(204)
      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

    webServer.get(DOCUMENT_PATH) { req, res ->
      try {
        val pathParams = req.pathParams()
        val queryParams = req.queryParams()
        val collection = pathParams["collection"] ?: error(MISSING_COLLECTION)
        val documentId = pathParams["documentId"] ?: error(MISSING_DOCUMENT_ID)

        val timestamp = queryParams["timestamp"]
          ?.get(0)
          ?.takeIf { it.isNotBlank() }
          ?.toLong()

        val timestampStart = queryParams["timestamp_start"]
          ?.get(0)
          ?.takeIf { it.isNotBlank() }
          ?.toLong()

        val timestampEnd = queryParams["timestamp_end"]
          ?.get(0)
          ?.takeIf { it.isNotBlank() }
          ?.toLong()

        if (timestampStart != null || timestampEnd != null) {
          val result = strategy.getUpdates(
            collection, documentId, timestampStart, timestampEnd
          )

          when (result) {
            is Success -> res.send(200, json(mapOf("updates" to result.value)))
            is Failure -> res.send(result.error.statusCode, json(result.error))
          }
        } else {
          val result = strategy.getDocument(collection, documentId, timestamp)

          when (result) {
            is Success -> res.send(200, json(result.value))
            is Failure -> res.send(result.error.statusCode, json(result.error))
          }
        }


      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

    webServer.put(DOCUMENT_PATH) { req, res ->
      try {
        val pathParams = req.pathParams()
        val collection = pathParams["collection"] ?: error(MISSING_COLLECTION)
        val documentId = pathParams["documentId"] ?: error(MISSING_DOCUMENT_ID)
        val body = req.body()?.let { json(it) } ?: error(MISSING_DATA)
        strategy.saveDocument(
          collection,
          Document(documentId, Clock.System.now().toEpochMilliseconds(), false, body)
        )?.run { res.send(this, json) }
          ?: res.send(201)
      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

    webServer.delete(DOCUMENT_PATH) { req, res ->
      try {
        val pathParams = req.pathParams()
        val collection = pathParams["collection"] ?: error(MISSING_COLLECTION)
        val documentId = pathParams["documentId"] ?: error(MISSING_DOCUMENT_ID)
        documentRepository.deleteDocument(collection, documentId)
          ?.run { res.send(this, json) }
          ?: res.send(204)
      } catch (e: IllegalStateException) {
        res.send(badRequest(e), json)
      }
    }

  }

  private fun badRequest(e: IllegalStateException): WebError = WebError(400, e.message)

}
