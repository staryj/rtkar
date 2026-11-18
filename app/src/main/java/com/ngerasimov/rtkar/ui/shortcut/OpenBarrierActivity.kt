package com.ngerasimov.rtkar.ui.shortcut

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.ApiError
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import com.ngerasimov.rtkar.databinding.ActivityOpenBarrierBinding
import com.ngerasimov.rtkar.shortcut.OpenBarrierDecision
import com.ngerasimov.rtkar.shortcut.OpenBarrierEvaluator
import com.ngerasimov.rtkar.shortcut.OpenBarrierRejectReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OpenBarrierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenBarrierBinding
    private lateinit var repository: DeviceRepository
    private var openJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenBarrierBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = DeviceRepository(
            apiWrapper = RtkApiWrapper(applicationContext),
            storageManager = StorageManager(applicationContext)
        )
        startOpen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startOpen(intent)
    }

    private fun startOpen(intent: Intent) {
        openJob?.cancel()
        val requestedTitle = intent.getStringExtra(EXTRA_TITLE)
        showStatus(
            getString(
                R.string.open_barrier_opening,
                requestedTitle?.ifBlank { null } ?: getString(R.string.shortcut_barrier_fallback_label)
            ),
            inProgress = true
        )
        openJob = lifecycleScope.launch {
            val token = repository.authToken.first()
            val decision = OpenBarrierEvaluator.decide(
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID),
                requestedTitle = requestedTitle,
                entities = repository.getEntitiesSync(),
                token = token
            )
            when (decision) {
                is OpenBarrierDecision.Reject -> showResult(messageFor(decision.reason), success = false)
                is OpenBarrierDecision.Open -> {
                    val title = decision.title.ifBlank { getString(R.string.shortcut_barrier_fallback_label) }
                    showStatus(getString(R.string.open_barrier_opening, title), inProgress = true)
                    val result = repository.openDevice(token.orEmpty(), decision.deviceId)
                    result.onSuccess {
                        showResult(getString(R.string.open_barrier_success), success = true)
                    }.onFailure { exception ->
                        val message = when (exception) {
                            is ApiError -> exception.getUserMessage(this@OpenBarrierActivity)
                            else -> exception.message ?: getString(R.string.error_failed_to_open_device)
                        }
                        showResult(message, success = false)
                    }
                }
            }
        }
    }

    private suspend fun showResult(message: String, success: Boolean) {
        showStatus(message, inProgress = false)
        delay(if (success) SUCCESS_DISMISS_MS else ERROR_DISMISS_MS)
        finish()
    }

    private fun showStatus(message: String, inProgress: Boolean) {
        binding.status.text = message
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
    }

    private fun messageFor(reason: OpenBarrierRejectReason): String {
        return when (reason) {
            OpenBarrierRejectReason.MissingId -> getString(R.string.open_barrier_missing)
            OpenBarrierRejectReason.NotSelected -> getString(R.string.open_barrier_not_selected)
            OpenBarrierRejectReason.MissingToken -> getString(R.string.error_auth_token_missing)
        }
    }

    companion object {
        const val EXTRA_DEVICE_ID = "com.ngerasimov.rtkar.extra.DEVICE_ID"
        const val EXTRA_TITLE = "com.ngerasimov.rtkar.extra.TITLE"
        private const val SUCCESS_DISMISS_MS = 1600L
        private const val ERROR_DISMISS_MS = 3500L
    }
}
