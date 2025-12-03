package com.ngerasimov.rtkar.ui.control

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ngerasimov.rtkar.databinding.FragmentControlBinding
import kotlinx.coroutines.launch

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ControlViewModel
    private lateinit var adapter: ControlEntityAdapter

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
        val itemsPerRow = 6
        
        // Setup single RecyclerView for all entities
        adapter = ControlEntityAdapter(
            onItemClick = { entity ->
                viewModel.triggerControlAction(entity)
            },
            isLoading = { deviceId ->
                viewModel.isLoading(deviceId)
            }
        )
        binding.recyclerViewControl.layoutManager = GridLayoutManager(requireContext(), itemsPerRow)
        binding.recyclerViewControl.adapter = adapter
    }

    private fun observeViewModel() {
        // Observe all entities
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allEntities.collect { entities ->
                adapter.submitList(entities)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
