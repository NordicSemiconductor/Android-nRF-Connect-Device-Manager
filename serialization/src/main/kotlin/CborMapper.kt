package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.juul.mcumgr.message.CoreDownloadRequest
import com.juul.mcumgr.message.CoreDownloadResponse
import com.juul.mcumgr.message.EchoRequest
import com.juul.mcumgr.message.EchoResponse
import com.juul.mcumgr.message.FileDownloadRequest
import com.juul.mcumgr.message.FileDownloadResponse
import com.juul.mcumgr.message.FileUploadRequest
import com.juul.mcumgr.message.FileUploadResponse
import com.juul.mcumgr.message.ImageUploadRequest
import com.juul.mcumgr.message.ImageUploadResponse

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

    // System
    addMixIn(EchoRequest::class.java, EchoRequestPayload::class.java)
    addMixIn(EchoResponse::class.java, EchoResponsePayload::class.java)
    // Image
    addMixIn(ImageUploadRequest::class.java, ImageUploadRequestPayload::class.java)
    addMixIn(ImageUploadResponse::class.java, ImageUploadResponsePayload::class.java)
    addMixIn(CoreDownloadRequest::class.java, CoreDownloadRequestPayload::class.java)
    addMixIn(CoreDownloadResponse::class.java, CoreDownloadResponsePayload::class.java)
    // Files
    addMixIn(FileUploadRequest::class.java, FileUploadRequestPayload::class.java)
    addMixIn(FileUploadResponse::class.java, FileUploadResponsePayload::class.java)
    addMixIn(FileDownloadRequest::class.java, FileDownloadRequestPayload::class.java)
    addMixIn(FileDownloadResponse::class.java, FileDownloadResponsePayload::class.java)
}
