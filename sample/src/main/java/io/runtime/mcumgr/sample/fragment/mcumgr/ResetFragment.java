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

import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardResetBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ResetViewModel;

public class ResetFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    
    private FragmentCardResetBinding binding;

    private ResetViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ResetViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardResetBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.reset_dialog_help_title,
                        R.string.reset_dialog_help_message);
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

        viewModel.getError().observe(getViewLifecycleOwner(), e -> binding.resetError.setText(StringUtils.toString(requireContext(), e)));
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionReset.setEnabled(!busy));
        binding.actionReset.setOnClickListener(v -> {
            final int bootMode = binding.optionBootloader.isChecked()
                    ? DefaultManager.BOOT_MODE_TYPE_BOOTLOADER : DefaultManager.BOOT_MODE_TYPE_NORMAL;
            final boolean force = binding.optionForce.isChecked();
            viewModel.reset(bootMode, force);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
