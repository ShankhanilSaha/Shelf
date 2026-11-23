plugins {
    alias(libs.plugins.shelf.android.library)
    alias(libs.plugins.shelf.android.room)
    alias(libs.plugins.shelf.hilt)
}

android {
    namespace = "com.example.shelf.core.database"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
