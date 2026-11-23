plugins {
    alias(libs.plugins.shelf.android.application.compose)
    alias(libs.plugins.shelf.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase is optional. With app/google-services.json (git-ignored, see
// google-services.json.example) the Google Services and Crashlytics plugins are applied and the
// app can sign in and sync; without it a fresh clone still builds and runs with sync disabled.
val googleServicesJson: File = file("google-services.json")
if (googleServicesJson.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

/** The OAuth web client (client_type 3) of [packageName] in google-services.json, or "". */
fun googleWebClientId(json: File, packageName: String): String {
    if (!json.exists()) return ""
    val root = groovy.json.JsonSlurper().parse(json) as Map<*, *>
    val client = (root["client"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().firstOrNull { client ->
        val info = (client["client_info"] as? Map<*, *>)?.get("android_client_info") as? Map<*, *>
        info?.get("package_name") == packageName
    } ?: return ""
    val oauthClients = (client["oauth_client"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
    return oauthClients.firstOrNull { it["client_type"] == WEB_CLIENT_TYPE }?.get("client_id") as? String ?: ""
}

val WEB_CLIENT_TYPE = 3

// Release signing comes from the environment (CI secrets, see .github/workflows/release.yml).
// Without it, release builds are signed with the debug key so that the minified build can be
// installed and tested locally; such an APK must never be published.
val releaseKeystore: String? = providers.environmentVariable("SHELF_KEYSTORE_FILE").orNull

android {
    namespace = "com.example.shelf"

    defaultConfig {
        applicationId = "com.example.shelf"
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${googleWebClientId(googleServicesJson, "com.example.shelf")}\"",
        )
        // Crash reports only from release builds (and only when Firebase is configured).
        manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("SHELF_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SHELF_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SHELF_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
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
    implementation(projects.core.network)
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
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.navigationSuite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
