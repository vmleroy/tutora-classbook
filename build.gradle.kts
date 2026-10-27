plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    // Robolectric reaches into java.io file descriptors and jdk.internal.access, closed by
    // default on recent JDKs.
    tasks.withType<Test>().configureEach {
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        )
    }
}
