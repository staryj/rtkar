package com.ngerasimov.rtkar.ui.control

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.Entity

class ControlEntityAdapter(
    private val onItemClick: (Entity) -> Unit,
    private var isLoading: (String) -> Boolean,
    private val onDeleteClick: (Entity) -> Unit,
    private var isEditingMode: Boolean = false
) : ListAdapter<Entity, ControlEntityAdapter.EntityViewHolder>(EntityDiffCallback()) {
    
    fun updateLoadingChecker(checker: (String) -> Boolean) {
        isLoading = checker
        notifyDataSetChanged()
    }
    
    fun updateEditingMode(editing: Boolean) {
        isEditingMode = editing
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
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
        
        return EntityViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EntityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.device_card)
        private val titleText: TextView = itemView.findViewById(R.id.device_title)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(entity: Entity) {
            titleText.text = entity.title
            
            // Check if this entity is currently loading
            val isCurrentlyLoading = isLoading(entity.deviceId)
            
            // Show/hide delete button based on editing mode
            deleteButton.visibility = if (isEditingMode) View.VISIBLE else View.GONE
            
            // Set delete button click listener
            // Use setOnTouchListener to prevent drag interference
            deleteButton.setOnClickListener {
                if (isEditingMode) {
                    onDeleteClick(entity)
                }
            }
            
            // Prevent delete button from interfering with drag
            deleteButton.setOnTouchListener { view, event ->
                // Allow the touch event to propagate if not in editing mode
                if (!isEditingMode) {
                    false
                } else {
                    // In editing mode, handle the click but don't block drag
                    false
                }
            }
            
            // Set card appearance based on loading state and editing mode
            if (isCurrentlyLoading) {
                // Disabled/grey appearance when loading
                cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
                cardView.strokeColor = Color.parseColor("#9E9E9E")
                cardView.strokeWidth = 2
                cardView.alpha = 0.6f
                itemView.isEnabled = false
                itemView.isClickable = false
            } else {
                // Normal appearance - blue/active color
                cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                cardView.strokeColor = Color.parseColor("#2196F3")
                cardView.strokeWidth = 2
                cardView.alpha = 1.0f
                // In editing mode, disable item clicks
                itemView.isEnabled = !isEditingMode
                itemView.isClickable = !isEditingMode
            }
            
            itemView.setOnClickListener {
                if (!isCurrentlyLoading && !isEditingMode) {
                    onItemClick(entity)
                }
            }
        }
    }

    class EntityDiffCallback : DiffUtil.ItemCallback<Entity>() {
        override fun areItemsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem.deviceId == newItem.deviceId && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem == newItem
        }
    }
}
