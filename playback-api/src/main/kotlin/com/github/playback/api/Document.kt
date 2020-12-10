package com.github.playback.api

data class Document(
    val id: String,
    val timestamp: Long,
    val patch: Boolean,
    val fields: Map<String, Any?>
)
