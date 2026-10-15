package com.spidervpn.app.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spidervpn.app.BuildConfig
import com.spidervpn.app.R
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Fixed dark palette (KSH-VPN-inspired). Independent of the system theme so
// the customer UI always looks the same.
// ---------------------------------------------------------------------------
private val SpiderBgTop = Color(0xFF060A15)
private val SpiderBgBottom = Color(0xFF0D1526)
private val SpiderCyan = Color(0xFF22D3EE)
private val SpiderBlue = Color(0xFF3B82F6)
private val SpiderGreen = Color(0xFF34D399)
private val SpiderCard = Color(0xFF121A2E)
private val SpiderCardBorder = Color(0xFF223052)
private val SpiderTextDim = Color(0xFF8B98B8)
private val SpiderTextFaint = Color(0xFF5B6B8C)

private const val TAB_VPN = 0
private const val TAB_SETTINGS = 1

/**
 * SpiderVPN customer home screen (KSH-VPN style).
 *
 * Dark navy UI. Customers see only: header, big circular connect button with
 * spider mascot, status + duration timer, VPN location card, UUID field and a
 * minimal bottom nav (VPN / Settings). No server, port, TLS or routing
 * settings are exposed — the VLESS profile is baked in and only changes via
 * APK updates.
 */
@Composable
fun SpiderHomeScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TAB_VPN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpiderBgTop, SpiderBgBottom)))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (tab == TAB_VPN) {
                    SpiderVpnTab(uiState = uiState, onAction = onAction)
                } else {
                    SpiderSettingsTab(onAction = onAction)
                }
            }
            SpiderBottomNav(selectedTab = tab, onSelect = { tab = it })
        }
    }
}

// ---------------------------------------------------------------------------
// VPN tab
// ---------------------------------------------------------------------------
@Composable
private fun SpiderVpnTab(
    uiState: MainUiState,
    onAction: (MainAction) -> Unit,
) {
    val isRunning = uiState.isRunning
    var uuidText by rememberSaveable(uiState.uuid) { mutableStateOf(uiState.uuid) }

    // Connection duration timer (transient presentation state).
    var connectStartMs by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            if (connectStartMs == 0L) connectStartMs = System.currentTimeMillis()
        } else {
            connectStartMs = 0L
        }
    }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    fun onConnectClick() {
        val text = uuidText.trim()
        // If text field is empty, use the saved UUID from the profile.
        // The diagnostic confirms the profile has a valid UUID; the text
        // field is only for entering a NEW UUID, not required for connect.
        val effectiveUuid = if (text.isEmpty()) uiState.uuid else text
        if (!UuidValidator.isValid(effectiveUuid)) {
            onAction(MainAction.UpdateUuid(text))
            return
        }
        if (text.isNotEmpty() && text != uiState.uuid) {
            onAction(MainAction.UpdateUuid(text))
        }
        if (isRunning && effectiveUuid != uiState.uuid) {
            onAction(MainAction.RestartService)
        } else {
            onAction(MainAction.ToggleService)
        }
    }

    val connectDescription = stringResource(
        if (isRunning) R.string.spider_disconnect else R.string.spider_connect
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        // Header: mini spider + app name.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpiderMascot(
                modifier = Modifier.size(44.dp),
                bodyColor = SpiderCyan
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.spider_app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        // Big circular connect button.
        ConnectButton(
            isRunning = isRunning,
            contentDescription = connectDescription,
            onClick = ::onConnectClick
        )

        Spacer(Modifier.height(18.dp))

        // Status text.
        Text(
            text = stringResource(
                if (isRunning) R.string.spider_connected else R.string.spider_disconnected
            ),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isRunning) SpiderGreen else SpiderTextDim
        )

        // Duration timer while connected.
        if (isRunning) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.spider_duration),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SpiderTextFaint,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDuration(nowMs - connectStartMs),
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                color = SpiderCyan,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // Location card.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SpiderCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpiderCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationPinIcon(modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.spider_location_title),
                        fontSize = 12.sp,
                        color = SpiderTextDim
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.spider_location_value),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // UUID field.
        OutlinedTextField(
            value = uuidText,
            onValueChange = {
                uuidText = it
                if (uiState.uuidInvalid) onAction(MainAction.UuidEdited)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.spider_uuid)) },
            placeholder = { Text(stringResource(R.string.spider_uuid_hint)) },
            isError = uiState.uuidInvalid,
            supportingText = {
                if (uiState.uuidInvalid) {
                    Text(stringResource(R.string.spider_uuid_invalid))
                }
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConnectClick() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpiderCyan,
                unfocusedBorderColor = SpiderCardBorder,
                errorBorderColor = Color(0xFFF87171),
                focusedLabelColor = SpiderCyan,
                unfocusedLabelColor = SpiderTextDim,
                errorLabelColor = Color(0xFFF87171),
                cursorColor = SpiderCyan,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorTextColor = Color.White,
                focusedPlaceholderColor = SpiderTextFaint,
                unfocusedPlaceholderColor = SpiderTextFaint,
                focusedSupportingTextColor = Color(0xFFF87171),
                unfocusedSupportingTextColor = SpiderTextDim
            )
        )

        Spacer(Modifier.height(16.dp))

        // Save UUID button: validates and persists UUID to the hidden profile.
        OutlinedButton(
            onClick = {
                val text = uuidText.trim()
                onAction(MainAction.UpdateUuid(text))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SpiderCyan
            ),
            border = BorderStroke(1.dp, SpiderCyan.copy(alpha = 0.5f))
        ) {
            Text(text = stringResource(R.string.spider_save_uuid))
        }

        Spacer(Modifier.height(8.dp))

        // Reset UUID button on first page.
        TextButton(
            onClick = { onAction(MainAction.ResetUuid) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.spider_reset_uuid),
                color = SpiderTextDim
            )
        }

        Spacer(Modifier.height(8.dp))

        // Test button: runs diagnostics and shows detailed status.
        OutlinedButton(
            onClick = { onAction(MainAction.RunDiagnostic) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF4ADE80)
            ),
            border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.5f))
        ) {
            Text(text = stringResource(R.string.spider_test))
        }

        // Diagnostic result dialog
        uiState.diagnostic?.let { diagnostic ->
            DiagnosticDialog(
                diagnostic = diagnostic,
                onDismiss = { onAction(MainAction.DismissDiagnostic) }
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Big circular connect button with glow + spider mascot.
// ---------------------------------------------------------------------------
@Composable
private fun ConnectButton(
    isRunning: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "spiderGlow")
    val pulse by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spiderPulse"
    )
    val glowAlpha = if (isRunning) pulse else 0.30f
    val ringColor = if (isRunning) SpiderGreen else SpiderCyan

    Box(
        modifier = Modifier
            .size(212.dp)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            // Soft outer glow.
            drawCircle(
                color = ringColor.copy(alpha = 0.10f * glowAlpha),
                radius = r
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.16f * glowAlpha),
                radius = r * 0.88f
            )
            // Bright ring.
            drawCircle(
                color = ringColor.copy(alpha = 0.55f + 0.35f * glowAlpha),
                radius = r * 0.74f,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            )
            // Inner dark disc.
            drawCircle(
                color = Color(0xFF0A1120),
                radius = r * 0.68f
            )
            // Faint inner ring.
            drawCircle(
                color = ringColor.copy(alpha = 0.25f),
                radius = r * 0.68f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        SpiderMascot(
            modifier = Modifier.size(112.dp),
            bodyColor = if (isRunning) SpiderGreen else SpiderCyan
        )
    }
}

// ---------------------------------------------------------------------------
// Spider mascot drawn with Canvas: abdomen, cephalothorax, eight jointed
// legs, red eyes. Reused for header, connect button and settings.
// ---------------------------------------------------------------------------
@Composable
fun SpiderMascot(
    modifier: Modifier = Modifier,
    bodyColor: Color = SpiderCyan,
) {
    val accentColor = SpiderBlue
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Eight legs: four per side, each with a knee joint.
        val legStroke = w * 0.035f
        for (side in listOf(-1f, 1f)) {
            for (i in 0..3) {
                val startY = h * (0.36f + i * 0.075f)
                val startX = cx + side * w * 0.07f
                val kneeX = cx + side * w * (0.30f - i * 0.015f)
                val kneeY = h * (0.30f + i * 0.10f)
                val footX = cx + side * w * 0.44f
                val footY = h * (0.24f + i * 0.165f)
                val leg = Path().apply {
                    moveTo(startX, startY)
                    lineTo(kneeX, kneeY)
                    lineTo(footX, footY)
                }
                drawPath(
                    path = leg,
                    color = bodyColor,
                    style = Stroke(width = legStroke, cap = StrokeCap.Round)
                )
            }
        }

        // Abdomen (large oval, lower).
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - w * 0.17f, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.36f)
        )
        // Abdomen accent stripe.
        drawOval(
            color = accentColor,
            topLeft = Offset(cx - w * 0.045f, h * 0.56f),
            size = androidx.compose.ui.geometry.Size(w * 0.09f, h * 0.26f)
        )
        // Cephalothorax (head circle, upper).
        drawCircle(
            color = bodyColor,
            radius = w * 0.11f,
            center = Offset(cx, h * 0.40f)
        )
        // Eyes.
        val eyeColor = Color(0xFFE53935)
        drawCircle(
            color = eyeColor,
            radius = w * 0.028f,
            center = Offset(cx - w * 0.045f, h * 0.385f)
        )
        drawCircle(
            color = eyeColor,
            radius = w * 0.028f,
            center = Offset(cx + w * 0.045f, h * 0.385f)
        )
    }
}

/** Small location-pin glyph for the location card. */
@Composable
private fun LocationPinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val pin = Path().apply {
            moveTo(cx, h * 0.92f)
            cubicTo(
                cx - w * 0.42f, h * 0.62f,
                cx - w * 0.36f, h * 0.08f,
                cx, h * 0.08f
            )
            cubicTo(
                cx + w * 0.36f, h * 0.08f,
                cx + w * 0.42f, h * 0.62f,
                cx, h * 0.92f
            )
            close()
        }
        drawPath(pin, SpiderCyan.copy(alpha = 0.25f))
        drawCircle(
            color = SpiderCyan,
            radius = w * 0.13f,
            center = Offset(cx, h * 0.36f)
        )
    }
}

// ---------------------------------------------------------------------------
// Settings tab: about + UUID reset. No server/network settings exposed.
// ---------------------------------------------------------------------------
@Composable
private fun SpiderSettingsTab(
    onAction: (MainAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        SpiderMascot(modifier = Modifier.size(72.dp), bodyColor = SpiderCyan)

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.spider_app_name),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SpiderCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpiderCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.spider_about),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.spider_about_text),
                    fontSize = 14.sp,
                    color = SpiderTextDim,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.spider_version)}: ${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    color = SpiderTextFaint
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { onAction(MainAction.ResetUuid) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.6f))
        ) {
            Text(
                text = stringResource(R.string.spider_reset_uuid),
                color = Color(0xFFF87171),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ---------------------------------------------------------------------------
// Bottom nav: VPN / Settings.
// ---------------------------------------------------------------------------
@Composable
private fun SpiderBottomNav(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0F1E))
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SpiderNavItem(
            selected = selectedTab == TAB_VPN,
            label = stringResource(R.string.spider_tab_vpn),
            onClick = { onSelect(TAB_VPN) },
            icon = { tint -> VpnTabIcon(Modifier.size(26.dp), tint) }
        )
        SpiderNavItem(
            selected = selectedTab == TAB_SETTINGS,
            label = stringResource(R.string.spider_tab_settings),
            onClick = { onSelect(TAB_SETTINGS) },
            icon = { tint -> SettingsTabIcon(Modifier.size(26.dp), tint) }
        )
    }
}

@Composable
private fun SpiderNavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val tint = if (selected) SpiderCyan else SpiderTextFaint
    Column(
        modifier = Modifier
            .semantics {
                contentDescription = label
                role = Role.Tab
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            textAlign = TextAlign.Center
        )
    }
}

/** Shield-with-check glyph for the VPN tab. */
@Composable
private fun VpnTabIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.22f)
            lineTo(w * 0.88f, h * 0.52f)
            cubicTo(w * 0.88f, h * 0.78f, w * 0.68f, h * 0.90f, w * 0.5f, h * 0.96f)
            cubicTo(w * 0.32f, h * 0.90f, w * 0.12f, h * 0.78f, w * 0.12f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.22f)
            close()
        }
        drawPath(shield, tint, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
        val check = Path().apply {
            moveTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(check, tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
    }
}

/** Gear glyph for the Settings tab. */
@Composable
private fun SettingsTabIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val cx = w / 2f
        val cy = size.height / 2f
        // Teeth.
        for (i in 0 until 8) {
            rotate(degrees = i * 45f, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - w * 0.07f, cy - w * 0.48f),
                    size = androidx.compose.ui.geometry.Size(w * 0.14f, w * 0.20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
                )
            }
        }
        // Hub.
        drawCircle(color = Color(0xFF0A0F1E), radius = w * 0.34f, center = Offset(cx, cy))
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.09f)
        )
        drawCircle(color = tint, radius = w * 0.10f, center = Offset(cx, cy))
    }
}

internal fun formatDuration(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs.coerceAtLeast(0L) / 1000).toInt()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/**
 * Diagnostic result dialog for the Test button (Build #28).
 * Shows detailed connection checks without exposing secrets.
 */
@Composable
internal fun DiagnosticDialog(
    diagnostic: com.spidervpn.app.handler.DefaultProfileManager.Diagnostic,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_title),
                color = Color.White
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                if (diagnostic.error != null) {
                    androidx.compose.material3.Text(
                        text = diagnostic.error,
                        color = Color(0xFFF87171)
                    )
                } else {
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_profile),
                        ok = diagnostic.profileExists
                    )
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_selected),
                        ok = diagnostic.selectedGuidValid
                    )
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_server),
                        ok = diagnostic.serverCorrect
                    )
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_port),
                        ok = diagnostic.portCorrect
                    )
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_tls),
                        ok = diagnostic.tlsCorrect
                    )
                    DiagnosticRow(
                        label = androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_uuid),
                        ok = diagnostic.uuidPresent
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.ui.Modifier.height(8.dp)
                    )
                    androidx.compose.material3.Text(
                        text = if (diagnostic.allOk)
                            androidx.compose.ui.res.stringResource(R.string.spider_diagnostic_ok)
                        else
                            "Check failed items above",
                        color = if (diagnostic.allOk) Color(0xFF4ADE80) else Color(0xFFF87171)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("OK", color = Color(0xFF4ADE80))
            }
        },
        containerColor = Color(0xFF0A0F1E)
    )
}

@Composable
private fun DiagnosticRow(label: String, ok: Boolean) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.Text(text = label, color = Color.White)
        androidx.compose.material3.Text(
            text = androidx.compose.ui.res.stringResource(
                if (ok) R.string.spider_diagnostic_pass else R.string.spider_diagnostic_fail
            ),
            color = if (ok) Color(0xFF4ADE80) else Color(0xFFF87171)
        )
    }
}
