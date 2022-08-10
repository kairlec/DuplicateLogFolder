package com.kairlec

import mu.KLogger
import mu.Marker
import mu.internal.ErrorMessageProducer
import org.slf4j.event.Level

object KLoggerContext {
    val current = ThreadLocal<Long>()
    val buffer = ThreadLocal<MutableList<LogResult>>()
}

const val RepeatTimesMdcKey = "REPEAT_TIMES"

inline fun withRepeatTimes(id: Long, block: () -> Unit) {
    KLoggerContext.current.set(id)
    KLoggerContext.buffer.set(mutableListOf())
    try {
        block()
    } finally {
        KLoggerContext.current.remove()
        KLoggerContext.buffer.remove()
    }
}

interface LogResultLoader {
    fun loadFromString(str: String): LogResult
}

sealed class LogResult {
    abstract fun asString(): String

    companion object : LogResultLoader {
        override fun loadFromString(str: String): LogResult {
            val mode = str[0]
            return when (mode) {
                '0' -> LazyLogResult.loadFromString(str)
                '1' -> ArgLogResult.loadFromString(str)
                else -> error("Invalid log result mode: $mode")
            }
        }
    }
}


class LazyLogResult(
    val t: String?,
    val msg: String
) : LogResult() {
    override fun asString(): String {
        return buildString {
            val mode = 0
            val stackTraceLength = t?.length ?: -1
            val msgLength = msg.length
            append("$mode $stackTraceLength $msgLength")
            append("\n")
            if (t != null) {
                append(t)
            }
            append(msg)
        }
    }

    companion object : LogResultLoader {
        override fun loadFromString(str: String): LogResult {
            val firstLine = str.substringBefore("\n")
            val left = str.substringAfter("\n")
            val array = firstLine.split(' ').map { it.toInt() }
            val stackTraceLength = array[1]
            val msgLength = array[2]
            val extra = StringExtra(left)
            val t = extra.get(stackTraceLength)
            val msg = extra.get(msgLength)
            return LazyLogResult(t, msg ?: "")
        }
    }
}

class StringExtra(private val str: String) {
    private var offset = 0
    fun get(length: Int): String? {
        return if (length == -1) {
            null
        } else {
            if (length == 0) {
                ""
            } else {
                str.substring(offset, length).also {
                    offset += length
                }
            }
        }
    }
}

class ArgLogResult(
    val t: String?,
    val format: String?,
    var argArray: List<String?>
) : LogResult() {
    override fun asString(): String {
        return buildString {
            val mode = 1
            val stackTraceLength = t?.length ?: -1
            val formatLength = format?.length ?: -1
            val arrayCount: Int = argArray.size
            append("$mode $stackTraceLength $formatLength $arrayCount")
            argArray.forEach {
                append(" ")
                append(it?.length ?: -1)
            }
            append("\n")
            if (t != null) {
                append(t)
            }
            if (format != null) {
                append(format)
            }
            argArray.forEach {
                if (it != null) {
                    append(it)
                }
            }
        }
    }

    companion object : LogResultLoader {
        override fun loadFromString(str: String): LogResult {
            val firstLine = str.substringBefore("\n")
            val left = str.substringAfter("\n")
            val array = firstLine.split(' ').map { it.toInt() }
            val stackTraceLength = array[1]
            val formatLength = array[2]
//            val arrayCount = array[3]
            val arrayContentLength = array.subList(4, array.size)
            val extra = StringExtra(left)
            val t = extra.get(stackTraceLength)
            val format = extra.get(formatLength)
            val argArray = arrayContentLength.map { extra.get(it) }
            return ArgLogResult(t, format, argArray)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).toStringSafe(): String {
    return try {
        invoke().toString()
    } catch (e: Exception) {
        ErrorMessageProducer.getErrorLog(e)
    }
}

sealed class LogBuffer {
    abstract fun asResult(): LogResult
}


class LazyLogBuffer(
    val t: Throwable?,
    val msg: () -> Any?
) : LogBuffer() {
    override fun asResult(): LogResult {
        return LazyLogResult(t?.stackTraceToString(), msg.toStringSafe())
    }
}

class ArgLogBuffer(
    val t: Throwable?,
    val format: String?,
    var argArray: Array<out Any?>
) : LogBuffer() {
    override fun asResult(): LogResult {
        return ArgLogResult(t?.stackTraceToString(), format, argArray.map { it?.toString() })
    }
}

fun repeatLog(level: Level, t: Throwable?, msg: () -> Any?): Boolean {
    TODO()
}

class KLoggerAdapter(private val kLogger: KLogger) : KLogger by kLogger {
    override fun debug(msg: () -> Any?) {
        kLogger.debug(msg)
    }

    override fun debug(t: Throwable?, msg: () -> Any?) {
        kLogger.debug(t, msg)
    }

    override fun debug(marker: Marker?, msg: () -> Any?) {
        kLogger.debug(marker, msg)
    }

    override fun debug(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        kLogger.debug(marker, t, msg)
    }

    override fun debug(msg: String?) {
        kLogger.debug(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        kLogger.debug(format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        kLogger.debug(format, arg1, arg2)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        kLogger.debug(format, arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        kLogger.debug(msg, t)
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?) {
        kLogger.debug(marker, msg)
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        kLogger.debug(marker, format, arg)
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        kLogger.debug(marker, format, arg1, arg2)
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        kLogger.debug(marker, format, arguments)
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        kLogger.debug(marker, msg, t)
    }

    override fun error(msg: () -> Any?) {
        kLogger.error(msg)
    }

    override fun error(t: Throwable?, msg: () -> Any?) {
        kLogger.error(t, msg)
    }

    override fun error(marker: Marker?, msg: () -> Any?) {
        kLogger.error(marker, msg)
    }

    override fun error(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        kLogger.error(marker, t, msg)
    }

    override fun error(msg: String?) {
        kLogger.error(msg)
    }

    override fun error(format: String?, arg: Any?) {
        kLogger.error(format, arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        kLogger.error(format, arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        kLogger.error(format, arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        kLogger.error(msg, t)
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?) {
        kLogger.error(marker, msg)
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        kLogger.error(marker, format, arg)
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        kLogger.error(marker, format, arg1, arg2)
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        kLogger.error(marker, format, arguments)
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        kLogger.error(marker, msg, t)
    }

    override fun info(msg: () -> Any?) {
        kLogger.info(msg)
    }

    override fun info(t: Throwable?, msg: () -> Any?) {
        kLogger.info(t, msg)
    }

    override fun info(marker: Marker?, msg: () -> Any?) {
        kLogger.info(marker, msg)
    }

    override fun info(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        kLogger.info(marker, t, msg)
    }

    override fun info(msg: String?) {
        kLogger.info(msg)
    }

    override fun info(format: String?, arg: Any?) {
        kLogger.info(format, arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        kLogger.info(format, arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        kLogger.info(format, arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        kLogger.info(msg, t)
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?) {
        kLogger.info(marker, msg)
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        kLogger.info(marker, format, arg)
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        kLogger.info(marker, format, arg1, arg2)
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        kLogger.info(marker, format, arguments)
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        kLogger.info(marker, msg, t)
    }

    override fun trace(msg: () -> Any?) {
        kLogger.trace(msg)
    }

    override fun trace(t: Throwable?, msg: () -> Any?) {
        kLogger.trace(t, msg)
    }

    override fun trace(marker: Marker?, msg: () -> Any?) {
        kLogger.trace(marker, msg)
    }

    override fun trace(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        kLogger.trace(marker, t, msg)
    }

    override fun trace(msg: String?) {
        kLogger.trace(msg)
    }

    override fun trace(format: String?, arg: Any?) {
        kLogger.trace(format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        kLogger.trace(format, arg1, arg2)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        kLogger.trace(format, arguments)
    }

    override fun trace(msg: String?, t: Throwable?) {
        kLogger.trace(msg, t)
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?) {
        kLogger.trace(marker, msg)
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        kLogger.trace(marker, format, arg)
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        kLogger.trace(marker, format, arg1, arg2)
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, vararg argArray: Any?) {
        kLogger.trace(marker, format, argArray)
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        kLogger.trace(marker, msg, t)
    }

    override fun warn(msg: () -> Any?) {
        kLogger.warn(msg)
    }

    override fun warn(t: Throwable?, msg: () -> Any?) {
        kLogger.warn(t, msg)
    }

    override fun warn(marker: Marker?, msg: () -> Any?) {
        kLogger.warn(marker, msg)
    }

    override fun warn(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        kLogger.warn(marker, t, msg)
    }

    override fun warn(msg: String?) {
        kLogger.warn(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        kLogger.warn(format, arg)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        kLogger.warn(format, arguments)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        kLogger.warn(format, arg1, arg2)
    }

    override fun warn(msg: String?, t: Throwable?) {
        kLogger.warn(msg, t)
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?) {
        kLogger.warn(marker, msg)
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        kLogger.warn(marker, format, arg)
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        kLogger.warn(marker, format, arg1, arg2)
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        kLogger.warn(marker, format, arguments)
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        kLogger.warn(marker, msg, t)
    }
}