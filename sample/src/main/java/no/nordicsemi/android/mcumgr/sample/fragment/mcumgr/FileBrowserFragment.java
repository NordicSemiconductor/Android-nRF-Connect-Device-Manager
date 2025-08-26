/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.viewmodel.FileBrowserViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import timber.log.Timber;

public abstract class FileBrowserFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("unused")
    private static final String TAG = FileBrowserFragment.class.getSimpleName();

    private static final int LOAD_FILE_LOADER_REQ = 2;
    private static final String EXTRA_FILE_URI = "uri";

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    private ActivityResultLauncher<String> fileBrowserLauncher;
    private FileBrowserViewModel viewModel;
    private Handler handler;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(FileBrowserViewModel.class);

        fileBrowserLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null)
                        return;
                    clearFileContent();

                    final String scheme = uri.getScheme();
                    if (scheme != null && scheme.equals("content")) {
                        // File name and size must be obtained from Content Provider
                        final Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_FILE_URI, uri);
                        LoaderManager.getInstance(this).restartLoader(LOAD_FILE_LOADER_REQ, bundle, this);
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull @NotNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getFileContent().observe(getViewLifecycleOwner(), bytes -> {
            if (bytes != null) {
                onFileLoaded(bytes);
            } else {
                onFileCleared();
            }
        });
    }

    /**
     * Returns the selected file content, or null, if no file has been selected.
     *
     * @return The file content, or null.
     */
    @Nullable
    byte[] getFileContent() {
        return viewModel.getFileContent().getValue();
    }

    void setFileContent(@NonNull final byte[] data) {
        viewModel.setFileContent(data);
    }

    /**
     * Releases the reference to the file content and calls {@link #onFileCleared()}.
     */
    void clearFileContent() {
        viewModel.setFileContent(null);
    }

    /**
     * Returns whether the file has been selected.
     *
     * @return True if the file has been selected, false otherwise.
     */
    boolean isFileLoaded() {
        return viewModel.isFileLoaded();
    }

    /**
     * A callback called as a result to {@link #clearFileContent()}.
     */
    protected abstract void onFileCleared();

    /**
     * A callback called after the file has been selected. The file content has not yet been loaded.
     * {@link #onFileLoaded(byte[])} will be called afterwards, when the content has been loaded.
     *
     * @param fileName the file name.
     * @param fileSize the file size in bytes.
     */
    protected abstract void onFileSelected(@NonNull final String fileName, final int fileSize);

    /**
     * A callback called when the file content has been loaded.
     */
    protected abstract void onFileLoaded(@NonNull final byte[] data);

    /**
     * A callback called when loading the file has failed.
     *
     * @param error the error text resource ID.
     */
    protected abstract void onFileLoadingFailed(@StringRes final int error);

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(final int id, @Nullable final Bundle args) {
        if (id == LOAD_FILE_LOADER_REQ) {
            final Uri uri = args.getParcelable(EXTRA_FILE_URI);
            return new CursorLoader(requireContext(), uri,
                    null/* projection */, null, null, null);
        }
        throw new UnsupportedOperationException("Invalid loader ID: " + id);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<Cursor> loader, final Cursor data) {
        if (data == null) {
            Toast.makeText(requireContext(), R.string.file_loader_error_loading_file_failed,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader.getId() == LOAD_FILE_LOADER_REQ) {
            final int displayNameColumn = data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            final int sizeColumn = data.getColumnIndex(MediaStore.MediaColumns.SIZE);

            if (displayNameColumn == -1 || sizeColumn == -1) {
                Toast.makeText(requireContext(), R.string.file_loader_error_loading_file_failed,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (data.moveToNext()) {
                final String fileName = data.getString(displayNameColumn);
                final int fileSize = data.getInt(sizeColumn);
                if (fileName == null || fileSize < 0) {
                    Toast.makeText(requireContext(), R.string.file_loader_error_loading_file_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                onFileSelected(fileName, fileSize);

                final CursorLoader cursorLoader = (CursorLoader) loader;
                new Thread(() -> loadContent(cursorLoader.getUri())).start();
            } else {
                Timber.e("Empty cursor");
                onFileLoadingFailed(R.string.file_loader_error_no_uri);
            }
            // Reset the loader as the URU read permission is one time only.
            // We keep the file content in the fragment so no need to load it again.
            // onLoaderReset(...) will be called after that.
            LoaderManager.getInstance(this).destroyLoader(LOAD_FILE_LOADER_REQ);
        }
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
        // ignore
    }

    /**
     * Opens the File Browser application.
     *
     * @param mimeType required MIME TYPE of a file.
     */
    @SuppressWarnings("SameParameterValue")
    void selectFile(@Nullable final String mimeType) {
        fileBrowserLauncher.launch(mimeType);
    }

    /**
     * Loads content from the stream.
     *
     * @param uri the URI of the file.
     */
    private void loadContent(@NonNull final Uri uri) {
        try (final InputStream is = requireContext().getContentResolver()
                .openInputStream(uri)) {
            if (is == null) {
                handler.post(() -> onFileLoadingFailed(R.string.file_loader_error_loading_file_failed));
                return;
            }

            final BufferedInputStream buf = new BufferedInputStream(is);
            final int size = buf.available();
            final byte[] bytes = new byte[size];
            try {
                int offset = 0;
                int retry = 0;
                while (offset < size && retry < 5) {
                    offset += buf.read(bytes, offset, size - offset);
                    retry++;
                }
            } finally {
                buf.close();
            }
            setFileContent(bytes);
        } catch (final IOException e) {
            Timber.e(e, "Reading file content failed");
            handler.post(() -> onFileLoadingFailed(R.string.file_loader_error_loading_file_failed));
        }
    }
}
