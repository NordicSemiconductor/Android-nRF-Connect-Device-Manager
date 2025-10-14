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

public class WarningDialogFragment extends AppCompatDialogFragment {
    private static final String ARG_TITLE_RES_ID = "titleResId";
    private static final String ARG_MESSAGE_RES_ID = "messageResId";
    private static final String ARG_TITLE_STRING = "titleString";
    private static final String ARG_MESSAGE_STRING = "messageString";


    @NonNull
    public static DialogFragment getInstance(@StringRes final int titleResId,
                                             @StringRes final int messageResId) {
        final DialogFragment fragment = new WarningDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TITLE_RES_ID, titleResId);
        args.putInt(ARG_MESSAGE_RES_ID, messageResId);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    public static DialogFragment getInstance(@StringRes final int titleResId,
                                             @NonNull final String message) {
        final DialogFragment fragment = new WarningDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TITLE_RES_ID, titleResId);
        args.putString(ARG_MESSAGE_STRING, message);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    public static DialogFragment getInstance(@NonNull final String title,
                                             @NonNull final String message) {
        final DialogFragment fragment = new WarningDialogFragment();

        final Bundle args = new Bundle();
        args.putString(ARG_TITLE_STRING, title);
        args.putString(ARG_MESSAGE_STRING, message);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null) {
            throw new UnsupportedOperationException("WarningDialogFragment created without arguments");
        }

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(android.R.string.ok, null);

        // Handle title - prefer string over resource ID
        final String titleString = args.getString(ARG_TITLE_STRING);
        if (titleString != null) {
            builder.setTitle(titleString);
        } else {
            final int titleResId = args.getInt(ARG_TITLE_RES_ID);
            builder.setTitle(titleResId);
        }

        // Handle message - prefer string over resource ID
        final String messageString = args.getString(ARG_MESSAGE_STRING);
        if (messageString != null) {
            builder.setMessage(messageString);
        } else {
            final int messageResId = args.getInt(ARG_MESSAGE_RES_ID);
            builder.setMessage(messageResId);
        }

        return builder.create();
    }
}
