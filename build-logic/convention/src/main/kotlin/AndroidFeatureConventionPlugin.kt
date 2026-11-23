import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: a Compose library with Hilt, a serializable Navigation 3 key, and access to
 * the core layers. Features never depend on other features; `:app` wires them together.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "shelf.android.library.compose")
            apply(plugin = "shelf.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":core:data"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:ui"))

                "implementation"(libs.library("androidx-activity-compose"))
                "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-navigation3-runtime"))

                "testImplementation"(project(":core:testing"))
                "testImplementation"(libs.library("androidx-compose-ui-test-junit4"))
                "testImplementation"(libs.library("robolectric"))
                "debugImplementation"(libs.library("androidx-compose-ui-test-manifest"))

                // Instrumented accessibility checks (ATF), which need a device on API 34+.
                "androidTestImplementation"(libs.library("androidx-compose-ui-test-junit4"))
                "androidTestImplementation"(libs.library("androidx-compose-ui-test-accessibility"))
                "androidTestImplementation"(libs.library("androidx-test-ext-junit"))
                "androidTestImplementation"(libs.library("androidx-test-runner"))
            }
        }
    }
}
