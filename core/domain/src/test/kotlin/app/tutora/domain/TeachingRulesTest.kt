package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachingRulesTest {
    @Test
    fun `AC-014-19 what is not chosen is hidden - Escola without school, Salinha and Cursinho without salinha`() {
        val onlyPrivate = setOf(Teaching.PRIVATE)
        assertEquals(listOf(PlaceKind.PRIVATE), ownKindsShown(onlyPrivate, emptySet()))
        assertFalse(shown(Teaching.SCHOOL, onlyPrivate, emptySet()))
        assertFalse(shown(Teaching.SALINHA, onlyPrivate, emptySet()))
        val school = setOf(Teaching.SCHOOL)
        assertEquals(listOf(PlaceKind.SCHOOL, PlaceKind.PRIVATE), ownKindsShown(school, emptySet()))
        assertFalse(shown(Teaching.PRIVATE, school, emptySet()))
        assertEquals(PlaceKind.own, ownKindsShown(Teaching.entries.toSet(), emptySet()))
    }

    @Test
    fun `AC-014-20 anything hidden shows again once the teacher has a place or class of that kind`() {
        val onlyPrivate = setOf(Teaching.PRIVATE)
        val existing = setOf(PlaceKind.COURSE.teaching())
        assertTrue(shown(Teaching.SALINHA, onlyPrivate, existing))
        assertEquals(listOf(PlaceKind.SALINHA, PlaceKind.COURSE, PlaceKind.PRIVATE), ownKindsShown(onlyPrivate, existing))
    }
}
