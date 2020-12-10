package com.github.playback.api

interface WebResponse {

  fun send(status: Int, body: String? = null)

  fun send(error: WebError, json: JsonMapper) {
    send(error.statusCode, json(error))
  }

}
