package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CheckerboardDark
import com.example.ui.theme.CheckerboardLight
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    isTransparentBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    val animatedPosition by animateFloatAsState(targetValue = sliderPosition, label = "slider")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F111A))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                    sliderPosition = newPos
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newPos = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                    sliderPosition = newPos
                }
            }
            .testTag("before_after_slider"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val beforeImageBitmap = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
        val afterImageBitmap = remember(afterBitmap) { afterBitmap.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val splitX = width * animatedPosition

            // Draw checkerboard if transparent mode
            if (isTransparentBackground) {
                val squareSize = 20.dp.toPx()
                val cols = (width / squareSize).toInt() + 1
                val rows = (height / squareSize).toInt() + 1
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val color = if ((r + c) % 2 == 0) CheckerboardLight else CheckerboardDark
                        drawRect(
                            color = color,
                            topLeft = Offset(c * squareSize, r * squareSize),
                            size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // Draw "After" (Ghost 3D Mannequin) on the right side
            clipRect(left = 0f, top = 0f, right = width, bottom = height) {
                drawImage(
                    image = afterImageBitmap,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width.roundToInt(), height.roundToInt())
                )
            }

            // Draw "Before" (Original Flat-Lay) clipped to the left of the divider
            clipRect(left = 0f, top = 0f, right = splitX, bottom = height) {
                drawImage(
                    image = beforeImageBitmap,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width.roundToInt(), height.roundToInt())
                )
            }

            // Divider vertical line
            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, height),
                strokeWidth = 3.dp.toPx()
            )
            // Accent glow line
            drawLine(
                color = AccentCyan.copy(alpha = 0.6f),
                start = Offset(splitX, 0f),
                end = Offset(splitX, height),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Circular draggable handle in the center of divider
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val splitX = (maxWidth.toPx() * animatedPosition) - 18.dp.toPx()
                    IntOffset(splitX.roundToInt(), 0)
                }
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentCyan)
                .testTag("slider_handle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Slide to compare before and after",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        // "Original Flat-Lay" Badge on Top Left
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "BEFORE (FLAT-LAY)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // "Ghost Mannequin 3D" Badge on Top Right
        Surface(
            color = AccentCyan.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                text = "AFTER (GHOST 3D)",
                color = Color(0xFF04201A),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
