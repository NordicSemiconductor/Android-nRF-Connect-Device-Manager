package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties("operation", "group", "command")
internal sealed class Payload

internal data class EchoRequestPayload(
    @JsonProperty("d") val echo: String
) : Payload()

internal data class EchoResponsePayload(
    @JsonProperty("r") val echo: String
) : Payload()

internal data class ImageWriteRequestPayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val size: Int? = null,
    @JsonProperty("sha") val hash: ByteArray? = null
) : Payload()

internal data class ImageWriteResponsePayload(
    @JsonProperty("off") val offset: Int
) : Payload()

internal data class CoreReadRequestPayload(
    @JsonProperty("off") val offset: Int
) : Payload()

internal data class CoreReadResponsePayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Payload()

internal data class FileWriteRequestPayload(
    @JsonProperty("name") val fileName: String,
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Payload()

internal data class FileWriteResponsePayload(
    @JsonProperty("off") val offset: Int
) : Payload()

internal data class FileReadRequestPayload(
    @JsonProperty("name") val fileName: String,
    @JsonProperty("off") val offset: Int
) : Payload()

internal data class FileReadResponsePayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
) : Payload()
