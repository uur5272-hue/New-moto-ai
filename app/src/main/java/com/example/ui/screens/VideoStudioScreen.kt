package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MotoViewModel
import com.example.ui.theme.DarkSpaceBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun VideoStudioScreen(
    viewModel: MotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoPrompt by viewModel.videoPrompt.collectAsState()
    val videoResolution by viewModel.videoResolution.collectAsState()
    val isGeneratingVideo by viewModel.isGeneratingVideo.collectAsState()
    val videoStatusMessage by viewModel.videoStatusMessage.collectAsState()
    val videoResult by viewModel.generatedVideoResult.collectAsState()
    val videoError by viewModel.videoError.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSpaceBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Studio",
                tint = NeonCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Veo Video Studio",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate AI videos with Google Veo. Default 720p for fast playback.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Input
        OutlinedTextField(
            value = videoPrompt,
            onValueChange = { viewModel.setVideoPrompt(it) },
            label = { Text("Video Prompt", color = TextSecondary) },
            placeholder = { Text("e.g. Cinematic drone tracking shot of a neon hovercar flying over rain-slicked city streets", color = TextTertiary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_prompt_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkSurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resolution Selector
        Text(
            text = "Target Video Resolution",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "720p is the recommended default for smooth performance on standard phones.",
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val resolutions = listOf(
                "720p" to "720p (Default)",
                "1080p" to "1080p (FHD)",
                "480p" to "480p",
                "360p" to "360p"
            )

            resolutions.forEach { (res, label) ->
                val isSelected = videoResolution == res
                Surface(
                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) NeonCyan else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setVideoResolutionChoice(res) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generate Button
        Button(
            onClick = { viewModel.generateVideo() },
            enabled = !isGeneratingVideo && videoPrompt.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_video_button")
        ) {
            if (isGeneratingVideo) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkSpaceBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating with Veo...", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = DarkSpaceBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Video ($videoResolution)", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
            }
        }

        // Live Status Progress Box
        if (isGeneratingVideo || videoStatusMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyanGlow, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGeneratingVideo) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = "Veo Generation Status",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = videoStatusMessage ?: "Connecting to video generation pipeline...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Video Error Display
        videoError?.let { err ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricViolet, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚠️ Generation Notice",
                        color = ElectricViolet,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = err,
                        color = TextPrimary,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tip: Veo video generation requires an authorized Gemini API key with Veo access enabled on your Google Cloud Project.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Generated Video Card
        videoResult?.let { res ->
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Video Ready (${res.resolution})",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${res.prompt}\"",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveGeneratedVideoToGallery() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_video_gallery_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = DarkSpaceBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                        }

                        if (!res.videoUri.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(res.videoUri), "video/mp4")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(playIntent)
                                    } catch (e: Exception) {
                                        viewModel.showNotice("No video player installed.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_video_button")
                            ) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Video", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
