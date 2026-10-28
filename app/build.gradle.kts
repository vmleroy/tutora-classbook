plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from the environment, never from the repo (constitution rule 19).
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "app.tutora"
    compileSdk = 37

    defaultConfig {
        // A new id, so the Kotlin app can sit next to the archived Expo app on the phone.
        applicationId = "app.tutora.app"
        minSdk = 26
        targetSdk = 37
        // Must grow on every release build installed over the previous one.
        versionCode = env("TUTORA_VERSION_CODE")?.toInt() ?: 1
        // Follows the last spec shipped: 1.5.0 = specs 001 to 005.
        versionName = "1.6.0"
    }

    signingConfigs {
        create("release") {
            env("TUTORA_KEYSTORE_PATH")?.let { storeFile = file(it) }
            storePassword = env("TUTORA_KEYSTORE_PASSWORD")
            keyAlias = env("TUTORA_KEY_ALIAS")
            keyPassword = env("TUTORA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (env("TUTORA_KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // The app switches language itself (Ajustes), so every language stays in the base APK.
    bundle { language { enableSplit = false } }

    androidResources {
        // Lets the per-app language list include pt-BR and en (Android 13+ settings page).
        generateLocaleConfig = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    lint {
        warningsAsErrors = true
        error += listOf("MissingTranslation", "ExtraTranslation")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.espresso.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.framework)
    testImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
