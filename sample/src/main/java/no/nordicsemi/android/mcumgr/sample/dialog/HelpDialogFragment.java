/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import no.nordicsemi.android.mcumgr.sample.R;

public class HelpDialogFragment extends AppCompatDialogFragment {
    private static final String ARG_TITLE_RES_ID = "titleResId";
    private static final String ARG_MESSAGE_RES_ID = "messageResId";
    private static final String ARG_LINK = "link";


    @NonNull
    public static DialogFragment getInstance(@StringRes final int titleResId,
                                             @StringRes final int messageResId) {
        return getInstance(titleResId, messageResId, null);
    }

    @NonNull
    public static DialogFragment getInstance(@StringRes final int titleResId,
                                             @StringRes final int messageResId,
                                             final String link) {
        final DialogFragment fragment = new HelpDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TITLE_RES_ID, titleResId);
        args.putInt(ARG_MESSAGE_RES_ID, messageResId);
        args.putString(ARG_LINK, link);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null) {
            throw new UnsupportedOperationException("HelpDialogFragment created without arguments");
        }

        final int titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
        final int messageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
        final String link = getArguments().getString(ARG_LINK);

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_help)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok, null);
        if (link != null) {
            builder.setNeutralButton(R.string.action_learn_more, (dialog, which) -> {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                try {
                    startActivity(browserIntent);
                } catch (Exception ignored) {
                    // Nothing to do
                }
            });
        }
        return builder.create();
    }
}
