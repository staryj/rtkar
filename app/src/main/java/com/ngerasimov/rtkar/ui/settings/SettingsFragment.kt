package com.ngerasimov.rtkar.ui.settings

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import androidx.recyclerview.widget.GridLayoutManager
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel
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

        // Initialize ViewModel with manual dependency injection
        val application = requireActivity().application
        val repository = DeviceRepository(
            apiWrapper = RtkApiWrapper(application),
            storageManager = StorageManager(application)
        )
        viewModel = SettingsViewModel(application, repository)

        setupRecyclerViews()
        setupReloadButtons()
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

    private fun setupReloadButtons() {
        binding.barriersReloadButton.setOnClickListener {
            viewModel.reloadBarriers()
        }
        
        binding.doorsReloadButton.setOnClickListener {
            viewModel.reloadIntercoms()
        }
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shortcutNotice.collect { notice ->
                binding.shortcutNotice.text = notice
                binding.shortcutNotice.visibility = if (notice.isNullOrBlank()) View.GONE else View.VISIBLE
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

        // Observe barriers loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingBarriers.collect { isLoading ->
                updateReloadButtonState(binding.barriersReloadButton, isLoading)
            }
        }

        // Observe intercoms loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingIntercoms.collect { isLoading ->
                updateReloadButtonState(binding.doorsReloadButton, isLoading)
            }
        }
    }

    private fun updateReloadButtonState(button: android.widget.ImageButton, isLoading: Boolean) {
        button.isEnabled = !isLoading
        button.isClickable = !isLoading
        val tintColor = if (isLoading) {
            ContextCompat.getColor(requireContext(), R.color.reload_button_loading)
        } else {
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.colorControlNormal, typedValue, true)
            ContextCompat.getColor(requireContext(), typedValue.resourceId)
        }
        button.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
