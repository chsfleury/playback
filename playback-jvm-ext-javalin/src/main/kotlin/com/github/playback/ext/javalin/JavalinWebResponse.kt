package com.github.playback.ext.javalin

import com.github.playback.api.WebResponse
import io.javalin.http.Context

class JavalinWebResponse(private val ctx: Context) : WebResponse {

  override fun send(status: Int, body: String?) {
    ctx.status(status)
    if (body != null) {
      ctx.result(body)
    }
  }

}
