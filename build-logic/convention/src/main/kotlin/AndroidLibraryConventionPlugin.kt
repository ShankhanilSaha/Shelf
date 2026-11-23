import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.example.shelf.buildlogic.ShelfBuildConfig
import com.example.shelf.buildlogic.configureKotlinAndroid
import com.example.shelf.buildlogic.disableReleaseUnitTests
import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = ShelfBuildConfig.TARGET_SDK
                lint.targetSdk = ShelfBuildConfig.TARGET_SDK
                // Resources of ":feature:search" must start with "search_", of ":core:ui" with "ui_".
                resourcePrefix = path.substringAfterLast(':').lowercase() + "_"
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableReleaseUnitTests()
            }

            dependencies {
                "testImplementation"(libs.library("kotlin-test"))
                "testImplementation"(libs.library("kotlin-test-junit"))
                "testImplementation"(libs.library("junit4"))
            }
        }
    }
}
