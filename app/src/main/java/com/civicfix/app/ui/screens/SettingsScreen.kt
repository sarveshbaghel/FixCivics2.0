package com.civicfix.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.SettingsUpdateRequest
import com.civicfix.app.ui.theme.CivicFixBlue
import com.civicfix.app.ui.theme.ThemeState
import kotlinx.coroutines.launch

/** SharedPreferences file name used for local app settings. */
const val CIVICFIX_PREFS = "civicfix_prefs"
/** Key for the "Enable Twitter Posting" toggle (boolean, default false). */
const val PREF_POST_TO_TWITTER = "pref_post_to_twitter"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    token: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var autoPostEnabled by remember { mutableStateOf(false) }
    var apiConnected by remember { mutableStateOf(false) }
    var bearerConfigured by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var toggling by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Local Twitter posting toggle (SharedPreferences)
    val prefs = remember { context.getSharedPreferences(CIVICFIX_PREFS, Context.MODE_PRIVATE) }
    var twitterPostEnabled by remember { mutableStateOf(prefs.getBoolean(PREF_POST_TO_TWITTER, false)) }

    // Load settings
    LaunchedEffect(Unit) {
        if (token == null) {
            error = "Login required"
            loading = false
            return@LaunchedEffect
        }
        try {
            val settings = RetrofitClient.api.getSettings("Bearer $token")
            autoPostEnabled = settings.xAutoPostEnabled
            apiConnected = settings.xApiConnected
            bearerConfigured = settings.xBearerConfigured
            loading = false
        } catch (e: Exception) {
            error = "Failed to load settings"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A202C)
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CivicFixBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Appearance Card (Dark Mode Toggle) ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🎨", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Appearance",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Choose your preferred app theme. This affects all screens.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        val currentMode = ThemeState.darkModePreference
                        val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            options.forEach { (value, label) ->
                                val isSelected = currentMode == value
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (isSelected) Modifier else Modifier
                                        ),
                                    color = if (isSelected) CivicFixBlue else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                    onClick = {
                                        ThemeState.setDarkMode(context, value)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Twitter Posting Toggle Card (local SharedPreferences) ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🐦", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Enable Twitter Posting",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "After submitting a report, share it directly to Twitter/X with a pre-filled tweet and attached photo.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Share to Twitter after Submit",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A202C)
                                )
                                Text(
                                    if (twitterPostEnabled)
                                        "Twitter composer will open after submitting a report"
                                    else
                                        "Reports will be saved without opening Twitter",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = twitterPostEnabled,
                                onCheckedChange = { newValue ->
                                    twitterPostEnabled = newValue
                                    prefs.edit().putBoolean(PREF_POST_TO_TWITTER, newValue).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1DA1F2),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // ── X Integration Card (server-side auto-post) ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("𝕏", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "X (Twitter) Integration",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                            Spacer(Modifier.weight(1f))
                            // Connection badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (apiConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (apiConnected) "✅ Connected" else "⚠️ Partial",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (apiConnected) Color(0xFF166534) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "When enabled, every new report submitted will automatically be posted to your X account.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(Modifier.height(16.dp))

                        // Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Automatic Post to X",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A202C)
                                )
                                Text(
                                    if (autoPostEnabled) "New reports will be posted to X"
                                    else "Reports must be posted to X manually",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = autoPostEnabled,
                                onCheckedChange = { newValue ->
                                    if (token == null) {
                                        error = "Login required to change settings"
                                        return@Switch
                                    }
                                    scope.launch {
                                        toggling = true
                                        error = null
                                        successMsg = null
                                        try {
                                            val result = RetrofitClient.api.updateSettings(
                                                "Bearer $token",
                                                SettingsUpdateRequest(newValue)
                                            )
                                            autoPostEnabled = result.xAutoPostEnabled
                                            successMsg = if (newValue) "Auto-post enabled ✓"
                                                          else "Auto-post disabled"
                                        } catch (e: Exception) {
                                            error = "Failed to update: ${e.localizedMessage}"
                                        } finally {
                                            toggling = false
                                        }
                                    }
                                },
                                enabled = !toggling,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // Success/Error messages
                        successMsg?.let {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    it, fontSize = 13.sp,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        error?.let {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    it, fontSize = 13.sp,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Connection Status",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A202C)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // OAuth status
                        ConnectionStatusRow(
                            name = "OAuth 1.0a (Post access)",
                            connected = apiConnected,
                            detail = if (apiConnected) "API keys configured"
                                     else "Access Token & Secret needed"
                        )

                        Spacer(Modifier.height(12.dp))

                        // Bearer status
                        ConnectionStatusRow(
                            name = "Bearer Token (Read access)",
                            connected = bearerConfigured,
                            detail = if (bearerConfigured) "Bearer token configured"
                                     else "Bearer token not set"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusRow(
    name: String,
    connected: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (connected) Color(0xFF10B981) else Color(0xFFF59E0B))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A202C))
            Text(detail, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}
