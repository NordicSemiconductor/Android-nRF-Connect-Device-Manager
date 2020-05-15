package io.runtime.mcumgr.sample.observable;

public enum ConnectionState {
    CONNECTING,
    INITIALIZING,
    READY,
    DISCONNECTING,
    DISCONNECTED,
    TIMEOUT,
    NOT_SUPPORTED
}
