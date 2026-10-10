package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
fun ImageStudioScreen(
    viewModel: MotoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val imagePrompt by viewModel.imagePrompt.collectAsState()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val generatedImageBitmap by viewModel.generatedImageBitmap.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
    val imageGenError by viewModel.imageGenError.collectAsState()

    val editSourceBitmap by viewModel.editSourceBitmap.collectAsState()
    val editInstructions by viewModel.editInstructions.collectAsState()
    val editedResultBitmap by viewModel.editedResultBitmap.collectAsState()
    val isEditingPhoto by viewModel.isEditingPhoto.collectAsState()
    val photoEditError by viewModel.photoEditError.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.setEditSourceBitmap(bitmap)
            } catch (e: Exception) {
                viewModel.showNotice("Failed to load photo.")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSpaceBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceCard,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Generate Image", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_generate_image")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Edit & Transform", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_edit_image")
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (selectedTab == 0) {
                Text(
                    text = "AI Image Generator",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by Gemini 2.5 Flash Image. Generate visuals from text prompts.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { viewModel.setImagePrompt(it) },
                    label = { Text("Enter Image Prompt", color = TextSecondary) },
                    placeholder = { Text("e.g. Futuristic flying hoverbike in neon Tokyo rainy night", color = TextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_prompt_input"),
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

                Spacer(modifier = Modifier.height(14.dp))

                Text("Aspect Ratio", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ratios = listOf("1:1", "16:9", "9:16", "4:3")
                    ratios.forEach { ratio ->
                        val isSelected = selectedAspectRatio == ratio
                        Surface(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, if (isSelected) NeonCyan else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSelectedAspectRatio(ratio) }
                        ) {
                            Text(
                                text = ratio,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Art Style", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf("Cyberpunk Neon", "Photorealistic", "Digital Art", "Anime 3D")
                    styles.forEach { style ->
                        val isSelected = selectedStyle == style
                        Surface(
                            color = if (isSelected) ElectricViolet.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, if (isSelected) ElectricViolet else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSelectedStyle(style) }
                        ) {
                            Text(
                                text = style,
                                color = if (isSelected) ElectricViolet else TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateImage() },
                    enabled = !isGeneratingImage && imagePrompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_image_button")
                ) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkSpaceBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating Artwork...", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkSpaceBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Image", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                }

                imageGenError?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = DarkSurfaceCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyanGlow, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "⚠️ $err",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                generatedImageBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Result", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Generated Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveGeneratedImageToGallery() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_image_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = DarkSpaceBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareGeneratedImage() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_image_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = NeonCyan)
                        }

                        IconButton(
                            onClick = { viewModel.generateImage() },
                            modifier = Modifier.testTag("regenerate_image_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = TextSecondary)
                        }
                    }
                }
            } else {
                // PHOTO EDIT SECTION
                Text(
                    text = "AI Photo Editor & Inpainting",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upload a photo and describe changes (e.g., replace background, change colors, enhance).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_source_photo_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Photo", fontWeight = FontWeight.Bold)
                    }
                }

                editSourceBitmap?.let { srcBmp ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Original Photo", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ElectricViolet, RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            bitmap = srcBmp.asImageBitmap(),
                            contentDescription = "Original Source",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = editInstructions,
                    onValueChange = { viewModel.setEditInstructions(it) },
                    label = { Text("Editing Instructions", color = TextSecondary) },
                    placeholder = { Text("e.g. Replace background with a futuristic skyline", color = TextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_instructions_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.executePhotoEdit() },
                    enabled = !isEditingPhoto && editSourceBitmap != null && editInstructions.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_photo_edit_button")
                ) {
                    if (isEditingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Edits...", color = TextPrimary, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply AI Edits", fontWeight = FontWeight.Bold)
                    }
                }

                photoEditError?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = DarkSurfaceCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ $err",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                editedResultBitmap?.let { editedBmp ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Edited Result", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = editedBmp.asImageBitmap(),
                            contentDescription = "Edited Result",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveEditedPhotoToGallery() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_edited_photo_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = DarkSpaceBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery", color = DarkSpaceBackground, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareEditedPhoto() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_edited_photo_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}
