package com.fiap.cutwatch.view.knife

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaPlayer
import android.net.Uri

import android.os.Bundle
import android.os.Environment

import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.fiap.cutwatch.R
import com.fiap.cutwatch.databinding.FormularioKnifeBinding
import com.fiap.cutwatch.databinding.OpcoesImagemKnifeBinding
import com.fiap.cutwatch.domain.extensions.snackbar
import com.fiap.cutwatch.domain.ia.LocalObjectDetector
import com.fiap.cutwatch.domain.model.DetectedFrame
import com.fiap.cutwatch.domain.model.Knife
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.view.adapter.DetectedFrameAdapter
import com.fiap.cutwatch.view.viewmodel.Componentes
import com.fiap.cutwatch.view.viewmodel.FormularioKnifeViewModel
import com.fiap.cutwatch.view.viewmodel.EstadoAppViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

import android.content.Context
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class MediaType { IMAGE, VIDEO }

class FormularioKnifeFragment : Fragment() {

    private var _binding: FormularioKnifeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FormularioKnifeViewModel by viewModel()
    private val estadoAppViewModel: EstadoAppViewModel by sharedViewModel()
    private val navController by lazy { findNavController() }
    private var currentMediaType = MediaType.IMAGE

    // pra manter os frames detectados e alimentar o RecyclerView
    private val detectedFrames = mutableListOf<DetectedFrame>()
    private lateinit var adapter: DetectedFrameAdapter
    private var mediaPlayer: MediaPlayer? = null

// alterar a chave e id do chat do Telegram

    private  val TELEGRAM_BOT_TOKEN = "5555574327:BBFYbK9zg9_MIJdwJWa5WiezFypTChgl6tM"  // troque pelo seu
    private  val TELEGRAM_CHAT_ID  = "992203541"


    private var photoFile: File? = null
    private var photoUri: Uri?  = null

    companion object {
        private const val REQUEST_IMAGE_GET = 1
        private const val REQUEST_IMAGE_CAPTURE = 42
        private const val REQUEST_VIDEO_GET = 1002
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private const val FILE_NAME = "photo.jpg"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FormularioKnifeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoUri?.let  { outState.putString("photo_uri", it.toString()) }
        photoFile?.let { outState.putString("photo_file", it.absolutePath) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            it.getString("photo_uri")?.let { uriStr ->
                photoUri = Uri.parse(uriStr)
            }
            it.getString("photo_file")?.let { path ->
                photoFile = File(path)
            }
        }
        // Torna esta toolbar a ActionBar da Activity
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)

        // 2) trata cliques do menu


        estadoAppViewModel.setComponentes(
            Componentes(appBar = true)
        )

        configuraCarregamentoDeImagem()
        binding.formularioMediaImagem.setOnClickListener { mostraOpcoesMidia() }

        // inicializa o adapter horizontal
        adapter = DetectedFrameAdapter(detectedFrames)
        binding.rvDetectedFrames.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvDetectedFrames.adapter = adapter
    }

    private fun configuraCarregamentoDeImagem() {
        viewModel.imagemCarregada.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                binding.formularioMediaImagem.load(url)
            } else {
                binding.formularioMediaImagem.load(R.drawable.imagem_insercao_padrao)
            }
        }
    }

    private fun mostraOpcoesMidia() {
        val dialog = BottomSheetDialog(requireContext())
        val ops = OpcoesImagemKnifeBinding.inflate(layoutInflater)
        ops.opcoesImagemKnifeGaleria.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" },
                REQUEST_IMAGE_GET
            ); dialog.dismiss()
        }
        ops.opcoesImagemKnifeGaleriaVideo.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" },
                REQUEST_VIDEO_GET
            ); dialog.dismiss()
        }
        ops.opcoesImagemKnifeFotografar.setOnClickListener {
            fotografaImagem(); dialog.dismiss()
        }
        ops.opcoesImagemKnifeRemover.setOnClickListener {
            viewModel.removeImagem(); dialog.dismiss()
        }
        dialog.setContentView(ops.root)
        dialog.show()
    }

    private fun fotografaImagem() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // peça PERMISSÃO para a própria Fragment
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }
    // trate o retorno da permissão
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // usuário permitiu: dispare agora a câmera
                dispatchTakePictureIntent()
            } else {
                // perdeu permissão
                Toast.makeText(requireContext(),
                    "Permissão de câmera é necessária para tirar foto",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun dispatchTakePictureIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) == null) {
            Toast.makeText(requireContext(), "Nenhum app de câmera disponível", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filename = "IMG_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cutwatch")
            }
            photoUri = requireContext().contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (photoUri == null) {
                Toast.makeText(requireContext(), "Não foi possível criar URI", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            photoFile = File.createTempFile(
                FILE_NAME, ".jpg",
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile!!
            )
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val resInfos = requireContext().packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in resInfos) {
            requireContext().grantUriPermission(
                info.activityInfo.packageName,
                photoUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                // se não tivermos URI, aborta
                val uri = photoUri
                if (uri == null) {
                    Toast
                        .makeText(requireContext(), "Não foi possível recuperar a foto", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val bmp: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requireContext().contentResolver.openInputStream(uri)
                        ?.use { BitmapFactory.decodeStream(it) }
                } else {
                    photoFile?.absolutePath
                        ?.let { BitmapFactory.decodeFile(it) }
                }

                if (bmp == null) {
                    Toast
                        .makeText(requireContext(), "Erro ao carregar foto", Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                binding.formularioMediaImagem.setImageBitmap(bmp)
                currentMediaType = MediaType.IMAGE

                // atualiza na ViewModel
                val imageRef = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    uri.toString()
                } else {
                    photoFile!!.absolutePath
                }
                viewModel.atualizaImagem(imageRef)

                processaImage(bmp)
            }
            REQUEST_IMAGE_GET -> data?.data?.also { uri ->
                binding.formularioMediaImagem.load(uri)
                currentMediaType = MediaType.IMAGE
                viewModel.atualizaImagem(uri.toString())
                val bmp = BitmapFactory.decodeStream(
                    requireContext().contentResolver.openInputStream(uri)
                )
                processaImage(bmp)
            }
            REQUEST_VIDEO_GET -> data?.data?.also { uri ->
                // thumbnail
                val retr = MediaMetadataRetriever().apply {
                    setDataSource(requireContext(), uri)
                }
                val thumb = retr.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retr.release()
                binding.formularioMediaImagem.setImageBitmap(thumb)
                binding.videoIndicator.visibility = View.VISIBLE
                currentMediaType = MediaType.VIDEO
                viewModel.atualizaVideo(uri.toString())
                processaVideo(uri)
            }
        }
    }

    private fun enviaKnife() {
        val msg = binding.formularioKnifeDescricao.text.toString()
        val knife = Knife(id = null, mensagem = msg)
        val content = if (currentMediaType == MediaType.VIDEO)
            prepareVideoUpload() else prepareImageUpload()

        viewModel.salvaKnife(knife, content).observe(viewLifecycleOwner) { state ->
            when (state) {
                is RequestState.Success<*> -> navController.popBackStack()
                is RequestState.Error     -> binding.root.snackbar("Erro ao enviar")
                else                      -> { /* nada a fazer */ }    }
        }
    }

    private fun prepareImageUpload(): ByteArray {
        val bmp = binding.formularioMediaImagem.drawable.toBitmap()
        return ByteArrayOutputStream().apply {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, this)
        }.toByteArray()
    }

    private fun prepareVideoUpload(): ByteArray {
        val uri = Uri.parse(viewModel.videoUriNaoNula)
        val retr = MediaMetadataRetriever().apply {
            setDataSource(requireContext(), uri)
        }
        val thumb = retr.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retr.release()
        return ByteArrayOutputStream().apply {
            thumb?.compress(Bitmap.CompressFormat.JPEG, 90, this)
        }.toByteArray()
    }

    private fun processaImage(bitmap: Bitmap) {
        // mostra indicador de progresso
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Processando imagem..."

        lifecycleScope.launch(Dispatchers.Default) {
            // salva pra debug (pode remover em produção)
            saveBitmapForDebug(bitmap, "input_image")

            // roda detecção
            val detected = LocalObjectDetector(requireContext()).detect(bitmap)

            withContext(Dispatchers.Main) {
                // oculta progresso
                binding.progressBar.visibility = View.GONE
                binding.tvProgressStatus.visibility = View.GONE

                if (detected) {
                    showKnifeAlert()
                } else {
                    // 🚩 devolve o Toast de “nenhuma faca encontrada”
                    Toast.makeText(requireContext(),
                        "Nenhuma faca encontrada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun processaVideo(videoUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Processando vídeo..."

        val retriever = MediaMetadataRetriever().apply {
            setDataSource(requireContext(), videoUri)
        }
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val intervalMs = 500L

        // lista pra filtrar repetidos
        val recentFrames = mutableListOf<Bitmap>()
        detectedFrames.clear()
        var processed = 0L
        var positives = 0L
        val total = durationMs / intervalMs

        lifecycleScope.launch(Dispatchers.IO) {
            for (t in 0L until durationMs step intervalMs) {
                processed++
                val frame = retriever.getFrameAtTime(t * 1000, OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    if (LocalObjectDetector(requireContext()).detect(frame)) {
                        positives++
                        // salva para debug
                        saveBitmapForDebug(frame, "frame_$t")
                        // compara similaridade
                        val isSimilar = recentFrames.any { areBitmapsSimilar(it, frame) }
                        if (!isSimilar) {
                            val copy = frame.copy(Bitmap.Config.ARGB_8888, true)
                            detectedFrames.add( DetectedFrame(copy, t, true) )
                            recentFrames.add(copy)
                            if (recentFrames.size > 5) recentFrames.removeAt(0)
                        }
                    }
                }
                val prog = ((processed * 100) / total).toInt()
                withContext(Dispatchers.Main) {
                    binding.progressBar.progress = prog
                    binding.tvProgressStatus.text = "Processando: $prog%"
                }
            }
            retriever.release()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.tvProgressStatus.text =
                    "Processados $processed frames.\nDetectados: ${detectedFrames.size} (de $positives)"
                adapter.updateFrames(detectedFrames)

                if (detectedFrames.isNotEmpty()) {
                    // se achou alguma coisa, chama seu alerta “forte”
                    showKnifeAlert()

                } else {
                    // se não achou, um aviso simples
                    Toast
                        .makeText(
                            requireContext(),
                            "Nenhum objeto cortante detectado no vídeo.",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }
    }


    // compara dois bitmaps reduzidos a 32x32
    private fun areBitmapsSimilar(bmp1: Bitmap, bmp2: Bitmap, threshold: Float = 0.1f): Boolean {
        val size = 32
        val s1 = Bitmap.createScaledBitmap(bmp1, size, size, true)
        val s2 = Bitmap.createScaledBitmap(bmp2, size, size, true)
        var totalDiff = 0f
        for (x in 0 until size) for (y in 0 until size) {
            val p1 = s1.getPixel(x, y)
            val p2 = s2.getPixel(x, y)
            val dr = abs(Color.red(p1)   - Color.red(p2))   / 255f
            val dg = abs(Color.green(p1) - Color.green(p2)) / 255f
            val db = abs(Color.blue(p1)  - Color.blue(p2))  / 255f
            totalDiff += (dr + dg + db) / 3f
        }
        val avg = totalDiff / (size * size)
        Log.d("SimilarityCheck","avgDiff: ${"%.2f".format(avg*100)}%")
        return avg < threshold
    }

    // salva frame em arquivo interno para depurar
    private fun saveBitmapForDebug(bitmap: Bitmap, name: String) {
        try {
            val file = File(requireContext().filesDir, "$name.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("Debug", "Bitmap salvo em: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("Debug","Erro ao salvar bitmap", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_formulario_knife, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_formulario_knife_enviar -> {
                enviaKnife()
                true
            }
            R.id.menu_formulario_knife_remove -> {
                apresentaDialogoDeRemocao()
                true
            }
            R.id.menu_formulario_knife_fotografar -> {
                fotografaImagem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun apresentaDialogoDeRemocao() {
        AlertDialog.Builder(requireContext())
            .setTitle("Removendo Knife")
            .setMessage("Você quer remover esse knife?")
            .setPositiveButton("Sim") { _, _ ->
         //       knifeId?.let(this::remove)
            }
            .setNegativeButton("Não")
            { _, _ -> }
            .show()
    }

    private fun remove(knifeId: String) {
        viewModel.remove(knifeId).observe(viewLifecycleOwner) {
            view?.snackbar("Knife foi removido!")
            navController.popBackStack()
        }
    }
    private fun showKnifeAlert() {
        // inflar view custom
        val dialogView = layoutInflater.inflate(R.layout.dialog_alert_knife, null)
        val dlg = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // configurar o botão
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        btnOk.setOnClickListener {
            // para som e vibração
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            val vib = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vib.cancel()

            dlg.dismiss()
        }

        // tocar som em loop e vibrar
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.siren).apply {
            isLooping = true
            start()
        }
        vibratePattern()

        dlg.window
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.show()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeString = sdf.format(Date(now))
        sendTelegramMessage("⚠️ *Objeto cortante detectado!* ⚠️\nHora: ${timeString}".also {
            // se quiser formatar melhor, use algum formatter de data
        })
    }
    private fun sendTelegramMessage(text: String) {
        val url = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
        val body = FormBody.Builder()
            .add("chat_id", TELEGRAM_CHAT_ID)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("Telegram", "Erro ao enviar msg", e)
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                Log.d("Telegram", "Resposta Telegram: ${response.body?.string()}")
                response.close()
            }
        })
    }


    private fun vibratePattern() {
        val vib = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 200, 500)  // espera, vibra, pausa, vibra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }


    // lembre de liberar a binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
