package com.juul.mcumgr

sealed class Group(val value: Int) {
    object System : Group(0)
    object Image : Group(1)
    object Stats : Group(2)
    object Config : Group(3)
    object Logs : Group(4)
    object Crash : Group(5)
    object Split : Group(6) // TODO No implementation, ask chris
    object Run : Group(7) // TODO No implementation, ask chris
    object Files : Group(8)
}
