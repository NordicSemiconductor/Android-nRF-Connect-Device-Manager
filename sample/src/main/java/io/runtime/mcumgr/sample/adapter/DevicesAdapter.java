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
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.ScannerActivity;
import io.runtime.mcumgr.sample.viewmodel.DevicesLiveData;

@SuppressWarnings("unused")
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private final ScannerActivity mContext;
    private List<DiscoveredBluetoothDevice> mDevices;
    private OnItemClickListener mOnItemClickListener;

    @FunctionalInterface
    public interface OnItemClickListener {
        /**
         * Callback called when the device row has been clicked.
         *
         * @param device the Bluetooth device under the clicked row.
         */
        void onItemClick(final BluetoothDevice device);
    }

    /**
     * Sets the listener that will be called when user clicks on a device row.
     *
     * @param listener the listener.
     */
    public void setOnItemClickListener(final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @SuppressWarnings("ConstantConditions")
    public DevicesAdapter(final ScannerActivity activity, final DevicesLiveData devicesLiveData) {
        mContext = activity;
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
        final View layoutView = LayoutInflater.from(mContext)
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final DiscoveredBluetoothDevice device = mDevices.get(position);
        final String deviceName = device.getName();

        if (!TextUtils.isEmpty(deviceName)) {
            holder.deviceName.setText(deviceName);
            // Set device icon. This is just guessing, based on the device name.
            if (deviceName.toLowerCase(Locale.US).contains("zephyr"))
                holder.icon.setImageResource(R.drawable.ic_device_zephyr);
            else if (deviceName.toLowerCase(Locale.US).contains("nimble"))
                holder.icon.setImageResource(R.drawable.ic_device_mynewt);
            else
                holder.icon.setImageResource(R.drawable.ic_device_other);
        } else {
            holder.deviceName.setText(R.string.unknown_device);
            holder.icon.setImageResource(R.drawable.ic_device_other);
        }
        holder.deviceAddress.setText(device.getAddress());
        final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
        holder.rssi.setImageLevel(rssiPercent);
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
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.device_address)
        TextView deviceAddress;
        @BindView(R.id.device_name)
        TextView deviceName;
        @BindView(R.id.rssi)
        ImageView rssi;

        private ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);

            view.findViewById(R.id.device_container).setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(mDevices.get(getAdapterPosition()).getDevice());
                }
            });
        }
    }
}
