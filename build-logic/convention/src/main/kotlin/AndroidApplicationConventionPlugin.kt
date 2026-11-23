import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
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

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = ShelfBuildConfig.TARGET_SDK
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
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
