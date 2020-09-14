package com.juul.mcumgr

// TODO: IDK if this is a real api yet. Useful for now
class McuManager(val transport: Transport) {

    val system = SystemManager(transport)
    val image = ImageManager(transport)
    val stats = StatsManager(transport)
    val config = ConfigManager(transport)
    val logs = LogsManager(transport)
    val crash = CrashManager(transport)
    val run = RunManager(transport)
    val files = FileManager(transport)
}
