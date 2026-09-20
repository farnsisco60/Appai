package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processor.BatchItem
import com.example.processor.BatchItemStatus
import com.example.ui.GhostStudioViewModel
import com.example.ui.GhostUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    viewModel: GhostStudioViewModel,
    batchState: GhostUiState.BatchProcessing,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Batch Studio Pipeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${batchState.completedCount} of ${batchState.totalCount} garments transformed",
                            fontSize = 11.sp,
                            color = AccentCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("batch_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (batchState.completedCount > 0) {
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("btn_export_all_batch")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Save All (${batchState.completedCount})",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
            )
        },
        containerColor = StudioDarkBg,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overall Batch Progress Banner
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (batchState.isFinished) "BATCH PROCESSING COMPLETE" else "PROCESSING BATCH OF 10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = if (batchState.isFinished) AccentCyan else AccentPurple
                        )
                        Text(
                            text = "${((batchState.completedCount.toFloat() / batchState.totalCount.coerceAtLeast(1)) * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (batchState.totalCount > 0) {
                                batchState.completedCount.toFloat() / batchState.totalCount
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentCyan,
                        trackColor = StudioSurfaceVariant
                    )

                    Text(
                        text = if (batchState.isFinished) {
                            "All ${batchState.completedCount} garments processed with offline ghost mannequin & studio floor shadows. Tap any item to inspect in Full Editor."
                        } else {
                            "Offline AI background cutout + 3D hollow lining synthesis running sequentially…"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(batchState.items) { index, item ->
                    BatchItemCard(
                        index = index,
                        item = item,
                        onInspect = {
                            viewModel.openBatchItemInEditor(item)
                        }
                    )
                }
            }
        }
    }

    // Batch Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = StudioSurface,
            title = {
                Text(
                    text = "Save All Batch Results",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Export all ${batchState.completedCount} processed ghost mannequin garments to device gallery:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.saveAllBatchCompleted(context, Bitmap.CompressFormat.PNG) { count ->
                                    Toast.makeText(
                                        context,
                                        "Saved $count garments as PNG to Pictures/GhostStudio",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = AccentCyan)
                            Column {
                                Text(
                                    text = "PNG Format (Transparent / White)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Lossless quality for e-commerce stores",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.saveAllBatchCompleted(context, Bitmap.CompressFormat.JPEG) { count ->
                                    Toast.makeText(
                                        context,
                                        "Saved $count garments as JPG to Pictures/GhostStudio",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = AccentPurple)
                            Column {
                                Text(
                                    text = "JPG Format (Pure Studio White)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Lightweight for Amazon & web listings",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = TextTertiary)
                }
            }
        )
    }
}

@Composable
fun BatchItemCard(
    index: Int,
    item: BatchItem,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.status == BatchItemStatus.PROCESSING) AccentCyan else StudioCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.status == BatchItemStatus.COMPLETED) { onInspect() }
            .testTag("batch_item_$index")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail Preview
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioDarkBg)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val previewBitmap = item.result?.finalProcessedBitmap ?: item.sourceBitmap
                if (previewBitmap != null) {
                    val imgBmp = remember(previewBitmap) { previewBitmap.asImageBitmap() }
                    Image(
                        bitmap = imgBmp,
                        contentDescription = "Batch thumbnail",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info & Status
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.filename,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "#${index + 1}",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                when (item.status) {
                    BatchItemStatus.QUEUED -> {
                        Text(
                            text = "Waiting in queue…",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    BatchItemStatus.PROCESSING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = AccentCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Isolating & fitting hollow collar…",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    BatchItemStatus.COMPLETED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Ready (${item.result?.executionTimeMs ?: 0}ms) • Tap to edit",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp
                            )
                        }
                    }
                    BatchItemStatus.FAILED -> {
                        Text(
                            text = "Failed: ${item.errorMessage ?: "Unknown"}",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Action Chevron / Icon
            if (item.status == BatchItemStatus.COMPLETED) {
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit Garment",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
