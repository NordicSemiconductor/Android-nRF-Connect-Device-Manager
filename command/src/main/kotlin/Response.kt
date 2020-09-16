package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Redeclaration of [com.juul.mcumgr.Response] for serialization annotations.
 */
@JsonIgnoreProperties("group", "command")
abstract class Response : com.juul.mcumgr.Response()
