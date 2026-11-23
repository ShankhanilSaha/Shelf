plugins {
    alias(libs.plugins.shelf.android.feature)
}

android {
    namespace = "com.example.shelf.feature.auth"
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.turbine)
}
