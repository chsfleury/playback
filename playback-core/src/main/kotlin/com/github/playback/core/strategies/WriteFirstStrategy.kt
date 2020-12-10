package com.github.playback.core.strategies

import com.github.playback.api.Document
import com.github.playback.api.DocumentRepository
import com.github.playback.api.JsonMapper
import com.github.playback.api.Result
import com.github.playback.api.WebError
import kotlinx.datetime.Clock

class WriteFirstStrategy(
  override val documentRepository: DocumentRepository,
  override val jsonMapper: JsonMapper
) : PlaybackStrategy {

  override fun getUpdates(
    collection: String,
    documentId: String,
    timestampStart: Long?,
    timestampEnd: Long?
  ): Result<WebError, List<Document>> {
    return documentRepository.getUpdates(collection, documentId, timestampStart, timestampEnd)
  }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> {
    return documentRepository.getDocument(collection, documentId, timestamp)
  }

  override fun saveDocument(collection: String, document: Document): WebError? {
    return documentRepository.saveDocument(
      collection,
      Document(document.id, Clock.System.now().toEpochMilliseconds(), false, document.fields)
    )
  }

}
