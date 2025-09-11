package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.observability.ObservabilityManager
import javax.inject.Inject
import javax.inject.Named

class ObservabilityViewModel @Inject constructor(
    observabilityManager: ObservabilityManager,
    @Named("busy") busyState: MutableLiveData<Boolean?>
) : McuMgrViewModel(busyState) {
    private val _state: MutableLiveData<ObservabilityManager.State?> = MutableLiveData()
    val state = _state as LiveData<ObservabilityManager.State?>

    init {
        observabilityManager.state
            .onEach { _state.postValue(it) }
            .launchIn(viewModelScope)
    }
}