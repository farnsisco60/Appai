package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processor.GhostStudioOptions
import com.example.ui.theme.*

@Composable
fun InnerNeckAdjuster(
    compositedBitmap: Bitmap,
    options: GhostStudioOptions,
    onOptionsChanged: (GhostStudioOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember(options.collarOffsetX) { mutableStateOf(options.collarOffsetX) }
    var offsetY by remember(options.collarOffsetY) { mutableStateOf(options.collarOffsetY) }
    var scale by remember(options.collarScale) { mutableStateOf(options.collarScale) }

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Manual Inner Neck Fitting",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PINCH & PAN",
                                color = AccentCyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Pinch with 2 fingers to zoom lining, drag to align collar",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Reset adjustments button
                IconButton(
                    onClick = {
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1.0f
                        onOptionsChanged(
                            options.copy(
                                collarOffsetX = 0f,
                                collarOffsetY = 0f,
                                collarScale = 1.0f
                            )
                        )
                    },
                    modifier = Modifier.testTag("btn_reset_collar_adjust")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset collar position",
                        tint = AccentCyan
                    )
                }
            }

            // Interactive Gesture Target View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioDarkBg)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.6f, 2.0f)
                            val newOffsetX = (offsetX + pan.x * 0.8f).coerceIn(-120f, 120f)
                            val newOffsetY = (offsetY + pan.y * 0.8f).coerceIn(-120f, 120f)

                            scale = newScale
                            offsetX = newOffsetX
                            offsetY = newOffsetY

                            onOptionsChanged(
                                options.copy(
                                    collarOffsetX = newOffsetX,
                                    collarOffsetY = newOffsetY,
                                    collarScale = newScale
                                )
                            )
                        }
                    }
                    .testTag("neck_adjust_gesture_area"),
                contentAlignment = Alignment.Center
            ) {
                // Live preview image with current adjustments
                val imageBitmap = remember(compositedBitmap) { compositedBitmap.asImageBitmap() }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Collar Fitting Preview",
                    modifier = Modifier.fillMaxSize()
                )

                // Visual crosshair guide at collar alignment center
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, AccentCyan.copy(alpha = 0.8f), CircleShape)
                )

                // Gesture hint overlay badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Zoom: ${(scale * 100).toInt()}% • X: ${offsetX.toInt()}px • Y: ${offsetY.toInt()}px",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Fine-Tuning Sliders for precise 1-pixel micro adjustment
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Scale Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Inner Part Size (Scale)", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = scale,
                    onValueChange = { newScale ->
                        scale = newScale
                        onOptionsChanged(options.copy(collarScale = newScale))
                    },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("slider_collar_scale")
                )

                // Vertical Position Y Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Vertical Alignment (Y Offset)", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = "${offsetY.toInt()} px",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = offsetY,
                    onValueChange = { newY ->
                        offsetY = newY
                        onOptionsChanged(options.copy(collarOffsetY = newY))
                    },
                    valueRange = -80f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("slider_collar_offset_y")
                )

                // Horizontal Position X Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Horizontal Alignment (X Offset)", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = "${offsetX.toInt()} px",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = offsetX,
                    onValueChange = { newX ->
                        offsetX = newX
                        onOptionsChanged(options.copy(collarOffsetX = newX))
                    },
                    valueRange = -80f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("slider_collar_offset_x")
                )
            }
        }
    }
}
