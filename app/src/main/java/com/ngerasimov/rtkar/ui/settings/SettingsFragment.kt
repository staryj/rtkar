package com.ngerasimov.rtkar.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var barriersAdapter: DeviceListAdapter
    private lateinit var doorsAdapter: DeviceListAdapter
    
    companion object {
        private const val GRID_ITEMS_PER_ROW = 6
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerViews()
        observeViewModel()

        return root
    }

    private fun setupRecyclerViews() {
        // Setup barriers RecyclerView
        barriersAdapter = DeviceListAdapter(
            onItemClick = { device ->
                viewModel.toggleDeviceSelection(device, EntityType.BARRIER)
            },
            isSelected = { deviceId ->
                viewModel.isDeviceSelected(deviceId)
            }
        )
        binding.barriersRecyclerView.layoutManager = GridLayoutManager(requireContext(), GRID_ITEMS_PER_ROW)
        binding.barriersRecyclerView.adapter = barriersAdapter

        // Setup doors (intercoms) RecyclerView
        doorsAdapter = DeviceListAdapter(
            onItemClick = { device ->
                viewModel.toggleDeviceSelection(device, EntityType.DOOR)
            },
            isSelected = { deviceId ->
                viewModel.isDeviceSelected(deviceId)
            }
        )
        binding.doorsRecyclerView.layoutManager = GridLayoutManager(requireContext(), GRID_ITEMS_PER_ROW)
        binding.doorsRecyclerView.adapter = doorsAdapter
    }

    private fun observeViewModel() {
        // Observe barrier devices
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.barrierDevices.collect { devices ->
                android.util.Log.d("SettingsFragment", "Barrier devices updated: ${devices.size}")
                barriersAdapter.submitList(devices)
                binding.barriersEmptyText.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe intercom devices
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.intercomDevices.collect { devices ->
                android.util.Log.d("SettingsFragment", "Intercom devices updated: ${devices.size}")
                doorsAdapter.submitList(devices)
                binding.doorsEmptyText.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observe error state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                binding.errorText.text = error
                binding.errorText.visibility = if (error != null) View.VISIBLE else View.GONE
            }
        }

        // Observe selected device IDs to update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDeviceIds.collect {
                // Update selection checker and notify adapters
                barriersAdapter.updateSelectionChecker { deviceId ->
                    viewModel.isDeviceSelected(deviceId)
                }
                doorsAdapter.updateSelectionChecker { deviceId ->
                    viewModel.isDeviceSelected(deviceId)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
