package com.github.playback.core

import com.github.playback.api.*
import com.github.playback.ext.gson.GsonJsonMapper
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PlaybackTest {
  private val webServer: TestWebServer = TestWebServer()
  private val json: JsonMapper = GsonJsonMapper()
  private val documentRepository: DocumentRepository = mockk()
  private val webRequest: WebRequest = mockk()
  private val webResponse: WebResponse = mockk()

  @BeforeEach
  fun init() {
    Playback.initialize(webServer, json, documentRepository)
  }

  @Test
  fun listCollections_empty() {
    every { documentRepository.listCollections() } returns success(listOf())
    val slot = slot<String>()
    every { webResponse.send(200, capture(slot)) } returns Unit
    webServer.get("/", webRequest, webResponse)
    val captured = slot.captured
    assertThat(captured).isNotNull
    val data = json(captured)
    assertThat(data).containsKey("collections")
    assertThat(data["collections"] as List<*>).isEmpty()
  }

  @Test
  fun listCollections_notEmpty() {
    every { documentRepository.listCollections() } returns success(listOf("toto", "titi"))
    val slot = slot<String>()
    every { webResponse.send(200, capture(slot)) } returns Unit
    webServer.get("/", webRequest, webResponse)
    val captured = slot.captured
    assertThat(captured).isNotNull
    val data = json(captured)
    assertThat(data).containsKey("collections")
    assertThat(data["collections"] as List<*>).containsExactly("toto", "titi")
  }

  @Test
  fun listCollections_error() {
    every { documentRepository.listCollections() } returns failure(WebError(404, "not found"))
    val slot = slot<WebError>()
    every { webResponse.send(capture(slot), json) } returns Unit
    webServer.get("/", webRequest, webResponse)
    val captured = slot.captured
    assertThat(captured).isNotNull
    assertThat(captured.statusCode).isEqualTo(404)
    assertThat(captured.message).isEqualTo("not found")
  }

  @Test
  fun createCollections_ok() {
    val slot = slot<String>()
    every { documentRepository.createCollection(capture(slot)) } returns null
    every { webResponse.send(201, null) } returns Unit
    every { webRequest.body() } returns json(mapOf("name" to "toto"))

    webServer.post("/", webRequest, webResponse)
    val captured = slot.captured
    assertThat(captured)
      .isNotNull
      .isEqualTo("toto")
  }

  @Test
  fun createCollections_badRequest() {
    val errorSlot = slot<WebError>()
    every { webResponse.send(capture(errorSlot), json) } returns Unit
    every { webRequest.body() } returns json(mapOf("name" to null))

    webServer.post("/", webRequest, webResponse)

    val error = errorSlot.captured
    assertThat(error.statusCode).isEqualTo(400)
    assertThat(error.message).isEqualTo("missing collection name")
  }

  @Test
  fun createCollections_error() {
    val collectionSlot = slot<String>()
    val errorSlot = slot<WebError>()
    every { documentRepository.createCollection(capture(collectionSlot)) } returns WebError(500, "database error")
    every { webResponse.send(capture(errorSlot), json) } returns Unit
    every { webRequest.body() } returns json(mapOf("name" to "toto"))

    webServer.post("/", webRequest, webResponse)
    val capturedCollection = collectionSlot.captured
    assertThat(capturedCollection).isEqualTo("toto")

    val error = errorSlot.captured
    assertThat(error.statusCode).isEqualTo(500)
    assertThat(error.message).isEqualTo("database error")
  }

}
