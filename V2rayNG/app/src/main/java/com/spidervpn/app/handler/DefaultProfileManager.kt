package com.spidervpn.app.handler

import com.spidervpn.app.dto.entities.ProfileItem
import com.spidervpn.app.enums.EConfigType
import com.spidervpn.app.util.Utils

object DefaultProfileManager {
    private const val PREF_DONE = "spidervpn_default_profile_done"
    fun ensureDefaultProfile() {
        // Repair duplicates from earlier runs (LazyColumn needs unique keys)
        val cur = MmkvManager.decodeServerList("")
        val fixed = cur.distinct()
        if (fixed.size != cur.size) {
            MmkvManager.encodeServerList(fixed.toMutableList(), "")
        }
        if (MmkvManager.decodeSettingsBool(PREF_DONE)) return
        if (MmkvManager.decodeAllServerList().isNotEmpty()) {
            MmkvManager.encodeSettings(PREF_DONE, true); return
        }
        val profile = ProfileItem.create(EConfigType.VLESS).apply {
            remarks = "SpiderVPN"
            server = "cow.uninoka.uk"
            serverPort = "443"
            password = ""
            method = "none"
            network = "ws"
            path = "/"
            security = "tls"
            sni = "cow.uninoka.uk"
            insecure = false
            fingerPrint = "chrome"
        }
        val guid = Utils.getUuid()
        MmkvManager.encodeServerConfig(guid, profile)
        val list = MmkvManager.decodeServerList("").distinct().toMutableList()
        if (!list.contains(guid)) {
            list.add(guid)
        }
        MmkvManager.encodeServerList(list, "")
        MmkvManager.setSelectServer(guid)
        MmkvManager.encodeSettings(PREF_DONE, true)
    }
}
