package no.nordicsemi.android.mcumgr.log

import no.nordicsemi.kotlin.log.Log

/**
 * A log emitter used internally by the Mcu Manager library.
 *
 * Each layer of the library owns an instance of this class, bound to the [Category] that layer
 * reports under. The [sink] is assigned by the application, and passed down from one layer to
 * the next; it may be changed at any time, or set to `null` to stop logging. Log messages are
 * built only when a sink is set.
 *
 * The formatting methods accept the `{}` placeholder syntax and, like most logging facades, treat
 * a trailing [Throwable] that has no matching placeholder as the throwable associated with
 * the entry:
 * ```java
 * LOG.info("Uploading {} bytes", length);
 * LOG.error("Upload failed", exception);
 * ```
 *
 * From Kotlin, prefer the lambda overloads, which avoid building the message when no sink is set:
 * ```kotlin
 * LOG.info { "Uploading $length bytes" }
 * ```
 *
 * @property category The category all entries from this logger are reported under.
 * @property source The source of the log entries, or `null` for none. A transport passes the
 * address of the device it is connected to, so that entries coming from several devices can be
 * told apart. Managers leave it `null` and let the application's sink decide how to identify
 * the device.
 */
class McuMgrLogger @JvmOverloads constructor(
    val category: Category,
    private val source: String? = null,
) {
    /**
     * The sink receiving the log entries, assigned by the application.
     *
     * When `null` (the default), nothing is logged and no messages are built.
     */
    var sink: Log.Sink<Category>? = null

    // Formatting variants, intended for Java callers.

    /** Logs a message at the [TRACE][Log.Level.TRACE] level. */
    fun trace(format: String, vararg args: Any?) = log(Log.Level.TRACE, format, args)

    /** Logs a message at the [DEBUG][Log.Level.DEBUG] level. */
    fun debug(format: String, vararg args: Any?) = log(Log.Level.DEBUG, format, args)

    /** Logs a message at the [INFO][Log.Level.INFO] level. */
    fun info(format: String, vararg args: Any?) = log(Log.Level.INFO, format, args)

    /** Logs a message at the [WARN][Log.Level.WARN] level. */
    fun warn(format: String, vararg args: Any?) = log(Log.Level.WARN, format, args)

    /** Logs a message at the [ERROR][Log.Level.ERROR] level. */
    fun error(format: String, vararg args: Any?) = log(Log.Level.ERROR, format, args)

    // Lazy variants, intended for Kotlin callers.

    /** Logs a lazily built message at the [TRACE][Log.Level.TRACE] level. */
    fun trace(throwable: Throwable? = null, message: () -> String) =
        log(Log.Level.TRACE, throwable, message)

    /** Logs a lazily built message at the [DEBUG][Log.Level.DEBUG] level. */
    fun debug(throwable: Throwable? = null, message: () -> String) =
        log(Log.Level.DEBUG, throwable, message)

    /** Logs a lazily built message at the [INFO][Log.Level.INFO] level. */
    fun info(throwable: Throwable? = null, message: () -> String) =
        log(Log.Level.INFO, throwable, message)

    /** Logs a lazily built message at the [WARN][Log.Level.WARN] level. */
    fun warn(throwable: Throwable? = null, message: () -> String) =
        log(Log.Level.WARN, throwable, message)

    /** Logs a lazily built message at the [ERROR][Log.Level.ERROR] level. */
    fun error(throwable: Throwable? = null, message: () -> String) =
        log(Log.Level.ERROR, throwable, message)

    private fun log(level: Log.Level, throwable: Throwable?, message: () -> String) {
        sink?.log(category, level, source, throwable, message)
    }

    private fun log(level: Log.Level, format: String, args: Array<out Any?>) {
        // Return early, so that neither the message nor the argument array are inspected
        // when there is nothing to log to.
        val sink = sink ?: return

        // A trailing Throwable without a matching placeholder is the throwable of the entry,
        // not a message argument.
        val placeholders = format.placeholderCount()
        val throwable = if (args.size > placeholders) args.lastOrNull() as? Throwable else null
        val messageArgs = if (throwable != null) args.copyOf(args.size - 1) else args

        sink.log(category, level, source, throwable) { format.formatWith(messageArgs) }
    }
}

private const val PLACEHOLDER = "{}"

private fun String.placeholderCount(): Int {
    var count = 0
    var index = indexOf(PLACEHOLDER)
    while (index >= 0) {
        count++
        index = indexOf(PLACEHOLDER, index + PLACEHOLDER.length)
    }
    return count
}

private fun String.formatWith(args: Array<out Any?>): String {
    if (args.isEmpty()) return this

    val result = StringBuilder(length + 16 * args.size)
    var argIndex = 0
    var index = 0
    while (index < length) {
        val next = indexOf(PLACEHOLDER, index)
        if (next < 0 || argIndex == args.size) {
            result.append(this, index, length)
            break
        }
        result.append(this, index, next)
        result.append(args[argIndex++].stringify())
        index = next + PLACEHOLDER.length
    }
    return result.toString()
}

private fun Any?.stringify(): String = when (this) {
    null -> "null"
    is ByteArray -> joinToString(separator = " ") { "%02X".format(it) }
    is Array<*> -> contentDeepToString()
    is IntArray -> contentToString()
    is LongArray -> contentToString()
    is ShortArray -> contentToString()
    is BooleanArray -> contentToString()
    is CharArray -> contentToString()
    is FloatArray -> contentToString()
    is DoubleArray -> contentToString()
    else -> toString()
}
