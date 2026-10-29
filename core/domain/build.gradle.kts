import groovy.json.JsonSlurper

// Plain Kotlin only (constitution rule 7): no Android, Compose or Room, so this module can
// become the shared commonMain of a Kotlin Multiplatform build when iOS comes.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
}

/**
 * Generates `ColorSets.kt` (the eight token sets as hex strings) from `design/tokens.json`
 * on every build, so the palette rules read the same values as the design system (AC-002-1).
 */
abstract class GenerateColorSets : DefaultTask() {
    @get:InputFile
    abstract val tokens: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST") // JsonSlurper returns untyped maps and lists
        val color = (JsonSlurper().parse(tokens.get().asFile) as Map<String, Any?>)["color"] as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val tokens = color["tokens"] as List<Map<String, Any?>>

        @Suppress("UNCHECKED_CAST")
        val sets = (color["themes"] as List<Map<String, Any?>>).map { it["id"].toString() }
        val out = StringBuilder()
        out.appendLine("// GENERATED from design/tokens.json by :core:domain:generateColorSets. Do not edit.")
        out.appendLine("@file:Suppress(\"ktlint\")\n")
        out.appendLine("package app.tutora.domain.theme\n")
        out.appendLine("/** Every color token name, in the order of tokens.json. */")
        out.appendLine("val colorTokens: List<String> = listOf(${tokens.joinToString { "\"${it["name"]}\"" }})\n")
        out.appendLine("/** The eight token sets, keyed `theme-scheme`, each token as `#rrggbb` or `#rrggbbaa`. */")
        out.appendLine("val colorSets: Map<String, Map<String, String>> = mapOf(")
        sets.forEach { set ->
            out.appendLine("    \"$set\" to mapOf(")
            tokens.forEach { t ->
                @Suppress("UNCHECKED_CAST")
                val v = (t["value"] as Map<String, Any?>)[set].toString().lowercase()
                out.appendLine("        \"${t["name"]}\" to \"$v\",")
            }
            out.appendLine("    ),")
        }
        out.appendLine(")")
        val file = outputDir.get().file("app/tutora/domain/theme/ColorSets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(out.toString())
    }
}

val generateColorSets =
    tasks.register<GenerateColorSets>("generateColorSets") {
        tokens.set(rootProject.layout.projectDirectory.file("design/tokens.json"))
        outputDir.set(layout.buildDirectory.dir("generated/colorSets"))
    }

sourceSets["main"].kotlin.srcDir(generateColorSets.map { it.outputDir })

dependencies {
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.property)
}
