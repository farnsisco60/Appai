package com.example.processor

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

object ImageProcessor {
    private const val TAG = "ImageProcessor"

    /**
     * Primary entry point to process a garment photo through all pipeline stages.
     */
    suspend fun processGarment(
        originalBitmap: Bitmap,
        options: GhostStudioOptions,
        context: Context
    ): ProcessingResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Remove background (ML Kit or U2Net)
        val (isolatedForeground, engineUsed) = removeBackgroundWithEngine(originalBitmap, context)

        // 2. Build final composite with chosen options
        val finalBitmap = renderCompositedResult(isolatedForeground, options)

        val totalTime = System.currentTimeMillis() - startTime
        ProcessingResult(
            originalBitmap = originalBitmap,
            isolatedForeground = isolatedForeground,
            finalProcessedBitmap = finalBitmap,
            executionTimeMs = totalTime,
            engineUsed = engineUsed
        )
    }

    /**
     * Re-renders the final composite from the already isolated foreground.
     * Fast for real-time sliders and toggles without re-running ML!
     */
    fun renderCompositedResult(
        isolatedForeground: Bitmap,
        options: GhostStudioOptions
    ): Bitmap {
        var result = isolatedForeground

        // Apply Ghost Mannequin 3D Hollow Neck Lining
        if (options.isGhostCollarEnabled) {
            result = applyGhostMannequin(result, options)
        }

        // Apply Studio Ground Shadows
        if (options.isShadowEnabled) {
            result = addShadow(result, Color.BLACK, 40f * options.shadowIntensity, 25f)
        }

        // Apply Image Enhancements (Brightness, Contrast, Sharpness)
        if (options.isAutoEnhanceEnabled) {
            result = applyEnhancement(
                result,
                options.brightnessBoost,
                options.contrastBoost,
                options.sharpnessBoost
            )
        }

        // Apply Background Color Replacement
        return compositeBackground(result, options.backgroundType)
    }

    /**
     * Step A: Background Removal (Free & 100% Offline)
     */
    suspend fun removeBackground(bitmap: Bitmap, context: Context): Bitmap {
        return removeBackgroundWithEngine(bitmap, context).first
    }

    private suspend fun removeBackgroundWithEngine(bitmap: Bitmap, context: Context): Pair<Bitmap, String> {
        // Check if custom TFLite U2-Net model is present in assets
        if (TFLiteHelper.checkAndInitModel(context)) {
            val u2netMask = TFLiteHelper.runU2Net(bitmap)
            if (u2netMask != null) {
                val isolated = applyMaskToBitmap(bitmap, u2netMask)
                return Pair(isolated, "TensorFlow Lite (U2-Net)")
            }
        }

        // Run Google ML Kit Selfie / Subject Segmenter (100% Offline)
        val isolated = runMLKitSegmentation(bitmap)
        return Pair(isolated, "Google ML Kit (Offline AI)")
    }

    private suspend fun runMLKitSegmentation(original: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val maxDim = 1200
        val scale = if (max(original.width, original.height) > maxDim) {
            maxDim.toFloat() / max(original.width, original.height)
        } else 1.0f

        val workingBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else {
            original.copy(Bitmap.Config.ARGB_8888, true)
        }

        val segmenterOptions = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        val segmenter = Segmentation.getClient(segmenterOptions)
        val inputImage = InputImage.fromBitmap(workingBitmap, 0)

        val segmentationMask = suspendCancellableCoroutine { continuation ->
            segmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    continuation.resume(mask)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }

        val maskWidth = segmentationMask.width
        val maskHeight = segmentationMask.height
        val maskBuffer: ByteBuffer = segmentationMask.buffer
        maskBuffer.rewind()

        // Extract confidence values and enhance with color/edge boundary refinement
        val maskPixels = IntArray(maskWidth * maskHeight)
        val workingPixels = IntArray(maskWidth * maskHeight)
        workingBitmap.getPixels(workingPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

        // Sample background corner color (typical floor/table color in flat-lays)
        val cornerColors = intArrayOf(
            workingPixels[0],
            workingPixels[maskWidth - 1],
            workingPixels[(maskHeight - 1) * maskWidth],
            workingPixels[(maskHeight - 1) * maskWidth + maskWidth - 1]
        )
        val bgAvgR = cornerColors.map { Color.red(it) }.average().toFloat()
        val bgAvgG = cornerColors.map { Color.green(it) }.average().toFloat()
        val bgAvgB = cornerColors.map { Color.blue(it) }.average().toFloat()

        for (i in 0 until (maskWidth * maskHeight)) {
            val confidence = maskBuffer.float
            val origColor = workingPixels[i]
            val r = Color.red(origColor)
            val g = Color.green(origColor)
            val b = Color.blue(origColor)

            val colorDiff = sqrt(
                (r - bgAvgR).pow(2) + (g - bgAvgG).pow(2) + (b - bgAvgB).pow(2)
            )

            // Dynamic blend: if ML Kit is confident, preserve. If on floor threshold, clean up
            val finalAlpha = when {
                confidence > 0.65f -> 255
                confidence < 0.25f -> 0
                colorDiff < 32f && confidence < 0.5f -> 0
                else -> {
                    val smoothNorm = (confidence - 0.25f) / 0.4f
                    (smoothNorm.coerceIn(0f, 1f) * 255).toInt()
                }
            }

            maskPixels[i] = Color.argb(finalAlpha, r, g, b)
        }

        val resultBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(maskPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

        // Rescale back to original dimensions if needed
        if (resultBitmap.width != original.width || resultBitmap.height != original.height) {
            Bitmap.createScaledBitmap(resultBitmap, original.width, original.height, true)
        } else {
            resultBitmap
        }
    }

    private fun applyMaskToBitmap(source: Bitmap, mask: Bitmap): Bitmap {
        val scaledMask = if (mask.width != source.width || mask.height != source.height) {
            Bitmap.createScaledBitmap(mask, source.width, source.height, true)
        } else mask

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawBitmap(source, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(scaledMask, 0f, 0f, paint)
        return output
    }

    /**
     * Step B: Ghost Mannequin Effect (Invisible Mannequin 3D Hollow Look)
     * Detects neckline/collar dip and synthesizes/fits the 3D inner neck lining.
     */
    fun applyGhostMannequin(bitmap: Bitmap, options: GhostStudioOptions): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Find collar and garment boundary coordinates
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        // Find garment bounding box
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = Color.alpha(pixels[y * width + x])
                if (alpha > 50) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX <= minX || maxY <= minY) return bitmap

        val garmentWidth = maxX - minX
        val garmentHeight = maxY - minY

        // Collar search zone: top 5% to 45% of garment
        val scanStartY = minY
        val scanEndY = min(height - 1, minY + (garmentHeight * 0.45f).toInt())
        val centerMinX = minX + (garmentWidth * 0.22f).toInt()
        val centerMaxX = minX + (garmentWidth * 0.78f).toInt()
        val spanWidth = centerMaxX - centerMinX
        if (spanWidth <= 10) return bitmap

        // Trace upper contour across neckline with edge detection and smoothing
        val rawProfile = IntArray(spanWidth) { scanEndY }
        for (x in centerMinX until centerMaxX) {
            val idx = x - centerMinX
            for (y in scanStartY..scanEndY) {
                val alpha = Color.alpha(pixels[y * width + x])
                if (alpha > 90) {
                    rawProfile[idx] = y
                    break
                }
            }
        }

        // Apply moving average smoothing (kernel radius 3) to eliminate frayed threads/artifacts
        val smoothProfile = FloatArray(spanWidth)
        val kernelR = 3
        for (i in 0 until spanWidth) {
            var sum = 0f
            var count = 0
            for (k in -kernelR..kernelR) {
                val p = (i + k).coerceIn(0, spanWidth - 1)
                sum += rawProfile[p]
                count++
            }
            smoothProfile[i] = sum / count
        }

        // Detect lowest point of the concave collar (maximum Y value in the central 60% of collar span)
        val innerMinIdx = (spanWidth * 0.20f).toInt()
        val innerMaxIdx = (spanWidth * 0.80f).toInt()
        var maxDipY = scanStartY.toFloat()
        var maxDipIdx = spanWidth / 2

        for (i in innerMinIdx..innerMaxIdx) {
            if (smoothProfile[i] > maxDipY) {
                maxDipY = smoothProfile[i]
                maxDipIdx = i
            }
        }
        val maxDipX = centerMinX + maxDipIdx

        // Detect Left Shoulder peak (minimum Y on the left side)
        var leftShoulderY = Float.MAX_VALUE
        var leftShoulderIdx = 0
        for (i in 0 until maxDipIdx) {
            if (smoothProfile[i] < leftShoulderY) {
                leftShoulderY = smoothProfile[i]
                leftShoulderIdx = i
            }
        }
        val leftShoulderX = centerMinX + leftShoulderIdx

        // Detect Right Shoulder peak (minimum Y on the right side)
        var rightShoulderY = Float.MAX_VALUE
        var rightShoulderIdx = spanWidth - 1
        for (i in maxDipIdx until spanWidth) {
            if (smoothProfile[i] < rightShoulderY) {
                rightShoulderY = smoothProfile[i]
                rightShoulderIdx = i
            }
        }
        val rightShoulderX = centerMinX + rightShoulderIdx

        // Natural collar opening calculations
        val avgShoulderY = (leftShoulderY + rightShoulderY) / 2f
        val naturalCollarHeight = max(maxDipY - avgShoulderY, garmentHeight * 0.09f)
        val collarOpeningHeight = naturalCollarHeight * options.collarDepthMultiplier * options.collarScale
        val neckWidth = ((rightShoulderX - leftShoulderX).toFloat() * options.collarScale)
            .coerceAtLeast(garmentWidth * 0.18f)

        // Apply interactive Pinch-to-Zoom / Pan manual offsets
        val adjustedCenterX = maxDipX.toFloat() + options.collarOffsetX
        val adjustedCenterY = maxDipY + options.collarOffsetY
        val adjustedLeftShoulderX = adjustedCenterX - (neckWidth / 2f)
        val adjustedRightShoulderX = adjustedCenterX + (neckWidth / 2f)
        val adjustedShoulderY = avgShoulderY + options.collarOffsetY

        // Sample garment fabric color near collar for authentic lining harmony
        var sampleR = 30
        var sampleG = 35
        var sampleB = 45
        val sampleY = min(height - 1, (maxDipY + 18).toInt())
        val samplePixel = pixels[sampleY * width + maxDipX]
        if (Color.alpha(samplePixel) > 100) {
            sampleR = Color.red(samplePixel)
            sampleG = Color.green(samplePixel)
            sampleB = Color.blue(samplePixel)
        }

        // Synthesize the 3D Inner Collar Hollow Layer
        val hollowLining = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val liningCanvas = Canvas(hollowLining)

        val liningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Base lining color: slightly deeper tone of the garment fabric
        val liningBaseColor = Color.rgb(
            (sampleR * 0.52f).toInt().coerceIn(12, 230),
            (sampleG * 0.52f).toInt().coerceIn(15, 230),
            (sampleB * 0.58f).toInt().coerceIn(20, 240)
        )
        val liningHighlightColor = Color.rgb(
            (sampleR * 0.78f).toInt().coerceIn(25, 245),
            (sampleG * 0.78f).toInt().coerceIn(30, 245),
            (sampleB * 0.82f).toInt().coerceIn(40, 250)
        )

        // Draw curved back collar arch
        val collarArchPath = Path()
        val archTopY = adjustedShoulderY - (collarOpeningHeight * 0.50f)
        val archBottomY = adjustedCenterY + (collarOpeningHeight * 0.38f)

        collarArchPath.moveTo(adjustedLeftShoulderX, adjustedShoulderY)
        // Upward back collar curve
        collarArchPath.cubicTo(
            adjustedLeftShoulderX + neckWidth * 0.25f, archTopY,
            adjustedRightShoulderX - neckWidth * 0.25f, archTopY,
            adjustedRightShoulderX, adjustedShoulderY
        )
        // Downward interior neckline curve
        collarArchPath.cubicTo(
            adjustedRightShoulderX - neckWidth * 0.2f, archBottomY,
            adjustedLeftShoulderX + neckWidth * 0.2f, archBottomY,
            adjustedLeftShoulderX, adjustedShoulderY
        )
        collarArchPath.close()

        // Gradient giving cylindrical 3D depth to interior neck cavity
        val liningShader = LinearGradient(
            adjustedCenterX, archTopY,
            adjustedCenterX, archBottomY,
            intArrayOf(liningHighlightColor, liningBaseColor, Color.BLACK),
            floatArrayOf(0.0f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )
        liningPaint.shader = liningShader
        liningCanvas.drawPath(collarArchPath, liningPaint)
        liningPaint.shader = null

        // Inner lining stitched seam arch
        val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(95, 255, 255, 255)
            strokeWidth = 2.5f * options.collarScale
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
        }
        val seamPath = Path()
        val seamTopY = archTopY + (collarOpeningHeight * 0.18f)
        seamPath.moveTo(adjustedLeftShoulderX + neckWidth * 0.1f, adjustedShoulderY + 5f)
        seamPath.cubicTo(
            adjustedLeftShoulderX + neckWidth * 0.3f, seamTopY,
            adjustedRightShoulderX - neckWidth * 0.3f, seamTopY,
            adjustedRightShoulderX - neckWidth * 0.1f, adjustedShoulderY + 5f
        )
        liningCanvas.drawPath(seamPath, stitchPaint)

        // Authentic Designer Brand Label Tag in inner back collar
        val tagWidth = neckWidth * 0.28f
        val tagHeight = collarOpeningHeight * 0.32f
        val tagLeft = adjustedCenterX - tagWidth / 2f
        val tagTop = archTopY + (collarOpeningHeight * 0.22f)
        val tagRect = RectF(tagLeft, tagTop, tagLeft + tagWidth, tagTop + tagHeight)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 245, 245, 248)
            style = Paint.Style.FILL
        }
        liningCanvas.drawRoundRect(tagRect, 4f, 4f, tagPaint)

        val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = max(9f, tagHeight * 0.42f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        liningCanvas.drawText("STUDIO • M", adjustedCenterX, tagTop + tagHeight * 0.68f, tagTextPaint)

        // Drop shadow from front collar/lapel onto the inner lining for 3D realism
        val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(140, 0, 0, 0)
            strokeWidth = 14f * options.collarScale
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        liningCanvas.drawPath(collarArchPath, innerShadowPaint)

        // Composite: Draw the synthesized inner collar behind the original front garment!
        val compositeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(compositeBitmap)
        finalCanvas.drawBitmap(hollowLining, 0f, 0f, null)
        finalCanvas.drawBitmap(bitmap, 0f, 0f, null)

        return compositeBitmap
    }

    /**
     * Step B (continued): Add realistic studio floor shadow
     * Adds soft contact shadow and ambient ground shadow beneath the garment hem.
     */
    fun addShadow(
        garmentBitmap: Bitmap,
        shadowColor: Int = Color.BLACK,
        blurRadius: Float = 35f,
        offsetY: Float = 20f
    ): Bitmap {
        val width = garmentBitmap.width
        val height = garmentBitmap.height

        val pixels = IntArray(width * height)
        garmentBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = 0
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = Color.alpha(pixels[y * width + x])
                if (alpha > 50) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX <= minX || maxY == 0) return garmentBitmap

        val garmentWidth = (maxX - minX).toFloat()
        val centerX = (minX + maxX) / 2f
        val shadowGroundY = min(height - 15f, maxY + offsetY)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Ambient soft oval shadow (wider and diffused)
        val ambientShadowWidth = garmentWidth * 0.88f
        val ambientShadowHeight = max(24f, garmentWidth * 0.12f)
        val ambientRect = RectF(
            centerX - ambientShadowWidth / 2f,
            shadowGroundY - ambientShadowHeight * 0.3f,
            centerX + ambientShadowWidth / 2f,
            shadowGroundY + ambientShadowHeight * 0.7f
        )
        val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(85, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
            maskFilter = BlurMaskFilter(blurRadius * 0.9f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawOval(ambientRect, ambientPaint)

        // 2. Contact shadow (darker, tighter directly at garment bottom hem)
        val contactShadowWidth = garmentWidth * 0.65f
        val contactShadowHeight = max(12f, garmentWidth * 0.05f)
        val contactRect = RectF(
            centerX - contactShadowWidth / 2f,
            maxY - contactShadowHeight * 0.2f,
            centerX + contactShadowWidth / 2f,
            maxY + contactShadowHeight * 0.8f
        )
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(160, 0, 0, 0)
            maskFilter = BlurMaskFilter(blurRadius * 0.35f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawOval(contactRect, contactPaint)

        // 3. Draw garment on top of shadows
        canvas.drawBitmap(garmentBitmap, 0f, 0f, null)
        return output
    }

    /**
     * Step C: Background Replacement
     */
    fun compositeBackground(garmentBitmap: Bitmap, backgroundType: BackgroundType): Bitmap {
        if (backgroundType.isTransparent) {
            return garmentBitmap
        }

        val width = garmentBitmap.width
        val height = garmentBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val bgColor = backgroundType.colorHex?.toInt() ?: Color.WHITE
        canvas.drawColor(bgColor)
        canvas.drawBitmap(garmentBitmap, 0f, 0f, null)
        return output
    }

    /**
     * Step D: Image Enhancement (Auto brightness, contrast, and sharpness)
     */
    fun applyEnhancement(
        bitmap: Bitmap,
        brightness: Float = 1.05f,
        contrast: Float = 1.12f,
        sharpness: Float = 1.25f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // ColorMatrix for Brightness and Contrast
        // Contrast formula: c * (pixel - 128) + 128 + b
        val c = contrast
        val b = (brightness - 1.0f) * 255f
        val translate = (-0.5f * c + 0.5f) * 255f + b

        val matrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Apply edge sharpness boost if enabled
        return if (sharpness > 1.05f) {
            applySharpnessKernel(output, sharpness)
        } else {
            output
        }
    }

    private fun applySharpnessKernel(bitmap: Bitmap, sharpness: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val sharpenedPixels = IntArray(width * height)
        System.arraycopy(pixels, 0, sharpenedPixels, 0, pixels.size)

        val factor = (sharpness - 1.0f).coerceIn(0.1f, 0.8f)
        val centerWeight = 1.0f + 4.0f * factor

        for (y in 1 until (height - 1)) {
            val yOffset = y * width
            for (x in 1 until (width - 1)) {
                val center = pixels[yOffset + x]
                val alpha = Color.alpha(center)
                if (alpha == 0) continue

                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[yOffset + x - 1]
                val right = pixels[yOffset + x + 1]

                val r = (centerWeight * Color.red(center) - factor * (Color.red(top) + Color.red(bottom) + Color.red(left) + Color.red(right))).toInt().coerceIn(0, 255)
                val g = (centerWeight * Color.green(center) - factor * (Color.green(top) + Color.green(bottom) + Color.green(left) + Color.green(right))).toInt().coerceIn(0, 255)
                val b = (centerWeight * Color.blue(center) - factor * (Color.blue(top) + Color.blue(bottom) + Color.blue(left) + Color.blue(right))).toInt().coerceIn(0, 255)

                sharpenedPixels[yOffset + x] = Color.argb(alpha, r, g, b)
            }
        }

        val sharpened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(sharpenedPixels, 0, width, 0, 0, width, height)
        return sharpened
    }
}
