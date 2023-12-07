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

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.image.McuMgrImage;
import io.runtime.mcumgr.image.SUITImage;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageUploadBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.dialog.SelectImageDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageUploadFragment extends FileBrowserFragment implements Injectable, SelectImageDialogFragment.OnImageSelectedListener {
    private static final int REQUEST_UPLOAD = 0;

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    private FragmentCardImageUploadBinding binding;

    private ImageUploadViewModel viewModel;
    private boolean requiresImageSelection;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ImageUploadViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardImageUploadBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.image_upload_dialog_help_title,
                        R.string.image_upload_dialog_help_message);
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

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.actionUpload.setEnabled(isFileLoaded());
            binding.actionCancel.setEnabled(state.canCancel());
            binding.actionPauseResume.setEnabled(state.canPauseOrResume());
            binding.actionPauseResume.setText(state == ImageUploadViewModel.State.PAUSED ?
                    R.string.image_action_resume : R.string.image_action_pause);

            binding.actionSelectFile.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionUpload.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
            binding.actionCancel.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            binding.actionPauseResume.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
            // Update status
            switch (state) {
                case VALIDATING -> binding.status.setText(R.string.image_upload_status_validating);
                case UPLOADING -> binding.status.setText(R.string.image_upload_status_uploading);
                case PAUSED -> binding.status.setText(R.string.image_upload_status_paused);
                case COMPLETE -> {
                    clearFileContent();
                    binding.status.setText(R.string.image_upload_status_completed);
                    binding.speed.setText(null);
                }
            }
        });
        viewModel.getTransferSpeed().observe(getViewLifecycleOwner(), speed ->
                binding.speed.setText(getString(R.string.image_upload_speed, speed))
        );
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress ->
                binding.progress.setProgress(progress)
        );
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.actionSelectFile.setVisibility(View.VISIBLE);
            binding.actionUpload.setVisibility(View.VISIBLE);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
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
            binding.actionUpload.setVisibility(View.VISIBLE);
            binding.actionUpload.setEnabled(false);
            binding.actionCancel.setVisibility(View.GONE);
            binding.actionPauseResume.setVisibility(View.GONE);
        });
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            binding.actionSelectFile.setEnabled(!busy);
            binding.actionUpload.setEnabled(isFileLoaded() && !busy);
        });

        // Configure SELECT FILE action
        binding.actionSelectFile.setOnClickListener(v -> selectFile("*/*"));

        // Restore UPLOAD action state after rotation
        binding.actionUpload.setEnabled(isFileLoaded());
        binding.actionUpload.setOnClickListener(v -> {
            if (requiresImageSelection) {
                final DialogFragment dialog = SelectImageDialogFragment.getInstance(REQUEST_UPLOAD);
                dialog.show(getChildFragmentManager(), null);
            } else {
                onImageSelected(REQUEST_UPLOAD, 0);
            }
        });

        // Cancel and Pause/Resume buttons
        binding.actionCancel.setOnClickListener(v -> viewModel.cancel());
        binding.actionPauseResume.setOnClickListener(v -> {
            if (viewModel.getState().getValue() == ImageUploadViewModel.State.UPLOADING) {
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

    @Override
    protected void onFileCleared() {
        binding.actionUpload.setEnabled(false);
    }

    @Override
    protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
        binding.fileName.setText(fileName);
        binding.fileSize.setText(getString(R.string.image_upgrade_size_value, fileSize));
    }

    @Override
    protected void onFileLoaded(@NonNull final byte[] data) {
        try {
            final byte[] hash = McuMgrImage.getHash(data);
            binding.fileHash.setText(StringUtils.toHex(hash));
            binding.actionUpload.setEnabled(true);
            binding.status.setText(R.string.image_upgrade_status_ready);
            requiresImageSelection = true;
        } catch (final McuMgrException e) {
            // Support for SUIT (Software Update for Internet of Things) format.
            try {
                // Try parsing SUIT file.
                final byte[] hash = SUITImage.getHash(data);
                binding.fileHash.setText(StringUtils.toHex(hash));
                binding.actionUpload.setEnabled(true);
                binding.status.setText(R.string.image_upgrade_status_ready);
                requiresImageSelection = false;
            } catch (final Exception e2) {
                binding.fileHash.setText(null);
                clearFileContent();
                onFileLoadingFailed(R.string.image_error_file_not_valid);
            }
        }
    }

    @Override
    protected void onFileLoadingFailed(final int error) {
        binding.status.setText(error);
    }

    @Override
    public void onImageSelected(final int requestId, final int image) {
        final byte[] data = getFileContent();
        if (data != null) {
            viewModel.upload(data, image);
        }
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
