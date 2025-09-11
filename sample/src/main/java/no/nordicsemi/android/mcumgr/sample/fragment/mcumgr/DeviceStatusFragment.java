/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardDeviceStatusBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.dialog.HelpDialogFragment;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.DeviceStatusViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import no.nordicsemi.android.observability.bluetooth.MonitoringAndDiagnosticsService;

public class DeviceStatusFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    private FragmentCardDeviceStatusBinding binding;

    private DeviceStatusViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(DeviceStatusViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardDeviceStatusBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.status_dialog_help_title,
                        R.string.status_dialog_help_message,
                        "https://mflt.io/nrf-app-discover-cloud-services");
                dialog.show(getChildFragmentManager(), null);
                return true;
            }
            return false;
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case CONNECTING:
                    binding.mcumgrSupported.setText(R.string.status_connecting);
                    break;
                case INITIALIZING:
                    binding.mcumgrSupported.setText(R.string.status_initializing);
                    break;
                case READY:
                    binding.mcumgrSupported.setText(R.string.status_connected);
                    break;
                case DISCONNECTING:
                    binding.mcumgrSupported.setText(R.string.status_disconnecting);
                    break;
                case DISCONNECTED:
                    binding.mcumgrSupported.setText(R.string.status_disconnected);
                    break;
                case TIMEOUT:
                    binding.mcumgrSupported.setText(R.string.status_error_connection_timeout);
                    break;
                case NOT_SUPPORTED:
                    binding.mcumgrSupported.setText(R.string.status_not_supported);
                    binding.mcumgrBufferSize.setText(R.string.not_applicable);
                    binding.bootloaderMode.setText(R.string.not_applicable);
                    binding.bootloaderName.setText(R.string.not_applicable);
                    binding.activeB0Slot.setText(R.string.not_applicable);
                    binding.kernel.setText(R.string.not_applicable);
                    break;
            }
        });
        viewModel.getBondState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case NONE:
                    binding.bondingStatus.setText(R.string.status_not_bonded);
                    break;
                case BONDING:
                    binding.bondingStatus.setText(R.string.status_bonding);
                    break;
                case BONDED:
                    binding.bondingStatus.setText(R.string.status_bonded);
                    break;
            }
        });
        viewModel.getBufferParams().observe(getViewLifecycleOwner(), params -> {
            if (params != null) {
                final String text = getString(R.string.status_mcumgr_buffer_size, params.getCount(), params.getSize());
                binding.mcumgrBufferSize.setText(text);
            } else {
                binding.mcumgrBufferSize.setText(R.string.status_unknown);
            }
        });
        viewModel.getBootloaderName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) {
                binding.bootloaderName.setText(name);
            } else {
                binding.bootloaderName.setText(R.string.status_unknown);
            }
        });
        viewModel.getBootloaderMode().observe(getViewLifecycleOwner(), mode -> {
            if (mode != null) {
                final CharSequence[] modes = getResources().getTextArray(R.array.bootloader_modes);
                if (mode >= 0 && mode < modes.length) {
                    binding.bootloaderMode.setText(modes[mode]);
                } else {
                    binding.bootloaderMode.setText(getString(R.string.status_unknown_value, mode));
                }
            } else {
                binding.bootloaderMode.setText(R.string.status_unknown);
            }
        });
        viewModel.getActiveB0Slot().observe(getViewLifecycleOwner(), slot -> {
            if (slot != null) {
                final CharSequence[] slots = getResources().getTextArray(R.array.b0_slots);
                if (slot >= 0 && slot < slots.length) {
                    binding.activeB0Slot.setText(slots[slot]);
                } else {
                    binding.bootloaderMode.setText(getString(R.string.status_unknown_value, slot));
                }
            } else {
                binding.activeB0Slot.setText(R.string.status_unknown);
            }
        });
        viewModel.getAppInfo().observe(getViewLifecycleOwner(), kernel -> {
            if (kernel != null) {
                binding.kernel.setText(kernel);
            } else {
                binding.kernel.setText(R.string.status_unknown);
            }
        });
        viewModel.getOtaInfo().observe(getViewLifecycleOwner(), deviceInfo -> {
            if (deviceInfo != null) {
                binding.otaSupported.setText(R.string.status_supported);
            } else {
                binding.otaSupported.setText(R.string.status_not_supported);
            }
        });
        viewModel.getObservabilityState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case MonitoringAndDiagnosticsService.State.Connecting ignored ->
                        binding.observabilitySupported.setText(R.string.status_connecting);
                case MonitoringAndDiagnosticsService.State.Initializing ignored ->
                        binding.observabilitySupported.setText(R.string.status_connecting);
                case MonitoringAndDiagnosticsService.State.Connected ignored ->
                        binding.observabilitySupported.setText(R.string.status_mds_supported);
                case MonitoringAndDiagnosticsService.State.Disconnected disconnected -> {
                    switch (disconnected.getReason()) {
                        case NOT_SUPPORTED -> binding.observabilitySupported.setText(R.string.status_not_supported);
                        case BONDING_FAILED -> binding.observabilitySupported.setText(R.string.status_bonding_failed);
                        case FAILED_TO_CONNECT, TIMEOUT, CONNECTION_LOST ->
                                binding.observabilitySupported.setText(R.string.status_disconnected);
                        case null -> binding.observabilitySupported.setText(R.string.status_disconnected);
                    }
                }
                case null,
                default -> binding.observabilitySupported.setText(R.string.status_unknown);
            }
        });
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy ->
                binding.workIndicator.setVisibility(busy ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
