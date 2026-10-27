import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.vmleroy.tutora.designsystem"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    testOptions.unitTests.isIncludeAndroidResources = true
    lint { warningsAsErrors = true }
}

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
}

/**
 * Generates `TutoraTokens.kt` from `design/tokens.json`, the only source of colors, type,
 * spacing, radius and sizes. It is generated on every build and never committed, so it
 * cannot drift from the JSON (AC-002-1).
 */
abstract class GenerateTutoraTokens : DefaultTask() {
    @get:InputFile
    abstract val tokens: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private fun camel(name: String) =
        name
            .split('-', '.')
            .filter { it.isNotEmpty() }
            .mapIndexed { i, p ->
                if (i == 0) p else p.replaceFirstChar(Char::uppercase)
            }.joinToString("")

    private fun px(v: Any?) = v.toString().removeSuffix("px")

    private fun color(hex: String): String {
        val h = hex.removePrefix("#").uppercase()
        val argb = if (h.length == 8) h.substring(6) + h.substring(0, 6) else "FF$h"
        return "Color(0x$argb)"
    }

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST") // JsonSlurper returns untyped maps and lists
        val json = JsonSlurper().parse(tokens.get().asFile) as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val colorSection = json["color"] as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val colorTokens = colorSection["tokens"] as List<Map<String, Any?>>

        @Suppress("UNCHECKED_CAST")
        val sets = (colorSection["themes"] as List<Map<String, Any?>>).map { it["id"].toString() }

        fun list(section: String) =
            @Suppress("UNCHECKED_CAST")
            ((json[section] as Map<String, Any?>)["tokens"] as List<Map<String, Any?>>)

        @Suppress("UNCHECKED_CAST")
        val typeStyles =
            ((json["type"] as Map<String, Any?>)["groups"] as List<Map<String, Any?>>)
                .flatMap { it["styles"] as List<Map<String, Any?>> }

        val out = StringBuilder()
        out.appendLine("// GENERATED from design/tokens.json by :core:designsystem:generateTutoraTokens. Do not edit.")
        out.appendLine("package com.vmleroy.tutora.designsystem.tokens\n")
        out.appendLine("import androidx.compose.ui.graphics.Color")
        out.appendLine("import androidx.compose.ui.unit.dp")
        out.appendLine("import androidx.compose.ui.unit.sp\n")
        out.appendLine("/** One theme × mode: every semantic color token. */")
        out.appendLine("data class TutoraColors(")
        colorTokens.forEach { out.appendLine("    val ${camel(it["name"].toString())}: Color,") }
        out.appendLine(")\n")
        out.appendLine("data class TypeToken(val size: Int, val lineHeight: Int, val weight: Int, val letterSpacing: Float)\n")
        out.appendLine("object TutoraTokens {")
        out.appendLine("    /** The eight token sets, keyed `theme-mode` (\"quadro-light\"). */")
        out.appendLine("    val colorSets: Map<String, TutoraColors> = mapOf(")
        sets.forEach { set ->
            out.appendLine("        \"$set\" to TutoraColors(")
            colorTokens.forEach { t ->
                @Suppress("UNCHECKED_CAST")
                val value = (t["value"] as Map<String, Any?>)[set].toString()
                out.appendLine("            ${camel(t["name"].toString())} = ${color(value)},")
            }
            out.appendLine("        ),")
        }
        out.appendLine("    )\n")
        out.appendLine("    object Spacing {")
        list("spacing").forEach {
            out.appendLine("        val ${camel(it["name"].toString().replace("space-", "s").replace('.', '_'))} = ${px(it["value"])}.dp")
        }
        out.appendLine("    }\n")
        out.appendLine("    object Radius {")
        list("radius").forEach {
            out.appendLine(
                "        val ${camel(
                    it["name"].toString().removePrefix("radius-").let { n ->
                        if (n[0].isDigit()) "x$n" else n
                    },
                )} = ${px(it["value"])}.dp",
            )
        }
        out.appendLine("    }\n")
        out.appendLine("    object Size {")
        list("size").forEach { out.appendLine("        val ${camel(it["name"].toString().removePrefix("size-"))} = ${px(it["value"])}.dp") }
        out.appendLine("    }\n")
        out.appendLine("    object Type {")
        typeStyles.forEach { s ->
            val spacing = s["letterSpacing"]?.let { px(it) } ?: "0"
            out.appendLine(
                "        val ${camel(
                    s["name"].toString(),
                )} = TypeToken(${px(s["fontSize"])}, ${px(s["lineHeight"])}, ${s["fontWeight"]}, ${spacing}f)",
            )
        }
        out.appendLine("    }")
        out.appendLine("}")
        val file = outputDir.get().file("com/vmleroy/tutora/designsystem/tokens/TutoraTokens.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(out.toString())
    }
}

val generateTutoraTokens =
    tasks.register<GenerateTutoraTokens>("generateTutoraTokens") {
        tokens.set(rootProject.layout.projectDirectory.file("design/tokens.json"))
        outputDir.set(layout.buildDirectory.dir("generated/tokens"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(generateTutoraTokens, GenerateTutoraTokens::outputDir)
    }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    // The Compose test rule brings Espresso 3.5, which breaks on Android 37.
    testImplementation(libs.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
