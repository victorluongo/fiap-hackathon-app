package com.fiap.cutwatch.view.video

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fiap.cutwatch.databinding.FragmentVideoProcessingBinding
import com.fiap.cutwatch.domain.ia.LocalObjectDetector
import com.fiap.cutwatch.domain.model.DetectedFrame
import com.fiap.cutwatch.view.adapter.DetectedFrameAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class VideoProcessingFragment : Fragment() {

    private var _binding: FragmentVideoProcessingBinding? = null
    private val binding get() = _binding!!
    private lateinit var detector: LocalObjectDetector
    private val detectedFrames = mutableListOf<DetectedFrame>()
    private lateinit var adapter: DetectedFrameAdapter

    private val REQUEST_VIDEO_GET = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detector = LocalObjectDetector(requireContext())

        // Configura o RecyclerView com layout horizontal
        adapter = DetectedFrameAdapter(detectedFrames)
        binding.rvDetectedFrames.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvDetectedFrames.adapter = adapter

        binding.btnSelectVideo.setOnClickListener {
            selectVideoFromGallery()
        }

        binding.switchViewMode.setOnCheckedChangeListener { _, isChecked ->
            binding.rvDetectedFrames.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun selectVideoFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" }
        startActivityForResult(intent, REQUEST_VIDEO_GET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let { videoUri ->
                processVideo(videoUri)
            }
        }
    }

    private fun processVideo(videoUri: Uri) {
        binding.tvVideoStatus.text = "Processando vídeo..."
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(requireContext(), videoUri)
        } catch (e: Exception) {
            binding.tvVideoStatus.text = "Erro ao carregar vídeo."
            return
        }

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val intervalMs = 500L   // 0,5 segundo por frame

        // Lista para manter os últimos 5 frames para filtragem de duplicatas
        val recentFrames = mutableListOf<Bitmap>()
        detectedFrames.clear()

        var totalFramesProcessados = 0L
        var totalFramesDetectados = 0L

        CoroutineScope(Dispatchers.IO).launch {
            val totalFrames = durationMs / intervalMs
            for (timeMs in 0L until durationMs step intervalMs) {
                totalFramesProcessados++
                Log.d("FrameOrder", "Processando frame no tempo: ${timeMs} ms")
                // Obtém o frame (tempo em microsegundos)
                val frameBitmap: Bitmap? = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frameBitmap != null) {
                    val detected = detector.detect(frameBitmap)
                    if (detected) {
                        totalFramesDetectados++
                        // Salva para debug (opcional)
                        saveBitmapForDebug(frameBitmap, "frame_$timeMs")
                        val isSimilar = recentFrames.any { areBitmapsSimilar(it, frameBitmap) }
                        if (!isSimilar) {
                            // Evita problemas com smart cast: copia o bitmap para uma variável imutável
                            val frameToAdd = frameBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            detectedFrames.add(DetectedFrame(frameToAdd, timeMs, detected))
                            recentFrames.add(frameToAdd)
                            if (recentFrames.size > 5) {
                                recentFrames.removeAt(0)
                            }
                            Log.d("FrameAdded", "Novo frame adicionado em ${timeMs} ms")
                        }
                    }
                }
                val progressPercent = ((totalFramesProcessados * 100) / totalFrames).toInt()
                withContext(Dispatchers.Main) {
                    binding.progressBar.progress = progressPercent
                }
            }
            retriever.release()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.tvVideoStatus.text = "Processados $totalFramesProcessados frames.\nFrames detectados: ${detectedFrames.size} (entre $totalFramesDetectados positivos)"
                adapter.updateFrames(detectedFrames)
                binding.tvVideoStatus.alpha = 0f
                binding.tvVideoStatus.animate().alpha(1f).setDuration(500).start()
                Log.d("Debug", "Total frames processados: $totalFramesProcessados; Total detectados: $totalFramesDetectados; Frames exibidos: ${detectedFrames.size}")
            }
        }
    }

    // Compara dois bitmaps reduzindo-os para 32x32 e calculando a diferença média normalizada
    private fun areBitmapsSimilar(bmp1: Bitmap, bmp2: Bitmap, threshold: Float = 0.1f): Boolean {
        val scaledSize = 32
        val scaled1 = Bitmap.createScaledBitmap(bmp1, scaledSize, scaledSize, true)
        val scaled2 = Bitmap.createScaledBitmap(bmp2, scaledSize, scaledSize, true)
        var totalDiff = 0f
        for (x in 0 until scaledSize) {
            for (y in 0 until scaledSize) {
                val pixel1 = scaled1.getPixel(x, y)
                val pixel2 = scaled2.getPixel(x, y)
                val diffRed = abs(Color.red(pixel1) - Color.red(pixel2)) / 255f
                val diffGreen = abs(Color.green(pixel1) - Color.green(pixel2)) / 255f
                val diffBlue = abs(Color.blue(pixel1) - Color.blue(pixel2)) / 255f
                totalDiff += (diffRed + diffGreen + diffBlue) / 3f
            }
        }
        val avgDiff = totalDiff / (scaledSize * scaledSize)
        Log.d("SimilarityCheck", "Diferença média: ${String.format("%.2f", avgDiff * 100)}%")
        return avgDiff < threshold
    }

    // Função de debug para salvar bitmaps no armazenamento interno
    private fun saveBitmapForDebug(bitmap: Bitmap, name: String) {
        try {
            val file = File(requireContext().filesDir, "$name.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("Debug", "Bitmap salvo: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("Debug", "Erro ao salvar bitmap", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
