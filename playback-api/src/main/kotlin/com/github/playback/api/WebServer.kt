package com.github.playback.api

interface WebServer {

  fun get(path: String, handler: (WebRequest, WebResponse) -> Unit)
  fun post(path: String, handler: (WebRequest, WebResponse) -> Unit)
  fun put(path: String, handler: (WebRequest, WebResponse) -> Unit)
  fun patch(path: String, handler: (WebRequest, WebResponse) -> Unit)
  fun delete(path: String, handler: (WebRequest, WebResponse) -> Unit)

}
