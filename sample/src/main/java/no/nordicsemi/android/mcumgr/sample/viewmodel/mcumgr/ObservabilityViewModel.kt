package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.ble.McuMgrBleTransport
import no.nordicsemi.android.observability.ObservabilityManager
import no.nordicsemi.android.observability.ObservabilityManager.Companion.create
import javax.inject.Inject
import javax.inject.Named

class ObservabilityViewModel @Inject constructor(
    transport: McuMgrTransport,
    device: BluetoothDevice,
    context: Context,
    @Named("busy") busyState: MutableLiveData<Boolean?>
) : McuMgrViewModel(busyState) {
    private val _state: MutableLiveData<ObservabilityManager.State?> = MutableLiveData()
    val state = _state as LiveData<ObservabilityManager.State?>

    private val observabilityManager: ObservabilityManager = create(context)
        .apply {
            this.state
                .onEach { _state.postValue(it) }
                .launchIn(viewModelScope)
        }

    init {
        transport.addObserver(object : McuMgrTransport.ConnectionObserver {
            override fun onConnected() {
                observabilityManager.connect(context, device)
            }

            override fun onDisconnected() {
                observabilityManager.disconnect()
            }
        })

        // The observer is not called if the transport is already connected.
        if (transport is McuMgrBleTransport && transport.isConnected) {
            observabilityManager.connect(context, device)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observabilityManager.disconnect()
    }
}