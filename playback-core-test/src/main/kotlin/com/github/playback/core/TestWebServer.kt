package com.github.playback.core

import com.github.playback.api.WebRequest
import com.github.playback.api.WebResponse
import com.github.playback.api.WebServer

class TestWebServer: WebServer {

  val endpoints = mutableMapOf<Pair<String, String>, (WebRequest, WebResponse) -> Unit>()

  fun get(path: String, request: WebRequest, response: WebResponse) = endpoints["get" to path]?.invoke(request, response)

  override fun get(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    endpoints["get" to path] = handler
  }

  fun post(path: String, request: WebRequest, response: WebResponse) = endpoints["post" to path]?.invoke(request, response)

  override fun post(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    endpoints["post" to path] = handler
  }

  fun put(path: String, request: WebRequest, response: WebResponse) = endpoints["put" to path]?.invoke(request, response)

  override fun put(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    endpoints["put" to path] = handler
  }

  fun patch(path: String, request: WebRequest, response: WebResponse) = endpoints["patch" to path]?.invoke(request, response)

  override fun patch(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    endpoints["patch" to path] = handler
  }

  fun delete(path: String, request: WebRequest, response: WebResponse) = endpoints["delete" to path]?.invoke(request, response)

  override fun delete(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    endpoints["delete" to path] = handler
  }

}
