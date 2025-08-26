package no.nordicsemi.android.mcumgr.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;

import no.nordicsemi.android.mcumgr.McuMgrScheme;
import no.nordicsemi.android.mcumgr.response.img.McuMgrImageStateResponse;

public class McuMgrResponseTest {

    @Test
    public void buildResponse_two_slots() throws IOException {
        /*
        Header:
        01-00-00-F4-00-01-00-00

        Message:
        http://cbor.me/?bytes=BF(66(696D61676573)-9F(BF(64(736C6F74)-00-67(76657273696F6E)-65(332E302E30)-64(68617368)-58.20(0D4F2C73D8581CE93015A2DAE6387399F0201C3C0C5615F0DD7A2D49CDF346B2)-68(626F6F7461626C65)-F5-67(70656E64696E67)-F4-69(636F6E6669726D6564)-F5-66(616374697665)-F5-69(7065726D616E656E74)-F4-FF)-BF(64(736C6F74)-01-67(76657273696F6E)-65(332E302E30)-64(68617368)-58.20(3AA147C63CD8E871104F7EBB1CD0B2E742D83DD79A581CB5AE0E05DB79444799)-68(626F6F7461626C65)-F5-67(70656E64696E67)-F4-69(636F6E6669726D6564)-F4-66(616374697665)-F4-69(7065726D616E656E74)-F4-FF)-FF)-6B(73706C6974537461747573)-00-FF)

        BF                                      # map(*)
           66                                   # text(6)
              696D61676573                      # "images"
           9F                                   # array(*)
              BF                                # map(*)
                 64                             # text(4)
                    736C6F74                    # "slot"
                 00                             # unsigned(0)
                 67                             # text(7)
                    76657273696F6E              # "version"
                 65                             # text(5)
                    332E302E30                  # "3.0.0"
                 64                             # text(4)
                    68617368                    # "hash"
                 58 20                          # bytes(32)
                    0D4F2C73D8581CE93015A2DAE6387399F0201C3C0C5615F0DD7A2D49CDF346B2 # "\rO,s\xD8X\x1C\xE90\x15\xA2\xDA\xE68s\x99\xF0 \x1C<\fV\x15\xF0\xDDz-I\xCD\xF3F\xB2"
                 68                             # text(8)
                    626F6F7461626C65            # "bootable"
                 F5                             # primitive(21)
                 67                             # text(7)
                    70656E64696E67              # "pending"
                 F4                             # primitive(20)
                 69                             # text(9)
                    636F6E6669726D6564          # "confirmed"
                 F5                             # primitive(21)
                 66                             # text(6)
                    616374697665                # "active"
                 F5                             # primitive(21)
                 69                             # text(9)
                    7065726D616E656E74          # "permanent"
                 F4                             # primitive(20)
                 FF                             # primitive(*)
              BF                                # map(*)
                 64                             # text(4)
                    736C6F74                    # "slot"
                 01                             # unsigned(1)
                 67                             # text(7)
                    76657273696F6E              # "version"
                 65                             # text(5)
                    332E302E30                  # "3.0.0"
                 64                             # text(4)
                    68617368                    # "hash"
                 58 20                          # bytes(32)
                    3AA147C63CD8E871104F7EBB1CD0B2E742D83DD79A581CB5AE0E05DB79444799 # ":\xA1G\xC6<\xD8\xE8q\x10O~\xBB\x1C\xD0\xB2\xE7B\xD8=\xD7\x9AX\x1C\xB5\xAE\x0E\x05\xDByDG\x99"
                 68                             # text(8)
                    626F6F7461626C65            # "bootable"
                 F5                             # primitive(21)
                 67                             # text(7)
                    70656E64696E67              # "pending"
                 F4                             # primitive(20)
                 69                             # text(9)
                    636F6E6669726D6564          # "confirmed"
                 F4                             # primitive(20)
                 66                             # text(6)
                    616374697665                # "active"
                 F4                             # primitive(20)
                 69                             # text(9)
                    7065726D616E656E74          # "permanent"
                 F4                             # primitive(20)
                 FF                             # primitive(*)
              FF                                # primitive(*)
           6B                                   # text(11)
              73706C6974537461747573            # "splitStatus"
           00                                   # unsigned(0)
           FF                                   # primitive(*)

         */
        final byte[] data = { (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xF4, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9,
                (byte) 0x30, (byte) 0x15, (byte) 0xA2, (byte) 0xDA, (byte) 0xE6, (byte) 0x38, (byte) 0x73, (byte) 0x99, (byte) 0xF0,
                (byte) 0x20, (byte) 0x1C, (byte) 0x3C, (byte) 0x0C, (byte) 0x56, (byte) 0x15, (byte) 0xF0, (byte) 0xDD, (byte) 0x7A,
                (byte) 0x2D, (byte) 0x49, (byte) 0xCD, (byte) 0xF3, (byte) 0x46, (byte) 0xB2, (byte) 0x68, (byte) 0x62, (byte) 0x6F,
                (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5, (byte) 0x67, (byte) 0x70,
                (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4, (byte) 0x69, (byte) 0x63,
                (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0xF5,
                (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0xF5, (byte) 0x69,
                (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65, (byte) 0x6E, (byte) 0x74,
                (byte) 0xF4, (byte) 0xFF, (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x01,
                (byte) 0x67, (byte) 0x76, (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65,
                (byte) 0x33, (byte) 0x2E, (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73,
                (byte) 0x68, (byte) 0x58, (byte) 0x20, (byte) 0x3A, (byte) 0xA1, (byte) 0x47, (byte) 0xC6, (byte) 0x3C, (byte) 0xD8,
                (byte) 0xE8, (byte) 0x71, (byte) 0x10, (byte) 0x4F, (byte) 0x7E, (byte) 0xBB, (byte) 0x1C, (byte) 0xD0, (byte) 0xB2,
                (byte) 0xE7, (byte) 0x42, (byte) 0xD8, (byte) 0x3D, (byte) 0xD7, (byte) 0x9A, (byte) 0x58, (byte) 0x1C, (byte) 0xB5,
                (byte) 0xAE, (byte) 0x0E, (byte) 0x05, (byte) 0xDB, (byte) 0x79, (byte) 0x44, (byte) 0x47, (byte) 0x99, (byte) 0x68,
                (byte) 0x62, (byte) 0x6F, (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5,
                (byte) 0x67, (byte) 0x70, (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4,
                (byte) 0x69, (byte) 0x63, (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65,
                (byte) 0x64, (byte) 0xF4, (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65,
                (byte) 0xF4, (byte) 0x69, (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65,
                (byte) 0x6E, (byte) 0x74, (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, (byte) 0x6B, (byte) 0x73, (byte) 0x70, (byte) 0x6C,
                (byte) 0x69, (byte) 0x74, (byte) 0x53, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x75, (byte) 0x73, (byte) 0x00,
                (byte) 0xFF};

        McuMgrImageStateResponse response = McuMgrResponse.buildResponse(McuMgrScheme.BLE, data, McuMgrImageStateResponse.class);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.images);
        assertEquals(2, response.images.length);

        assertEquals(0, response.images[0].slot);
        assertEquals("3.0.0", response.images[0].version);
        assertTrue(response.images[0].bootable);
        assertFalse(response.images[0].pending);
        assertTrue(response.images[0].confirmed);
        assertTrue(response.images[0].active);
        assertFalse(response.images[0].permanent);

        assertEquals(1, response.images[1].slot);
        assertEquals("3.0.0", response.images[1].version);
        assertTrue(response.images[1].bootable);
        assertFalse(response.images[1].pending);
        assertFalse(response.images[1].confirmed);
        assertFalse(response.images[1].active);
        assertFalse(response.images[1].permanent);

        assertEquals(0, response.splitStatus);
    }

    @Test
    public void buildResponse_one_slot() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9,
                (byte) 0x30, (byte) 0x15, (byte) 0xA2, (byte) 0xDA, (byte) 0xE6, (byte) 0x38, (byte) 0x73, (byte) 0x99, (byte) 0xF0,
                (byte) 0x20, (byte) 0x1C, (byte) 0x3C, (byte) 0x0C, (byte) 0x56, (byte) 0x15, (byte) 0xF0, (byte) 0xDD, (byte) 0x7A,
                (byte) 0x2D, (byte) 0x49, (byte) 0xCD, (byte) 0xF3, (byte) 0x46, (byte) 0xB2, (byte) 0x68, (byte) 0x62, (byte) 0x6F,
                (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5, (byte) 0x67, (byte) 0x70,
                (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4, (byte) 0x69, (byte) 0x63,
                (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0xF5,
                (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0xF5, (byte) 0x69,
                (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65, (byte) 0x6E, (byte) 0x74,
                (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, // no slot 2
                (byte) 0x6B, (byte) 0x73, (byte) 0x70, (byte) 0x6C,
                (byte) 0x69, (byte) 0x74, (byte) 0x53, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x75, (byte) 0x73, (byte) 0x00,
                (byte) 0xFF};

        McuMgrImageStateResponse response = McuMgrResponse.buildResponse(McuMgrScheme.BLE, data, McuMgrImageStateResponse.class);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.images);
        assertEquals(1, response.images.length);

        assertEquals(0, response.images[0].slot);
        assertEquals("3.0.0", response.images[0].version);
        assertTrue(response.images[0].bootable);
        assertFalse(response.images[0].pending);
        assertTrue(response.images[0].confirmed);
        assertTrue(response.images[0].active);
        assertFalse(response.images[0].permanent);

        assertEquals(0, response.splitStatus);
    }

    @Test
    public void buildResponse_missing_splitStatus() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9,
                (byte) 0x30, (byte) 0x15, (byte) 0xA2, (byte) 0xDA, (byte) 0xE6, (byte) 0x38, (byte) 0x73, (byte) 0x99, (byte) 0xF0,
                (byte) 0x20, (byte) 0x1C, (byte) 0x3C, (byte) 0x0C, (byte) 0x56, (byte) 0x15, (byte) 0xF0, (byte) 0xDD, (byte) 0x7A,
                (byte) 0x2D, (byte) 0x49, (byte) 0xCD, (byte) 0xF3, (byte) 0x46, (byte) 0xB2, (byte) 0x68, (byte) 0x62, (byte) 0x6F,
                (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5, (byte) 0x67, (byte) 0x70,
                (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4, (byte) 0x69, (byte) 0x63,
                (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0xF5,
                (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0xF5, (byte) 0x69,
                (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65, (byte) 0x6E, (byte) 0x74,
                (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, // no slot 2
                (byte) 0xFF};

        McuMgrImageStateResponse response = McuMgrResponse.buildResponse(McuMgrScheme.BLE, data, McuMgrImageStateResponse.class);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.images);
        assertEquals(1, response.images.length);

        assertEquals(0, response.images[0].slot);
        assertEquals("3.0.0", response.images[0].version);
        assertTrue(response.images[0].bootable);
        assertFalse(response.images[0].pending);
        assertTrue(response.images[0].confirmed);
        assertTrue(response.images[0].active);
        assertFalse(response.images[0].permanent);

        assertEquals(0, response.splitStatus);
    }

    @Test
    public void buildResponse_typo() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9,
                (byte) 0x30, (byte) 0x15, (byte) 0xA2, (byte) 0xDA, (byte) 0xE6, (byte) 0x38, (byte) 0x73, (byte) 0x99, (byte) 0xF0,
                (byte) 0x20, (byte) 0x1C, (byte) 0x3C, (byte) 0x0C, (byte) 0x56, (byte) 0x15, (byte) 0xF0, (byte) 0xDD, (byte) 0x7A,
                (byte) 0x2D, (byte) 0x49, (byte) 0xCD, (byte) 0xF3, (byte) 0x46, (byte) 0xB2, (byte) 0x68, (byte) 0x62, (byte) 0x6F,
                (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5, (byte) 0x67, (byte) 0x70,
                (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4, (byte) 0x69, (byte) 0x63,
                (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0xF5,
                (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0xF5, (byte) 0x69,
                (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65, (byte) 0x6E, (byte) 0x74,
                (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, // no slot 2
                (byte) 0x6B, (byte) 0x73, (byte) 0x70, (byte) 0x6D, // 6C -> 6D
                (byte) 0x69, (byte) 0x74, (byte) 0x53, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x75, (byte) 0x73, (byte) 0x00,
                (byte) 0xFF};

        McuMgrImageStateResponse response = McuMgrResponse.buildResponse(McuMgrScheme.BLE, data, McuMgrImageStateResponse.class);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.images);
        assertEquals(1, response.images.length);

        assertEquals(0, response.images[0].slot);
        assertEquals("3.0.0", response.images[0].version);
        assertTrue(response.images[0].bootable);
        assertFalse(response.images[0].pending);
        assertTrue(response.images[0].confirmed);
        assertTrue(response.images[0].active);
        assertFalse(response.images[0].permanent);

        assertEquals(0, response.splitStatus);
    }

    @Test
    public void getExpectedLength_full() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9,
                (byte) 0x30, (byte) 0x15, (byte) 0xA2, (byte) 0xDA, (byte) 0xE6, (byte) 0x38, (byte) 0x73, (byte) 0x99, (byte) 0xF0,
                (byte) 0x20, (byte) 0x1C, (byte) 0x3C, (byte) 0x0C, (byte) 0x56, (byte) 0x15, (byte) 0xF0, (byte) 0xDD, (byte) 0x7A,
                (byte) 0x2D, (byte) 0x49, (byte) 0xCD, (byte) 0xF3, (byte) 0x46, (byte) 0xB2, (byte) 0x68, (byte) 0x62, (byte) 0x6F,
                (byte) 0x6F, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0xF5, (byte) 0x67, (byte) 0x70,
                (byte) 0x65, (byte) 0x6E, (byte) 0x64, (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0xF4, (byte) 0x69, (byte) 0x63,
                (byte) 0x6F, (byte) 0x6E, (byte) 0x66, (byte) 0x69, (byte) 0x72, (byte) 0x6D, (byte) 0x65, (byte) 0x64, (byte) 0xF5,
                (byte) 0x66, (byte) 0x61, (byte) 0x63, (byte) 0x74, (byte) 0x69, (byte) 0x76, (byte) 0x65, (byte) 0xF5, (byte) 0x69,
                (byte) 0x70, (byte) 0x65, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x6E, (byte) 0x65, (byte) 0x6E, (byte) 0x74,
                (byte) 0xF4, (byte) 0xFF, (byte) 0xFF, // no slot 2
                (byte) 0xFF};

        assertEquals(129, McuMgrResponse.getExpectedLength(McuMgrScheme.BLE, data));
    }

    @Test
    public void getExpectedLength_partial() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0xBF, (byte) 0x66, (byte) 0x69, (byte) 0x6D, (byte) 0x61, (byte) 0x67, (byte) 0x65, (byte) 0x73, (byte) 0x9F,
                (byte) 0xBF, (byte) 0x64, (byte) 0x73, (byte) 0x6C, (byte) 0x6F, (byte) 0x74, (byte) 0x00, (byte) 0x67, (byte) 0x76,
                (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x33, (byte) 0x2E,
                (byte) 0x30, (byte) 0x2E, (byte) 0x30, (byte) 0x64, (byte) 0x68, (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x58,
                (byte) 0x20, (byte) 0x0D, (byte) 0x4F, (byte) 0x2C, (byte) 0x73, (byte) 0xD8, (byte) 0x58, (byte) 0x1C, (byte) 0xE9};

        assertEquals(129, McuMgrResponse.getExpectedLength(McuMgrScheme.BLE, data));
    }

    @Test(expected = IOException.class)
    public void getExpectedLength_tooShort() throws IOException {
        final byte[] data = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x79, (byte) 0x00, (byte) 0x01, (byte) 0x00};

        McuMgrResponse.getExpectedLength(McuMgrScheme.BLE, data);
        fail("Incorrectly parsed too short header");
    }
}