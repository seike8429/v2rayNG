package com.spidervpn.app

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.spidervpn.app.AppConfig.ANG_PACKAGE
import com.spidervpn.app.handler.AppLocaleManager
import com.spidervpn.app.handler.DefaultProfileManager
import com.spidervpn.app.handler.MmkvManager
import com.spidervpn.app.handler.SettingsManager
import com.spidervpn.app.ui.compose.ThemeManager

class AngApplication : Application() {
    companion object {
        lateinit var application: AngApplication
    }

    /**
     * Attaches the base context to the application.
     * @param base The base context.
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let(ContextCompat::getContextForLanguage))
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    /**
     * Initializes the application.
     */
    override fun onCreate() {
        super.onCreate()

        // DEBUG: capture crash to file
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sb = StringBuilder()
                sb.append(throwable.toString()).append("\n")
                for (el in throwable.stackTrace) sb.append("    at ").append(el.toString()).append("\n")
                var cause = throwable.cause
                while (cause != null) {
                    sb.append("Caused by: ").append(cause.toString()).append("\n")
                    for (el in cause.stackTrace) sb.append("    at ").append(el.toString()).append("\n")
                    cause = cause.cause
                }
                getExternalFilesDir(null)?.let { dir ->
                    java.io.File(dir, "spidervpn_crash.txt").writeText(sb.toString())
                }
            } catch (_: Exception) { }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        MmkvManager.initialize(this)

        AppLocaleManager.initialize(this)

        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)

        // Ensure critical preference defaults are present in MMKV early
        SettingsManager.initApp(this)

        try {
            DefaultProfileManager.ensureDefaultProfile()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Init error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }

        // Initialize theme state from MMKV
        ThemeManager.refresh()
    }
}
