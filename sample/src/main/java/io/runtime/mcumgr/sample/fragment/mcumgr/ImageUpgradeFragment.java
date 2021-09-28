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
    McuMgrViewModelFactory mViewModelFactory;
    
    private FragmentCardImageUpgradeBinding mBinding;

    private ImageUpgradeViewModel mViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(ImageUpgradeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardImageUpgradeBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mBinding.actionStart.setEnabled(isFileLoaded());
            mBinding.actionCancel.setEnabled(state.canCancel());
            mBinding.actionPauseResume.setEnabled(state.canPauseOrResume());
            mBinding.actionPauseResume.setText(state == ImageUpgradeViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            mBinding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionStart.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            mBinding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING:
                    mBinding.status.setText(R.string.image_upgrade_status_validating);
                    mBinding.optionEraseSettings.setEnabled(false);
                    break;
                case UPLOADING:
                    mBinding.status.setText(R.string.image_upgrade_status_uploading);
                    break;
                case PAUSED:
                    mBinding.status.setText(R.string.image_upgrade_status_paused);
                    break;
                case TESTING:
                    mBinding.status.setText(R.string.image_upgrade_status_testing);
                    mBinding.speed.setText(null);
                    break;
                case CONFIRMING:
                    mBinding.status.setText(R.string.image_upgrade_status_confirming);
                    mBinding.speed.setText(null);
                    break;
                case RESETTING:
                    mBinding.status.setText(R.string.image_upgrade_status_resetting);
                    mBinding.speed.setText(null);
                    break;
                case COMPLETE:
                    clearFileContent();
                    mBinding.status.setText(R.string.image_upgrade_status_completed);
                    mBinding.speed.setText(null);
                    mBinding.optionEraseSettings.setEnabled(true);
                    break;
            }
        });
        mViewModel.getTransferSpeed().observe(getViewLifecycleOwner(), speed ->
                mBinding.speed.setText(getString(R.string.image_upgrade_speed, speed))
        );
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress ->
                mBinding.progress.setProgress(progress)
        );
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionStart.setVisibility(View.VISIBLE);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
            mBinding.optionEraseSettings.setEnabled(true);
            printError(error);
        });
        mViewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            mBinding.fileName.setText(null);
            mBinding.fileSize.setText(null);
            mBinding.fileHash.setText(null);
            mBinding.status.setText(null);
            mBinding.speed.setText(null);
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionStart.setVisibility(View.VISIBLE);
            mBinding.actionStart.setEnabled(false);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
            mBinding.optionEraseSettings.setEnabled(true);
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mBinding.actionSelectFile.setEnabled(!busy);
            mBinding.actionStart.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        mBinding.actionSelectFile.setOnClickListener(v -> selectFile("application/*"));

        // Restore START action state after rotation
        mBinding.actionStart.setEnabled(isFileLoaded());
        mBinding.actionStart.setOnClickListener(v -> {
            // Show a mode picker. When mode is selected, the upgrade(Mode) method will be called.
            final DialogFragment dialog = FirmwareUpgradeModeDialogFragment.getInstance();
            dialog.show(getChildFragmentManager(), null);
        });

        // Cancel and Pause/Resume buttons
        mBinding.actionCancel.setOnClickListener(v -> mViewModel.cancel());
        mBinding.actionPauseResume.setOnClickListener(v -> {
            if (mViewModel.getState().getValue() == ImageUpgradeViewModel.State.UPLOADING) {
                mViewModel.pause();
            } else {
                mViewModel.resume();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    /**
     * Starts the Firmware Upgrade using a selected mode.
     */
    @SuppressWarnings("ConstantConditions")
    public void start(@NonNull final FirmwareUpgradeManager.Mode mode) {
        mViewModel.upgrade(getFileContent(), mode, mBinding.optionEraseSettings.isChecked());
    }

    @Override
    protected void onFileCleared() {
        mBinding.actionStart.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        mBinding.fileName.setText(fileName);
        mBinding.fileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        try {
            // Try parsing BIN file for single/main core (core0) update.
            final byte[] hash = McuMgrImage.getHash(data);
            mBinding.fileHash.setText(StringUtils.toHex(hash));
            mBinding.actionStart.setEnabled(true);
            mBinding.status.setText(R.string.image_upgrade_status_ready);
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
                mBinding.fileHash.setText(hashBuilder.toString());
                mBinding.fileSize.setText(sizeBuilder.toString());
                mBinding.actionStart.setEnabled(true);
                mBinding.status.setText(R.string.image_upgrade_status_ready);
            } catch (final Exception e1) {
                clearFileContent();
                onFileLoadingFailed(R.string.image_error_file_not_valid);
            }
        }
    }

    @Override
    protected void onFileLoadingFailed(final int error) {
        mBinding.status.setText(error);
    }

    private void printError(@Nullable final McuMgrException error) {
        final String message = StringUtils.toString(requireContext(), error);
        if (message == null) {
            mBinding.status.setText(null);
            mBinding.speed.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mBinding.status.setText(spannable);
        mBinding.speed.setText(null);
    }
}
