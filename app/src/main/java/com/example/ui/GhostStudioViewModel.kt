package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.processor.*
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GhostUiState {
    object Idle : GhostUiState
    data class Processing(val status: String, val stepIndex: Int) : GhostUiState
    data class Ready(
        val originalBitmap: Bitmap,
        val isolatedForeground: Bitmap,
        val compositedBitmap: Bitmap,
        val options: GhostStudioOptions,
        val processingTimeMs: Long,
        val mlEngine: String
    ) : GhostUiState
    data class BatchProcessing(
        val items: List<BatchItem>,
        val currentProcessingIndex: Int,
        val completedCount: Int,
        val totalCount: Int,
        val isFinished: Boolean
    ) : GhostUiState
    data class Error(val message: String) : GhostUiState
}

class GhostStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GhostUiState>(GhostUiState.Idle)
    val uiState: StateFlow<GhostUiState> = _uiState.asStateFlow()

    private val _lastSavedMessage = MutableStateFlow<String?>(null)
    val lastSavedMessage: StateFlow<String?> = _lastSavedMessage.asStateFlow()

    fun processGarmentBitmap(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = GhostUiState.Processing("Removing background offline with AI…", 1)

                val defaultOptions = GhostStudioOptions()
                val result = ImageProcessor.processGarment(bitmap, defaultOptions, context)

                _uiState.value = GhostUiState.Ready(
                    originalBitmap = result.originalBitmap,
                    isolatedForeground = result.isolatedForeground,
                    compositedBitmap = result.finalProcessedBitmap,
                    options = defaultOptions,
                    processingTimeMs = result.executionTimeMs,
                    mlEngine = result.engineUsed
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = GhostUiState.Error(
                    "Processing failed: ${e.localizedMessage ?: "Unknown error occurred"}"
                )
            }
        }
    }

    fun loadSampleGarment(context: Context) {
        val sampleBitmap = ImageUtils.loadBitmapFromResource(context, R.drawable.sample_jacket_flatlay)
        if (sampleBitmap != null) {
            processGarmentBitmap(sampleBitmap, context)
        } else {
            _uiState.value = GhostUiState.Error("Failed to load sample garment asset.")
        }
    }

    fun updateOptions(newOptions: GhostStudioOptions) {
        val currentState = _uiState.value
        if (currentState is GhostUiState.Ready) {
            viewModelScope.launch(Dispatchers.Default) {
                val updatedComposite = ImageProcessor.renderCompositedResult(
                    currentState.isolatedForeground,
                    newOptions
                )
                withContext(Dispatchers.Main) {
                    _uiState.value = currentState.copy(
                        options = newOptions,
                        compositedBitmap = updatedComposite
                    )
                }
            }
        }
    }

    fun saveHD(context: Context, format: Bitmap.CompressFormat, onResult: (Boolean, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState is GhostUiState.Ready) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = ImageUtils.saveBitmapToGallery(
                    context,
                    currentState.compositedBitmap,
                    format,
                    "GhostStudio"
                )
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { uri ->
                            val msg = "Saved HD ${if (format == Bitmap.CompressFormat.PNG) "PNG" else "JPG"} to Pictures/GhostStudio"
                            _lastSavedMessage.value = msg
                            onResult(true, msg)
                        },
                        onFailure = { err ->
                            val msg = "Failed to save: ${err.localizedMessage}"
                            _lastSavedMessage.value = msg
                            onResult(false, msg)
                        }
                    )
                }
            }
        }
    }

    fun shareCurrent(context: Context) {
        val currentState = _uiState.value
        if (currentState is GhostUiState.Ready) {
            ImageUtils.shareBitmap(context, currentState.compositedBitmap)
        }
    }

    fun clearSavedMessage() {
        _lastSavedMessage.value = null
    }

    fun resetToHome() {
        _uiState.value = GhostUiState.Idle
    }

    /**
     * Start batch processing up to 10 garment images.
     */
    fun startBatchProcessingUris(uris: List<android.net.Uri>, context: Context) {
        val selectedUris = uris.take(10)
        val initialItems = selectedUris.mapIndexed { index, uri ->
            BatchItem(
                id = "batch_${System.currentTimeMillis()}_$index",
                sourceUri = uri,
                filename = "Garment #${index + 1}",
                status = BatchItemStatus.QUEUED
            )
        }
        _uiState.value = GhostUiState.BatchProcessing(
            items = initialItems,
            currentProcessingIndex = 0,
            completedCount = 0,
            totalCount = initialItems.size,
            isFinished = false
        )
        processBatchQueue(initialItems, context)
    }

    /**
     * Generate demo batch of up to 10 sample garments for instant testing.
     */
    fun startDemoBatch(context: Context, count: Int = 10) {
        val baseSample = ImageUtils.loadBitmapFromResource(context, R.drawable.sample_jacket_flatlay)
        if (baseSample == null) {
            _uiState.value = GhostUiState.Error("Failed to load sample garment asset.")
            return
        }

        val colors = listOf(
            "Original Navy",
            "Charcoal Black",
            "Burgundy Wine",
            "Olive Green",
            "Camel Brown",
            "Cobalt Blue",
            "Slate Grey",
            "Crimson Red",
            "Forest Spruce",
            "Midnight Violet"
        ).take(count)

        val demoItems = colors.mapIndexed { index, name ->
            val tinted = ImageUtils.tintGarmentBitmap(baseSample, index)
            BatchItem(
                id = "demo_batch_$index",
                sourceBitmap = tinted,
                filename = name,
                status = BatchItemStatus.QUEUED
            )
        }

        _uiState.value = GhostUiState.BatchProcessing(
            items = demoItems,
            currentProcessingIndex = 0,
            completedCount = 0,
            totalCount = demoItems.size,
            isFinished = false
        )
        processBatchQueue(demoItems, context)
    }

    private fun processBatchQueue(items: List<BatchItem>, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val workingList = items.toMutableList()
            val options = GhostStudioOptions()

            for (i in workingList.indices) {
                // Update current item to PROCESSING
                workingList[i] = workingList[i].copy(status = BatchItemStatus.PROCESSING, progress = 0.2f)
                updateBatchState(workingList, currentIndex = i, isFinished = false)

                val bitmap = workingList[i].sourceBitmap ?: workingList[i].sourceUri?.let { uri ->
                    ImageUtils.loadBitmapFromUri(context, uri)
                }

                if (bitmap != null) {
                    try {
                        workingList[i] = workingList[i].copy(progress = 0.5f)
                        val result = ImageProcessor.processGarment(bitmap, options, context)
                        workingList[i] = workingList[i].copy(
                            status = BatchItemStatus.COMPLETED,
                            progress = 1.0f,
                            result = result
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        workingList[i] = workingList[i].copy(
                            status = BatchItemStatus.FAILED,
                            errorMessage = e.localizedMessage ?: "Processing error"
                        )
                    }
                } else {
                    workingList[i] = workingList[i].copy(
                        status = BatchItemStatus.FAILED,
                        errorMessage = "Could not load image"
                    )
                }

                updateBatchState(workingList, currentIndex = i + 1, isFinished = (i == workingList.size - 1))
            }
        }
    }

    private suspend fun updateBatchState(items: List<BatchItem>, currentIndex: Int, isFinished: Boolean) {
        withContext(Dispatchers.Main) {
            val completed = items.count { it.status == BatchItemStatus.COMPLETED }
            _uiState.value = GhostUiState.BatchProcessing(
                items = items.toList(),
                currentProcessingIndex = currentIndex,
                completedCount = completed,
                totalCount = items.size,
                isFinished = isFinished
            )
        }
    }

    fun openBatchItemInEditor(item: BatchItem) {
        val res = item.result ?: return
        _uiState.value = GhostUiState.Ready(
            originalBitmap = res.originalBitmap,
            isolatedForeground = res.isolatedForeground,
            compositedBitmap = res.finalProcessedBitmap,
            options = GhostStudioOptions(),
            processingTimeMs = res.executionTimeMs,
            mlEngine = res.engineUsed
        )
    }

    fun saveAllBatchCompleted(context: Context, format: Bitmap.CompressFormat, onDone: (Int) -> Unit) {
        val state = _uiState.value
        if (state is GhostUiState.BatchProcessing) {
            viewModelScope.launch(Dispatchers.IO) {
                var saved = 0
                state.items.filter { it.status == BatchItemStatus.COMPLETED }.forEach { item ->
                    item.result?.finalProcessedBitmap?.let { bmp ->
                        val res = ImageUtils.saveBitmapToGallery(
                            context,
                            bmp,
                            format,
                            "GhostStudio"
                        )
                        if (res.isSuccess) saved++
                    }
                }
                withContext(Dispatchers.Main) {
                    onDone(saved)
                }
            }
        }
    }
}
