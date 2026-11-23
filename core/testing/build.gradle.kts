plugins {
    alias(libs.plugins.shelf.android.library)
}

android {
    namespace = "com.example.shelf.core.testing"
}

dependencies {
    api(projects.core.common)
    api(projects.core.data)
    api(projects.core.model)

    api(libs.junit4)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
