/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.DialogGenerateFileBinding;
import no.nordicsemi.android.mcumgr.sample.fragment.mcumgr.FilesUploadFragment;

public class GenerateFileDialogFragment extends DialogFragment {
    private InputMethodManager imm;

    public static DialogFragment getInstance() {
        return new GenerateFileDialogFragment();
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final LayoutInflater inflater = requireActivity().getLayoutInflater();
        final DialogGenerateFileBinding binding = DialogGenerateFileBinding.inflate(inflater);
        final EditText fileSize = binding.fileSize;

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.files_upload_generate_title)
                .setView(binding.getRoot())
                // Setting the positive button listener here would cause the dialog to dismiss.
                // We have to validate the value before.
                .setPositiveButton(R.string.files_action_generate, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> imm.showSoftInput(fileSize, InputMethodManager.SHOW_IMPLICIT));
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                final int size = Integer.parseInt(fileSize.getText().toString());

                final FilesUploadFragment parent = (FilesUploadFragment) getParentFragment();
                parent.onGenerateFileRequested(size);
                dismiss();
            } catch (final NumberFormatException e) {
                fileSize.setError(getString(R.string.files_upload_generate_error));
            }
        });
        return dialog;
    }
}
