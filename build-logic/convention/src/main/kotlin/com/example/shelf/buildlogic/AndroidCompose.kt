package com.example.shelf.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Compose for an Android module and adds the BOM plus tooling dependencies.
 * The Compose compiler Gradle plugin must already be applied.
 */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.compose = true
    }

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        "implementation"(platform(bom))
        "testImplementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
        "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
    }
}
