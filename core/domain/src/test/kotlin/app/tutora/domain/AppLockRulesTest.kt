package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockRulesTest {
    private val minute = 60_000L

    @Test
    fun `AC-015-4 Bloquear apos offers Imediatamente, 1 and 5 minutes, 5 at first`() {
        assertEquals(listOf(0, 1, 5), LockAfter.entries.map { it.minutes })
        assertEquals(LockAfter.FIVE_MINUTES, Settings(Language.PT_BR).lockAfter)
        assertFalse(Settings(Language.PT_BR).appLock)
    }

    @Test
    fun `AC-015-8 it locks again only once away longer than Bloquear apos, and never with the lock off`() {
        val left = 1_000_000L
        assertTrue(shouldLock(true, LockAfter.IMMEDIATELY, left, left + 1))
        assertFalse(shouldLock(true, LockAfter.ONE_MINUTE, left, left + minute - 1))
        assertTrue(shouldLock(true, LockAfter.ONE_MINUTE, left, left + minute))
        assertFalse(shouldLock(true, LockAfter.FIVE_MINUTES, left, left + 4 * minute))
        assertTrue(shouldLock(true, LockAfter.FIVE_MINUTES, left, left + 5 * minute))
        assertFalse(shouldLock(false, LockAfter.IMMEDIATELY, left, left + 10 * minute))
        assertFalse(shouldLock(true, LockAfter.IMMEDIATELY, null, left))
    }

    @Test
    fun `AC-015-16 every kind of Pix key is masked as banks show it, typed with or without punctuation`() {
        // CPF (529.982.247-25 is a valid test CPF)
        assertEquals("•••.982.247-••", maskPix("529.982.247-25"))
        assertEquals("•••.982.247-••", maskPix("52998224725"))
        // CNPJ
        assertEquals("••.345.678/0001-••", maskPix("12.345.678/0001-95"))
        assertEquals("••.345.678/0001-••", maskPix("12345678000195"))
        // Phone, with +55 or as typed; 11 digits that fail the CPF check are a phone
        assertEquals("(••) •••••-4321", maskPix("+55 11 98765-4321"))
        assertEquals("(••) •••••-4321", maskPix("+5511987654321"))
        assertEquals("(••) •••••-4321", maskPix("(11) 98765-4321"))
        assertEquals(PixKind.PHONE, pixKind("11987654321"))
        // E-mail
        assertEquals("vi•••@gmail.com", maskPix("victor@gmail.com"))
        assertEquals("a•••@b.com", maskPix("ab@b.com"))
        // Random key, with or without hyphens
        assertEquals("123e••••4000", maskPix("123e4567-e89b-12d3-a456-426614174000"))
        assertEquals(PixKind.RANDOM, pixKind("123e4567e89b12d3a456426614174000"))
        // Anything else
        assertEquals("•••• abcd", maskPix("chave-abcd"))
        assertEquals("••••", maskPix("1234"))
        assertEquals("", maskPix("  "))
    }
}
