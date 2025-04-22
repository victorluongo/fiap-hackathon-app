package com.fiap.cutwatch.view.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.fiap.cutwatch.domain.model.Knife
import com.fiap.cutwatch.domain.repository.KnifeRepository
import com.fiap.cutwatch.domain.state.RequestState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class FormularioKnifeViewModel(
    private val repository: KnifeRepository
) : ViewModel() {

    private val _imagemCarregada = MutableLiveData<String?>()
    val imagemCarregada: LiveData<String?> = _imagemCarregada

    fun atualizaImagem(uri: String) {
        _imagemCarregada.value = uri
    }
    fun removeImagem() {
        _imagemCarregada.value = null
    }
    // Variável privada para armazenar a URI do vídeo como String
    private var _videoUri: String? = null

    // Exposta (read-only) se necessário ou crie um getter separado
    val videoUri: String?
        get() = _videoUri

    // Função para atualizar/definir a URI do vídeo
    fun atualizaVideo(uri: String) {
        _videoUri = uri
    }

    // Função que retorna a URI do vídeo (ou uma string vazia se não definida)
    val videoUriNaoNula: String
        get() = _videoUri ?: ""

    fun buscaKnife(id: String) = repository.buscaPorId(id).asLiveData()

    fun remove(knifeId: String) =
        liveData<RequestState<Unit>> {
            try {
                repository.remove(knifeId)
                emit(RequestState.Success())
                repository.removeImagem(knifeId)
            } catch (e: Exception) {
                emit(RequestState.Error(e))
            }
        }

    fun edita(knife: Knife, imagem: ByteArray) =
        liveData<RequestState<Unit>> {
            try {
                repository.edita(knife)
                emit(RequestState.Success())
                knife.id?.let { knifeId ->
                    coroutineScope {
                        launch {
                            tentaEnviarImagem(knifeId, imagem)
                        }
                    }
                }
            } catch (e: Exception) {
                emit(RequestState.Error(e))
            }
        }

    private suspend fun tentaEnviarImagem(knifeId: String, imagem: ByteArray) {
        imagemCarregada.value?.let {
            repository.enviaImagem(knifeId, imagem)
        }
    }


    fun salvaKnife(knife: Knife, mediaContent: ByteArray) = liveData<RequestState<Unit>> {
        try {
            // Salva o knife no Firestore e obtém um id
            val id = repository.salva(knife)
            emit(RequestState.Success())
            // Envia a mídia associada (imagem ou vídeo) para o Storage
            repository.enviaImagem(id, mediaContent)
        } catch (e: Exception) {
            Log.e("FormularioKnifeVM", "salvaKnife: falha ao enviar knife", e)
            emit(RequestState.Error(e))
        }
    }


}