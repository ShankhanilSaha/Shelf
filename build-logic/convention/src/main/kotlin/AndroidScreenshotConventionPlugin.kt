import com.android.build.api.dsl.LibraryExtension
import com.example.shelf.buildlogic.library
import com.example.shelf.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose Preview Screenshot Testing: `@PreviewTest` previews in `src/screenshotTest` are rendered
 * with layoutlib and compared with the reference images in `src/screenshotTestDebug/reference`.
 * `./gradlew updateDebugScreenshotTest` records them, `validateDebugScreenshotTest` checks them.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.compose.screenshot")

            extensions.configure<LibraryExtension> {
                experimentalProperties["android.experimental.enableScreenshotTest"] = true
            }

            dependencies {
                "screenshotTestImplementation"(libs.library("screenshot-validation-api"))
                "screenshotTestImplementation"(libs.library("androidx-compose-ui-tooling"))
            }
        }
    }
}
