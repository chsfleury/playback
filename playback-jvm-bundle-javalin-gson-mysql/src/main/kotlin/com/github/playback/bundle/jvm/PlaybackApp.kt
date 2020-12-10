package com.github.playback.bundle.jvm

import com.github.playback.core.Playback
import com.github.playback.ext.gson.GsonJsonMapper
import com.github.playback.ext.javalin.JavalinWebServer
import com.github.playback.ext.mysql.MysqlDocumentRepository
import io.javalin.Javalin

object PlaybackApp {

  @JvmStatic
  fun main(args: Array<String>) {
    val javalin = Javalin.create()
    val webServer = JavalinWebServer(javalin)
    val jsonMapper = GsonJsonMapper()
    val documentRepository = MysqlDocumentRepository(
        jsonMapper,
        "jdbc:mysql://localhost:3306/playback?serverTimezone=Europe/Berlin",
        "playback",
        "playback"
    )
    Playback.initialize(webServer, jsonMapper, documentRepository, 5)
    javalin.start(9753)
  }

}
