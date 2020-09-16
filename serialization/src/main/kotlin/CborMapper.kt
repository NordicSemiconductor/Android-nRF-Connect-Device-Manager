package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * CBOR object mapper used my mcumgr serialization.
 */
val cbor = CBORMapper().apply {
    registerModule(KotlinModule())

    // Do not fail on unknown properties when decoding responses
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    // Do not fail when attempting to serialize an object w/ no annotated fields
    disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

    // Do not serialize fields with null values
    setSerializationInclusion(JsonInclude.Include.NON_NULL)

    // Fail if a required property is not available
    enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
}
