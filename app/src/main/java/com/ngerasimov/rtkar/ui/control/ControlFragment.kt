package com.ngerasimov.rtkar.ui.control

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.ngerasimov.rtkar.databinding.FragmentControlBinding
import kotlinx.coroutines.launch

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ControlViewModel
    private lateinit var adapter: ControlEntityAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var isDragging = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[ControlViewModel::class.java]

        _binding = FragmentControlBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        observeViewModel()

        return root
    }

    private fun setupRecyclerView() {
        // Setup single RecyclerView for all entities
        adapter = ControlEntityAdapter(
            onItemClick = { entity ->
                viewModel.triggerControlAction(entity)
            },
            isLoading = { deviceId ->
                viewModel.isLoading(deviceId)
            },
            onDeleteClick = { entity ->
                viewModel.removeEntity(entity)
            },
            isEditingMode = false
        )
        
        // Set initial layout manager (will be updated dynamically)
        binding.recyclerViewControl.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewControl.adapter = adapter
        
        // Ensure items recalculate size after RecyclerView is laid out
        binding.recyclerViewControl.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.recyclerViewControl.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // Trigger adapter to recalculate item sizes
                adapter.notifyDataSetChanged()
            }
        })
        
        // Setup drag-and-drop
        setupItemTouchHelper()
    }
    
    private fun updateGridLayout(itemCount: Int) {
        val columns = when {
            itemCount <= 4 -> 2
            itemCount <= 9 -> 3
            itemCount <= 16 -> 4
            else -> 5
        }
        
        val currentLayoutManager = binding.recyclerViewControl.layoutManager as? GridLayoutManager
        if (currentLayoutManager?.spanCount != columns) {
            binding.recyclerViewControl.layoutManager = GridLayoutManager(requireContext(), columns)
            // Post to ensure layout is complete before notifying adapter to recalculate sizes
            binding.recyclerViewControl.post {
                adapter.notifyDataSetChanged()
            }
        } else {
            // Even if columns didn't change, item count might have, so recalculate sizes
            binding.recyclerViewControl.post {
                adapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun setupItemTouchHelper() {
        updateItemTouchHelper()
    }
    
    private fun updateItemTouchHelper() {
        // Detach existing helper if any
        itemTouchHelper?.attachToRecyclerView(null)
        
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            // Track the last valid positions to avoid rapid updates
            private var lastFromPosition = -1
            private var lastToPosition = -1
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                // Only allow reordering in editing mode
                if (!viewModel.isEditingMode.value) {
                    return false
                }
                
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                
                // Validate positions
                if (fromPosition == androidx.recyclerview.widget.RecyclerView.NO_POSITION || 
                    toPosition == androidx.recyclerview.widget.RecyclerView.NO_POSITION ||
                    fromPosition == toPosition) {
                    return false
                }
                
                // Skip if this is the same move we just processed
                if (fromPosition == lastFromPosition && toPosition == lastToPosition) {
                    return true
                }
                
                // Get current list from adapter - use a snapshot to avoid concurrent modification
                val currentList = adapter.currentList
                if (fromPosition >= currentList.size || toPosition >= currentList.size) {
                    return false
                }
                
                // Create a new mutable list to avoid issues
                val newList = currentList.toMutableList()
                
                // Move item in the list
                val item = newList.removeAt(fromPosition)
                newList.add(toPosition, item)
                
                // Update last positions
                lastFromPosition = fromPosition
                lastToPosition = toPosition
                
                // Update adapter immediately for smooth animation
                // Use a copy to ensure DiffUtil works correctly
                adapter.submitList(ArrayList(newList))
                
                // Mark that we're dragging to prevent Flow updates from interfering
                isDragging = true
                
                return true
            }
            
            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                direction: Int
            ) {
                // Not used, but required by interface
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                // Only enable drag in editing mode
                return viewModel.isEditingMode.value
            }
            
            override fun onSelectedChanged(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        // Drag started
                        isDragging = true
                        lastFromPosition = -1
                        lastToPosition = -1
                        viewHolder?.itemView?.alpha = 0.7f
                        viewHolder?.itemView?.elevation = 8f
                    }
                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        // Drag ended - save the final order
                        if (isDragging) {
                            isDragging = false
                            lastFromPosition = -1
                            lastToPosition = -1
                            // Save the current order from adapter to ViewModel
                            val currentList = adapter.currentList
                            if (currentList.isNotEmpty()) {
                                viewModel.saveEntityOrder(currentList)
                            }
                        }
                        viewHolder?.itemView?.alpha = 1.0f
                        viewHolder?.itemView?.elevation = 0f
                    }
                }
            }
            
            override fun clearView(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.elevation = 0f
            }
        }
        
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(binding.recyclerViewControl)
    }

    private fun observeViewModel() {
        // Observe all entities
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allEntities.collect { entities ->
                // Update grid layout based on item count
                updateGridLayout(entities.size)
                
                // Only update adapter if we're not currently dragging
                // This prevents conflicts during drag operations
                if (!isDragging) {
                    adapter.submitList(entities)
                }
                binding.emptyText.visibility = if (entities.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe loading state to update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadingEntityIds.collect {
                // Update loading checker and notify adapter
                adapter.updateLoadingChecker { deviceId ->
                    viewModel.isLoading(deviceId)
                }
            }
        }
        
        // Observe editing mode state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isEditingMode.collect { isEditing ->
                adapter.updateEditingMode(isEditing)
                // Recreate ItemTouchHelper when editing mode changes
                updateItemTouchHelper()
            }
        }
        
        // Setup FAB click listener
        binding.fabEdit.setOnClickListener {
            viewModel.toggleEditingMode()
        }
        
        // Observe error messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { errorMsg ->
                errorMsg?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    // Clear the error after displaying
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
