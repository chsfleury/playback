package com.github.playback.core.strategies

import com.github.playback.api.Document
import com.github.playback.api.DocumentRepository
import com.github.playback.api.JsonMapper
import com.github.playback.api.Result
import com.github.playback.api.WebError

interface PlaybackStrategy {

  val documentRepository: DocumentRepository
  val jsonMapper: JsonMapper

  fun getUpdates(collection: String, documentId: String, timestampStart: Long? = null, timestampEnd: Long? = null): Result<WebError, List<Document>>
  fun getDocument(collection: String, documentId: String, timestamp: Long? = null): Result<WebError, Document>
  fun saveDocument(collection: String, document: Document): WebError?

}
