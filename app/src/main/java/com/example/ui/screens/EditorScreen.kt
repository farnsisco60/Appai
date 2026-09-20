package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processor.BackgroundType
import com.example.ui.GhostStudioViewModel
import com.example.ui.GhostUiState
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.InnerNeckAdjuster
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: GhostStudioViewModel,
    state: GhostUiState.Ready,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    val options = state.options
    val lastMsg by viewModel.lastSavedMessage.collectAsState()

    LaunchedEffect(lastMsg) {
        lastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ghost Studio Editor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess)
                            )
                            Text(
                                text = "${state.mlEngine} • ${state.processingTimeMs}ms",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Share Button
                    IconButton(
                        onClick = { viewModel.shareCurrent(context) },
                        modifier = Modifier.testTag("editor_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary
                        )
                    }

                    // Save HD Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save HD",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        },
        containerColor = StudioDarkBg,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // Interactive Before / After Split Slider
            BeforeAfterSlider(
                beforeBitmap = state.originalBitmap,
                afterBitmap = state.compositedBitmap,
                isTransparentBackground = options.backgroundType.isTransparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )

            // Background Color Replacement Section
            Text(
                text = "BACKGROUND REPLACEMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = AccentCyan
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BackgroundOptionButton(
                    type = BackgroundType.PURE_WHITE,
                    isSelected = options.backgroundType == BackgroundType.PURE_WHITE,
                    testTag = "bg_pure_white",
                    onClick = {
                        viewModel.updateOptions(options.copy(backgroundType = BackgroundType.PURE_WHITE))
                    },
                    modifier = Modifier.weight(1f)
                )

                BackgroundOptionButton(
                    type = BackgroundType.TRANSPARENT,
                    isSelected = options.backgroundType == BackgroundType.TRANSPARENT,
                    testTag = "bg_transparent",
                    onClick = {
                        viewModel.updateOptions(options.copy(backgroundType = BackgroundType.TRANSPARENT))
                    },
                    modifier = Modifier.weight(1f)
                )

                BackgroundOptionButton(
                    type = BackgroundType.LIGHT_GRAY,
                    isSelected = options.backgroundType == BackgroundType.LIGHT_GRAY,
                    testTag = "bg_light_gray",
                    onClick = {
                        viewModel.updateOptions(options.copy(backgroundType = BackgroundType.LIGHT_GRAY))
                    },
                    modifier = Modifier.weight(1f)
                )

                BackgroundOptionButton(
                    type = BackgroundType.DARK_SLATE,
                    isSelected = options.backgroundType == BackgroundType.DARK_SLATE,
                    testTag = "bg_dark_slate",
                    onClick = {
                        viewModel.updateOptions(options.copy(backgroundType = BackgroundType.DARK_SLATE))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // E-Commerce Features Panel
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "STUDIO POST-PROCESSING CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )

                    // 1. Ghost Mannequin 3D Hollow Neck Lining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "3D Hollow Collar Lining",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    color = AccentPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PHOROROOM FX",
                                        color = AccentPurple,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Synthesizes inner back neck lining with 3D depth & stitching",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = options.isGhostCollarEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateOptions(options.copy(isGhostCollarEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentCyan,
                                uncheckedTrackColor = StudioSurfaceVariant
                            ),
                            modifier = Modifier.testTag("toggle_ghost_collar")
                        )
                    }

                    if (options.isGhostCollarEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Collar Cavity Depth", color = TextTertiary, fontSize = 11.sp)
                                Text(
                                    text = "${(options.collarDepthMultiplier * 100).toInt()}%",
                                    color = AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = options.collarDepthMultiplier,
                                onValueChange = { newDepth ->
                                    viewModel.updateOptions(options.copy(collarDepthMultiplier = newDepth))
                                },
                                valueRange = 0.5f..1.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentCyan,
                                    activeTrackColor = AccentCyan,
                                    inactiveTrackColor = StudioSurfaceVariant
                                )
                            )
                        }

                        // Pinch-to-zoom & pan manual inner collar adjustment card
                        InnerNeckAdjuster(
                            compositedBitmap = state.compositedBitmap,
                            options = options,
                            onOptionsChanged = { newOpts ->
                                viewModel.updateOptions(newOpts)
                            }
                        )
                    }

                    HorizontalDivider(color = StudioCardBorder, thickness = 1.dp)

                    // 2. Realistic Studio Shadow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Studio Ground Shadow",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Adds soft contact & ambient floor shadow so garment looks standing",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = options.isShadowEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateOptions(options.copy(isShadowEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentCyan,
                                uncheckedTrackColor = StudioSurfaceVariant
                            ),
                            modifier = Modifier.testTag("toggle_shadow")
                        )
                    }

                    if (options.isShadowEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Shadow Opacity & Spread", color = TextTertiary, fontSize = 11.sp)
                                Text(
                                    text = "${(options.shadowIntensity * 100).toInt()}%",
                                    color = AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = options.shadowIntensity,
                                onValueChange = { newIntensity ->
                                    viewModel.updateOptions(options.copy(shadowIntensity = newIntensity))
                                },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentCyan,
                                    activeTrackColor = AccentCyan,
                                    inactiveTrackColor = StudioSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = StudioCardBorder, thickness = 1.dp)

                    // 3. Image Enhancement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Image Enhancement",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Auto contrast, brightness balance, and unsharp fabric sharpening",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = options.isAutoEnhanceEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateOptions(options.copy(isAutoEnhanceEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentCyan,
                                uncheckedTrackColor = StudioSurfaceVariant
                            ),
                            modifier = Modifier.testTag("toggle_enhancement")
                        )
                    }
                }
            }

            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_new_garment")
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "New Garment", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_export_dialog")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Export Image", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Export format dialog (PNG Transparent vs JPG White)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = StudioSurface,
            title = {
                Text(text = "Export HD Studio Asset", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose your desired export format for Shopify, Amazon, or social media:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.saveHD(context, Bitmap.CompressFormat.PNG) { _, _ -> }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = AccentCyan)
                            Column {
                                Text(text = "PNG HD (Lossless)", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = if (options.backgroundType.isTransparent) "Preserves transparent cutout" else "Crisp lossless RGB output",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.saveHD(context, Bitmap.CompressFormat.JPEG) { _, _ -> }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Collections, contentDescription = null, tint = AccentPurple)
                            Column {
                                Text(text = "JPG HD (Standard Web)", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = "Ideal for Amazon / e-commerce web listing",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(text = "Cancel", color = TextTertiary)
                }
            }
        )
    }
}

@Composable
fun BackgroundOptionButton(
    type: BackgroundType,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentCyan.copy(alpha = 0.15f) else StudioSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) AccentCyan else StudioCardBorder
        ),
        modifier = modifier
            .height(72.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (type.isTransparent) Color.DarkGray else Color(type.colorHex ?: 0xFFFFFFFF)
                    )
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type.title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AccentCyan else TextSecondary,
                maxLines = 1
            )
        }
    }
}
