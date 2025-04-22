package com.fiap.cutwatch.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fiap.cutwatch.databinding.ItemDetectedFrameBinding
import com.fiap.cutwatch.domain.model.DetectedFrame

class DetectedFrameAdapter(
    private var frames: List<DetectedFrame>
) : RecyclerView.Adapter<DetectedFrameAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemDetectedFrameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(frame: DetectedFrame) {
            binding.ivFrame.setImageBitmap(frame.bitmap)
            binding.tvFrameInfo.text = "Detectado em ${frame.timeMs} ms"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectedFrameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(frames[position])
    }

    override fun getItemCount(): Int = frames.size

    fun updateFrames(newFrames: List<DetectedFrame>) {
        frames = newFrames
        notifyDataSetChanged()
    }
}
