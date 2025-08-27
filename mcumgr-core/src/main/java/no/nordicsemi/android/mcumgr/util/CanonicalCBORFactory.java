package no.nordicsemi.android.mcumgr.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.cbor.CBORConstants;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is a copy of {@link CBORFactory} with the only difference
 * that it returns {@link CanonicalCBORGenerator} instead of {@link CBORGenerator}.
 */
public class CanonicalCBORFactory extends CBORFactory {

    @Override
    public CBORGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        final IOContext ctxt = _createContext(_createContentReference(out), false);
        return _createCBORGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec,
                _decorate(out, ctxt));
    }

    @Override
    public CBORGenerator createGenerator(OutputStream out) throws IOException {
        final IOContext ctxt = _createContext(_createContentReference(out), false);
        return _createCBORGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec,
                _decorate(out, ctxt));
    }

    @Override
    protected CBORGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return _createCBORGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec, out);
    }

    // These methods are required to make the overriding class to work:

    private CBORGenerator _createCBORGenerator(IOContext ctxt,
                                               int stdFeat, int formatFeat, ObjectCodec codec, OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        CanonicalCBORGenerator gen = new CanonicalCBORGenerator(ctxt, stdFeat, formatFeat, _objectCodec, out);
        if (CBORGenerator.Feature.WRITE_TYPE_HEADER.enabledIn(formatFeat)) {
            gen.writeTag(CBORConstants.TAG_ID_SELF_DESCRIBE);
        }
        return gen;
    }
}
