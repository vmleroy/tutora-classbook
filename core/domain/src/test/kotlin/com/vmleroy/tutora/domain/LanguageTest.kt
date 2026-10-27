package com.vmleroy.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {
    @Test
    fun `AC-001-5 Portuguese in any region opens in pt-BR, anything else in English`() {
        for (tag in listOf("pt-BR", "pt-PT", "pt-AO", "pt")) assertEquals(Language.PT_BR, firstRunLanguage(listOf(tag)))
        for (tag in listOf("en-US", "es-AR", "fr-FR")) assertEquals(Language.EN, firstRunLanguage(listOf(tag)))
        assertEquals(Language.EN, firstRunLanguage(emptyList()))
        assertEquals(Language.EN, firstRunLanguage(listOf("en-GB", "pt-BR")))
    }

    @Test
    fun `AC-001-6 a saved language wins over the phone language`() {
        assertEquals(Language.EN, startupLanguage(Language.EN, listOf("pt-BR")))
        assertEquals(Language.PT_BR, startupLanguage(Language.PT_BR, listOf("en-US")))
        assertEquals(Language.PT_BR, startupLanguage(null, listOf("pt-BR")))
    }
}
