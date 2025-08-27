/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.suit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

/** @noinspection unused*/
public class McuMgrManifestStateResponse extends McuMgrResponse {
    // Vendor UUIDs
    // https://github.com/nrfconnect/sdk-nrf/blob/6c4ccc353909f1908d6f1317779233c1db7a6d59/subsys/suit/storage/src/suit_storage_nrf54h20.c#L171
    /**
     * The UUID is generated using the following code:
     * <code>uuid5(uuid.NAMESPACE_DNS, ‘nordicsemi.com’)</code>
     */
    private static final UUID NORDIC_VID = UUID.fromString("7617daa5-71fd-5a85-8f94-e28d735ce9f4");

    // Class IDs
    /** <code>uuid5(NORDIC_VID, 'nRF54H20_nordic_top')</code> */
    private static final UUID CLASS_ID_nRF54H20_nordic_top = UUID.fromString("f03d385e-a731-5605-b15d-037f6da6097f");
    /** <code>uuid5(NORDIC_VID, 'nRF54H20_sec')</code> */
    private static final UUID CLASS_ID_nRF54H20_sec = UUID.fromString("d96b40b7-092b-5cd1-a59f-9af80c337eba");
    /** <code>uuid5(NORDIC_VID, 'nRF54H20_sys')</code> */
    private static final UUID CLASS_ID_nRF54H20_sys = UUID.fromString("c08a25d7-35e6-592c-b7ad-43acc8d1d1c8");

    // Link to source code in nRF Connect SDK:
    // https://github.com/nrfconnect/sdk-nrf/blob/main/subsys/suit/metadata/include/suit_metadata.h

    /**
     * The manifest role.
     *
     * @see KnownRole
     */
    public int role;

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
    public DigestAlgorithm digestAlgorithm;
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

    /**
     * Returns the name associated with the {@link #getClassId() class ID}.
     * @return the name, or null if the class ID is unknown.
     */
    @Nullable
    public String getKnownClassName() {
        final UUID classId = getClassId();
        if (classId.equals(CLASS_ID_nRF54H20_nordic_top))
            return "nRF54H20_nordic_top";
        if (classId.equals(CLASS_ID_nRF54H20_sec))
            return "nRF54H20_sec";
        if (classId.equals(CLASS_ID_nRF54H20_sys))
            return "nRF54H20_sys";
        return null;
    }

    /** Returns the Manifest vendor ID. */
    public UUID getVendorId() {
        return convertBytesToUUID(vendorId);
    }

    /**
     * Returns whether the manifest is owned by Nordic Semiconductor.
     */
    public boolean isVendorNordic() {
        return getVendorId().equals(NORDIC_VID);
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

        private final int code;

        DigestAlgorithm(int code) {
            this.code = code;
        }

        @JsonValue
        int getCode() {
            return code;
        }

        @JsonCreator
        public static DigestAlgorithm fromCode(int code) {
            for (DigestAlgorithm algorithm : values()) {
                if (algorithm.code == code) {
                    return algorithm;
                }
            }
            throw new IllegalArgumentException("Invalid digest algorithm: " + code);
        }

        @NotNull
        @Override
        public String toString() {
            switch (this) {
                case SHA_256:
                    return "SHA-256";
                case SHA_512:
                    return "SHA-512";
                default:
                    return super.toString();
            }
        }
    }

    public enum SignatureVerification {
        /** Digest status value uninitialized (invalid). */
        UNKNOWN(0),
        /** Digest value does not match. */
        MISMATCH(1),
        /** Digest value match, but was not checked for authenticity. */
        UNAUTHENTICATED(2),
        /** Digest value match, but signature verification failed. */
        INCORRECT_SIGNATURE(3),
        /** Digest value match and authenticated. */
        AUTHENTICATED(4);

        private final int code;

        SignatureVerification(int code) {
            this.code = code;
        }

        @JsonValue
        int getCode() {
            return code;
        }

        @JsonCreator
        public static SignatureVerification fromCode(int code) {
            for (SignatureVerification value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid signature verification value: " + code);
        }

        @NotNull
        @Override
        public String toString() {
            switch (this) {
                case UNKNOWN:
                    return "Unknown";
                case MISMATCH:
                    return "Mismatch";
                case UNAUTHENTICATED:
                    return "Unauthenticated";
                case INCORRECT_SIGNATURE:
                    return "Incorrect signature";
                case AUTHENTICATED:
                    return "Authenticated";
                default:
                    return super.toString();
            }
        }
    }

    public enum DowngradePreventionPolicy {
        /** No downgrade prevention. */
        DISABLED(1),
        /** Update forbidden if candidate version is lower than installed version */
        ENABLED(2),
        /** Unknown downgrade prevention policy */
        UNKNOWN(3);

        private final int code;

        DowngradePreventionPolicy(int code) {
            this.code = code;
        }

        @JsonValue
        int getCode() {
            return code;
        }

        @JsonCreator
        public static DowngradePreventionPolicy fromCode(int code) {
            for (DowngradePreventionPolicy value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid downgrade prevention policy value: " + code);
        }

        @NotNull
        @Override
        public String toString() {
            switch (this) {
                case DISABLED:
                    return "Disabled";
                case ENABLED:
                    return "Enabled";
                case UNKNOWN:
                    return "Unknown";
                default:
                    return super.toString();
            }
        }
    }

    public enum IndependentUpdateabilityPolicy {
        /** Independent update is forbidden. */
        DENIED(1),
        /** Independent update is allowed. */
        ALLOWED(2),
        /** Unknown independent updateability policy. */
        UNKNOWN(3);

        private final int code;

        IndependentUpdateabilityPolicy(int code) {
            this.code = code;
        }

        @JsonValue
        int getCode() {
            return code;
        }

        @JsonCreator
        public static IndependentUpdateabilityPolicy fromCode(int code) {
            for (IndependentUpdateabilityPolicy value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid independent updateability policy value: " + code);
        }

        @NotNull
        @Override
        public String toString() {
            switch (this) {
                case DENIED:
                    return "Denied";
                case ALLOWED:
                    return "Allowed";
                case UNKNOWN:
                    return "Unknown";
                default:
                    return super.toString();
            }
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

        private final int code;

        SignatureVerificationPolicy(int code) {
            this.code = code;
        }

        @JsonValue
        int getCode() {
            return code;
        }

        @JsonCreator
        public static SignatureVerificationPolicy fromCode(int code) {
            for (SignatureVerificationPolicy value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid signature verification policy value: " + code);
        }

        @NotNull
        @Override
        public String toString() {
            switch (this) {
                case DISABLED:
                    return "Disabled";
                case ENABLED_ON_UPDATE:
                    return "Enabled on update";
                case ENABLED_ON_UPDATE_AND_BOOT:
                    return "Enabled on update and boot";
                case UNKNOWN:
                    return "Unknown";
                default:
                    return super.toString();

            }
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
        int major = 0, minor = 0, patch = 0, releaseNumber = -1;

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
        return major + "." + minor + "." + patch + type.text + (releaseNumber >= 0 ? "." + releaseNumber : "");
    }
}
