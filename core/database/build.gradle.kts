plugins {
    alias(libs.plugins.shelf.android.library)
    alias(libs.plugins.shelf.hilt)
}

android {
    namespace = "com.example.shelf.core.database"
}

dependencies {
    implementation(projects.core.common)
}
