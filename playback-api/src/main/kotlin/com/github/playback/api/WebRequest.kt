package com.github.playback.api

interface WebRequest {

  fun pathParams(): Map<String, String>
  fun queryParams(): Map<String, List<String>>
  fun body(): String?

}
