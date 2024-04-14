package com.android.marker.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.marker.databinding.SavedMarkerItemBinding
import com.android.marker.model.models.SavedMarkers

class MarkersAdapter(
    private val dataSet: MutableList<SavedMarkers>,
    private val onItemClick: (data: SavedMarkers) -> Unit
) :
    RecyclerView.Adapter<MarkersAdapter.ViewHolder>() {

    private lateinit var binding: SavedMarkerItemBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = SavedMarkerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        binding.markerItemTv.text = dataSet[position].name
        binding.markerAddressTv.text = dataSet[position].address
        binding.markerLayout.setOnClickListener {
            onItemClick(dataSet[position])
        }
        if (position == dataSet.size - 1) {
            binding.divider.visibility = View.GONE
        }
    }

    class ViewHolder(binding: SavedMarkerItemBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun getItemCount(): Int = dataSet.size
}