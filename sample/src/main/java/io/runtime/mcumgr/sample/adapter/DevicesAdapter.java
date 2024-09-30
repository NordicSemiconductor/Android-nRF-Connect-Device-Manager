/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.adapter;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.DeviceItemBinding;
import io.runtime.mcumgr.sample.viewmodel.scanner.DevicesLiveData;

@SuppressWarnings("unused")
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private List<DiscoveredBluetoothDevice> devices;
    private OnItemClickListener onItemClickListener;

    @FunctionalInterface
    public interface OnItemClickListener {
        /**
         * Callback called when the device row has been clicked.
         *
         * @param device the Bluetooth device under the clicked row.
         */
        void onItemClick(@NonNull final BluetoothDevice device);
    }

    /**
     * Sets the listener that will be called when user clicks on a device row.
     *
     * @param listener the listener.
     */
    public void setOnItemClickListener(final OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public DevicesAdapter(final LifecycleOwner owner, final DevicesLiveData devicesLiveData) {
        setHasStableIds(true);
        devicesLiveData.observe(owner, devices -> {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                    new DeviceDiffCallback(this.devices, devices), false);
            this.devices = devices;
            result.dispatchUpdatesTo(this);
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final DeviceItemBinding binding = DeviceItemBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final DiscoveredBluetoothDevice device = devices.get(position);
        final String deviceName = device.getName();

        if (!TextUtils.isEmpty(deviceName)) {
            holder.binding.deviceName.setText(deviceName);
            holder.binding.deviceName.setEnabled(true);
            // Set device icon. This is just guessing, based on the device name.
            if (deviceName.toLowerCase(Locale.US).contains("zephyr"))
                holder.binding.icon.setImageResource(R.drawable.ic_device_zephyr);
            else if (deviceName.toLowerCase(Locale.US).contains("nimble"))
                holder.binding.icon.setImageResource(R.drawable.ic_device_mynewt);
            else
                holder.binding.icon.setImageResource(R.drawable.ic_device_other);
        } else {
            holder.binding.deviceName.setText(R.string.unknown_device);
            holder.binding.deviceName.setEnabled(false);
            holder.binding.icon.setImageResource(R.drawable.ic_device_other);
        }
        holder.binding.deviceAddress.setText(device.getAddress());
        final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
        holder.binding.rssi.setImageLevel(rssiPercent);
        holder.binding.rssi.setVisibility(device.getRssi() != -128 ? View.VISIBLE : View.GONE);
    }

    @Override
    public long getItemId(final int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    public final class ViewHolder extends RecyclerView.ViewHolder {
        private final DeviceItemBinding binding;

        private ViewHolder(final DeviceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.deviceContainer.setOnClickListener(v -> {
                final OnItemClickListener listener = onItemClickListener;
                final List<DiscoveredBluetoothDevice> devices = DevicesAdapter.this.devices;
                if (listener != null && devices != null) {
                    try {
                        listener.onItemClick(devices.get(getBindingAdapterPosition()).getDevice());
                    } catch (final IndexOutOfBoundsException e) {
                        // Do nothing
                    }
                }
            });
        }
    }
}
