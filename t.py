#!/usr/bin/env python3
"""Transform v2rayNG fork into SpiderVPN. Run from repo root."""
import os, shutil, re

ROOT = os.getcwd()
APP = "V2rayNG/app/src/main"
JAVA = f"{APP}/java"

def log(m): print(f"[SpiderVPN] {m}")

# 1. Package rename: com.v2ray.ang -> com.spidervpn.app
old_pkg = "com/v2ray/ang"
new_pkg = "com/spidervpn/app"
old_java = f"{JAVA}/{old_pkg}"
new_java = f"{JAVA}/{new_pkg}"
if os.path.exists(old_java):
    os.makedirs(os.path.dirname(new_java), exist_ok=True)
    shutil.move(old_java, new_java)
    # cleanup empty parent dirs
    for d in [f"{JAVA}/com/v2ray", f"{JAVA}/com"]:
        try: os.rmdir(d)
        except: pass
    log("moved java sources")
    # rewrite package declarations
    for dp, _, fns in os.walk(new_java):
        for fn in fns:
            if fn.endswith((".kt", ".java")):
                p = os.path.join(dp, fn)
                s = open(p, encoding="utf-8").read()
                s2 = s.replace("package com.v2ray.ang", "package com.spidervpn.app")
                s2 = s2.replace("import com.v2ray.ang", "import com.spidervpn.app")
                s2 = re.sub(r"\bcom\.v2ray\.ang\b", "com.spidervpn.app", s2)
                if s2 != s:
                    open(p, "w", encoding="utf-8").write(s2)
    log("rewrote package refs")

# 2. build.gradle.kts: namespace, applicationId, compileSdk/targetSdk
bg = "V2rayNG/app/build.gradle.kts"
s = open(bg).read()
s = s.replace('namespace = "com.v2ray.ang"', 'namespace = "com.spidervpn.app"')
s = s.replace('applicationId = "com.v2ray.ang"', 'applicationId = "com.spidervpn.app"')
s = s.replace("compileSdk = 37", "compileSdk = 35").replace("targetSdk = 37", "targetSdk = 35")
open(bg, "w").write(s)
log("patched build.gradle.kts")

# 3. App name
sx = f"{APP}/res/values/strings.xml"
s = open(sx).read()
s = s.replace('>v2rayNG<', '>SpiderVPN<')
open(sx, "w").write(s)
log("renamed app")

# 4. DefaultProfileManager.kt (pre-configured server, UUID blank, chrome fingerprint)
dpm = '''package com.spidervpn.app.handler

import com.spidervpn.app.dto.entities.ProfileItem
import com.spidervpn.app.enums.EConfigType
import com.spidervpn.app.util.Utils

object DefaultProfileManager {
    private const val PREF_DONE = "spidervpn_default_profile_done"
    fun ensureDefaultProfile() {
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
        val list = MmkvManager.decodeServerList("")
        list.add(guid)
        MmkvManager.encodeServerList(list, "")
        MmkvManager.setSelectServer(guid)
        MmkvManager.encodeSettings(PREF_DONE, true)
    }
}
'''
open(f"{new_java}/handler/DefaultProfileManager.kt", "w").write(dpm)
log("wrote DefaultProfileManager")

# 5. Hook into AngApplication.onCreate
aa = f"{new_java}/AngApplication.kt"
s = open(aa).read()
if "DefaultProfileManager.ensureDefaultProfile()" not in s:
    s = s.replace(
        "SettingsManager.initApp(this)",
        "SettingsManager.initApp(this)\n\n        DefaultProfileManager.ensureDefaultProfile()"
    )
    open(aa, "w").write(s)
    log("hooked AngApplication")

log("DONE. Review with: git status --short")
