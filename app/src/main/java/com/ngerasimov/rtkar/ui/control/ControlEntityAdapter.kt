package com.ngerasimov.rtkar.ui.control

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType

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
        
        // Calculate item size based on RecyclerView dimensions and grid configuration
        // Optimized for 1920x720 device
        view.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                
                val recyclerView = parent as? androidx.recyclerview.widget.RecyclerView
                val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
                val adapter = recyclerView?.adapter as? ControlEntityAdapter
                
                if (recyclerView != null && layoutManager != null && adapter != null && 
                    recyclerView.width > 0 && recyclerView.height > 0) {
                    
                    val itemCount = adapter.itemCount
                    val columns = layoutManager.spanCount
                    val rows = if (itemCount > 0) {
                        (itemCount + columns - 1) / columns // Ceiling division
                    } else {
                        1
                    }
                    
                    val density = parent.context.resources.displayMetrics.density
                    
                    // Calculate available space
                    // RecyclerView padding: 4dp start, 4dp end, 4dp top, 80dp bottom
                    val horizontalPadding = (4 * 2 * density).toInt()
                    val verticalPadding = (4 * density).toInt() // Top padding only for height calculation
                    val bottomPadding = (80 * density).toInt()
                    
                    // Item margins: 4dp on each side = 8dp total per item
                    val itemMargin = (4 * 2 * density).toInt()
                    
                    // Calculate item height to fill available vertical space
                    val availableHeight = recyclerView.height - verticalPadding - bottomPadding
                    val totalVerticalMargins = itemMargin * rows
                    val itemHeight = if (rows > 0) {
                        (availableHeight - totalVerticalMargins) / rows
                    } else {
                        availableHeight
                    }
                    
                    // Set minimum height to ensure readability (at least 120dp)
                    val minHeight = (120 * density).toInt()
                    val finalHeight = maxOf(itemHeight, minHeight)
                    
                    val layoutParams = view.layoutParams
                    layoutParams.height = finalHeight
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
        private val iconView: ImageView = itemView.findViewById(R.id.device_icon)
        private val titleText: TextView = itemView.findViewById(R.id.device_title)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(entity: Entity) {
            titleText.text = entity.title
            
            // Set icon based on entity type
            val iconRes = when (entity.type) {
                EntityType.DOOR -> R.drawable.ic_door_24dp
                EntityType.BARRIER -> R.drawable.ic_barrier_24dp
            }
            iconView.setImageResource(iconRes)
            
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
            val context = itemView.context
            if (isCurrentlyLoading) {
                // Disabled/grey appearance when loading
                cardView.setCardBackgroundColor(
                    MaterialColors.getColor(context, R.attr.cardBackgroundLoading, "cardBackgroundLoading")
                )
                cardView.strokeColor = MaterialColors.getColor(context, R.attr.cardStrokeLoading, "cardStrokeLoading")
                cardView.strokeWidth = 2
                cardView.alpha = 0.6f
                itemView.isEnabled = false
                itemView.isClickable = false
                
                // Set content color for loading state
                val contentColor = ResourcesCompat.getColor(context.resources, R.color.card_content_loading, null)
                titleText.setTextColor(contentColor)
                iconView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            } else {
                // Normal appearance - blue/active color
                cardView.setCardBackgroundColor(
                    MaterialColors.getColor(context, R.attr.cardBackgroundActive, "cardBackgroundActive")
                )
                cardView.strokeColor = MaterialColors.getColor(context, R.attr.cardStrokeActive, "cardStrokeActive")
                cardView.strokeWidth = 2
                cardView.alpha = 1.0f
                // In editing mode, disable item clicks
                itemView.isEnabled = !isEditingMode
                itemView.isClickable = !isEditingMode
                
                // Set content color for active state
                val contentColor = ResourcesCompat.getColor(context.resources, R.color.card_content_active, null)
                titleText.setTextColor(contentColor)
                iconView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
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
