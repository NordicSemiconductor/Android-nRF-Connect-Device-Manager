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

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardFilesDownloadBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesDownloadFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;
    @Inject
    FsUtils mFsUtils;

    private FragmentCardFilesDownloadBinding mBinding;

    private FilesDownloadViewModel mViewModel;
    private InputMethodManager mImm;
    private String mPartition;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(FilesDownloadViewModel.class);
        mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardFilesDownloadBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.fileName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s,
                                      final int start, final int before, final int count) {
                mBinding.filePath.setText(getString(R.string.files_file_path, mPartition, s));
            }
        });

        mFsUtils.getPartition().observe(getViewLifecycleOwner(), partition -> {
            mPartition = partition;
            final String fileName = mBinding.fileName.getText().toString();
            mBinding.filePath.setText(getString(R.string.files_file_path, partition, fileName));
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mBinding.progress.setProgress(progress));
        mViewModel.getResponse().observe(getViewLifecycleOwner(), this::printContent);
        mViewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> mBinding.actionDownload.setEnabled(!busy));
        mBinding.actionHistory.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
            final Menu menu = popupMenu.getMenu();
            final Set<String> recents = mFsUtils.getRecents();
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
                mBinding.fileName.setError(null);
                mBinding.fileName.setText(item.getTitle());
                return true;
            });
            popupMenu.show();
        });
        mBinding.actionDownload.setOnClickListener(v -> {
            final String fileName = mBinding.fileName.getText().toString();
            if (TextUtils.isEmpty(fileName)) {
                mBinding.fileName.setError(getString(R.string.files_download_empty));
            } else {
                hideKeyboard();
                mFsUtils.addRecent(fileName);
                mViewModel.download(mBinding.filePath.getText().toString());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void hideKeyboard() {
        mImm.hideSoftInputFromWindow(mBinding.fileName.getWindowToken(), 0);
    }

    private void printContent(@Nullable final byte[] data) {
        mBinding.divider.setVisibility(View.VISIBLE);
        mBinding.fileResult.setVisibility(View.VISIBLE);
        mBinding.image.setVisibility(View.VISIBLE);
        mBinding.image.setImageDrawable(null);

        if (data == null) {
            mBinding.fileResult.setText(R.string.files_download_error_file_not_found);
        } else {
            if (data.length == 0) {
                mBinding.fileResult.setText(R.string.files_download_file_empty);
            } else {
                final String path = mBinding.filePath.getText().toString();
                final Bitmap bitmap = FsUtils.toBitmap(getResources(), data);
                if (bitmap != null) {
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_image, path, data.length));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    mBinding.fileResult.setText(spannable);
                    mBinding.image.setImageBitmap(bitmap);
                } else {
                    final String content = new String(data);
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_file, path, data.length, content));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    mBinding.fileResult.setText(spannable);
                }
            }
        }
    }

    private void printError(@Nullable final String error) {
        mBinding.divider.setVisibility(View.VISIBLE);
        mBinding.fileResult.setVisibility(View.VISIBLE);

        if (error != null) {
            final SpannableString spannable = new SpannableString(error);
            spannable.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(requireContext(), R.color.colorError)),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            mBinding.fileResult.setText(spannable);
        } else {
            mBinding.fileResult.setText(null);
        }
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
