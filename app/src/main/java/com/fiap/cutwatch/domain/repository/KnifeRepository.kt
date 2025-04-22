package com.fiap.cutwatch.domain.repository



import android.util.Log
import com.fiap.cutwatch.domain.model.Knife
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.utils.Constants.FIRESTORE_COLLECTION_CUTWATCHS
import com.fiap.cutwatch.utils.Constants.TAG

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class KnifeRepository(
private val firestore: FirebaseFirestore,
private val storage: FirebaseStorage
) {

    suspend fun salva(knife: Knife): String {
        val documento = firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
            .document()
        documento
            .set(DocumentoKnife(knife))
            .await()
        return documento.id
    }

    suspend fun enviaImagem(knifeId: String, imagem: ByteArray) {
        GlobalScope.launch {
            try {
                val documento = firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
                    .document(knifeId)

                documento
                    .update(mapOf("temImagem" to true))
                    .await()

                val referencia = storage.reference.child("knive/$knifeId.jpg")
                referencia.putBytes(imagem).await()
                val url = referencia.downloadUrl.await()

                documento
                    .update(mapOf("imagem" to url.toString()))
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "enviaImagem: falha ao enviar a imagem", e)
            }
        }
    }

    suspend fun removeImagem(knifeId: String) {
        GlobalScope.launch {
            try {
                val referencia = storage.reference.child("knive/$knifeId.jpg")
                referencia.delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "removeImagem: falha ao remover a imagem", e)
            }
        }
    }

    suspend fun edita(knife: Knife) {
        val knifeId =
            knife.id ?: throw IllegalArgumentException("Id não pode ser nulo ao editar um knife")
        firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
            .document(knifeId)
            .set(DocumentoKnife(knife))
            .await()
    }

    fun buscaTodos() = callbackFlow<RequestState<List<Knife>>> {
        val listener = firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
            .addSnapshotListener { query, erro ->
                if (erro != null) {
                    //   offer(Resultado.Erro(erro))

                    trySend(RequestState.Error(erro)).isSuccess
                    close(erro) // Fechar o canal em caso de erro
                    return@addSnapshotListener
                }

                val knive = query?.documents?.mapNotNull { documento ->
                    documento.paraPost()
                } ?: return@addSnapshotListener

                if (!isClosedForSend) { // Verificar se o canal ainda está aberto para enviar valores
                    //        offer(Resultado.Sucesso(knive))
                    trySend(RequestState.Success(knive)).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }

    fun buscaPorId(id: String) = callbackFlow<Knife?> {
        val listener = firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
            .document(id)
            .addSnapshotListener { documento, _ ->
                if (!isClosedForSend) { // Verificar se o canal ainda está aberto para enviar valores
                    //          offer(documento?.paraPost())
                    trySend(documento?.paraPost()).isSuccess
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun remove(id: String) {
        firestore.collection(FIRESTORE_COLLECTION_CUTWATCHS)
            .document(id)
            .delete().await()
    }

    private fun DocumentSnapshot.paraPost(): Knife? {
        return this.toObject(DocumentoKnife::class.java)?.paraKnife(this.id)
    }

}

private class DocumentoKnife(
    val mensagem: String = "",
    val imagem: String? = null,
    val temImagem: Boolean = false
) {

    constructor(knife: Knife) : this(
        mensagem = knife.mensagem,
        imagem = knife.imagem,
        temImagem = knife.temImagem
    )

    fun paraKnife(id: String? = null) = Knife(
        id = id,
        mensagem = mensagem,
        imagem = imagem,
        temImagem = temImagem
    )

}



