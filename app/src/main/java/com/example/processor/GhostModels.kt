package com.example.processor

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

enum class BackgroundType(val title: String, val colorHex: Long?, val isTransparent: Boolean = false) {
    PURE_WHITE("Pure White", 0xFFFFFFFF),
    TRANSPARENT("Transparent", null, isTransparent = true),
    LIGHT_GRAY("Light Gray", 0xFFF3F4F6),
    DARK_SLATE("Dark Slate", 0xFF161822);

    fun getComposeColor(): Color {
        return colorHex?.let { Color(it) } ?: Color.Transparent
    }
}

data class GhostStudioOptions(
    val backgroundType: BackgroundType = BackgroundType.PURE_WHITE,
    val isGhostCollarEnabled: Boolean = true,
    val collarDepthMultiplier: Float = 1.0f,
    val collarOffsetX: Float = 0f,
    val collarOffsetY: Float = 0f,
    val collarScale: Float = 1.0f,
    val isShadowEnabled: Boolean = true,
    val shadowIntensity: Float = 0.55f,
    val isAutoEnhanceEnabled: Boolean = true,
    val brightnessBoost: Float = 1.05f,
    val contrastBoost: Float = 1.12f,
    val sharpnessBoost: Float = 1.25f
)

data class ProcessingResult(
    val originalBitmap: Bitmap,
    val isolatedForeground: Bitmap,
    val finalProcessedBitmap: Bitmap,
    val executionTimeMs: Long,
    val engineUsed: String
)
