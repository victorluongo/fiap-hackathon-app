package com.fiap.cutwatch.view.adapter



import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fiap.cutwatch.R
import com.fiap.cutwatch.databinding.ItemKnifeBinding
import com.fiap.cutwatch.domain.model.Knife

class ListaKniveAdapter(
    private val ctx: Context,
    private val quandoClicaNoItem:(String)->Unit
) : RecyclerView.Adapter<ListaKniveAdapter.VH>() {

    private val items = mutableListOf<Knife>()
    override fun onCreateViewHolder(p:ViewGroup,vt:Int) = VH(
        ItemKnifeBinding.inflate(LayoutInflater.from(ctx),p,false)
    )
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h:VH,pos:Int) = h.vincula(items[pos])
    fun atualiza(n:List<Knife>) {
        items.clear(); items.addAll(n); notifyDataSetChanged()
    }

    inner class VH(val b: ItemKnifeBinding): RecyclerView.ViewHolder(b.root) {
        fun vincula(k: Knife) {
            b.itemKnifeMensagem.text = k.mensagem
            b.itemKnifeImagem.visibility = if (k.temImagem) VISIBLE else GONE
            k.imagem?.let{ b.itemKnifeImagem.load(it){ placeholder(R.drawable.imagem_carregando_placeholder) } }
            b.root.setOnClickListener{ k.id?.let(quandoClicaNoItem) }
        }
    }
}

