package com.github.playback.api

interface JsonMapper {

  operator fun invoke(json: String): Map<String, Any?>
  operator fun invoke(data: Any): String

}
