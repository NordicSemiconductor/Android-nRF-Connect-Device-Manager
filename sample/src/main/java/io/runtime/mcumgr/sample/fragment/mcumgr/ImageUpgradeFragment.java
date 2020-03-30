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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.dfu.FirmwareUpgradeManager;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.FirmwareUpgradeModeDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUpgradeViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUpgradeFragment extends FileBrowserFragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    @BindView(R.id.file_name)
    TextView mFileName;
    @BindView(R.id.file_hash)
    TextView mFileHash;
    @BindView(R.id.file_size)
    TextView mFileSize;
    @BindView(R.id.status)
    TextView mStatus;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.action_select_file)
    Button mSelectFileAction;
    @BindView(R.id.action_start)
    Button mStartAction;
    @BindView(R.id.action_cancel)
    Button mCancelAction;
    @BindView(R.id.action_pause_resume)
    Button mPauseResumeAction;

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
        return inflater.inflate(R.layout.fragment_card_image_upgrade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mStartAction.setEnabled(isFileLoaded());
            mCancelAction.setEnabled(state.canCancel());
            mPauseResumeAction.setEnabled(state.canPauseOrResume());
            mPauseResumeAction.setText(state == ImageUpgradeViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            mSelectFileAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mStartAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mCancelAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            mPauseResumeAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING:
                    mStatus.setText(R.string.image_upgrade_status_validating);
                    break;
                case UPLOADING:
                    mStatus.setText(R.string.image_upgrade_status_uploading);
                    break;
                case PAUSED:
                    mStatus.setText(R.string.image_upgrade_status_paused);
                    break;
                case TESTING:
                    mStatus.setText(R.string.image_upgrade_status_testing);
                    break;
                case CONFIRMING:
                    mStatus.setText(R.string.image_upgrade_status_confirming);
                    break;
                case RESETTING:
                    mStatus.setText(R.string.image_upgrade_status_resetting);
                    break;
                case COMPLETE:
                    clearFileContent();
                    mStatus.setText(R.string.image_upgrade_status_completed);
                    break;
            }
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mProgress.setProgress(progress));
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mSelectFileAction.setVisibility(View.VISIBLE);
            mStartAction.setVisibility(View.VISIBLE);
            mCancelAction.setVisibility(View.GONE);
            mPauseResumeAction.setVisibility(View.GONE);
            printError(error);
        });
        mViewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            mFileName.setText(null);
            mFileSize.setText(null);
            mFileHash.setText(null);
            mStatus.setText(null);
            mSelectFileAction.setVisibility(View.VISIBLE);
            mStartAction.setVisibility(View.VISIBLE);
            mStartAction.setEnabled(false);
            mCancelAction.setVisibility(View.GONE);
            mPauseResumeAction.setVisibility(View.GONE);
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mSelectFileAction.setEnabled(!busy);
            mStartAction.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        mSelectFileAction.setOnClickListener(v -> selectFile("application/*"));

        // Restore START action state after rotation
        mStartAction.setEnabled(isFileLoaded());
        mStartAction.setOnClickListener(v -> {
            // Show a mode picker. When mode is selected, the upgrade(Mode) method will be called.
            final DialogFragment dialog = FirmwareUpgradeModeDialogFragment.getInstance();
            dialog.show(getChildFragmentManager(), null);
        });

        // Cancel and Pause/Resume buttons
        mCancelAction.setOnClickListener(v -> mViewModel.cancel());
        mPauseResumeAction.setOnClickListener(v -> {
            if (mViewModel.getState().getValue() == ImageUpgradeViewModel.State.UPLOADING) {
                mViewModel.pause();
            } else {
                mViewModel.resume();
            }
        });
    }

    /**
     * Starts the Firmware Upgrade using a selected mode.
     */
    @SuppressWarnings("ConstantConditions")
    public void start(@NonNull final FirmwareUpgradeManager.Mode mode) {
        mViewModel.upgrade(getFileContent(), mode);
    }

    @Override
    protected void onFileCleared() {
        mStartAction.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        mFileName.setText(fileName);
        mFileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        try {
            final byte[] hash = McuMgrImage.getHash(data);
            mFileHash.setText(StringUtils.toHex(hash));
            mStartAction.setEnabled(true);
            mStatus.setText(R.string.image_upgrade_status_ready);
        } catch (final McuMgrException e) {
            clearFileContent();
            onFileLoadingFailed(R.string.image_error_file_not_valid);
        }
    }

    @Override
    protected void onFileLoadingFailed(final int error) {
        mStatus.setText(error);
    }

    private void printError(@NonNull final String error) {
        final SpannableString spannable = new SpannableString(error);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.error)),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mStatus.setText(spannable);
    }
}
