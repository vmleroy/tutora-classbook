plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from the environment, never from the repo (constitution rule 19).
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

// How the reminder before class gets exact alarms (AC-014-27): "use" (USE_EXACT_ALARM, granted at
// install) or "schedule" (SCHEDULE_EXACT_ALARM, which the teacher turns on). Build with
// -Ptutora.exactAlarm=schedule if Play refuses USE_EXACT_ALARM.
val exactAlarm = (findProperty("tutora.exactAlarm") as String?) ?: "use"
require(exactAlarm == "use" || exactAlarm == "schedule") { "tutora.exactAlarm is use or schedule, not $exactAlarm" }

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
        versionName = "1.11.0"
        buildConfigField("Boolean", "USE_EXACT_ALARM", (exactAlarm == "use").toString())
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
        // A release elsewhere must not fail the build: upgrades are a deliberate step (spec 001).
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
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
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // Automatic backups at 20:00, even with the app closed (spec 011).
    implementation(libs.androidx.work.runtime)

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

// USE_EXACT_ALARM is only declared in "use" builds, so a "schedule" build never shows it to Play.
androidComponents {
    onVariants { variant ->
        if (exactAlarm == "use") {
            variant.sources.manifests.addStaticManifestFile("src/exactAlarm/AndroidManifest.xml")
        }
    }
}
