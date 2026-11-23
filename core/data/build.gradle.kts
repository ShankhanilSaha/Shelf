plugins {
    alias(libs.plugins.shelf.android.library)
    alias(libs.plugins.shelf.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.shelf.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
}
