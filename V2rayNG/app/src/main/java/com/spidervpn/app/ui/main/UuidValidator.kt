package com.spidervpn.app.ui.main

/**
 * Validates the VLESS UUID typed by the customer on the simplified home screen.
 *
 * Accepts the canonical 8-4-4-4-12 hexadecimal form (case-insensitive).
 * Pure logic — covered by [UuidValidatorTest].
 */
object UuidValidator {
    private val UUID_REGEX =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun isValid(uuid: String): Boolean = UUID_REGEX.matches(uuid.trim())
}
