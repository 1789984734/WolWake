package com.example.wolwake.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.wolwake.data.Device
import com.example.wolwake.data.DeviceRepository
import com.example.wolwake.domain.WolSender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 列表页单一状态源：设备列表派生自仓库流，wakingId 为瞬态 UI 状态。 */
data class WolUiState(
    val devices: List<Device> = emptyList(),
    val wakingId: String? = null,
)

/** 一次性事件：唤醒结果，经 Snackbar 呈现。 */
sealed interface WolEvent {
    val message: String

    data class WakeFinished(
        val deviceName: String,
        val success: Boolean,
        override val message: String,
    ) : WolEvent
}

class WolViewModel(private val repository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WolUiState())
    val uiState: StateFlow<WolUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WolEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WolEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.devices.collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun wake(device: Device) {
        if (_uiState.value.wakingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(wakingId = device.id) }
            try {
                val mac = WolSender.parseMac(device.mac) ?: error("MAC 地址无效：${device.mac}")
                WolSender.wake(mac, device.host, device.port)
                _events.tryEmit(WolEvent.WakeFinished(device.name, true, "已向「${device.name}」发送唤醒包"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.tryEmit(
                    WolEvent.WakeFinished(device.name, false, "唤醒失败：${e.message ?: e.javaClass.simpleName}"),
                )
            } finally {
                _uiState.update { it.copy(wakingId = null) }
            }
        }
    }

    fun upsert(device: Device) {
        viewModelScope.launch { repository.upsert(device) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] ?: error("缺少 Application 上下文")
                WolViewModel(DeviceRepository(app))
            }
        }
    }
}
