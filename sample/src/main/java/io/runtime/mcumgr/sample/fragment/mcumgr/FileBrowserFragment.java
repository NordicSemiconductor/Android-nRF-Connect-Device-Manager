/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.utils.Utils;
import timber.log.Timber;

public abstract class FileBrowserFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("unused")
    private static final String TAG = FileBrowserFragment.class.getSimpleName();

    private static final int SELECT_FILE_REQ = 1;
    private static final int LOAD_FILE_LOADER_REQ = 2;
    private static final String EXTRA_FILE_URI = "uri";

    private static final String SIS_DATA = "data";
    private static final String SIS_URI = "uri";

    private byte[] mFileContent;
    private Uri mFileUri;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mFileContent = savedInstanceState.getByteArray(SIS_DATA);
            mFileUri = savedInstanceState.getParcelable(SIS_URI);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(SIS_DATA, mFileContent);
        outState.putParcelable(SIS_URI, mFileUri);
    }

    /**
     * Returns the selected file content, or null, if no file has been selected.
     *
     * @return The file content, or null.
     */
    @Nullable
    byte[] getFileContent() {
        return mFileContent;
    }

    void setFileContent(@NonNull final byte[] data) {
        mFileContent = data;
        onFileLoaded(data);
    }

    /**
     * Releases the reference to the file content and calls {@link #onFileCleared()}.
     */
    void clearFileContent() {
        mFileContent = null;
        onFileCleared();
    }

    /**
     * Returns whether the file has been selected.
     *
     * @return True if the file has been selected, false otherwise.
     */
    boolean isFileLoaded() {
        return mFileContent != null;
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


    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_FILE_REQ:
                    clearFileContent();

                    final Uri uri = data.getData();

                    if (uri == null) {
                        Toast.makeText(requireContext(), R.string.file_loader_error_no_uri,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String scheme = uri.getScheme();
                    if (scheme != null && scheme.equals("content")) {
                        // File name and size must be obtained from Content Provider
                        final Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_FILE_URI, uri);
                        LoaderManager.getInstance(this).restartLoader(LOAD_FILE_LOADER_REQ, bundle, this);
                    }
                    break;
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(final int id, @Nullable final Bundle args) {
        switch (id) {
            case LOAD_FILE_LOADER_REQ:
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

        switch (loader.getId()) {
            case LOAD_FILE_LOADER_REQ: {
                final int displayNameColumn = data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                final int sizeColumn = data.getColumnIndex(MediaStore.MediaColumns.SIZE);

                if (displayNameColumn == -1 || sizeColumn == -1) {
                    Toast.makeText(requireContext(), R.string.file_loader_error_loading_file_failed,
                            Toast.LENGTH_SHORT).show();
                    break;
                }

                if (data.moveToNext()) {
                    final String fileName = data.getString(displayNameColumn);
                    final int fileSize = data.getInt(sizeColumn);
                    if (fileName == null || fileSize < 0) {
                        Toast.makeText(requireContext(), R.string.file_loader_error_loading_file_failed,
                                Toast.LENGTH_SHORT).show();
                        break;
                    }

                    onFileSelected(fileName, fileSize);
                    try {
                        final CursorLoader cursorLoader = (CursorLoader) loader;
                        final InputStream is = requireContext().getContentResolver()
                                .openInputStream(cursorLoader.getUri());
                        loadContent(is);
                    } catch (final FileNotFoundException e) {
                        Timber.e(e, "File not found");
                        onFileLoadingFailed(R.string.file_loader_error_no_uri);
                    }
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
    void selectFile(@Nullable final String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ);
        } else {
            Toast.makeText(requireContext(), R.string.file_loader_error_no_file_browser,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads content from the stream.
     *
     * @param is the input stream to read the file from.
     */
    private void loadContent(@Nullable final InputStream is) {
        if (is == null) {
            onFileLoadingFailed(R.string.file_loader_error_loading_file_failed);
            return;
        }

        try {
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
            mFileContent = bytes;
            onFileLoaded(bytes);
        } catch (final IOException e) {
            Timber.e(e, "Reading file content failed");
            onFileLoadingFailed(R.string.file_loader_error_loading_file_failed);
        }
    }
}
