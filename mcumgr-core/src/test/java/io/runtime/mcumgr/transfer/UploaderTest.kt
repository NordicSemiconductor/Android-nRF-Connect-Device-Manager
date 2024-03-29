package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.mock.McuMgrHandler
import io.runtime.mcumgr.mock.MockBleMcuMgrTransport
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.img.McuMgrImageUploadResponse
import io.runtime.mcumgr.util.CBOR
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UploaderTest {

    @Test
    fun `test image Uploader`() {
        // Parameters
        val data = ByteArray(100000) { 0 } // keep it > 64k
        val mtu = 245
        val image = 1

        // Test values
        var received = 0

        // Upload handler
        val handler = object : McuMgrHandler {
            override fun <T : McuMgrResponse> handle(
                header: McuMgrHeader,
                payload: ByteArray,
                responseType: Class<T>
            ): T {
                assert(payload.size + McuMgrHeader.HEADER_LENGTH <= mtu) {
                    "Payload size is too big (${payload.size + McuMgrHeader.HEADER_LENGTH} bytes, mtu = $mtu) at $received"
                }

                val map = CBOR.toObjectMap(payload)
                assertTrue { map.containsKey("off") }
                assertTrue { map.containsKey("data") }

                val off = map["off"] as Int
                if (off == 0) {
                    assertTrue { map.containsKey("len") }
                    assertTrue { map.containsKey("sha") }
                    val len = map["len"] as Int
                    assertEquals(data.size, len)

                    if (image != 0) {
                        assertTrue { map.containsKey("image") }
                        val img = map["image"] as Int
                        assertEquals(image, img)
                    } else {
                        assertFalse { map.containsKey("image") }
                    }
                } else {
                    assertFalse { map.containsKey("len") }
                    assertFalse { map.containsKey("sha") }
                    assertFalse { map.containsKey("image") }
                }
                val chunk = map["data"] as ByteArray
                received += chunk.size

                return McuMgrImageUploadResponse()
                    .apply {
                        this.off = received
                        this.rc = 0 // Success
                    } as T
            }
        }

        // Test
        val im = ImageManager(MockBleMcuMgrTransport(handler))
        im.setUploadMtu(mtu)
        val uploader = ImageUploader(im, data, image, 4, 1)
        runBlocking { uploader.upload() }

        // Ensure that the
        assertEquals(data.size, received)
    }

}

