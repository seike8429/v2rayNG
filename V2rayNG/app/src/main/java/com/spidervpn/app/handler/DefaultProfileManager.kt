package com.spidervpn.app.handler

import com.spidervpn.app.dto.entities.ProfileItem
import com.spidervpn.app.enums.EConfigType
import com.spidervpn.app.util.Utils

/**
 * Injects the pre-configured SpiderVPN server profile on first launch.
 * Server settings are baked in; the UUID (password) is left empty for the
 * customer to enter. DNS is forced through the tunnel to minimize DNS errors.
 */
object DefaultProfileManager {

    private const val PREF_DEFAULT_PROFILE_DONE = "spidervpn_default_profile_done"

    // Pre-configured server (from Saya's V2BOX screenshot; UUID left blank)
    private const val SERVER = "cow.uninoka.uk"
    private const val PORT = "443"
    private const val NETWORK = "ws"
    private const val WS_PATH = "/"
    private const val SECURITY = "tls"
    private const val SNI = "cow.uninoka.uk"
    private const val ENCRYPTION = "none"

    fun ensureDefaultProfile() {
        if (MmkvManager.decodeSettingsBool(PREF_DEFAULT_PROFILE_DONE)) {
            return
        }
        // Only inject when the user has no servers at all (fresh install)
        if (MmkvManager.decodeAllServerList().isNotEmpty()) {
            MmkvManager.encodeSettings(PREF_DEFAULT_PROFILE_DONE, true)
            return
        }

        val profile = ProfileItem.create(EConfigType.VLESS).apply {
            remarks = "SpiderVPN"
            server = SERVER
            serverPort = PORT
            password = "" // customer enters their own UUID
            method = ENCRYPTION
            network = NETWORK
            path = WS_PATH
            security = SECURITY
            sni = SNI
            insecure = false
            // DPI resistance: uTLS Chrome fingerprint (TLS looks like real Chrome)
            fingerPrint = "chrome"
        }

        val guid = Utils.getUuid()
        MmkvManager.encodeServerConfig(guid, profile)
        // Add to the default (ungrouped) server list
        val list = MmkvManager.decodeServerList("")
        list.add(guid)
        MmkvManager.encodeServerList(list, "")
        // Select it as the active server
        MmkvManager.setSelectServer(guid)

        MmkvManager.encodeSettings(PREF_DEFAULT_PROFILE_DONE, true)
    }
}
