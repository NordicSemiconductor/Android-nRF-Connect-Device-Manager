/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.adapter;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.ScannerActivity;
import io.runtime.mcumgr.sample.databinding.DeviceItemBinding;
import io.runtime.mcumgr.sample.viewmodel.DevicesLiveData;

@SuppressWarnings("unused")
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private List<DiscoveredBluetoothDevice> mDevices;
    private OnItemClickListener mOnItemClickListener;

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
        mOnItemClickListener = listener;
    }

    public DevicesAdapter(final ScannerActivity activity, final DevicesLiveData devicesLiveData) {
        setHasStableIds(true);
        devicesLiveData.observe(activity, devices -> {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                    new DeviceDiffCallback(mDevices, devices), false);
            mDevices = devices;
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
        final DiscoveredBluetoothDevice device = mDevices.get(position);
        final String deviceName = device.getName();

        if (!TextUtils.isEmpty(deviceName)) {
            holder.mBinding.deviceName.setText(deviceName);
            // Set device icon. This is just guessing, based on the device name.
            if (deviceName.toLowerCase(Locale.US).contains("zephyr"))
                holder.mBinding.icon.setImageResource(R.drawable.ic_device_zephyr);
            else if (deviceName.toLowerCase(Locale.US).contains("nimble"))
                holder.mBinding.icon.setImageResource(R.drawable.ic_device_mynewt);
            else
                holder.mBinding.icon.setImageResource(R.drawable.ic_device_other);
        } else {
            holder.mBinding.deviceName.setText(R.string.unknown_device);
            holder.mBinding.icon.setImageResource(R.drawable.ic_device_other);
        }
        holder.mBinding.deviceAddress.setText(device.getAddress());
        final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
        holder.mBinding.rssi.setImageLevel(rssiPercent);
    }

    @Override
    public long getItemId(final int position) {
        return mDevices.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return mDevices != null ? mDevices.size() : 0;
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final DeviceItemBinding mBinding;

        private ViewHolder(final DeviceItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            binding.deviceContainer.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(mDevices.get(getBindingAdapterPosition()).getDevice());
                }
            });
        }
    }
}
