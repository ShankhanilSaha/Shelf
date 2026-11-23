plugins {
    alias(libs.plugins.shelf.android.library.compose)
}

android {
    namespace = "com.example.shelf.core.ui"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.compose.material3)
}
