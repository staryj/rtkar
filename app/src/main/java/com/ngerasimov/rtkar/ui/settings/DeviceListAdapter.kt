package com.ngerasimov.rtkar.ui.settings

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.api.Device

class DeviceListAdapter(
    private val onItemClick: (Device) -> Unit,
    private var isSelected: (String) -> Boolean
) : ListAdapter<Device, DeviceListAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    
    fun updateSelectionChecker(checker: (String) -> Boolean) {
        isSelected = checker
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_card, parent, false)
        
        // Make the item rectangular: height = width / 2
        // Use ViewTreeObserver to calculate after layout
        view.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val parentWidth = parent.width
                if (parentWidth > 0) {
                    // Account for padding (8dp * 2 = 16dp) and margins (4dp * 2 = 8dp per item)
                    val padding = (8 * 2 * parent.context.resources.displayMetrics.density).toInt()
                    val margins = (4 * 2 * 6 * parent.context.resources.displayMetrics.density).toInt() // 6 items with margins
                    val itemWidth = (parentWidth - padding - margins) / 6
                    val itemHeight = itemWidth / 2 // Height is 2 times less than width
                    val layoutParams = view.layoutParams
                    layoutParams.height = itemHeight
                    view.layoutParams = layoutParams
                }
            }
        })
        
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        android.util.Log.d("DeviceListAdapter", "onBindViewHolder called for position: $position, itemCount: ${itemCount}")
        holder.bind(getItem(position))
    }
    
    override fun getItemCount(): Int {
        val count = super.getItemCount()
        android.util.Log.d("DeviceListAdapter", "getItemCount: $count")
        return count
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.device_card)
        private val titleText: TextView = itemView.findViewById(R.id.device_title)

        fun bind(device: Device) {
            android.util.Log.d("DeviceListAdapter", "Binding device: ${device.name_by_company}, ID: ${device.id}")
            titleText.text = device.name_by_company
            
            // Set card color based on selection state
            val isSelected = isSelected(device.id)
            android.util.Log.d("DeviceListAdapter", "Device ${device.id} isSelected: $isSelected")
            if (isSelected) {
                // Green for selected
                cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                cardView.strokeColor = Color.parseColor("#4CAF50")
                cardView.strokeWidth = 4
            } else {
                // Grey for non-selected
                cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                cardView.strokeColor = Color.parseColor("#9E9E9E")
                cardView.strokeWidth = 2
            }
            
            itemView.setOnClickListener {
                onItemClick(device)
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}
