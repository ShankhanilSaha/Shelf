plugins {
    alias(libs.plugins.shelf.android.library)
}

android {
    namespace = "com.example.shelf.core.testing"
}

dependencies {
    api(projects.core.common)
    api(projects.core.data)
    api(projects.core.database)
    api(projects.core.model)
    api(projects.core.network)

    api(libs.androidx.paging.common)
    api(libs.androidx.paging.testing)
    api(libs.androidx.room.runtime)
    api(libs.junit4)
    api(libs.kotlin.test)
    api(libs.kotlin.test.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
