/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import io.runtime.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrManifestStateResponse extends McuMgrResponse {

    /**
     * Use {@link #getClassId()} to get the class ID as UUID.
     */
    @JsonProperty("class_id")
    public byte[] classId;
    /**
     * Use {@link #getVendorId()} to get the vendor ID as UUID.
     */
    @JsonProperty("vendor_id")
    public byte[] vendorId;
    @JsonProperty("downgrade_prevention_policy")
    public DowngradePreventionPolicy downgradePreventionPolicy;
    @JsonProperty("independent_updateability_policy")
    public IndependentUpdateabilityPolicy independentUpdateabilityPolicy;
    @JsonProperty("signature_verification_policy")
    public SignatureVerificationPolicy signatureVerificationPolicy;
    // In such case response will not contain ‘digest’ field, also remaining fields will not
    // contain any meaningful information.
    @JsonProperty("digest")
    public byte[] digest;
    @JsonProperty("digest_algorithm")
    public DigestAlgorithm digest_algorithm;
    @JsonProperty("signature_check")
    public SignatureVerification signatureCheck;
    @JsonProperty("sequence_number")
    public int sequenceNumber;
    /**
     * Use {@link #getVersion()} to get the human-readable version.
     */
    @JsonProperty("semantic_version")
    public int[] semanticVersion;

    @JsonCreator
    public McuMgrManifestStateResponse() {}

    /** Returns the Manifest class ID. */
    public UUID getClassId() {
        return convertBytesToUUID(classId);
    }

    /** Returns the Manifest vendor ID. */
    public UUID getVendorId() {
        return convertBytesToUUID(vendorId);
    }

    /**
     * Returns whether the manifest is owned by Nordic Semiconductor.
     * <br/>
     * The UUID is generated using the following code:
     * <code>uuid5(uuid.NAMESPACE_DNS, ‘nordicsemi.com’)</code>
     */
    public boolean isVendorNordic() {
        return getVendorId().equals(UUID.fromString("7617daa5-71fd-5a85-8f94-e28d735ce9f4"));
    }

    @Nullable
    private static UUID convertBytesToUUID(final byte @Nullable [] bytes) {
        if (bytes == null || bytes.length != 16)
            return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        final long msb = bb.getLong();
        final long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    public enum DigestAlgorithm {
        SHA_256(-16),
        SHA_512(-44);

        @JsonValue
        private final int code;

        DigestAlgorithm(int code) {
            this.code = code;
        }
    }

    public enum SignatureVerification {
        NOT_CHECKED(2),
        FAILED(3),
        PASSED(4);

        @JsonValue
        private final int code;

        SignatureVerification(int code) {
            this.code = code;
        }
    }

    public enum DowngradePreventionPolicy {
        /** No downgrade prevention. */
        DISABLED(1),
        /** Update forbidden if candidate version is lower than installed version */
        ENABLED(2),
        /** Unknown downgrade prevention policy */
        UNKNOWN(3);

        @JsonValue
        private final int code;

        DowngradePreventionPolicy(int code) {
            this.code = code;
        }
    }

    public enum IndependentUpdateabilityPolicy {
        /** Independent update is forbidden. */
        DENIED(1),
        /** Independent update is allowed. */
        ALLOWED(2),
        /** Unknown independent updateability policy. */
        UNKNOWN(3);

        @JsonValue
        private final int code;

        IndependentUpdateabilityPolicy(int code) {
            this.code = code;
        }
    }

    public enum SignatureVerificationPolicy {
        /** Do not verify the manifest signature */
        DISABLED(1),
        /** Verify the manifest signature only when performing update */
        ENABLED_ON_UPDATE(2),
        /** Verify the manifest signature only both when performing update and when booting */
        ENABLED_ON_UPDATE_AND_BOOT(3),
        /** Unknown signature verification policy */
        UNKNOWN(4);

        @JsonValue
        private final int code;

        SignatureVerificationPolicy(int code) {
            this.code = code;
        }
    }

    private enum ReleaseType {
        NORMAL(0, ""),
        RC(-1, "-rc"),
        BETA(-2, "-beta"),
        ALPHA(-3, "-alpha");

        private final int code;
        private final String text;

        static ReleaseType fromCode(int code) {
            for (ReleaseType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid release type: " + code);
        }

        ReleaseType(int code, String text) {
            this.code = code;
            this.text = text;
        }
    }

    /**
     * Returns human-readable version of the semantic version in format:
     * <code>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;[-&lt;type&gt;][.&lt;release_number&gt;]</code>,
     * e.g. 1.2.3-rc.4. or 1.0.0.
     * The release number is optional and present only for non-normal releases.
     * @return The version string or null if the semantic version is not set.
     * @throws IllegalArgumentException if the semantic version is invalid.
     * @see <a href="https://github.com/nrfconnect/sdk-nrf/blob/f441cf9c782099a82c4dc04fad5b110fe4d3072c/subsys/suit/metadata/src/suit_metadata.c#L23-L72">Source</a>
     */
    @Nullable
    public String getVersion() {
        if (semanticVersion == null) {
            return null;
        }

        // There can be only one negative value in the semantic version.
        // The negative value is the release type.
        // The value after the release type, if greater than 0, is the release number.
        // Normal releases have no release number.
        ReleaseType type = ReleaseType.NORMAL;
        int major = 0, minor = 0, patch = 0, releaseNumber = 0;

        // Iterate over the semantic version array.
        for (int i = 0; i < semanticVersion.length; i++) {
            // If the value is non-negative, assign the correct number. They all default to 0.
            if (semanticVersion[i] >= 0) {
                switch (i) {
                    case 0:
                        major = semanticVersion[i];
                        break;
                    case 1:
                        minor = semanticVersion[i];
                        break;
                    case 2:
                        patch = semanticVersion[i];
                        break;
                    default:
                        // Too many non-negative values.
                        throw new IllegalArgumentException("Invalid semantic version: " + Arrays.toString(semanticVersion));
                }
            } else {
                type = ReleaseType.fromCode(semanticVersion[i]);
                if (semanticVersion.length == i + 2 && semanticVersion[i + 1] >= 0) {
                    // The release type is followed by a version number.
                    releaseNumber = semanticVersion[i + 1];
                    break;
                } else if (semanticVersion.length == i + 1) {
                    break;
                } else {
                    // Duplicated release type or too many number.
                    throw new IllegalArgumentException("Invalid semantic version: " + Arrays.toString(semanticVersion));
                }
            }
        }
        return major + "." + minor + "." + patch + type.text + (releaseNumber > 0 ? "." + releaseNumber : "");
    }
}
