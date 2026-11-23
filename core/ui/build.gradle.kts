plugins {
    alias(libs.plugins.shelf.android.library.compose)
}

android {
    namespace = "com.example.shelf.core.ui"
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)

    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.coil.compose)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
