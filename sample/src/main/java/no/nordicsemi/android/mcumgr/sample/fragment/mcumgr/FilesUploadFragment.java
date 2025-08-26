/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

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

import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardFilesUploadBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.dialog.GenerateFileDialogFragment;
import no.nordicsemi.android.mcumgr.sample.utils.FsUtils;
import no.nordicsemi.android.mcumgr.sample.utils.StringUtils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.FilesUploadViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesUploadFragment extends FileBrowserFragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    @Inject
    FsUtils fsUtils;

    private FragmentCardFilesUploadBinding binding;

    private FilesUploadViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(FilesUploadViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardFilesUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fsUtils.getPartition().observe(getViewLifecycleOwner(), partition -> {
            if (isFileLoaded()) {
                final String fileName = binding.fileName.getText().toString();
                binding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
            }
        });
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.actionUpload.setEnabled(isFileLoaded());
            binding.actionCancel.setEnabled(state.canCancel());
            binding.actionPauseResume.setEnabled(state.canPauseOrResume());
            binding.actionPauseResume.setText(state == FilesUploadViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            binding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionUpload.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            binding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case UPLOADING:
                    binding.status.setText(R.string.files_upload_status_uploading);
                    break;
                case PAUSED:
                    binding.status.setText(R.string.files_upload_status_paused);
                    break;
                case COMPLETE:
                    clearFileContent();
                    binding.status.setText(R.string.files_upload_status_completed);
                    binding.speed.setText(null);
                    break;
            }
        });
        viewModel.getTransferSpeed().observe(getViewLifecycleOwner(), speed ->
                binding.speed.setText(getString(R.string.files_upload_speed, speed))
        );
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress ->
                binding.progress.setProgress(progress)
        );
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.actionGenerate.setVisibility(View.VISIBLE);
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionUpload.setVisibility(View.VISIBLE);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
            printError(error);
        });
        viewModel.getCancelledEvent().observe(getViewLifecycleOwner(), nothing -> {
            clearFileContent();
            binding.fileName.setText(null);
            binding.filePath.setText(null);
            binding.fileSize.setText(null);
            binding.status.setText(null);
            binding.speed.setText(null);
            binding.actionGenerate.setVisibility(View.VISIBLE);
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionUpload.setVisibility(View.VISIBLE);
            binding.actionUpload.setEnabled(false);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
        });
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            binding.actionGenerate.setEnabled(!busy);
            binding.actionSelectFile.setEnabled(!busy);
            binding.actionUpload.setEnabled(isFileLoaded() && !busy);
        });

        // Configure GENERATE FILE action
        binding.actionGenerate.setOnClickListener(v -> {
            final DialogFragment dialog = GenerateFileDialogFragment.getInstance();
            dialog.show(getChildFragmentManager(), null);
        });

        // Configure SELECT FILE action
        binding.actionSelectFile.setOnClickListener(v -> selectFile("*/*"));

        // Restore UPLOAD action state after rotation
        binding.actionUpload.setEnabled(isFileLoaded());
        binding.actionUpload.setOnClickListener(v -> {
            final String fileName = binding.fileName.getText().toString();
            fsUtils.addRecent(fileName);
            final String filePath = binding.filePath.getText().toString();
            viewModel.upload(filePath, getFileContent());
        });

        // Cancel and Pause/Resume buttons
        binding.actionCancel.setOnClickListener(v -> viewModel.cancel());
        binding.actionPauseResume.setOnClickListener(v -> {
            if (viewModel.getState().getValue() == FilesUploadViewModel.State.UPLOADING) {
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

    public void onGenerateFileRequested(final int fileSize) {
        onFileSelected("Lorem_" + fileSize + ".txt", fileSize);
        setFileContent(FsUtils.generateLoremIpsum(fileSize));
    }

    @Override
    protected void onFileCleared() {
        binding.actionUpload.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        final String partition = fsUtils.getPartitionString();
        binding.fileName.setText(fileName);
        binding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
        binding.fileSize.setText(getString(R.string.files_upload_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        binding.actionUpload.setEnabled(true);
        binding.status.setText(R.string.files_upload_status_ready);
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
