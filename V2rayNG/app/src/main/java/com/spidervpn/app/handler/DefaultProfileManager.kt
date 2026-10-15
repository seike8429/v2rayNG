package com.spidervpn.app.handler

import com.spidervpn.app.dto.entities.ProfileItem
import com.spidervpn.app.enums.EConfigType
import com.spidervpn.app.util.Utils

/**
 * Ensures the pre-configured SpiderVPN server profile always exists and is valid.
 * Server settings are baked in; the UUID (password) is left for the customer to enter.
 * DNS is forced through the tunnel to minimize DNS errors.
 *
 * Build #28: rewritten to ALWAYS verify/repair the profile on every launch,
 * not just on first install. Finds the profile by stable remarks identity,
 * repairs corrupted fields, deduplicates the server list, and ensures selection.
 */
object DefaultProfileManager {

    private const val PREF_DEFAULT_PROFILE_DONE = "spidervpn_default_profile_done"
    private const val PROFILE_REMARKS = "SpiderVPN"

    // Pre-configured server (from Saya's V2BOX screenshot; UUID left blank)
    private const val SERVER = "cow.uninoka.uk"
    private const val PORT = "443"
    private const val NETWORK = "ws"
    private const val WS_PATH = "/"
    private const val SECURITY = "tls"
    private const val SNI = "cow.uninoka.uk"
    private const val ENCRYPTION = "none"

    /**
     * Diagnostic result for the Test button. Contains no secrets.
     */
    data class Diagnostic(
        val profileExists: Boolean,
        val selectedGuidValid: Boolean,
        val serverCorrect: Boolean,
        val portCorrect: Boolean,
        val tlsCorrect: Boolean,
        val uuidPresent: Boolean,
        val guid: String?,
        val error: String?
    ) {
        val allOk: Boolean
            get() = error == null && profileExists && selectedGuidValid &&
                    serverCorrect && portCorrect && tlsCorrect && uuidPresent
    }

    /**
     * Find the SpiderVPN profile GUID by remarks identity.
     * Searches all server lists for a profile with remarks == "SpiderVPN".
     */
    private fun findSpiderVpnGuid(): String? {
        // Check default (ungrouped) list
        val defaultList = MmkvManager.decodeServerList("")
        for (guid in defaultList) {
            val profile = MmkvManager.decodeServerConfig(guid)
            if (profile?.remarks == PROFILE_REMARKS) {
                return guid
            }
        }
        // Check all subscriptions/groups
        val allGuids = MmkvManager.decodeAllServerList()
        for (guid in allGuids) {
            if (guid in defaultList) continue // already checked
            val profile = MmkvManager.decodeServerConfig(guid)
            if (profile?.remarks == PROFILE_REMARKS) {
                return guid
            }
        }
        return null
    }

    /**
     * Create a fresh SpiderVPN profile with baked-in server settings.
     */
    /**
     * Create a fresh SpiderVPN profile by parsing a template VLESS URL.
     * This uses the exact same import logic as v2rayNG (VlessFmt.parse),
     * ensuring 100% compatibility with manually imported configs.
     * The UUID (password) is cleared for the customer to enter.
     */
    private fun createProfile(): String {
        // Template URL matching the working v2rayNG config (UUID placeholder)
        val templateUrl = "vless://00000000-0000-0000-0000-000000000000@cow.uninoka.uk:443" +
                "?path=%2F&security=tls&encryption=none&type=ws&sni=cow.uninoka.uk#SpiderVPN"
        val profile = com.spidervpn.app.fmt.VlessFmt.parse(templateUrl)
            ?: throw IllegalStateException("Failed to parse template VLESS URL")
        profile.remarks = PROFILE_REMARKS
        profile.password = "" // customer enters their own UUID
        val guid = Utils.getUuid()
        MmkvManager.encodeServerConfig(guid, profile)
        return guid
    }

    /**
     * Repair a profile's server fields if they've been corrupted.
     * Preserves the customer's UUID (password).
     * Returns true if any field was repaired.
     */
    private fun repairProfile(guid: String): Boolean {
        val profile = MmkvManager.decodeServerConfig(guid) ?: return false
        var repaired = false
        if (profile.server != SERVER) { profile.server = SERVER; repaired = true }
        if (profile.serverPort != PORT) { profile.serverPort = PORT; repaired = true }
        if (profile.network != NETWORK) { profile.network = NETWORK; repaired = true }
        if (profile.host != SNI) { profile.host = SNI; repaired = true }
        if (profile.path != WS_PATH) { profile.path = WS_PATH; repaired = true }
        if (profile.security != SECURITY) { profile.security = SECURITY; repaired = true }
        if (profile.sni != SNI) { profile.sni = SNI; repaired = true }
        if (profile.method != ENCRYPTION) { profile.method = ENCRYPTION; repaired = true }
        if (profile.fingerPrint == "chrome") { profile.fingerPrint = ""; repaired = true }
        if (repaired) {
            MmkvManager.encodeServerConfig(guid, profile)
        }
        return repaired
    }

    /**
     * Ensure the SpiderVPN profile exists, is valid, and is selected.
     * This is idempotent and safe to call on every launch.
     * @return the GUID of the ensured profile, or null on failure
     */
    fun ensureDefaultProfile(): String? {
        try {
            // 1. Find existing or create new
            var guid = findSpiderVpnGuid()
            if (guid == null) {
                guid = createProfile()
            } else {
                // 2. Repair corrupted fields (preserves UUID)
                repairProfile(guid)
            }

            // 3. Ensure GUID is in the default server list (deduplicated)
            val list = MmkvManager.decodeServerList("").toMutableList()
            // Remove duplicates and stale entries
            val seen = mutableSetOf<String>()
            val cleaned = list.filter { g ->
                if (g in seen) false
                else { seen.add(g); MmkvManager.decodeServerConfig(g) != null }
            }.toMutableList()
            if (guid !in cleaned) {
                cleaned.add(guid)
            }
            if (cleaned != list) {
                MmkvManager.encodeServerList(cleaned, "")
            }

            // 4. Ensure it's selected
            val selected = MmkvManager.getSelectServer()
            if (selected != guid) {
                MmkvManager.setSelectServer(guid)
            }

            MmkvManager.encodeSettings(PREF_DEFAULT_PROFILE_DONE, true)
            return guid
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Run diagnostics on the SpiderVPN profile. Returns a Diagnostic
     * with no secrets exposed. Used by the Test button.
     */
    fun runDiagnostic(): Diagnostic {
        return try {
            val guid = findSpiderVpnGuid()
            if (guid == null) {
                return Diagnostic(
                    profileExists = false,
                    selectedGuidValid = false,
                    serverCorrect = false,
                    portCorrect = false,
                    tlsCorrect = false,
                    uuidPresent = false,
                    guid = null,
                    error = "Profile not found"
                )
            }

            val selected = MmkvManager.getSelectServer()
            val profile = MmkvManager.decodeServerConfig(guid)

            if (profile == null) {
                return Diagnostic(
                    profileExists = false,
                    selectedGuidValid = false,
                    serverCorrect = false,
                    portCorrect = false,
                    tlsCorrect = false,
                    uuidPresent = false,
                    guid = guid,
                    error = "Profile config corrupted"
                )
            }

            Diagnostic(
                profileExists = true,
                selectedGuidValid = (selected == guid),
                serverCorrect = (profile.server == SERVER),
                portCorrect = (profile.serverPort == PORT),
                tlsCorrect = (profile.security == SECURITY && profile.sni == SNI),
                uuidPresent = !profile.password.isNullOrEmpty(),
                guid = guid,
                error = null
            )
        } catch (e: Exception) {
            Diagnostic(
                profileExists = false,
                selectedGuidValid = false,
                serverCorrect = false,
                portCorrect = false,
                tlsCorrect = false,
                uuidPresent = false,
                guid = null,
                error = "Diagnostic failed: ${e.message}"
            )
        }
    }
}
