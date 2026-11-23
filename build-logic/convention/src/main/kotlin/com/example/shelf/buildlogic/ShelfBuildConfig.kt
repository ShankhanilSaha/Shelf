package com.example.shelf.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Single place for SDK levels and JVM targets used by every module.
 *
 * compileSdk 37.2 is the newest stable platform. Compose 1.13 (BOM 2026.11.01) declares
 * `minCompileSdk=37` and `minCompileMinorSdk=1` in its AAR metadata, so 37.1 is the minimum.
 */
internal object ShelfBuildConfig {
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 2
    const val TARGET_SDK = 37
    const val MIN_SDK = 26

    val javaVersion: JavaVersion = JavaVersion.VERSION_17
    val jvmTarget: JvmTarget = JvmTarget.JVM_17
}
