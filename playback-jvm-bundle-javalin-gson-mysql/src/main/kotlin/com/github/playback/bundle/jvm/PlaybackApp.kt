package com.github.playback.bundle.jvm

import com.github.playback.bundle.jvm.BundleConfig.databasePass
import com.github.playback.bundle.jvm.BundleConfig.databaseUrl
import com.github.playback.bundle.jvm.BundleConfig.databaseUser
import com.github.playback.bundle.jvm.BundleConfig.patchInterval
import com.github.playback.bundle.jvm.BundleConfig.port
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

    val config = Config { addSpec(BundleConfig) }
      .from.yaml.file("playback.yml")
      .from.env()
      .from.systemProperties()

    val javalin = Javalin.create()
    val webServer = JavalinWebServer(javalin)
    val jsonMapper = GsonJsonMapper()
    val documentRepository = MysqlDocumentRepository(
      jsonMapper,
      config[databaseUrl],
      config[databaseUser],
      config[databasePass]
    )

    Playback.initialize(webServer, jsonMapper, documentRepository, config[patchInterval])
    javalin.start(config[port])
  }

}
