package com.github.playback.ext.javalin

import com.github.playback.api.WebRequest
import io.javalin.http.Context

class JavalinWebRequest(private val ctx: Context) : WebRequest {

  override fun pathParams(): Map<String, String> = ctx.pathParamMap()
  override fun queryParams(): Map<String, List<String>> = ctx.queryParamMap()
  override fun body(): String? = ctx.body().takeIf { it.isNotEmpty() }

}
