package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DomainPurityTest {
    @Test
    fun `AC-001-16 the domain depends on nothing from Android, Compose or the database`() {
        val forbidden = Regex("""^import\s+(android|androidx|com\.google\.android|java\.sql)\.""", RegexOption.MULTILINE)
        val offenders =
            File("src/main/kotlin")
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { forbidden.containsMatchIn(it.readText()) }
                .map { it.name }
                .toList()
        assertEquals(emptyList<String>(), offenders)
        // The build file itself may only bring plain Kotlin libraries.
        val build = File("build.gradle.kts").readText()
        assertEquals(
            false,
            Regex("""android|room|compose""", RegexOption.IGNORE_CASE).containsMatchIn(build.substringAfter("dependencies")),
        )
    }
}
