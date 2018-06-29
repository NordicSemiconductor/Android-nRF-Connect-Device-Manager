/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble.callback;

import android.support.annotation.NonNull;

import java.io.IOException;

import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.data.DataMerger;
import no.nordicsemi.android.ble.data.DataStream;

public class SmpMerger implements DataMerger {
    private Integer mExpectedLength;

    @Override
    public boolean merge(@NonNull final DataStream output, @NonNull final byte[] lastPacket, final int index) {
        // Add the new packet to the output stream
        output.write(lastPacket);

        // If it's a first or a single packet of this message, try to read the expected length
        if (index == 0) {
            try {
                mExpectedLength = McuMgrResponse.getExpectedLength(McuMgrScheme.BLE, lastPacket);
            } catch (final IOException e) {
                return true;
            }
        }
        // The message is complete when the stream size is equal to expected length
        return output.size() == mExpectedLength;
    }
}
