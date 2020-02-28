package io.runtime.mcumgr

import io.runtime.mcumgr.image.McuMgrImage
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream

class McuMgrImageTest {

    @Test
    fun `parse image without protected tlvs success`() {
        val inputStream = this::class.java.classLoader?.getResourceAsStream("slinky-no-prot-tlv.img")!!
            ?: throw IllegalStateException("input stream is null")
        val imageData = toByteArray(inputStream)
        McuMgrImage.fromBytes(imageData)
    }

    @Test
    fun `parse image with protected tlvs success`() {
        val inputStream = this::class.java.classLoader?.getResourceAsStream("slinky-prot-tlv.img")
            ?: throw IllegalStateException("input stream is null")
        val imageData = toByteArray(inputStream)
        McuMgrImage.fromBytes(imageData)
    }

    private fun toByteArray(inputStream: InputStream): ByteArray {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            os.write(buffer, 0, len)
        }
        return os.toByteArray()
    }
}
