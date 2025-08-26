package no.nordicsemi.android.mcumgr.util;

import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.PREFIX_TYPE_OBJECT;
import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.SUFFIX_UINT16_ELEMENTS;
import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.SUFFIX_UINT32_ELEMENTS;
import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.SUFFIX_UINT8_ELEMENTS;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * This class is a copy of {@link CBORGenerator} with the only difference
 * that it writes maps in canonical form.
 * <p>
 * The implementation is copied from {@link CBORGenerator} at version 2.17.1, which cannot be used
 * as 2.14+ support only Android 8+.
 */
public class CanonicalCBORGenerator extends CBORGenerator {

    public CanonicalCBORGenerator(IOContext ctxt, int stdFeatures, int formatFeatures, ObjectCodec codec, OutputStream out) {
        super(ctxt, stdFeatures, formatFeatures, codec, out);
    }

    public CanonicalCBORGenerator(IOContext ctxt, int stdFeatures, int formatFeatures, ObjectCodec codec, OutputStream out, byte[] outputBuffer, int offset, boolean bufferRecyclable) {
        super(ctxt, stdFeatures, formatFeatures, codec, out, outputBuffer, offset, bufferRecyclable);
    }

    @Override
    public void writeStartObject(Object forValue, int elementsToWrite) throws IOException {
        _verifyValueWrite("start an object");
        _streamWriteContext = _streamWriteContext.createChildObjectContext(forValue);
        _pushRemainingElements();
        _currentRemainingElements = elementsToWrite;
        _writeLengthMarker(PREFIX_TYPE_OBJECT, elementsToWrite);
    }

    // These methods are required to make the overriding class to work:

    private void _pushRemainingElements() {
        if (_elementCounts.length == _elementCountsPtr) { // initially, as well as if full
            _elementCounts = Arrays.copyOf(_elementCounts, _elementCounts.length+10);
        }
        _elementCounts[_elementCountsPtr++] = _currentRemainingElements;
    }

    private void _writeLengthMarker(int majorType, int i)
            throws IOException {
        _ensureRoomForOutput(5);
        if (i < 24) {
            _outputBuffer[_outputTail++] = (byte) (majorType + i);
            return;
        }
        if (i <= 0xFF) {
            _outputBuffer[_outputTail++] = (byte) (majorType + SUFFIX_UINT8_ELEMENTS);
            _outputBuffer[_outputTail++] = (byte) i;
            return;
        }
        final byte b0 = (byte) i;
        i >>= 8;
        if (i <= 0xFF) {
            _outputBuffer[_outputTail++] = (byte) (majorType + SUFFIX_UINT16_ELEMENTS);
            _outputBuffer[_outputTail++] = (byte) i;
            _outputBuffer[_outputTail++] = b0;
            return;
        }
        _outputBuffer[_outputTail++] = (byte) (majorType + SUFFIX_UINT32_ELEMENTS);
        _outputBuffer[_outputTail++] = (byte) (i >> 16);
        _outputBuffer[_outputTail++] = (byte) (i >> 8);
        _outputBuffer[_outputTail++] = (byte) i;
        _outputBuffer[_outputTail++] = b0;
    }

    private void _ensureRoomForOutput(int needed) throws IOException {
        if ((_outputTail + needed) >= _outputEnd) {
            _flushBuffer();
        }
    }
}
