plugins {
    alias(libs.plugins.shelf.jvm.library)
    alias(libs.plugins.shelf.hilt)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
