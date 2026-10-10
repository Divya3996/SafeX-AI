plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sentinel.ai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sentinel.ai"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://api.sentinel.ai/\"")
        buildConfigField("String", "OPENPHISH_FEED_URL", "\"https://openphish.com/feed.txt\"")
        buildConfigField("String", "OPENPHISH_API_KEY", "\"\"")
        buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"\"")
        buildConfigField("String", "VIRUSTOTAL_LOOKUP_URL", "\"https://www.virustotal.com/api/v3/\"")
        buildConfigField("String", "REPUTATION_LOOKUP_TIMEOUT_MS", "\"10000\"")
    }

    val releaseKeys = listOf("SAFEX_KEYSTORE_PATH", "SAFEX_KEYSTORE_PASSWORD", "SAFEX_KEY_ALIAS", "SAFEX_KEY_PASSWORD")
        .map { providers.environmentVariable(it).orNull }
    require(releaseKeys.none { !it.isNullOrBlank() } || releaseKeys.all { !it.isNullOrBlank() }) {
        "Provide all four SAFEX release-signing environment variables, or leave all unset for an unsigned bundle."
    }
    if (releaseKeys.all { !it.isNullOrBlank() }) {
        signingConfigs.create("publisher") {
            storeFile = file(releaseKeys[0]!!)
            storePassword = releaseKeys[1]
            keyAlias = releaseKeys[2]
            keyPassword = releaseKeys[3]
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            if (releaseKeys.all { !it.isNullOrBlank() }) signingConfig = signingConfigs.getByName("publisher")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// LiteRT declares its API dependency as a Maven version range. Keep native and Java
// artifacts on the same reviewed version without repository metadata discovery.
configurations.configureEach {
    resolutionStrategy.force("com.google.ai.edge.litert:litert-api:1.4.0")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":agents"))
    implementation(project(":services"))
    implementation(project(":ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.timber)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.okhttp)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation(libs.gson)

    androidTestImplementation(libs.gson)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.room.ktx)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation("com.google.ai.edge.litert:litert:1.4.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
