package com.github.playback.ext.javalin

import com.github.playback.api.WebRequest
import com.github.playback.api.WebResponse
import com.github.playback.api.WebServer
import io.javalin.Javalin
import io.javalin.http.Handler
import kotlin.reflect.KClass

class JavalinWebServer(private val instance: Javalin) : WebServer {

  override fun get(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    instance.get(adapt(path), adapt(handler))
  }

  override fun post(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    instance.post(adapt(path), adapt(handler))
  }

  override fun put(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    instance.put(adapt(path), adapt(handler))
  }

  override fun patch(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    instance.patch(adapt(path), adapt(handler))
  }

  override fun delete(path: String, handler: (WebRequest, WebResponse) -> Unit) {
    instance.delete(adapt(path), adapt(handler))
  }

  private fun adapt(path: String): String = path
      .replace('{', ':')
      .replace("}", "")

  private fun adapt(handler: (WebRequest, WebResponse) -> Unit) = Handler { ctx ->
    handler(JavalinWebRequest(ctx), JavalinWebResponse(ctx))
  }

}
