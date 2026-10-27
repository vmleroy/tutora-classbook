// Plain Kotlin only (constitution rule 7): no Android, Compose or Room, so this module can
// become the shared commonMain of a Kotlin Multiplatform build when iOS comes.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
}

dependencies {
    api(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.property)
}
