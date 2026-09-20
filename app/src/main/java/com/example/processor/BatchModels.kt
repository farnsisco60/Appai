package com.example.processor

import android.graphics.Bitmap
import android.net.Uri

data class BatchItem(
    val id: String,
    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val filename: String = "Garment",
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val progress: Float = 0f,
    val result: ProcessingResult? = null,
    val errorMessage: String? = null
)

enum class BatchItemStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
