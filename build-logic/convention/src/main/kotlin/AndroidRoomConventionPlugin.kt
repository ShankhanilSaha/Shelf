import androidx.room.gradle.RoomExtension
import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<KspExtension> {
                arg("room.generateKotlin", "true")
            }

            extensions.configure<RoomExtension> {
                // One JSON file per schema version; required for auto-migrations and migration tests.
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.library("androidx-room-runtime"))
                "implementation"(libs.library("androidx-room-ktx"))
                "ksp"(libs.library("androidx-room-compiler"))
                "testImplementation"(libs.library("androidx-room-testing"))
            }
        }
    }
}
