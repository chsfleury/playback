package com.github.playback.ext.gson

import com.github.playback.api.JsonMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class GsonJsonMapper(private val gson: Gson = GsonBuilder().serializeNulls().create()) : JsonMapper {

  companion object {
    private val type: Type = object : TypeToken<Map<String, Any?>>() {}.type
  }

  override fun invoke(json: String): Map<String, Any?> = gson.fromJson(json, type)
  override fun invoke(data: Any): String = gson.toJson(data)

}
