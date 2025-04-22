package com.fiap.cutwatch.view.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.fiap.cutwatch.domain.repository.KnifeRepository


class ListaKniveViewModel(private val repository: KnifeRepository) : ViewModel() {

    fun buscaTodos() = repository.buscaTodos().asLiveData()

}