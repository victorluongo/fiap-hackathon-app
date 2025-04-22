package com.fiap.cutwatch.domain.ia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.fiap.cutwatch.domain.model.Detection
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

 
class LocalObjectDetector(private val context: Context) {

    // Inicializa o intérprete com o modelo "best_float32.tflite"
    private val interpreter = Interpreter(
        loadModelFile(context, "best_float32.tflite"),
        Interpreter.Options().apply {
            setNumThreads(4)
            setUseXNNPACK(true)
        }
    )

    /**
     * Carrega o modelo a partir dos assets e retorna um MappedByteBuffer.
     */
    private fun loadModelFile(ctx: Context, modelName: String): MappedByteBuffer {
        val fd = ctx.assets.openFd(modelName)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }
    /**
     * Roda o modelo, retorna lista de Detection (RectF em pixels + score).
     */
    fun detectWithBoxes(
        bitmap: Bitmap,
        confThreshold: Float = 0.25f,
        areaThreshold: Float = 0.01f
    ): List<Detection> {
        val inputSize = 640
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(scaled)
        val output = Array(1) { Array(5) { FloatArray(8400) } }
        interpreter.run(inputBuffer, output)

        val iw = bitmap.width.toFloat()
        val ih = bitmap.height.toFloat()
        val detections = mutableListOf<Detection>()

        for (i in 0 until 8400) {
            val cx = output[0][0][i]
            val cy = output[0][1][i]
            val w  = output[0][2][i]
            val h  = output[0][3][i]
            val score = output[0][4][i]
            if (score >= confThreshold && w*h >= areaThreshold) {
                val left   = (cx - w/2f) * iw
                val top    = (cy - h/2f) * ih
                val right  = (cx + w/2f) * iw
                val bottom = (cy + h/2f) * ih
                detections += Detection(RectF(left, top, right, bottom), score)
            }
        }
        Log.d("LocalObjectDetector", "detectWithBoxes: ${detections.size} encontrados")
        return detections
    }

    fun detect(bitmap: Bitmap): Boolean =
        detectWithBoxes(bitmap).isNotEmpty()

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val size = bitmap.width
        val buf = ByteBuffer.allocateDirect(4 * size * size * 3).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(size*size)
        bitmap.getPixels(pixels,0,size,0,0,size,size)
        pixels.forEach { px ->
            val r = ((px shr 16 and 0xFF)/255f - 0.5f)/0.5f
            val g = ((px shr  8 and 0xFF)/255f - 0.5f)/0.5f
            val b = ((px       and 0xFF)/255f - 0.5f)/0.5f
            buf.putFloat(r).putFloat(g).putFloat(b)
        }
        buf.rewind()
        return buf
    }
}