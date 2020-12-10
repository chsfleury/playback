package com.github.playback.api

data class WebError(
    val statusCode: Int,
    val message: String?,
    val details: String? = null
)
