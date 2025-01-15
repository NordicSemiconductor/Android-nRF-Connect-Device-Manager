/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

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

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardDeviceStatusBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.DeviceStatusViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

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
                        R.string.status_dialog_help_message);
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
                    binding.connectionStatus.setText(R.string.status_connecting);
                    break;
                case INITIALIZING:
                    binding.connectionStatus.setText(R.string.status_initializing);
                    break;
                case READY:
                    binding.connectionStatus.setText(R.string.status_connected);
                    break;
                case DISCONNECTING:
                    binding.connectionStatus.setText(R.string.status_disconnecting);
                    break;
                case DISCONNECTED:
                    binding.connectionStatus.setText(R.string.status_disconnected);
                    break;
                case TIMEOUT:
                    binding.connectionStatus.setText(R.string.status_connection_timeout);
                    break;
                case NOT_SUPPORTED:
                    binding.connectionStatus.setText(R.string.status_not_supported);
                    break;
            }
        });
        viewModel.getBondState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case NOT_BONDED:
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
                final String text = getString(R.string.status_mcumgr_buffer_size, params.count, params.size);
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
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy ->
                binding.workIndicator.setVisibility(busy ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
