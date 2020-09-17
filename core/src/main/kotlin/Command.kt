package com.juul.mcumgr

sealed class Command(val value: Int) {

    sealed class System {
        object Echo : Command(0)
        object ConsoleEchoControl : Command(1)
        object TaskStats : Command(2)
        object MemoryPoolStats : Command(3)
        object Datetime : Command(4)
        object Reset : Command(5)
    }

    sealed class Image {
        object State : Command(0)
        object Upload : Command(1)
        object File : Command(2)
        object CoreList : Command(3)
        object CoreLoad : Command(4)
        object Erase : Command(5)
        object EraseState : Command(6)
    }

    sealed class Stats {
        object Read : Command(0)
        object List : Command(0)
    }

    sealed class Config {
        object Config : Command(0)
    }

    sealed class Logs {
        object Read : Command(0)
        object Clear : Command(1)
        object Append : Command(2)
        object ModuleList : Command(3)
        object LevelList : Command(4)
        object LogsList : Command(5)
    }

    sealed class Crash {
        object Crash : Command(0)
    }

    sealed class Run {
        // TODO Look at source
    }

    sealed class File {
        object File : Command(0)
    }
}
