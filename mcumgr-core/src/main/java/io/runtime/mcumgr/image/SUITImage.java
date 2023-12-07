package io.runtime.mcumgr.image;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;

public class SUITImage implements ImageWithHash {
    private final byte @NotNull [] mHash;
    private final byte @NotNull [] mData;

    private SUITImage(byte @NotNull [] hash, byte @NotNull [] data) {
        mHash = hash;
        mData = data;
    }

    @Override
    public byte @NotNull [] getData() {
        return mData;
    }

    /**
     * Returns the SHA256 hash of the Root Manifest from the Envelope.
     */
    @Override
    public byte @NotNull [] getHash() {
        return mHash;
    }

    @Override
    public boolean needsConfirmation() {
        // The implementation will find out what to do from SUIT file.
        // No need to confirm, no need to send Reset.
        // Just upload the file and that's all.
        return false;
    }

    public static byte @NotNull [] getHash(byte @NotNull [] data) throws McuMgrException {
        return fromBytes(data).getHash();
    }

    public static SUITImage fromBytes(byte @NotNull [] data) throws McuMgrException {
        if (data.length < 2 || data[0] != (byte) 0xD8 || data[1] != (byte) 0x6B) {
            throw new McuMgrException("Invalid SUIT image");
        }
        // We could parse the SUIT Envelope here, but that would require migrating to Kotlin CBOR
        // (jackson-dataformat-cbor doesn't support non-String keys).
        // One day, perhaps. For now, let's do a trick.

        // The Root Manifest SHA256 is embedded in the Authentication Block in SUIT Envelope,
        // so we can just extract it.
        // The SHA is prefixed with:
        // 82         # array(2)
        //   2F       # negative(15) - this is actually -16, CBOR uses weird encoding.
        //     58 20  # bytes(32)
        //
        // This -16 above is the key for SHA256 algorithm-id in COSE registry:
        // REQUIRED to implement in SUIT:
        // cose-alg-sha-256 = -16
        //
        // OPTIONAL to implement in SUIT and ignored currently:
        // cose-alg-shake128 = -18
        // cose-alg-sha-384 = -43
        // cose-alg-sha-512 = -44
        // cose-alg-shake256 = -45
        //
        // Link to the registry: https://www.iana.org/assignments/cose/cose.xhtml#algorithms

        // Let's take the 32 bytes after the prefix. Btw, this may fail if the Envelope contains some
        // other data with the same set of bytes before the Authentication Block. But that's unlikely,
        // and even if, not a biggie.
        int pos = -1;
        for (int i = 0; i < data.length - 4 + 32; i++) {
            if (data[i] == (byte) 0x82 && data[i + 1] == (byte) 0x2F && data[i + 2] == (byte) 0x58 && data[i + 3] == (byte) 0x20) {
                pos = i + 4;
                break;
            }
        }
        if (pos == -1) {
            throw new McuMgrException("Invalid SUIT image");
        }
        byte[] hash = new byte[32];
        System.arraycopy(data, pos, hash, 0, 32);
        return new SUITImage(hash, data);
    }
}
