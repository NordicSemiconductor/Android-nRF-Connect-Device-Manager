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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageUploadBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUploadFragment extends FileBrowserFragment implements Injectable,
        Toolbar.OnMenuItemClickListener {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    private FragmentCardImageUploadBinding mBinding;

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
        mBinding = FragmentCardImageUploadBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.help);
        toolbar.setOnMenuItemClickListener(this);

        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mBinding.actionUpload.setEnabled(isFileLoaded());
            mBinding.actionCancel.setEnabled(state.canCancel());
            mBinding.actionPauseResume.setEnabled(state.canPauseOrResume());
            mBinding.actionPauseResume.setText(state == ImageUploadViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            mBinding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionUpload.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            mBinding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING:
                    mBinding.status.setText(R.string.image_upload_status_validating);
                    break;
                case UPLOADING:
                    mBinding.status.setText(R.string.image_upload_status_uploading);
                    break;
                case PAUSED:
                    mBinding.status.setText(R.string.image_upload_status_paused);
                    break;
                case COMPLETE:
                    clearFileContent();
                    mBinding.status.setText(R.string.image_upload_status_completed);
                    break;
            }
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mBinding.progress.setProgress(progress));
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setVisibility(View.VISIBLE);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
            printError(error);
        });
        mViewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            mBinding.fileName.setText(null);
            mBinding.fileSize.setText(null);
            mBinding.fileHash.setText(null);
            mBinding.status.setText(null);
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setEnabled(false);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mBinding.actionSelectFile.setEnabled(!busy);
            mBinding.actionUpload.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        mBinding.actionSelectFile.setOnClickListener(v -> selectFile("application/*"));

        // Restore UPLOAD action state after rotation
        mBinding.actionUpload.setEnabled(isFileLoaded());
        mBinding.actionUpload.setOnClickListener(v -> mViewModel.upload(getFileContent()));

        // Cancel and Pause/Resume buttons
        mBinding.actionCancel.setOnClickListener(v -> mViewModel.cancel());
        mBinding.actionPauseResume.setOnClickListener(v -> {
            if (mViewModel.getState().getValue() == ImageUploadViewModel.State.UPLOADING) {
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
        mBinding.actionUpload.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        mBinding.fileName.setText(fileName);
        mBinding.fileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        try {
            final byte[] hash = McuMgrImage.getHash(data);
            mBinding.fileHash.setText(StringUtils.toHex(hash));
            mBinding.actionUpload.setEnabled(true);
            mBinding.status.setText(R.string.image_upgrade_status_ready);
        } catch (final McuMgrException e) {
            clearFileContent();
            onFileLoadingFailed(R.string.image_error_file_not_valid);
        }
    }

    @Override
    protected void onFileLoadingFailed(final int error) {
        mBinding.status.setText(error);
    }

    private void printError(@NonNull final String error) {
        final SpannableString spannable = new SpannableString(error);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mBinding.status.setText(spannable);
    }
}
