/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.ble.callback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.runtime.mcumgr.McuMgrScheme;
import io.runtime.mcumgr.response.McuMgrResponse;
import no.nordicsemi.android.ble.data.DataMerger;
import no.nordicsemi.android.ble.data.DataStream;

public class SmpMerger implements DataMerger {
    private Integer mExpectedLength;

    @Override
    public boolean merge(@NonNull final DataStream output, @Nullable final byte[] lastPacket, final int index) {
        // Add the new packet to the output stream
        output.write(lastPacket);

        // If it's a first or a single packet of this message, try to read the expected length
        if (index == 0) {
            try {
                // This should never happen, but let's not crash if it did
                if (lastPacket == null) {
                    return true;
                }
                // Read the expected length from the header
                mExpectedLength = McuMgrResponse.getExpectedLength(McuMgrScheme.BLE, lastPacket);
            } catch (final IOException e) {
                return true;
            }
        }
        // The message is complete when the stream size is equal to expected length
        return output.size() == mExpectedLength;
    }
}
