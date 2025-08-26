package no.nordicsemi.android.mcumgr.mock

// TODO pull this out of tests in major version release
enum class McuMgrGroup(val value: Int) {
    DEFAULT(0),
    IMAGE(1),
    STATS(2),
    CONFIG(3),
    LOGS(4),
    CRASH(5),
    SPLIT(6),
    RUN(7),
    FS(8)
}
