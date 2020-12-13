package com.github.playback.core

import com.github.playback.api.Document
import com.github.playback.api.DocumentRepository
import com.github.playback.api.JsonMapper
import com.github.playback.core.strategies.CompactStrategy
import com.github.playback.core.strategies.PlaybackStrategy
import com.github.playback.ext.gson.GsonJsonMapper
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("UsePropertyAccessSyntax")
internal class CompactStrategyTest {

  private val documentRepository: DocumentRepository = mockk()
  private val json: JsonMapper = GsonJsonMapper()
  private val tested: PlaybackStrategy = CompactStrategy(documentRepository, json, 5)

  @Test
  fun testFromLastCompleteDocument() {
    val complete = doc(false)
    val complete2 = doc(false)
    val doc2 = doc(true)
    val doc3 = doc(true)
    val doc4 = doc(true)

    val list1 = listOf(complete, doc2, doc3, doc4)
    val result1 = CompactStrategy.fromLastCompleteDocument(list1)
    assertThat(result1).hasSize(4)
    assertThat(result1[0]).isSameAs(complete)
    assertThat(result1[1]).isSameAs(doc2)
    assertThat(result1[2]).isSameAs(doc3)
    assertThat(result1[3]).isSameAs(doc4)

    val list2 = listOf(doc2, complete, doc3, doc4)
    val result2 = CompactStrategy.fromLastCompleteDocument(list2)
    assertThat(result2).hasSize(3)
    assertThat(result2[0]).isSameAs(complete)
    assertThat(result2[1]).isSameAs(doc3)
    assertThat(result2[2]).isSameAs(doc4)

    val list3 = listOf(complete, doc2, complete2, doc3, doc4)
    val result3 = CompactStrategy.fromLastCompleteDocument(list3)
    assertThat(result3).hasSize(3)
    assertThat(result3[0]).isSameAs(complete2)
    assertThat(result3[1]).isSameAs(doc3)
    assertThat(result3[2]).isSameAs(doc4)

    val list4 = listOf(complete, complete2, doc3, doc4)
    val result4 = CompactStrategy.fromLastCompleteDocument(list4)
    assertThat(result4).hasSize(3)
    assertThat(result4[0]).isSameAs(complete2)
    assertThat(result4[1]).isSameAs(doc3)
    assertThat(result4[2]).isSameAs(doc4)

    val list5 = listOf(complete)
    val result5 = CompactStrategy.fromLastCompleteDocument(list5)
    assertThat(result5).hasSize(1)
    assertThat(result5[0]).isSameAs(complete)
  }

  private fun doc(patch: Boolean): Document = Document("id", 0, patch, emptyMap())

  @Test
  fun testComputeDelta1() {
    val current = """{ "field": "value" }"""
    val origin = """{ "origin": 0 }"""
    val expected = """{"field": "value", "origin": null}"""
    computeDelta(origin, current, expected)
  }

  @Test
  fun testComputeDelta2() {
    val current = """{ "field": "value" }"""
    val origin = """{ "field": "value" }"""
    val expected = """{}"""
    computeDelta(origin, current, expected)
  }

  @Test
  fun testComputeDelta3() {
    computeDelta("/origin.json", "/current.json", "/expected.json")
  }

  @Test
  fun testComputeDelta4() {
    computeDelta("/nuxt_10.12.2020_1109.json", "/nuxt_10.12.2020_1118.json", "/nuxt_expected.json")
  }

  @Test
  fun testComputeDelta5() {
    computeDelta("/list_delta_origin.json", "/list_delta_current.json", "/list_delta_expected.json")
  }

  private fun computeDelta(origin: String, current: String, expected: String) {
    if (origin.startsWith("/")) {
      val delta = CompactStrategy.computeMapDelta(load(origin), load(current))
      assertThat(delta).isEqualTo(load(expected))
    } else {
      val delta = CompactStrategy.computeMapDelta(json(origin), json(current))
      assertThat(delta).isEqualTo(json(expected))
    }
  }

  @Test
  fun testApplyDelta1() {
    val current = """{ "field": "value" }"""
    val delta = """{ "origin": 0 }"""
    val expected = """{ "field": "value", "origin": 0 }"""
    applyDelta(delta, current, expected)
  }

  @Test
  fun testApplyDelta2() {
    applyDelta("/expected.json", "/origin.json", "/current.json")
  }

  @Test
  fun testApplyDelta3() {
    val list: MutableList<Any?> = mutableListOf(1, 2, 3)
    CompactStrategy.applyDelta(
      listOf(1, 2, 3, 4),
      list
    )
    assertThat(list).containsExactly(1, 2, 3, 4)
  }

  @Test
  fun testApplyDelta4() {
    val list: MutableList<Any?> = mutableListOf(1, 2, 3)
    CompactStrategy.applyDelta(
      listOf(4, 5),
      list
    )
    assertThat(list).containsExactly(4, 5)
  }

  private fun applyDelta(delta: String, current: String, expected: String) {
    if (delta.startsWith("/")) {
      val result = load(current) as MutableMap<String, Any?>
      CompactStrategy.applyDelta(load(delta), result)
      assertThat(result).isEqualTo(load(expected))
    } else {
      val result = json(current) as MutableMap<String, Any?>
      CompactStrategy.applyDelta(json(delta), result)
      assertThat(result).isEqualTo(json(expected))
    }
  }

  private fun applyDelta(delta: List<Any?>, current: MutableList<Any?>) {
    CompactStrategy.applyDelta(delta, current)
  }

  private fun load(file: String): Map<String, Any?> = json(this::class.java.getResource(file).readText())

}
