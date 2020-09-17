package util

import kotlin.test.assertEquals

fun assertByteArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.toList(), actual.toList())
}
