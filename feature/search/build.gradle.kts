plugins {
    alias(libs.plugins.shelf.android.feature)
}

android {
    namespace = "com.example.shelf.feature.search"
}

dependencies {
    implementation(libs.androidx.paging.compose)
}
