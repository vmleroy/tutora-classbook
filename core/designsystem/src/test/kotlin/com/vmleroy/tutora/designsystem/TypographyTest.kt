package com.vmleroy.tutora.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypographyTest {
    @Test
    fun `AC-001-11 text uses Nunito and money uses tabular figures`() {
        for (style in listOf(TutoraTypography(1f).body, TutoraTypography(1f).titleXl, TutoraTypography(1f).moneyXl)) {
            assertEquals(Nunito, style.fontFamily)
        }
        assertEquals("tnum", TutoraTypography(1f).moneyXl.fontFeatureSettings)
        assertEquals("tnum", TutoraTypography(1f).moneyDisplay.fontFeatureSettings)
        assertNull(TutoraTypography(1f).body.fontFeatureSettings)
    }
}
