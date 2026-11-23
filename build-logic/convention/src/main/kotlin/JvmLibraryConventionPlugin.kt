import com.example.shelf.buildlogic.configureKotlinJvm
import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.tasks.JacocoReport

/** Pure Kotlin/JVM module (no Android dependencies), still checked by Android Lint. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "com.android.lint")
            apply(plugin = "jacoco")

            configureKotlinJvm()

            dependencies {
                "testImplementation"(libs.library("kotlin-test"))
                "testImplementation"(libs.library("kotlin-test-junit"))
                "testImplementation"(libs.library("junit4"))
            }

            tasks.named<JacocoReport>("jacocoTestReport") {
                dependsOn(tasks.named("test"))
                reports {
                    xml.required = true
                    html.required = true
                }
            }
        }
    }
}
