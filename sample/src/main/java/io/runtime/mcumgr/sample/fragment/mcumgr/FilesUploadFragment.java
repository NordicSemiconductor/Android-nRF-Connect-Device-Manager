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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardFilesUploadBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.GenerateFileDialogFragment;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesUploadFragment extends FileBrowserFragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;
    @Inject
    FsUtils mFsUtils;

    private FragmentCardFilesUploadBinding mBinding;

    private FilesUploadViewModel mViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(FilesUploadViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardFilesUploadBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFsUtils.getPartition().observe(getViewLifecycleOwner(), partition -> {
            if (isFileLoaded()) {
                final String fileName = mBinding.fileName.getText().toString();
                mBinding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
            }
        });
        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mBinding.actionUpload.setEnabled(isFileLoaded());
            mBinding.actionCancel.setEnabled(state.canCancel());
            mBinding.actionPauseResume.setEnabled(state.canPauseOrResume());
            mBinding.actionPauseResume.setText(state == FilesUploadViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            mBinding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionUpload.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            mBinding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            mBinding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case UPLOADING:
                    mBinding.status.setText(R.string.files_upload_status_uploading);
                    break;
                case PAUSED:
                    mBinding.status.setText(R.string.files_upload_status_paused);
                    break;
                case COMPLETE:
                    clearFileContent();
                    mBinding.status.setText(R.string.image_upgrade_status_completed);
                    break;
            }
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mBinding.progress.setProgress(progress));
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mBinding.actionGenerate.setVisibility(View.VISIBLE);
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setVisibility(View.VISIBLE);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
            printError(error);
        });
        mViewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            mBinding.fileName.setText(null);
            mBinding.filePath.setText(null);
            mBinding.fileSize.setText(null);
            mBinding.status.setText(null);
            mBinding.actionGenerate.setVisibility(View.VISIBLE);
            mBinding.actionSelectFile.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setVisibility(View.VISIBLE);
            mBinding.actionUpload.setEnabled(false);
            mBinding.actionCancel.setVisibility(View.GONE);
            mBinding.actionPauseResume.setVisibility(View.GONE);
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mBinding.actionGenerate.setEnabled(!busy);
            mBinding.actionSelectFile.setEnabled(!busy);
            mBinding.actionUpload.setEnabled(isFileLoaded() && !busy);
        });

        // Configure GENERATE FILE action
        mBinding.actionGenerate.setOnClickListener(v -> {
            final DialogFragment dialog = GenerateFileDialogFragment.getInstance();
            dialog.show(getChildFragmentManager(), null);
        });

        // Configure SELECT FILE action
        mBinding.actionSelectFile.setOnClickListener(v -> selectFile("*/*"));

        // Restore UPLOAD action state after rotation
        mBinding.actionUpload.setEnabled(isFileLoaded());
        mBinding.actionUpload.setOnClickListener(v -> {
            final String fileName = mBinding.fileName.getText().toString();
            mFsUtils.addRecent(fileName);
            final String filePath = mBinding.filePath.getText().toString();
            mViewModel.upload(filePath, getFileContent());
        });

        // Cancel and Pause/Resume buttons
        mBinding.actionCancel.setOnClickListener(v -> mViewModel.cancel());
        mBinding.actionPauseResume.setOnClickListener(v -> {
            if (mViewModel.getState().getValue() == FilesUploadViewModel.State.UPLOADING) {
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

    public void onGenerateFileRequested(final int fileSize) {
        onFileSelected("Lorem_" + fileSize + ".txt", fileSize);
        setFileContent(FsUtils.generateLoremIpsum(fileSize));
    }

    @Override
    protected void onFileCleared() {
        mBinding.actionUpload.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        final String partition = mFsUtils.getPartitionString();
        mBinding.fileName.setText(fileName);
        mBinding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
        mBinding.fileSize.setText(getString(R.string.files_upload_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        mBinding.actionUpload.setEnabled(true);
        mBinding.status.setText(R.string.files_upload_status_ready);
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
