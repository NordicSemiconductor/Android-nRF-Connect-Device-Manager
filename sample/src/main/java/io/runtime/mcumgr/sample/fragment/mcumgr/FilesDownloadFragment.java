/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardFilesDownloadBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesDownloadFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    @Inject
    FsUtils fsUtils;

    private FragmentCardFilesDownloadBinding binding;

    private FilesDownloadViewModel viewModel;
    private InputMethodManager imm;
    private String partition;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(FilesDownloadViewModel.class);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardFilesDownloadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.fileName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s,
                                      final int start, final int before, final int count) {
                binding.filePath.setText(getString(R.string.files_file_path, partition, s));
            }
        });

        fsUtils.getPartition().observe(getViewLifecycleOwner(), partition -> {
            this.partition = partition;
            final String fileName = binding.fileName.getText().toString();
            binding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
        });
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> binding.progress.setProgress(progress));
        viewModel.getResponse().observe(getViewLifecycleOwner(), this::printContent);
        viewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionDownload.setEnabled(!busy));
        binding.actionHistory.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
            final Menu menu = popupMenu.getMenu();
            final Set<String> recents = fsUtils.getRecents();
            if (recents.isEmpty()) {
                menu.add(R.string.files_download_recent_files_empty).setEnabled(false);
            } else {
                final String[] recentsArray = recents.toArray(new String[0]);
                Arrays.sort(recentsArray); // Alphabetic order
                for (final String fileName : recentsArray) {
                    menu.add(fileName);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                binding.fileName.setError(null);
                binding.fileName.setText(item.getTitle());
                return true;
            });
            popupMenu.show();
        });
        binding.actionDownload.setOnClickListener(v -> {
            final String fileName = binding.fileName.getText().toString();
            if (TextUtils.isEmpty(fileName)) {
                binding.fileName.setError(getString(R.string.files_download_empty));
            } else {
                hideKeyboard();
                fsUtils.addRecent(fileName);
                viewModel.download(binding.filePath.getText().toString());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.fileName.getWindowToken(), 0);
    }

    private void printContent(@Nullable final byte[] data) {
        binding.divider.setVisibility(View.VISIBLE);
        binding.fileResult.setVisibility(View.VISIBLE);
        binding.image.setVisibility(View.VISIBLE);
        binding.image.setImageDrawable(null);

        if (data == null) {
            binding.fileResult.setText(R.string.files_download_error_file_not_found);
        } else {
            if (data.length == 0) {
                binding.fileResult.setText(R.string.files_download_file_empty);
            } else {
                final String path = binding.filePath.getText().toString();
                final Bitmap bitmap = FsUtils.toBitmap(getResources(), data);
                if (bitmap != null) {
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_image, path, data.length));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    binding.fileResult.setText(spannable);
                    binding.image.setImageBitmap(bitmap);
                } else {
                    final String content = new String(data);
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_file, path, data.length, content));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    binding.fileResult.setText(spannable);
                }
            }
        }
    }

    private void printError(@Nullable final McuMgrException error) {
        binding.divider.setVisibility(View.VISIBLE);
        binding.fileResult.setVisibility(View.VISIBLE);

        String message = StringUtils.toString(requireContext(), error);
        if (error instanceof McuMgrErrorException e) {
            final McuMgrErrorCode code = e.getCode();
            if (code == McuMgrErrorCode.UNKNOWN) {
                message = getString(R.string.files_download_error_file_not_found);
            }
        }
        if (message == null) {
            binding.fileResult.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.fileResult.setText(spannable);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s,
                                      final int start, final int count, final int after) {
            // empty
        }

        @Override
        public void afterTextChanged(final Editable s) {
            // empty
        }
    }
}
