/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageUpgradeBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.FirmwareUpgradeModeDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.utils.ZipPackage;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUpgradeViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUpgradeFragment extends FileBrowserFragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    
    private FragmentCardImageUpgradeBinding binding;

    private ImageUpgradeViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ImageUpgradeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardImageUpgradeBinding.inflate(inflater, container, false);
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
                    binding.optionEraseSettings.setEnabled(false);
                    break;
                case UPLOADING:
                    binding.status.setText(R.string.image_upgrade_status_uploading);
                    break;
                case PAUSED:
                    binding.status.setText(R.string.image_upgrade_status_paused);
                    break;
                case TESTING:
                    binding.status.setText(R.string.image_upgrade_status_testing);
                    binding.speed.setText(null);
                    break;
                case CONFIRMING:
                    binding.status.setText(R.string.image_upgrade_status_confirming);
                    binding.speed.setText(null);
                    break;
                case RESETTING:
                    binding.status.setText(R.string.image_upgrade_status_resetting);
                    binding.speed.setText(null);
                    break;
                case COMPLETE:
                    clearFileContent();
                    binding.status.setText(R.string.image_upgrade_status_completed);
                    binding.speed.setText(null);
                    binding.optionEraseSettings.setEnabled(true);
                    break;
            }
        });
        viewModel.getTransferSpeed().observe(getViewLifecycleOwner(), speed ->
                binding.speed.setText(getString(R.string.image_upgrade_speed, speed))
        );
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress ->
                binding.progress.setProgress(progress)
        );
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionStart.setVisibility(View.VISIBLE);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
            binding.optionEraseSettings.setEnabled(true);
            printError(error);
        });
        viewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            binding.fileName.setText(null);
            binding.fileSize.setText(null);
            binding.fileHash.setText(null);
            binding.status.setText(null);
            binding.speed.setText(null);
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionStart.setVisibility(View.VISIBLE);
            binding.actionStart.setEnabled(false);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
            binding.optionEraseSettings.setEnabled(true);
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
        viewModel.upgrade(getFileContent(), mode, binding.optionEraseSettings.isChecked());
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
            binding.speed.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.status.setText(spannable);
        binding.speed.setText(null);
    }
}
