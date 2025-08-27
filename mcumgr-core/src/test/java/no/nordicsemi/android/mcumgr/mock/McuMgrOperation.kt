package no.nordicsemi.android.mcumgr.mock

// TODO pull this out of tests in major version release
enum class McuMgrOperation(val value: Int) {
    READ(0),
    READ_RESPONSE(1),
    WRITE(2),
    WRITE_RESPONSE(3)
}
