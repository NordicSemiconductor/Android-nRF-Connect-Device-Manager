/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.ota.ReleaseData;
import no.nordicsemi.android.ota.ReleaseInformation;

public class ReleaseInformationDialogFragment extends AppCompatDialogFragment {
    private static final String ARG_UPDATE_AVAILABLE = "updateAvailable";
    private static final String ARG_RELEASE_NOTES = "releaseNotes";
    private static final String ARG_VERSION = "version";
    private static final String ARG_SIZE = "size";
    private static final String ARG_LOCATION = "location";

    public interface OnDownloadClickedListener {
        void onDownload(String location);
    }

    @NonNull
    public static DialogFragment getInstance(final ReleaseInformation releaseInformation) {
        final DialogFragment fragment = new ReleaseInformationDialogFragment();

        final Bundle args = new Bundle();
        args.putBoolean(ARG_UPDATE_AVAILABLE, releaseInformation instanceof ReleaseInformation.UpdateAvailable);
        if (releaseInformation instanceof ReleaseInformation.UpdateAvailable update) {
            final ReleaseData release = update.getRelease();
            args.putString(ARG_RELEASE_NOTES, release.getReleaseNotes());
            args.putString(ARG_VERSION, release.getAppVersion());
            args.putLong(ARG_SIZE, release.getSize());
            args.putString(ARG_LOCATION, release.getLocation());
        }
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null) {
            throw new UnsupportedOperationException("ReleaseInformationDialogFragment created without arguments");
        }

        final boolean updateAvailable = getArguments().getBoolean(ARG_UPDATE_AVAILABLE);
        @StringRes final int titleResId = updateAvailable ?
                R.string.ota_update_available_title : R.string.ota_up_to_date_title;
        final String message = updateAvailable ?
                getString(R.string.ota_update_available_message,
                        getArguments().getString(ARG_VERSION),
                        getArguments().getLong(ARG_SIZE),
                        getArguments().getString(ARG_RELEASE_NOTES))
                : getString(R.string.ota_up_to_date_message);

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_help)
                .setTitle(titleResId)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null);
        if (updateAvailable) {
            final String location = getArguments().getString(ARG_LOCATION);
            builder.setPositiveButton(R.string.action_download, (dialog, which) -> {
                final OnDownloadClickedListener listener = (OnDownloadClickedListener) getParentFragment();
                if (listener != null)
                    listener.onDownload(location);
            });
            builder.setNegativeButton(android.R.string.cancel, null);
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }
        return builder.create();
    }
}
