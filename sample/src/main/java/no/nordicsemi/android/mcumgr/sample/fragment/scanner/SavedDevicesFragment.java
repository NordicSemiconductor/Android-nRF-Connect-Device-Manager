/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.MainActivity;
import no.nordicsemi.android.mcumgr.sample.adapter.DevicesAdapter;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentSavedDevicesBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.utils.Utils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.ViewModelFactory;
import no.nordicsemi.android.mcumgr.sample.viewmodel.scanner.SavedDevicesViewModel;
import no.nordicsemi.android.mcumgr.sample.viewmodel.scanner.ScannerStateLiveData;

public class SavedDevicesFragment extends Fragment implements Injectable, DevicesAdapter.OnItemClickListener {

    @Inject
    ViewModelFactory viewModelFactory;

    private SavedDevicesViewModel scannerViewModel;
    private FragmentSavedDevicesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentSavedDevicesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = view.getContext();

        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(getViewModelStore(), viewModelFactory)
                .get(SavedDevicesViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), this::refresh);

        // Configure the recycler view
        final RecyclerView recyclerView = binding.recyclerViewBleDevices;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        final DevicesAdapter adapter =
                new DevicesAdapter(getViewLifecycleOwner(), scannerViewModel.getDevices());
        adapter.setOnItemClickListener(this);
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            final Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            adapter.setHorizontalPadding(Math.max(bars.left, bars.right));
            return WindowInsetsCompat.CONSUMED;
        });
        recyclerView.setAdapter(adapter);

        // Set up permission request launcher
        final ActivityResultLauncher<String> requestPermission =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        result -> scannerViewModel.refresh()
                );

        // Configure views
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> requestBluetoothEnabled());
        if (Utils.isSorAbove()) {
            binding.noBluetoothPermission.actionGrantBluetoothPermission.setOnClickListener(v -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                        Manifest.permission.BLUETOOTH_CONNECT)) {
                    Utils.markBluetoothScanPermissionRequested(context);
                }
                requestPermission.launch(Manifest.permission.BLUETOOTH_CONNECT);
            });
            binding.noBluetoothPermission.actionPermissionSettings.setOnClickListener(v -> {
                Utils.clearBluetoothPermissionRequested(context);
                openPermissionSettings();
            });
        }
    }

    @Override
    public void onItemClick(@NonNull final BluetoothDevice device) {
        final Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE, device);
        startActivity(intent);
    }

    /**
     * Shows saved devices.
     */
    private void refresh(final ScannerStateLiveData state) {
        final Context context = requireContext();

        // On Android 12+ a new BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions need to be
        // requested.
        //
        // Note: This has to be done before asking user to enable Bluetooth, as
        //       sending BluetoothAdapter.ACTION_REQUEST_ENABLE intent requires
        //       BLUETOOTH_CONNECT permission.
        if (!Utils.isSorAbove() || Utils.isBluetoothConnectPermissionGranted(context)) {
            binding.noBluetoothPermission.getRoot().setVisibility(View.GONE);

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                binding.bluetoothOff.getRoot().setVisibility(View.GONE);

                // We can show saved devices.
                scannerViewModel.showDevices();

                if (!state.hasRecords()) {
                    binding.noSavedDevices.getRoot().setVisibility(View.VISIBLE);
                } else {
                    binding.noSavedDevices.getRoot().setVisibility(View.GONE);
                }
            } else {
                binding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                binding.noSavedDevices.getRoot().setVisibility(View.GONE);
                binding.noBluetoothPermission.getRoot().setVisibility(View.GONE);

                scannerViewModel.clear();
            }
        } else {
            binding.noBluetoothPermission.getRoot().setVisibility(View.VISIBLE);
            binding.bluetoothOff.getRoot().setVisibility(View.GONE);
            binding.noSavedDevices.getRoot().setVisibility(View.GONE);

            final boolean deniedForever = Utils.isBluetoothScanPermissionDeniedForever(requireActivity());
            binding.noBluetoothPermission.actionGrantBluetoothPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            binding.noBluetoothPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Opens application settings in Android Settings app.
     */
    private void openPermissionSettings() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Shows a prompt to the user to enable Bluetooth on the device.
     *
     * @implSpec On Android 12+ BLUETOOTH_CONNECT permission needs to be granted before calling
     *           this method. Otherwise, the app would crash with {@link SecurityException}.
     */
    @SuppressLint("MissingPermission")
    private void requestBluetoothEnabled() {
        if (Utils.isBluetoothConnectPermissionGranted(requireContext())) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }
}
