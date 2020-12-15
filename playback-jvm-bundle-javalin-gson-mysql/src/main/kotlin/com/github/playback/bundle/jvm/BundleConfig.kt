package com.github.playback.bundle.jvm

import com.uchuhimo.konf.ConfigSpec

object StrategySpec: ConfigSpec("strategy") {
  val patchInterval by optional(20)
}

object DatabaseSpec: ConfigSpec("database") {
  val url by optional("jdbc:mysql://localhost:3306/playback?serverTimezone=Europe/Berlin")
  val user by optional("playback")
  val pass by optional("playback")
}

object HttpSpec: ConfigSpec("http") {
  val port by optional(9753)
}
