/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardExecBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.utils.ShellUtils;
import no.nordicsemi.android.mcumgr.sample.utils.StringUtils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.ExecViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ExecFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    @Inject
    ShellUtils execUtils;

    private FragmentCardExecBinding binding;

    private ExecViewModel viewModel;
    private InputMethodManager imm;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ExecViewModel.class);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardExecBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.execCommand.setSelection(binding.execCommand.getText().length());

        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionSend.setEnabled(!busy));
        viewModel.getCommand().observe(getViewLifecycleOwner(), this::printCommand);
        viewModel.getOutput().observe(
                getViewLifecycleOwner(),
                this::printOutput
        );
        viewModel.getError().observe(
                getViewLifecycleOwner(),
                error -> printOutput(StringUtils.toString(requireContext(), error))
        );
        binding.actionHistory.setOnClickListener(v -> {
            final PopupMenu popupMenu = new PopupMenu(requireContext(), v);
            final Menu menu = popupMenu.getMenu();
            final Set<String> recents = execUtils.getRecents();
            if (recents.isEmpty()) {
                menu.add(R.string.exec_recent_commands_empty).setEnabled(false);
            } else {
                final String[] recentsArray = recents.toArray(new String[0]);
                Arrays.sort(recentsArray); // Alphabetic order
                for (final String fileName : recentsArray) {
                    menu.add(fileName);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                binding.execCommand.setError(null);
                binding.execCommand.setText(item.getTitle());
                return true;
            });
            popupMenu.show();
        });
        binding.actionSend.setOnClickListener(v -> {
            final String command = binding.execCommand.getText().toString();

            hideKeyboard();

            execUtils.addRecent(command);
            binding.execCommand.setText(null);
            viewModel.exec(command, null);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.execCommand.getWindowToken(), 0);
    }

    private void printCommand(@Nullable final String text) {
        final TextView view = binding.execConsole;
        final CharSequence currentText = view.getText();
        if (TextUtils.isEmpty(text)) {
            view.setText(getString(R.string.exec_console_cmd, currentText, ""));
        } else {
            view.setText(getString(R.string.exec_console_cmd, currentText, text));
        }
    }

    private void printOutput(@Nullable final String text) {
        final TextView view = binding.execConsole;
        final CharSequence currentText = view.getText();
        if (TextUtils.isEmpty(text)) {
            view.setText(getString(R.string.exec_console_output, currentText, ""));
        } else {
            view.setText(getString(R.string.exec_console_output, currentText, text));
        }
    }
}
