package com.github.playback.bundle.jvm

import com.uchuhimo.konf.ConfigSpec

object BundleConfig : ConfigSpec() {
  val port by optional(9753)
  val databaseUrl by optional("jdbc:mysql://localhost:3306/playback?serverTimezone=Europe/Berlin")
  val databaseUser by optional("playback")
  val databasePass by optional("playback")

  val patchInterval by optional(20)
}
