package com.spidervpn.app.ui.main

import com.spidervpn.app.dto.ConnectionTestResult
import com.spidervpn.app.dto.GroupMapItem
import com.spidervpn.app.dto.LocateTarget

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null,
    /** UUID persisted on the selected VLESS profile (customer-editable). */
    val uuid: String = "",
    /** True when the last UUID save attempt failed validation. */
    val uuidInvalid: Boolean = false,
    /** Diagnostic result from the Test button (null = not run yet). */
    val diagnostic: com.spidervpn.app.handler.DefaultProfileManager.Diagnostic? = null
)

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: com.spidervpn.app.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction

    data object LocateHandled : MainAction

    /** Save the customer-typed UUID onto the selected VLESS profile. */
    data class UpdateUuid(val uuid: String) : MainAction

    /** Clear the UUID validation error (user edited the field). */
    data object UuidEdited : MainAction

    /** Clear the customer-typed UUID (Settings tab). Refused while connected. */
    data object ResetUuid : MainAction

    /** Run connection diagnostics and show the result (Test button). */
    data object RunDiagnostic : MainAction

    /** Dismiss the diagnostic result dialog. */
    data object DismissDiagnostic : MainAction
}
