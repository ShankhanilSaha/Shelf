plugins {
    alias(libs.plugins.shelf.android.library)
    alias(libs.plugins.shelf.hilt)
}

android {
    namespace = "com.example.shelf.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
