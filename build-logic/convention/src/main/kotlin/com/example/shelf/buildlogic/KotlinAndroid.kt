package com.example.shelf.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configuration shared by application and library modules. AGP 9 compiles Kotlin itself
 * ("built-in Kotlin"), so the Kotlin Android plugin is not applied; the `kotlin` extension it
 * registers is configured here instead.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = ShelfBuildConfig.COMPILE_SDK
        compileSdkMinor = ShelfBuildConfig.COMPILE_SDK_MINOR

        defaultConfig.apply {
            minSdk = ShelfBuildConfig.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions.apply {
            sourceCompatibility = ShelfBuildConfig.javaVersion
            targetCompatibility = ShelfBuildConfig.javaVersion
        }

        buildTypes.getByName("debug").apply {
            // Produces createDebugUnitTestCoverageReport (JaCoCo) for every Android module.
            enableUnitTestCoverage = true
        }

        testOptions.apply {
            animationsDisabled = true
            unitTests.isIncludeAndroidResources = true
        }

        lint.apply {
            abortOnError = true
            checkReleaseBuilds = true
            warningsAsErrors = false
            // Version-freshness checks depend on the day the build runs, which makes CI flaky.
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        }

        packaging.apply {
            resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*.md")
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
    configureTestTasks()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = ShelfBuildConfig.javaVersion
        targetCompatibility = ShelfBuildConfig.javaVersion
    }
    configureKotlin<KotlinJvmProjectExtension>()
    configureTestTasks()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Warnings fail the build by default so the code base stays warning-free.
    // Override locally with -PwarningsAsErrors=false.
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(true)
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> error("Unsupported Kotlin extension ${T::class}")
    }.apply {
        jvmTarget = ShelfBuildConfig.jvmTarget
        allWarningsAsErrors = warningsAsErrors
    }
}

private fun Project.configureTestTasks() {
    tasks.withType<Test>().configureEach {
        // Robolectric sandboxes are memory hungry; modules already run in parallel.
        maxHeapSize = "2g"
        // KSP/Hilt generate classes into the unit-test output even for modules without tests.
        failOnNoDiscoveredTests = false
    }
    pluginManager.withPlugin("jacoco") {
        tasks.withType<Test>().configureEach {
            extensions.configure<JacocoTaskExtension> {
                // Needed for JaCoCo to see classes loaded by Robolectric's sandbox class loader.
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}
