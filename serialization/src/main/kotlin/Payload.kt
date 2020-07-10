package com.juul.mcumgr.serialization

import com.fasterxml.jackson.annotation.JsonProperty

data class EchoRequestPayload(
    @JsonProperty("d") val echo: String
)

data class EchoResponsePayload(
    @JsonProperty("r") val echo: String
)

data class ImageUploadRequestPayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val size: Int? = null,
    @JsonProperty("sha") val hash: ByteArray? = null
)

data class ImageUploadResponsePayload(
    @JsonProperty("off") val offset: Int
)

data class CoreDownloadRequestPayload(
    @JsonProperty("off") val offset: Int
)

data class CoreDownloadResponsePayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
)

data class FileUploadRequestPayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
)

data class FileUploadResponsePayload(
    @JsonProperty("off") val offset: Int
)

data class FileDownloadRequestPayload(
    @JsonProperty("off") val offset: Int
)

data class FileDownloadResponsePayload(
    @JsonProperty("data") val data: ByteArray,
    @JsonProperty("off") val offset: Int,
    @JsonProperty("len") val length: Int? = null
)
