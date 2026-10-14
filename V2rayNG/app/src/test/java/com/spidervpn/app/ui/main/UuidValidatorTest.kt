package com.spidervpn.app.ui.main

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UuidValidatorTest {

    @Test
    fun `accepts canonical lowercase uuid`() {
        assertTrue(UuidValidator.isValid("af9ca340-1b2c-3d4e-5f60-778899aabbcc"))
    }

    @Test
    fun `accepts uppercase uuid`() {
        assertTrue(UuidValidator.isValid("AF9CA340-1B2C-3D4E-5F60-778899AABBCC"))
    }

    @Test
    fun `accepts uuid with surrounding whitespace`() {
        assertTrue(UuidValidator.isValid("  af9ca340-1b2c-3d4e-5f60-778899aabbcc\n"))
    }

    @Test
    fun `rejects blank uuid`() {
        assertFalse(UuidValidator.isValid(""))
        assertFalse(UuidValidator.isValid("   "))
    }

    @Test
    fun `rejects uuid without dashes`() {
        assertFalse(UuidValidator.isValid("af9ca3401b2c3d4e5f60778899aabbcc"))
    }

    @Test
    fun `rejects truncated uuid`() {
        assertFalse(UuidValidator.isValid("af9ca340-1b2c-3d4e-5f60-778899aabb"))
    }

    @Test
    fun `rejects non-hex characters`() {
        assertFalse(UuidValidator.isValid("zf9ca340-1b2c-3d4e-5f60-778899aabbcc"))
    }

    @Test
    fun `rejects wrong group lengths`() {
        assertFalse(UuidValidator.isValid("af9ca34-1b2c-3d4e-5f60-778899aabbcc"))
    }
}
