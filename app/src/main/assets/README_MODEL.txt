===================================================================
GHOST STUDIO - TENSORFLOW LITE MODEL INSTRUCTIONS
===================================================================

Where to put your model:
Path: app/src/main/assets/u2net.tflite

How it works:
1. Ghost Studio comes with an offline dual-engine pipeline:
   - Google ML Kit Selfie/Subject Segmentation (Free & 100% Offline, runs out of the box)
   - TensorFlow Lite U2-Net / RMBG model engine (Loads u2net.tflite from app/src/main/assets/)

2. To use a custom U2-Net model:
   - Download or export a quantized U2-Net or U2-NetP TFLite model (e.g., 320x320 or 512x512 input).
   - Rename the file to: u2net.tflite
   - Overwrite the placeholder file located at:
     app/src/main/assets/u2net.tflite

3. Automatic Fallback:
   If `u2net.tflite` is not a binary model or is less than 1MB, Ghost Studio
   automatically runs Google ML Kit's native on-device segmenter with edge feathering
   and garment refinement. Both engines are 100% free and work with NO internet connection.
===================================================================
