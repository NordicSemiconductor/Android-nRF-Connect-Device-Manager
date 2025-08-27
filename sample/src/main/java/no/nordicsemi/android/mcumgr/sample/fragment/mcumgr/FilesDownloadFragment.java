/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.exception.McuMgrErrorException;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardFilesDownloadBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.utils.FsUtils;
import no.nordicsemi.android.mcumgr.sample.utils.StringUtils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.FilesDownloadViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesDownloadFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    @Inject
    FsUtils fsUtils;

    private FragmentCardFilesDownloadBinding binding;

    private FilesDownloadViewModel viewModel;
    private InputMethodManager imm;
    private String partition;

    private ActivityResultLauncher<FileData> saveFileLauncher;

    static class FileData {
        private final String fileName;
        private final String mimeType;

        public FileData(String fileName) {
            this.fileName = fileName;

            final String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
            final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            this.mimeType = Objects.requireNonNullElse(mimeType, "*/*");
        }
    }

    /**
     * A custom Activity result contract to create a new document.
     * <p>
     * The one form {@link androidx.activity.result.contract.ActivityResultContracts} requires
     * setting the MIME TYPE at the time of registration and cannot be changed later.
     * <p>
     * This contract allows to set the MIME TYPE when the file name is known.
     * @see FileData
     */
    static class CreateDocument extends ActivityResultContract<FileData, Uri> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, FileData input) {
            return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .setType(input.mimeType)
                    .putExtra(Intent.EXTRA_TITLE, input.fileName);
        }

        @Nullable
        @Override
        public SynchronousResult<Uri> getSynchronousResult(@NonNull Context context, FileData input) {
            return null;
        }

        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode == Activity.RESULT_OK && intent != null)
                return intent.getData();
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(FilesDownloadViewModel.class);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        saveFileLauncher = registerForActivityResult(new CreateDocument(), this::save);
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
        binding.actionSave.setOnClickListener(v -> {
            final String fileName = binding.fileName.getText().toString();
            saveFileLauncher.launch(new FileData(fileName));
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

    /**
     * Saves the downloaded file to the selected location.
     * @param uri the URI of the file to save to.
     */
    private void save(final @Nullable Uri uri) {
        final byte[] data = viewModel.getResponse().getValue();
        if (uri == null || data == null)
            return;
        try (final OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
            os.write(data);
            os.flush();
            Toast.makeText(requireContext(), R.string.files_download_saved, Toast.LENGTH_SHORT).show();
        } catch (final IOException e) {
            printError(new McuMgrException(e));
        }
    }

    private void printContent(@Nullable final byte[] data) {
        binding.divider2.setVisibility(View.VISIBLE);
        binding.fileResult.setVisibility(View.VISIBLE);
        binding.image.setVisibility(View.VISIBLE);
        binding.image.setImageDrawable(null);
        binding.actionSave.setEnabled(false);

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
                    binding.actionSave.setEnabled(true);
                } else {
                    final String content = new String(data);
                    final SpannableString spannable = new SpannableString(
                            getString(R.string.files_download_file, path, data.length, content));
                    spannable.setSpan(new StyleSpan(Typeface.BOLD),
                            0, path.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    binding.fileResult.setText(spannable);
                    binding.actionSave.setEnabled(true);
                }
            }
        }
    }

    private void printError(@Nullable final McuMgrException error) {
        binding.divider2.setVisibility(View.VISIBLE);
        binding.fileResult.setVisibility(View.VISIBLE);
        binding.actionSave.setEnabled(false);
        binding.image.setImageDrawable(null);

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
