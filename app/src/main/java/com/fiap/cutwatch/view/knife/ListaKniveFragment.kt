package com.fiap.cutwatch.view.knife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fiap.cutwatch.databinding.FragmentKniveAdapterBinding
import com.fiap.cutwatch.domain.extensions.onBackPress
import com.fiap.cutwatch.domain.extensions.snackbar
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.view.adapter.ListaKniveAdapter
import com.fiap.cutwatch.view.viewmodel.EstadoAppViewModel
import com.fiap.cutwatch.view.viewmodel.ListaKniveViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListaKniveFragment : Fragment() {
    private var _b: FragmentKniveAdapterBinding? = null
    private val binding get() = _b!!
    private val vm: ListaKniveViewModel by viewModel()
    private val nav by lazy { findNavController() }
    private val adapter by lazy {
        ListaKniveAdapter(requireContext()) { id ->
            nav.navigate(ListaKniveFragmentDirections.acaoListaKniveParaFormularioKnife(id))
        }
    }

    override fun onCreateView(i: LayoutInflater,c:ViewGroup?,b:Bundle?) =
        FragmentKniveAdapterBinding.inflate(i,c,false).also{_b=it}.root

    override fun onViewCreated(v:View,s:Bundle?){
        super.onViewCreated(v,s)
        binding.listaKniveRecyclerview.adapter = adapter
        vm.buscaTodos().observe(viewLifecycleOwner){
            if(it is RequestState.Success) it.data?.let(adapter::atualiza)
        }
        binding.listaKniveFabAdiciona.setOnClickListener {
            nav.navigate(ListaKniveFragmentDirections.acaoListaKniveParaFormularioKnife(null))
        }
    }
    override fun onDestroyView(){ super.onDestroyView(); _b=null }
}

