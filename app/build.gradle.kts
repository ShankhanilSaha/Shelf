plugins {
    alias(libs.plugins.shelf.android.application.compose)
    alias(libs.plugins.shelf.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.shelf"

    defaultConfig {
        applicationId = "com.example.shelf"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(projects.feature.auth)
    implementation(projects.feature.bookdetail)
    implementation(projects.feature.library)
    implementation(projects.feature.scanner)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)
    implementation(projects.feature.stats)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
