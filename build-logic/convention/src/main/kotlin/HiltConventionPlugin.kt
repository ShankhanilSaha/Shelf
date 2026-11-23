import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** Hilt with KSP, for both pure-Kotlin (hilt-core) and Android modules. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "ksp"(libs.library("hilt-compiler"))
                // Hilt bundles an older kotlin-metadata reader; this one understands Kotlin 2.4 metadata.
                "ksp"(libs.library("kotlin-metadata-jvm"))
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.library("hilt-core"))
                }
            }

            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "com.google.dagger.hilt.android")
                dependencies {
                    "implementation"(libs.library("hilt-android"))
                }
            }
        }
    }
}
