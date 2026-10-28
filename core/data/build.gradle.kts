plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "app.tutora.data"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    testOptions.unitTests.isIncludeAndroidResources = true
    lint {
        warningsAsErrors = true
        // A release elsewhere must not fail the build: upgrades are a deliberate step (spec 001).
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
}

room {
    // Every version's schema is committed, so each migration can be tested from it (rule 12).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:domain"))
    api(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room.testing)
    // The bundled driver's native library is for Android; tests use the framework SQLite
    // that Robolectric provides.
    testImplementation(libs.sqlite.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}
