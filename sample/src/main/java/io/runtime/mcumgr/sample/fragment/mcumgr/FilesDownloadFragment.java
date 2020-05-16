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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesDownloadFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;
    @Inject
    FsUtils mFsUtils;

    @BindView(R.id.file_name)
    EditText mFileName;
    @BindView(R.id.file_path)
    TextView mFilePath;
    @BindView(R.id.action_history)
    View mHistoryAction;
    @BindView(R.id.action_download)
    Button mDownloadAction;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.divider)
    View mDivider;
    @BindView(R.id.file_result)
    TextView mResult;
    @BindView(R.id.image)
    ImageView mImage;

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
        return inflater.inflate(R.layout.fragment_card_files_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mFileName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s,
                                      final int start, final int before, final int count) {
                mFilePath.setText(getString(R.string.files_file_path, mPartition, s));
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFsUtils.getPartition().observe(getViewLifecycleOwner(), partition -> {
            mPartition = partition;
            final String fileName = mFileName.getText().toString();
            mFilePath.setText(getString(R.string.files_file_path, partition, fileName));
        });
        mViewModel.getProgress().observe(getViewLifecycleOwner(), progress -> mProgress.setProgress(progress));
        mViewModel.getResponse().observe(getViewLifecycleOwner(), this::printContent);
        mViewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> mDownloadAction.setEnabled(!busy));
        mHistoryAction.setOnClickListener(v -> {
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
                mFileName.setError(null);
                mFileName.setText(item.getTitle());
                return true;
            });
            popupMenu.show();
        });
        mDownloadAction.setOnClickListener(v -> {
            final String fileName = mFileName.getText().toString();
            if (TextUtils.isEmpty(fileName)) {
                mFileName.setError(getString(R.string.files_download_empty));
            } else {
                hideKeyboard();
                mFsUtils.addRecent(fileName);
                mViewModel.download(mFilePath.getText().toString());
            }
        });
    }

    private void hideKeyboard() {
        mImm.hideSoftInputFromWindow(mFileName.getWindowToken(), 0);
    }

    private void printContent(@Nullable final byte[] data) {
        mDivider.setVisibility(View.VISIBLE);
        mResult.setVisibility(View.VISIBLE);
        mImage.setVisibility(View.VISIBLE);
        mImage.setImageDrawable(null);

        if (data == null) {
            mResult.setText(R.string.files_download_error_file_not_found);
        } else {
            if (data.length == 0) {
                mResult.setText(R.string.files_download_file_empty);
            } else {
                final String path = mFilePath.getText().toString();
                final Bitmap bitmap = FsUtils.toBitmap(getResources(), data);
                if (bitmap != null) {
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_image, path, data.length));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    mResult.setText(spannable);
                    mImage.setImageBitmap(bitmap);
                } else {
                    final String content = new String(data);
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_file, path, data.length, content));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    mResult.setText(spannable);
                }
            }
        }
    }

    private void printError(@Nullable final String error) {
        mDivider.setVisibility(View.VISIBLE);
        mResult.setVisibility(View.VISIBLE);

        if (error != null) {
            final SpannableString spannable = new SpannableString(error);
            spannable.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(requireContext(), R.color.colorError)),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            mResult.setText(spannable);
        } else {
            mResult.setText(null);
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
