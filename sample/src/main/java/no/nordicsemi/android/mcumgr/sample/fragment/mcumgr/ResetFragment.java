/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.checkbox.MaterialCheckBox;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.managers.DefaultManager;
import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardResetBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.dialog.HelpDialogFragment;
import no.nordicsemi.android.mcumgr.sample.utils.StringUtils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.ResetViewModel;

public class ResetFragment extends Fragment implements Injectable {
    private static final String PREFS_FL_MODE = "reset_fl_mode";
    private static final String PREFS_ADV_NAME_SET = "reset_adv_name_set";
    private static final String PREFS_ADV_NAME = "reset_adv_name";
    private static final String PREFS_FORCE = "reset_force";

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

        // Restore previous settings.
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        final boolean flModeEnabled = preferences.getBoolean(PREFS_FL_MODE, false);
        binding.optionBootloader.setChecked(flModeEnabled);
        binding.optionName.setEnabled(flModeEnabled);
        final boolean forceEnabled = preferences.getBoolean(PREFS_FORCE, false);
        binding.optionForce.setChecked(forceEnabled);

        final boolean advNameSet = preferences.getBoolean(PREFS_ADV_NAME_SET, false);
        if (advNameSet) {
            final String previousAdvName = preferences.getString(PREFS_ADV_NAME, null);
            binding.optionNameValue.setText(previousAdvName);
        }

        // Configure observers.
        viewModel.getError().observe(getViewLifecycleOwner(), e -> {
            binding.resetError.setVisibility(View.VISIBLE);
            binding.resetError.setText(StringUtils.toString(requireContext(), e));
        });
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionReset.setEnabled(!busy));
        binding.optionBootloader.addOnCheckedStateChangedListener((materialCheckBox, i) -> {
            binding.optionName.setEnabled(i == MaterialCheckBox.STATE_CHECKED);
        });
        binding.actionReset.setOnClickListener(v -> {
            binding.resetError.setVisibility(View.GONE);

            final int bootMode = binding.optionBootloader.isChecked()
                    ? DefaultManager.BOOT_MODE_TYPE_BOOTLOADER : DefaultManager.BOOT_MODE_TYPE_NORMAL;
            final String advName = binding.optionName.isEnabled()
                    ? binding.optionNameValue.getText().toString() : null;
            final boolean force = binding.optionForce.isChecked();

            final SharedPreferences.Editor editor = preferences.edit()
                    .putBoolean(PREFS_FL_MODE, binding.optionBootloader.isChecked())
                    .putBoolean(PREFS_FORCE, binding.optionForce.isChecked());
            if (binding.optionBootloader.isChecked()) {
                editor
                    .putBoolean(PREFS_ADV_NAME_SET, true)
                    .putString(PREFS_ADV_NAME, advName); // This can be null, that's fine.
            }
            editor.apply();

            viewModel.reset(bootMode, force, advName);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
