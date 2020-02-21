package io.runtime.mcumgr;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.runtime.mcumgr.image.McuMgrImage;

import static org.junit.Assert.assertNotNull;

public class McuMgrImageTest {

    @Test
    public void fromBytes_unprotectedTlvs_success() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        assertNotNull(classLoader);
        InputStream inputStream = classLoader.getResourceAsStream("slinky-no-prot-tlv.img");
        assertNotNull(inputStream);
        byte[] imageData = toByteArray(inputStream);
        McuMgrImage.fromBytes(imageData);
    }

    @Test
    public void fromBytes_protectedTlvs_success() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        assertNotNull(classLoader);
        InputStream inputStream = classLoader.getResourceAsStream("slinky-prot-tlv.img");
        assertNotNull(inputStream);
        byte[] imageData = toByteArray(inputStream);
        McuMgrImage.fromBytes(imageData);
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }
}
