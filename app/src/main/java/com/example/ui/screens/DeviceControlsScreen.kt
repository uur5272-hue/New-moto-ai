package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.MotoApplication
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
fun DeviceControlsScreen(
    viewModel: MotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by viewModel.installedApps.collectAsState()
    val deviceControl = MotoApplication.instance.deviceControlManager

    var appSearchQuery by remember { mutableStateOf("") }
    var callInput by remember { mutableStateOf("") }
    var msgRecipient by remember { mutableStateOf("") }
    var msgBody by remember { mutableStateOf("") }
    var webUrlInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSpaceBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = "Device Controls",
                tint = NeonCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Phone & App Controls",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Launch installed apps, adjust device settings, and initiate calls.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: Quick System Settings
        Text(
            text = "Android System Settings",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingButton(
                icon = Icons.Default.Wifi,
                title = "Wi-Fi",
                modifier = Modifier.weight(1f).testTag("btn_wifi_settings")
            ) {
                deviceControl.openSettingScreen(Settings.ACTION_WIFI_SETTINGS)
            }
            SettingButton(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth",
                modifier = Modifier.weight(1f).testTag("btn_bluetooth_settings")
            ) {
                deviceControl.openSettingScreen(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            SettingButton(
                icon = Icons.Default.Settings,
                title = "All Settings",
                modifier = Modifier.weight(1f).testTag("btn_all_settings")
            ) {
                deviceControl.openSettingScreen(Settings.ACTION_SETTINGS)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: Phone Call
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyanGlow, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Place Phone Call", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Requires your explicit confirmation before dialing.", color = TextSecondary, fontSize = 11.5.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = callInput,
                        onValueChange = { callInput = it },
                        placeholder = { Text("Phone number or name", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_input_field"),
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (callInput.isNotBlank()) {
                                viewModel.sendChatMessage("Call $callInput")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = callInput.isNotBlank(),
                        modifier = Modifier.testTag("initiate_call_button")
                    ) {
                        Text("Call", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: Messaging Draft
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Message, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Draft Message", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Drafts an SMS or WhatsApp message and confirms before opening.", color = TextSecondary, fontSize = 11.5.sp)

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = msgRecipient,
                    onValueChange = { msgRecipient = it },
                    placeholder = { Text("Recipient (e.g. 1234567890 or Name)", color = TextTertiary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("msg_recipient_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = msgBody,
                    onValueChange = { msgBody = it },
                    placeholder = { Text("Message text", color = TextTertiary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("msg_body_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (msgRecipient.isNotBlank()) {
                                viewModel.sendChatMessage("Send message to $msgRecipient saying $msgBody")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        enabled = msgRecipient.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("draft_sms_button")
                    ) {
                        Text("Draft SMS", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            deviceControl.draftWhatsApp(msgRecipient, msgBody)
                        },
                        modifier = Modifier.weight(1f).testTag("draft_whatsapp_button")
                    ) {
                        Text("WhatsApp", color = ElectricVioletLight)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: Browse Web
        Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Website", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = webUrlInput,
                        onValueChange = { webUrlInput = it },
                        placeholder = { Text("e.g. google.com or wikipedia.org", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).testTag("web_url_input"),
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (webUrlInput.isNotBlank()) {
                                deviceControl.openWebUrl(webUrlInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = webUrlInput.isNotBlank(),
                        modifier = Modifier.testTag("open_web_button")
                    ) {
                        Text("Open", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 5: Installed Applications Launcher
        Text(
            text = "Installed Apps (${apps.size})",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = appSearchQuery,
            onValueChange = { appSearchQuery = it },
            placeholder = { Text("Filter installed apps...", color = TextTertiary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("app_search_field"),
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

        Spacer(modifier = Modifier.height(10.dp))

        val filteredApps = if (appSearchQuery.isBlank()) {
            apps.take(15)
        } else {
            apps.filter { it.name.lowercase().contains(appSearchQuery.lowercase()) }.take(15)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (appInfo in filteredApps) {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val ok = deviceControl.launchAppByPackage(appInfo.packageName)
                            if (ok) {
                                viewModel.showNotice("Launched ${appInfo.name}")
                            } else {
                                viewModel.showNotice("Unable to launch ${appInfo.name}")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appInfo.name.take(1).uppercase(),
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appInfo.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = appInfo.packageName,
                                color = TextTertiary,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "Launch",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, NeonCyanGlow, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = NeonCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
