package mock.server.handler

import com.juul.mcumgr.command.Command
import com.juul.mcumgr.command.Group
import com.juul.mcumgr.command.Operation
import com.juul.mcumgr.serialization.Message

val readOnly: Set<Operation> = setOf(Operation.Read)
val writeOnly: Set<Operation> = setOf(Operation.Write)
val readWrite: Set<Operation> = setOf(Operation.Read, Operation.Write)

/**
 * Handles a request with a specific group, command, and operation.
 */
interface Handler {

    val group: Group
    val command: Command
    val accept: Set<Operation>

    fun handle(message: Message): Message
}

val defaultHandlers: List<Handler> get() = listOf(
    // System
    EchoHandler(),
    ConsoleEchoControlHandler(),
    TaskStatsHandler(),
    MemoryPoolStatsHandler(),
    ReadDatetimeHandler(),
    WriteDatetimeHandler(),
    ResetHandler(),
    // Image
    ImageStateHandler(),
    ImageEraseHandler(),
    ImageEraseStateHandler(),
    ImageWriteHandler(),
    CoreListHandler(),
    CoreReadHandler(),
    CoreEraseHandler(),
    // File
    FileWriteHandler(),
    FileReadHandler(),
    // Config
    ConfigHandler()
)
