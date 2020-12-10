package com.github.playback.core

import com.github.playback.api.Document
import com.github.playback.api.DocumentRepository
import com.github.playback.api.Success
import com.github.playback.api.WebError
import kotlinx.datetime.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("UsePropertyAccessSyntax", "MemberVisibilityCanBePrivate")
abstract class AbstractDocumentRepositoryTest(
  protected val tested: DocumentRepository
) {
  @Test
  fun testCollections() {
    // LIST
    verifyCollections()

    // CREATE
    assertThat(tested.createCollection("foo")).isNull()
    assertThat(tested.createCollection("bar")).isNull()

    verifyCollections("foo", "bar")

    // CLEAR
    val doc = Document("doc1", Clock.System.now().toEpochMilliseconds(), false, mapOf("field" to "value"))
    assertThat(tested.saveDocument("bar", doc)).isNull()

    assertThat(verifyDocumentExists("bar", "doc1")).isTrue()

    assertThat(tested.clearCollection("bar"))

    assertThat(verifyDocumentExists("bar", "doc1")).isFalse()

    assertThat(tested.clearCollection("unknown")).isNotNull()

    // DELETE
    assertThat(tested.deleteCollection("bar")).isNull()

    verifyCollections("foo")

    assertThat(tested.deleteCollection("foo")).isNull()
  }

  @Test
  fun testDocuments() {
    assertThat(tested.createCollection("collection")).isNull()
    assertThat(verifyDocumentExists("collection", "doc1")).isFalse()

    val doc = Document("doc1", Clock.System.now().toEpochMilliseconds(), false, mapOf("field" to "value"))
    assertThat(tested.saveDocument("collection", doc)).isNull()

    assertThat(verifyDocumentExists("collection", "doc1")).isTrue()
    val updateResult = tested.getUpdates("collection", "doc1")
    val updates = updateResult as Success<WebError, List<Document>>
    assertThat(updates.value).hasSize(1).extracting("id").containsExactly("doc1")
  }

  private fun verifyCollections(vararg collections: String) {
    val result = tested.listCollections()
    assertThat(result.isSuccess()).isTrue()
    val success = result as Success<WebError, List<String>>
    if (collections.isEmpty()) {
      assertThat(success.value).isEmpty()
    } else {
      assertThat(success.value).containsOnly(*collections)
    }
  }

  private fun verifyDocumentExists(collection: String, documentId: String): Boolean = tested.getDocument(collection, documentId).isSuccess()
}
