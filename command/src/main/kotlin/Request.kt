package com.juul.mcumgr.command

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Redeclaration of [com.juul.mcumgr.Request] for serialization annotations.
 */
@JsonIgnoreProperties("operation", "group", "command")
abstract class Request : com.juul.mcumgr.Request()
