/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import androidx.preference.PreferenceManager;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageUpgradeBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.FirmwareUpgradeModeDialogFragment;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.utils.ZipPackage;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUpgradeViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUpgradeFragment extends FileBrowserFragment implements Injectable {
    private static final String PREF_ERASE_APP_SETTINGS = "pref_erase_app_settings";
    private static final String PREF_ESTIMATED_SWAP_TIME = "pref_estimated_swap_time";
    private static final String PREF_WINDOW_CAPACITY = "pref_window_capacity";
    private static final String PREF_MEMORY_ALIGNMENT = "pref_memory_alignment";
    private static final String SIS_MEMORY_ALIGNMENT = "sis_memory_alignment";

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    
    private FragmentCardImageUpgradeBinding binding;

    private ImageUpgradeViewModel viewModel;
    private int memoryAlignment;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ImageUpgradeViewModel.class);

        if (savedInstanceState != null) {
            memoryAlignment = savedInstanceState.getInt(SIS_MEMORY_ALIGNMENT);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SIS_MEMORY_ALIGNMENT, memoryAlignment);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardImageUpgradeBinding.inflate(inflater, container, false);

        // Set up (i) buttons.
        binding.advancedEraseSettingsInfo.setOnClickListener(v -> {
            final DialogFragment dialog = HelpDialogFragment.getInstance(
                    R.string.image_upgrade_erase_storage,
                    R.string.image_upgrade_erase_storage_info);
            dialog.show(getChildFragmentManager(), null);
        });
        binding.advancedSwapTimeInfo.setOnClickListener(v -> {
            final DialogFragment dialog = HelpDialogFragment.getInstance(
                    R.string.image_upgrade_swap_time,
                    R.string.image_upgrade_swap_time_info);
            dialog.show(getChildFragmentManager(), null);
        });
        binding.advancedPipelineInfo.setOnClickListener(v -> {
            final DialogFragment dialog = HelpDialogFragment.getInstance(
                    R.string.image_upgrade_pipeline,
                    R.string.image_upgrade_pipeline_info);
            dialog.show(getChildFragmentManager(), null);
        });
        binding.advancedMemoryAlignmentInfo.setOnClickListener(v -> {
            final DialogFragment dialog = HelpDialogFragment.getInstance(
                    R.string.image_upgrade_memory_alignment,
                    R.string.image_upgrade_memory_alignment_info);
            dialog.show(getChildFragmentManager(), null);
        });

        final CharSequence[] items = getResources().getTextArray(R.array.image_upgrade_memory_alignment_options);
        binding.advancedMemoryAlignment.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.drop_down_item, items));
        binding.advancedMemoryAlignment.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 1: memoryAlignment = 2; break;
                case 2: memoryAlignment = 4; break;
                case 3: memoryAlignment = 8; break;
                case 4: memoryAlignment = 16; break;
                case 0:
                default:memoryAlignment = 1; break;
            }
        });

        // Fill default values.
        if (savedInstanceState == null) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            binding.advancedEraseSettings.setChecked(preferences.getBoolean(PREF_ERASE_APP_SETTINGS, true));
            binding.advancedSwapTime.setText(getString(R.string.value_int, preferences.getInt(PREF_ESTIMATED_SWAP_TIME, 10)));
            binding.advancedWindowCapacity.setText(getString(R.string.value_int, preferences.getInt(PREF_WINDOW_CAPACITY, 4)));
            memoryAlignment = preferences.getInt(PREF_MEMORY_ALIGNMENT, 4);
            int position;
            switch (memoryAlignment) {
                case 2: position = 1; break;
                case 4: position = 2; break;
                case 8: position = 3; break;
                case 16: position = 4; break;
                case 0:
                default: position = 0; break;
            }
            binding.advancedMemoryAlignment.setText(items[position], false);
        }

        // Set up Expand / Collapse buttons in the section's menu.
        binding.toolbar.inflateMenu(R.menu.expandable);
        binding.toolbar.getMenu().findItem(R.id.action_expand).setOnMenuItemClickListener(item -> {
            viewModel.setAdvancedSettingsExpanded(true);
            return true;
        });
        binding.toolbar.getMenu().findItem(R.id.action_collapse).setOnMenuItemClickListener(item -> {
            viewModel.setAdvancedSettingsExpanded(false);
            return true;
        });
        viewModel.getAdvancedSettingsState().observe(getViewLifecycleOwner(), expanded -> {
            final Menu menu = binding.toolbar.getMenu();
            menu.findItem(R.id.action_collapse).setVisible(expanded);
            menu.findItem(R.id.action_expand).setVisible(!expanded);
            binding.advancedGroup.setVisibility(expanded ? View.VISIBLE : View.GONE);
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.actionStart.setEnabled(isFileLoaded());
            binding.actionCancel.setEnabled(state.canCancel());
            binding.actionPauseResume.setEnabled(state.canPauseOrResume());
            binding.actionPauseResume.setText(state == ImageUpgradeViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            binding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionStart.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            binding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING:
                    binding.status.setText(R.string.image_upgrade_status_validating);
                    binding.advancedEraseSettings.setEnabled(false);
                    binding.advancedSwapTimeLayout.setEnabled(false);
                    binding.advancedPipelineLayout.setEnabled(false);
                    binding.advancedMemoryAlignmentLayout.setEnabled(false);
                    break;
                case UPLOADING:
                    binding.status.setText(R.string.image_upgrade_status_uploading);
                    break;
                case PAUSED:
                    binding.status.setText(R.string.image_upgrade_status_paused);
                    break;
                case TESTING:
                    binding.status.setText(R.string.image_upgrade_status_testing);
                    break;
                case CONFIRMING:
                    binding.status.setText(R.string.image_upgrade_status_confirming);
                    break;
                case RESETTING:
                    binding.status.setText(R.string.image_upgrade_status_resetting);
                    break;
                case COMPLETE:
                    binding.status.setText(R.string.image_upgrade_status_completed);
                    binding.advancedEraseSettings.setEnabled(true);
                    binding.advancedSwapTimeLayout.setEnabled(true);
                    binding.advancedPipelineLayout.setEnabled(true);
                    binding.advancedMemoryAlignmentLayout.setEnabled(true);
                    break;
            }
        });
        viewModel.getProgress().observe(getViewLifecycleOwner(), throughputData -> {
            if (throughputData == null) {
                binding.graph.setVisibility(View.GONE);
                binding.graph.clear();
            } else {
                binding.graph.setVisibility(View.VISIBLE);
                binding.graph.addProgress(
                        throughputData.progress,
                        throughputData.instantaneousThroughput,
                        throughputData.averageThroughput
                );
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionStart.setVisibility(View.VISIBLE);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
            binding.advancedEraseSettings.setEnabled(true);
            binding.advancedSwapTimeLayout.setEnabled(true);
            binding.advancedPipelineLayout.setEnabled(true);
            binding.advancedMemoryAlignmentLayout.setEnabled(true);
            printError(error);
        });
        viewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            binding.status.setText(R.string.image_upgrade_status_cancelled);
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionStart.setVisibility(View.VISIBLE);
            binding.actionStart.setEnabled(false);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
            binding.advancedEraseSettings.setEnabled(true);
            binding.advancedSwapTimeLayout.setEnabled(true);
            binding.advancedPipelineLayout.setEnabled(true);
            binding.advancedMemoryAlignmentLayout.setEnabled(true);
        });
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            binding.actionSelectFile.setEnabled(!busy);
            binding.actionStart.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        binding.actionSelectFile.setOnClickListener(v -> selectFile("application/*"));

        // Restore START action state after rotation
        binding.actionStart.setEnabled(isFileLoaded());
        binding.actionStart.setOnClickListener(v -> {
            // Show a mode picker. When mode is selected, the upgrade(Mode) method will be called.
            final DialogFragment dialog = FirmwareUpgradeModeDialogFragment.getInstance();
            dialog.show(getChildFragmentManager(), null);
        });

        // Cancel and Pause/Resume buttons
        binding.actionCancel.setOnClickListener(v -> viewModel.cancel());
        binding.actionPauseResume.setOnClickListener(v -> {
            if (viewModel.getState().getValue() == ImageUpgradeViewModel.State.UPLOADING) {
                viewModel.pause();
            } else {
                viewModel.resume();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Starts the Firmware Upgrade using a selected mode.
     */
    @SuppressWarnings("ConstantConditions")
    public void start(@NonNull final FirmwareUpgradeManager.Mode mode) {
        if (binding.advancedSwapTime.getText().toString().isEmpty()) {
            binding.advancedSwapTime.setText("0");
        }
        if (binding.advancedWindowCapacity.getText().toString().isEmpty()) {
            binding.advancedWindowCapacity.setText("1");
        }
        final boolean eraseAppSettings = binding.advancedEraseSettings.isChecked();
        int swapTimeSeconds;
        try {
            swapTimeSeconds = Integer.parseInt(binding.advancedSwapTime.getText().toString());
            binding.advancedSwapTimeLayout.setError(null);
        } catch (final NumberFormatException e) {
            binding.advancedSwapTimeLayout.setError(getText(R.string.image_upgrade_error));
            return;
        }
        int windowCapacity;
        try {
            windowCapacity = Integer.parseInt(binding.advancedWindowCapacity.getText().toString());
            if (windowCapacity < 1 || windowCapacity > 25)
                throw new NumberFormatException();
            binding.advancedPipelineLayout.setError(null);
        } catch (final NumberFormatException e) {
            binding.advancedPipelineLayout.setError(getText(R.string.image_upgrade_error));
            return;
        }
        final int memoryAlignment = this.memoryAlignment;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        preferences.edit()
                .putBoolean(PREF_ERASE_APP_SETTINGS, eraseAppSettings)
                .putInt(PREF_ESTIMATED_SWAP_TIME, swapTimeSeconds)
                .putInt(PREF_WINDOW_CAPACITY, windowCapacity)
                .putInt(PREF_MEMORY_ALIGNMENT, memoryAlignment)
                .apply();

        viewModel.upgrade(getFileContent(), mode,
                eraseAppSettings,
                swapTimeSeconds * 1000,
                Math.max(1, windowCapacity - 1), // 1 buffer is used for sending responses.
                memoryAlignment
        );
    }

    @Override
    protected void onFileCleared() {
        binding.actionStart.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        binding.fileName.setText(fileName);
        binding.fileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        try {
            // Try parsing BIN file for single/main core (core0) update.
            final byte[] hash = McuMgrImage.getHash(data);
            binding.fileHash.setText(StringUtils.toHex(hash));
            binding.actionStart.setEnabled(true);
            binding.status.setText(R.string.image_upgrade_status_ready);
        } catch (final McuMgrException e) {
            // For multi-core devices images are bundled in a ZIP file.
            try {
                final ZipPackage zip = new ZipPackage(data);
                final StringBuilder sizeBuilder = new StringBuilder();
                final StringBuilder hashBuilder = new StringBuilder();
                for (final Pair<Integer, byte[]> binary: zip.getBinaries()) {
                    final byte[] hash = McuMgrImage.getHash(binary.second);
                    hashBuilder
                            .append(StringUtils.toHex(hash));
                    sizeBuilder
                            .append(getString(R.string.image_upgrade_size_value, binary.second.length));
                    switch (binary.first) {
                        case 0:
                            hashBuilder.append(" (app core)");
                            sizeBuilder.append(" (app core)");
                            break;
                        case 1:
                            hashBuilder.append(" (net core)");
                            sizeBuilder.append(" (net core)");
                            break;
                        default:
                            hashBuilder.append(" (unknown core (").append(binary.first).append(")");
                            sizeBuilder.append(" (unknown core (").append(binary.first).append(")");
                    }
                    hashBuilder.append("\n");
                    sizeBuilder.append("\n");
                }
                hashBuilder.setLength(hashBuilder.length() - 1);
                sizeBuilder.setLength(sizeBuilder.length() - 1);
                binding.fileHash.setText(hashBuilder.toString());
                binding.fileSize.setText(sizeBuilder.toString());
                binding.actionStart.setEnabled(true);
                binding.status.setText(R.string.image_upgrade_status_ready);
            } catch (final Exception e1) {
                clearFileContent();
                onFileLoadingFailed(R.string.image_error_file_not_valid);
            }
        }
    }

    @Override
    protected void onFileLoadingFailed(final int error) {
        binding.status.setText(error);
    }

    private void printError(@Nullable final McuMgrException error) {
        final String message = StringUtils.toString(requireContext(), error);
        if (message == null) {
            binding.status.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.status.setText(spannable);
    }
}
