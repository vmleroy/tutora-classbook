package com.vmleroy.tutora.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypographyTest {
    @Test
    fun `AC-001-11 text uses Nunito and money uses tabular figures`() {
        for (style in listOf(TutoraTypography.body, TutoraTypography.titleXl, TutoraTypography.moneyXl)) {
            assertEquals(Nunito, style.fontFamily)
        }
        assertEquals("tnum", TutoraTypography.moneyXl.fontFeatureSettings)
        assertEquals("tnum", TutoraTypography.moneyDisplay.fontFeatureSettings)
        assertNull(TutoraTypography.body.fontFeatureSettings)
    }
}
