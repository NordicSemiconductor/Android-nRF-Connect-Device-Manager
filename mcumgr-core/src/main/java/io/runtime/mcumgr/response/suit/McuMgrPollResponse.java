/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

import io.runtime.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrPollResponse extends McuMgrResponse {


    /**
     * Session identifier. Non-zero value, unique for image request.
     * Not provided if there is no pending image request.
     */
    @JsonProperty("stream_session_id")
    public int streamSessionId;

    /**
     * Resource identifier, typically in form of a URI.
     * Not provided if there is no pending image request.
     */
    @JsonProperty("resource_id")
    public byte[] resourceId;

    /**
     * Checks whether the device is awaiting a resource.
     * @return true, if the client should deliver requested image to proceed with the update,
     * false otherwise.
     */
    public boolean isRequestingResource() {
        return streamSessionId != 0;
    }

    /**
     * Returns the resource URI, or null if no resource is requested.
     * @return the resource URI.
     * @throws IllegalArgumentException if the resource ID is not a valid URI.
     */
    @Nullable
    public URI getResourceUri() {
        if (resourceId == null) {
            return null;
        }
        return URI.create(new String(resourceId));
    }

    @JsonCreator
    public McuMgrPollResponse() {}
}
