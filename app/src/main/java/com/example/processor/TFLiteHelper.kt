package com.example.processor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object TFLiteHelper {
    private const val TAG = "GhostTFLite"
    private const val MODEL_FILENAME = "u2net.tflite"

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private var isPlaceholder = true

    fun checkAndInitModel(context: Context): Boolean {
        if (isModelLoaded) return !isPlaceholder
        try {
            val assetManager = context.assets
            val list = assetManager.list("") ?: emptyArray()
            if (MODEL_FILENAME !in list) {
                Log.d(TAG, "No $MODEL_FILENAME found in assets.")
                isPlaceholder = true
                isModelLoaded = true
                return false
            }

            val afd = assetManager.openFd(MODEL_FILENAME)
            val fileLength = afd.length
            // A real quantized U2-Net / U2-NetP model is typically > 4MB (4,000,000 bytes)
            if (fileLength < 500_000) {
                Log.i(TAG, "$MODEL_FILENAME is a placeholder (${fileLength} bytes). Using Google ML Kit engine.")
                afd.close()
                isPlaceholder = true
                isModelLoaded = true
                return false
            }

            val inputStream = FileInputStream(afd.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            isPlaceholder = false
            isModelLoaded = true
            Log.i(TAG, "Successfully loaded $MODEL_FILENAME with native TFLite Interpreter!")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize TFLite interpreter for $MODEL_FILENAME: ${e.message}. Defaulting to ML Kit.")
            isPlaceholder = true
            isModelLoaded = true
            return false
        }
    }

    fun isCustomModelActive(): Boolean {
        return !isPlaceholder && interpreter != null
    }

    fun runU2Net(inputBitmap: Bitmap): Bitmap? {
        val tflite = interpreter ?: return null
        return try {
            val inputSize = 320
            val scaledBitmap = Bitmap.createScaledBitmap(inputBitmap, inputSize, inputSize, true)
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(inputSize * inputSize)
            scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
            for (pixel in intValues) {
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }

            val outputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 1 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            tflite.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            val maskBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ALPHA_8)
            val maskPixels = ByteArray(inputSize * inputSize)
            for (i in 0 until (inputSize * inputSize)) {
                val prob = outputBuffer.float
                maskPixels[i] = (prob.coerceIn(0f, 1f) * 255f).toInt().toByte()
            }
            maskBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(maskPixels))
            maskBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error executing U2Net inference: ${e.message}")
            null
        }
    }
}
