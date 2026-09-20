package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.GhostStudioViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioDarkBg
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: GhostStudioViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        CameraPermissionDeniedView(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onNavigateBack = onNavigateBack
        )
    } else {
        CameraCaptureView(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}

@Composable
fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Camera Permission Needed",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "To take flat-lay photos of clothing and create invisible mannequin renders, Ghost Studio requires access to your camera.",
                fontSize = 14.sp,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_camera_permission")
            ) {
                Text(text = "Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onNavigateBack) {
                Text(text = "Go Back", color = Color.Gray)
            }
        }
    }
}

@Composable
fun CameraCaptureView(
    viewModel: GhostStudioViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Viewfinder
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = {
                imageCapture?.flashMode = flashMode
            }
        )

        // Flat-lay Garment Alignment Overlay
        GarmentAlignmentGuideOverlay(modifier = Modifier.fillMaxSize())

        // Top Controls: Back, Flash, Guidelines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentCyan)
                    )
                    Text(
                        text = "FLAT-LAY OVERHEAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = {
                    flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_ON) AccentCyan else Color.White
                )
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, AccentCyan, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) AccentCyan else Color.White)
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                takePhoto(
                                    imageCapture = imageCapture,
                                    context = context,
                                    onSuccess = { bitmap ->
                                        isCapturing = false
                                        viewModel.processGarmentBitmap(bitmap, context)
                                        onNavigateBack()
                                    },
                                    onError = {
                                        isCapturing = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                // Switch Camera
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Visual wireframe overlay showing clothing outline and collar alignment target.
 */
@Composable
fun GarmentAlignmentGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val stroke = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
        )
        val guideColor = AccentCyan.copy(alpha = 0.55f)

        // Torso and shoulder outline guide
        val path = Path()
        val centerX = width / 2f
        val shoulderY = height * 0.22f
        val collarDipY = height * 0.32f
        val waistY = height * 0.76f

        val shoulderHalfWidth = width * 0.35f
        val neckHalfWidth = width * 0.14f
        val waistHalfWidth = width * 0.32f

        // Neckline concave dip
        path.moveTo(centerX - neckHalfWidth, shoulderY + 8f)
        path.quadraticTo(centerX, collarDipY, centerX + neckHalfWidth, shoulderY + 8f)

        // Right shoulder
        path.lineTo(centerX + shoulderHalfWidth, shoulderY + 30f)
        // Right side
        path.lineTo(centerX + waistHalfWidth, waistY)
        // Bottom hem
        path.quadraticTo(centerX, waistY + 18f, centerX - waistHalfWidth, waistY)
        // Left side
        path.lineTo(centerX - shoulderHalfWidth, shoulderY + 30f)
        // Left shoulder
        path.lineTo(centerX - neckHalfWidth, shoulderY + 8f)

        drawPath(path, guideColor, style = stroke)

        // Center crosshair marker at collar
        drawLine(
            color = AccentCyan.copy(alpha = 0.8f),
            start = Offset(centerX - 16.dp.toPx(), collarDipY),
            end = Offset(centerX + 16.dp.toPx(), collarDipY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = AccentCyan.copy(alpha = 0.8f),
            start = Offset(centerX, collarDipY - 16.dp.toPx()),
            end = Offset(centerX, collarDipY + 16.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    val capture = imageCapture ?: return
    val executor = ContextCompat.getMainExecutor(context)

    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(imageProxy: ImageProxy) {
            try {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                } else {
                    rawBitmap
                }

                imageProxy.close()
                onSuccess(rotatedBitmap)
            } catch (e: Exception) {
                imageProxy.close()
                onError(e)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            onError(exception)
        }
    })
}
