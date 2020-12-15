package com.github.playback.bundle.jvm

import com.github.playback.core.Playback
import com.github.playback.ext.gson.GsonJsonMapper
import com.github.playback.ext.javalin.JavalinWebServer
import com.github.playback.ext.mysql.MysqlDocumentRepository
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.javalin.Javalin

object PlaybackApp {

  @JvmStatic
  fun main(args: Array<String>) {

    val config = Config {
      addSpec(HttpSpec)
      addSpec(DatabaseSpec)
      addSpec(StrategySpec)
    }.from.yaml.file("playback.yml", true)
      .from.env()
      .from.systemProperties()

    val javalin = Javalin.create()
    val webServer = JavalinWebServer(javalin)
    val jsonMapper = GsonJsonMapper()
    val documentRepository = MysqlDocumentRepository(
      jsonMapper,
      config[DatabaseSpec.url],
      config[DatabaseSpec.user],
      config[DatabaseSpec.pass]
    )

    Playback.initialize(webServer, jsonMapper, documentRepository, config[StrategySpec.patchInterval])
    javalin.start(config[HttpSpec.port])
  }

}
