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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUploadFragment extends FileBrowserFragment implements Injectable,
        Toolbar.OnMenuItemClickListener {

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
    @BindView(R.id.action_upload)
    Button mUploadAction;
    @BindView(R.id.action_cancel)
    Button mCancelAction;
    @BindView(R.id.action_pause_resume)
    Button mPauseResumeAction;

    private ImageUploadViewModel mViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(ImageUploadViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_image_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.help);
        toolbar.setOnMenuItemClickListener(this);

        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mUploadAction.setEnabled(isFileLoaded());
            mCancelAction.setEnabled(state.canCancel());
            mPauseResumeAction.setEnabled(state.canPauseOrResume());
            mPauseResumeAction.setText(state == ImageUploadViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            mSelectFileAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mUploadAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mCancelAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            mPauseResumeAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING:
                    mStatus.setText(R.string.image_upload_status_validating);
                    break;
                case UPLOADING:
                    mStatus.setText(R.string.image_upload_status_uploading);
                    break;
                case PAUSED:
                    mStatus.setText(R.string.image_upload_status_paused);
                    break;
                case COMPLETE:
                    clearFileContent();
                    mStatus.setText(R.string.image_upload_status_completed);
                    break;
            }
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mProgress.setProgress(progress));
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mSelectFileAction.setVisibility(View.VISIBLE);
            mUploadAction.setVisibility(View.VISIBLE);
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
            mUploadAction.setVisibility(View.VISIBLE);
            mUploadAction.setEnabled(false);
            mCancelAction.setVisibility(View.GONE);
            mPauseResumeAction.setVisibility(View.GONE);
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mSelectFileAction.setEnabled(!busy);
            mUploadAction.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        mSelectFileAction.setOnClickListener(v -> selectFile("application/*"));

        // Restore UPLOAD action state after rotation
        mUploadAction.setEnabled(isFileLoaded());
        mUploadAction.setOnClickListener(v -> mViewModel.upload(getFileContent()));

        // Cancel and Pause/Resume buttons
        mCancelAction.setOnClickListener(v -> mViewModel.cancel());
        mPauseResumeAction.setOnClickListener(v -> {
            if (mViewModel.getState().getValue() == ImageUploadViewModel.State.UPLOADING) {
                mViewModel.pause();
            } else {
                mViewModel.resume();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.image_upload_dialog_help_title,
                        R.string.image_upload_dialog_help_message);
                dialog.show(getChildFragmentManager(), null);
                return true;
        }
        return false;
    }

    @Override
    protected void onFileCleared() {
        mUploadAction.setEnabled(false);
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
            mUploadAction.setEnabled(true);
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
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mStatus.setText(spannable);
    }
}
