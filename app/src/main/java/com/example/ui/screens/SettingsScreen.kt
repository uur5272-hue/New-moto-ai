package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.MotoFloatingService
import com.example.service.MotoWakeWordService
import com.example.ui.MotoViewModel
import com.example.ui.theme.DarkSpaceBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    viewModel: MotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsState()
    val isWakeWordActive by MotoWakeWordService.isRunning.collectAsState()
    val isFloatingActive by MotoFloatingService.isFloatingActive.collectAsState()

    var customKeyInput by remember { mutableStateOf(prefs.customApiKey) }
    var speedSlider by remember { mutableFloatStateOf(prefs.speechSpeed) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSpaceBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NeonCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("MOTO AI Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Configure voice, Gemini API, wake words, and permissions.", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. API KEY CONFIGURATION
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (viewModel.isApiKeyReady()) NeonCyanGlow else ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure API Configuration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText = if (viewModel.isApiKeyReady()) "Active (${viewModel.getApiKeyDisplayStatus()})" else "Not configured"
                    Icon(
                        imageVector = if (viewModel.isApiKeyReady()) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (viewModel.isApiKeyReady()) NeonCyan else ElectricVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Status: $statusText",
                        color = if (viewModel.isApiKeyReady()) NeonCyan else ElectricVioletLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Google AI Studio automatically injects your GEMINI_API_KEY from the Secrets panel. You can also specify an override key below.",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customKeyInput,
                    onValueChange = { customKeyInput = it },
                    placeholder = { Text("Enter Gemini API key (AIza...)", color = TextTertiary, fontSize = 12.5.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_key_field"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.setCustomApiKey(customKeyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.align(Alignment.End).testTag("save_api_key_button")
                ) {
                    Text("Save Key", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. LANGUAGE & VOICE
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assistant Language", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val languages = listOf(
                    "auto" to "Auto-detect",
                    "en" to "English",
                    "hi" to "हिन्दी (Hindi)",
                    "ur" to "اردو (Urdu)",
                    "es" to "Español",
                    "fr" to "Français",
                    "ar" to "العربية"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.take(4).forEach { (code, label) ->
                        val isSelected = prefs.language == code
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, if (isSelected) NeonCyan else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setLanguage(code) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.drop(4).forEach { (code, label) ->
                        val isSelected = prefs.language == code
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, if (isSelected) NeonCyan else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setLanguage(code) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Speak Voice Responses", color = TextPrimary, fontSize = 13.5.sp)
                    }
                    Switch(
                        checked = prefs.autoSpeak,
                        onCheckedChange = { viewModel.setAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("switch_auto_speak")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Speech Speed: ${"%.1f".format(speedSlider)}x", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = speedSlider,
                    onValueChange = { speedSlider = it },
                    onValueChangeFinished = { viewModel.setSpeechSpeed(speedSlider) },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("slider_speech_speed")
                )

                OutlinedButton(
                    onClick = { viewModel.speakText("Hello! I am MOTO AI, ready to assist you.") },
                    modifier = Modifier.align(Alignment.End).testTag("test_voice_button")
                ) {
                    Text("Test Voice", color = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. WAKE WORD DETECTION
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hearing, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("\"Hey MOTO\" Wake Word", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isWakeWordActive) "Listening in foreground service" else "Service currently stopped",
                                color = if (isWakeWordActive) NeonCyan else TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Switch(
                        checked = prefs.wakeWordEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setWakeWordEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("switch_wake_word")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "When active, MOTO AI runs a foreground listening service with a persistent notification. Android restrictions require the service notification to remain active for background mic access.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. FLOATING ASSISTANT OVERLAY
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Floating MOTO AI Button", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isFloatingActive) "Overlay button active" else "Disabled",
                                color = if (isFloatingActive) ElectricVioletLight else TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Switch(
                        checked = prefs.floatingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(intent)
                                viewModel.showNotice("Grant overlay permission to enable floating button.")
                            } else {
                                viewModel.setFloatingAssistantEnabled(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricVioletLight,
                            checkedTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("switch_floating_button")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Displays a draggable floating shortcut over other apps for 1-tap assistant access.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. IDENTITY & OWNER ATTRIBUTION
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyanGlow, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identity & Ownership", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "I am MOTO AI, configured for Saif Ali Khan.",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Powered by Google Gemini models (Gemini 3.5 Flash, Gemini 2.5 Flash Image, Veo Video). Offline controls and voice operated via native Android APIs.",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version: 2.0 (Futuristic Edition)", color = TextTertiary, fontSize = 11.sp)
                    Text("Safe & Privacy-First", color = TextTertiary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
