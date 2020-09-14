package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.juul.mcumgr.command.ConsoleEchoControlRequest
import com.juul.mcumgr.command.ConsoleEchoControlResponse
import com.juul.mcumgr.command.CoreReadRequest
import com.juul.mcumgr.command.CoreReadResponse
import com.juul.mcumgr.command.EchoRequest
import com.juul.mcumgr.command.EchoResponse
import com.juul.mcumgr.command.FileReadRequest
import com.juul.mcumgr.command.FileReadResponse
import com.juul.mcumgr.command.FileWriteRequest
import com.juul.mcumgr.command.FileWriteResponse
import com.juul.mcumgr.command.ImageWriteRequest
import com.juul.mcumgr.command.ImageWriteResponse
import com.juul.mcumgr.command.MemoryPoolStatsRequest
import com.juul.mcumgr.command.MemoryPoolStatsResponse
import com.juul.mcumgr.command.ReadDatetimeRequest
import com.juul.mcumgr.command.ReadDatetimeResponse
import com.juul.mcumgr.command.ResetRequest
import com.juul.mcumgr.command.ResetResponse
import com.juul.mcumgr.command.TaskStatsRequest
import com.juul.mcumgr.command.TaskStatsResponse
import com.juul.mcumgr.command.WriteDatetimeRequest
import com.juul.mcumgr.command.WriteDatetimeResponse

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

    // System
    addMixIn(EchoRequest::class.java, System.EchoRequest::class.java)
    addMixIn(EchoResponse::class.java, System.EchoResponse::class.java)

    addMixIn(ConsoleEchoControlRequest::class.java, System.ConsoleEchoControlRequest::class.java)
    addMixIn(ConsoleEchoControlResponse::class.java, System.ConsoleEchoControlResponse::class.java)

    addMixIn(TaskStatsRequest::class.java, System.TaskStatsRequest::class.java)
    addMixIn(TaskStatsResponse::class.java, System.TaskStatsResponse::class.java)
    addMixIn(TaskStatsResponse.Task::class.java, System.TaskStatsResponse.Task::class.java)

    addMixIn(MemoryPoolStatsRequest::class.java, System.MemoryPoolStatsRequest::class.java)
    addMixIn(MemoryPoolStatsResponse::class.java, System.MemoryPoolStatsResponse::class.java)
    addMixIn(MemoryPoolStatsResponse.MemoryPool::class.java, System.MemoryPoolStatsResponse.MemoryPool::class.java)

    addMixIn(ReadDatetimeRequest::class.java, System.ReadDatetimeRequest::class.java)
    addMixIn(ReadDatetimeResponse::class.java, System.ReadDatetimeResponse::class.java)

    addMixIn(WriteDatetimeRequest::class.java, System.WriteDatetimeRequest::class.java)
    addMixIn(WriteDatetimeResponse::class.java, System.WriteDatetimeResponse::class.java)

    addMixIn(ResetRequest::class.java, System.ResetRequest::class.java)
    addMixIn(ResetResponse::class.java, System.ResetResponse::class.java)

    // Image
    addMixIn(ImageWriteRequest::class.java, Image.ImageWriteRequest::class.java)
    addMixIn(ImageWriteResponse::class.java, Image.ImageWriteResponse::class.java)
    addMixIn(CoreReadRequest::class.java, Image.CoreReadRequest::class.java)
    addMixIn(CoreReadResponse::class.java, Image.CoreReadResponse::class.java)

    // Files
    addMixIn(FileWriteRequest::class.java, File.WriteRequest::class.java)
    addMixIn(FileWriteResponse::class.java, File.WriteResponse::class.java)
    addMixIn(FileReadRequest::class.java, File.ReadRequest::class.java)
    addMixIn(FileReadResponse::class.java, File.ReadResponse::class.java)
}
