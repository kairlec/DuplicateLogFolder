@file:Suppress("DEPRECATION", "UnusedReceiverParameter", "RemoveRedundantQualifierName", "unused",
    "MemberVisibilityCanBePrivate"
)

package com.kairlec

import com.kairlec.log.*
import mu.KLogger
import mu.internal.ErrorMessageProducer
import org.slf4j.MDC
import org.slf4j.event.Level
import java.io.PrintStream
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger


data class MatchResult(
    var idx: Int = 0,
    var failed: Boolean = false
) {
    fun reset() {
        idx = 0
        failed = false
    }
}

object KLoggerContext {
    val current = InheritableThreadLocal<Long>()
    val last = InheritableThreadLocal<List<LogResult>>()
    val buffer = InheritableThreadLocal<MutableList<Any>>()
    val countBuffer = InheritableThreadLocal<AtomicInteger>()
    val matchResult = InheritableThreadLocal<MatchResult>()
}

const val RepeatTimesMdcKey = "REPEAT_TIMES"

inline fun KLogger.withRepeatTimes(id: Long, block: () -> Unit) {
    KLoggerContext.current.set(id)
    KLoggerContext.buffer.set(mutableListOf())
    val result = KLoggerContext.matchResult.get()
    if (result == null) {
        KLoggerContext.countBuffer.set(AtomicInteger(0))
        KLoggerContext.matchResult.set(MatchResult(failed = true))
        KLoggerContext.last.set(emptyList())
        Runtime.getRuntime().addShutdownHook(Thread {
            withMdc {
                flushLast()
                flushBuffer()
            }
        })
    } else {
        result.reset()
    }
    try {
        block()
    } finally {
        if (KLoggerContext.matchResult.get()?.failed == true) {
            KLoggerContext.last.remove()
            KLoggerContext.last.set(KLoggerContext.buffer.get().map {
                if (it is LogBuffer) {
                    it.asResult()
                } else {
                    it as LogResult
                }
            })
            KLoggerContext.countBuffer.get().set(0)
        } else {
            KLoggerContext.countBuffer.get().incrementAndGet()
        }
        KLoggerContext.current.remove()
        KLoggerContext.buffer.remove()
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

private class ThrowableWrapper(private val stackTraceMessage: String) : Throwable() {
    override fun printStackTrace(s: PrintStream) {
        s.print(stackTraceMessage)
    }

    override fun printStackTrace(s: PrintWriter) {
        s.print(stackTraceMessage)
    }

    override fun printStackTrace() {
        System.err.println(stackTraceMessage)
    }
}

fun LogResult.asThrowable(): Throwable? {
    if (this.hasLazyLogResult() && this.lazyLogResult.hasThrowableMessage()) {
        return ThrowableWrapper(this.lazyLogResult.throwableMessage)
    }
    if (this.hasArgLogResult() && this.argLogResult.hasThrowableMessage()) {
        return ThrowableWrapper(this.argLogResult.throwableMessage)
    }
    return null
}


class LazyLogBuffer(
    val logLevel: Level,
    val t: Throwable?,
    val msg: () -> Any?
) : LogBuffer() {
    override fun asResult(): LogResult {
        return logResult {
            this.level = logLevel.toInt()
            this.lazyLogResult = lazyLogResult {
                t?.stackTraceToString()?.let {
                    this.throwableMessage = it
                }
                this.message = msg.toStringSafe()
            }
        }
    }
}

class ArgLogBuffer(
    val logLevel: Level,
    val t: Throwable?,
    val format: String?,
    var argArray: ArrayWrapper?
) : LogBuffer() {
    override fun asResult(): LogResult {
        return logResult {
            this.level = logLevel.toInt()
            this.argLogResult = argLogResult {
                t?.stackTraceToString()?.let {
                    this.throwableMessage = it
                }
                format?.let {
                    this.formatMessage = it
                }
                argArray?.let { args ->
                    repeat(args.newSize) {
                        this.argMessages.add(argLogItem {
                            this.argMessageItem = args.array[it].toString()
                        })
                    }
                }
            }
        }
    }
}

fun LogResult.sameTo(other: LogResult, ignoreLevel: Boolean = false): Boolean {
    if (!ignoreLevel && this.level != other.level) {
        return false
    }
    if (this.hasArgLogResult()) {
        if (!other.hasArgLogResult()) {
            return false
        }
        return this.argLogResult.sameTo(other.argLogResult)
    }
    if (this.hasLazyLogResult()) {
        if (!other.hasLazyLogResult()) {
            return false
        }
        return this.lazyLogResult.sameTo(other.lazyLogResult)
    }
    return false
}

fun LazyLogResult.sameTo(other: LazyLogResult): Boolean {
    if (this.hasThrowableMessage()) {
        if (!other.hasThrowableMessage()) {
            return false
        }
        return this.throwableMessage == other.throwableMessage
    }
    return this.message == other.message
}

fun ArgLogResult.sameTo(other: ArgLogResult): Boolean {
    if (this.hasThrowableMessage()) {
        if (!other.hasThrowableMessage()) {
            return false
        }
        if (this.throwableMessage != other.throwableMessage) {
            return false
        }
    }
    if (this.hasFormatMessage()) {
        if (!other.hasFormatMessage()) {
            return false
        }
        if (this.formatMessage != other.formatMessage) {
            return false
        }
    }
    if (this.argMessagesCount != other.argMessagesCount) {
        return false
    }
    for (i in 0 until this.argMessagesCount) {
        if (!this.argMessagesList[i].sameTo(other.argMessagesList[i])) {
            return false
        }
    }
    return true
}

fun ArgLogItem.sameTo(other: ArgLogItem): Boolean {
    if (this.hasArgMessageItem()) {
        if (!other.hasArgMessageItem()) {
            return false
        }
        if (this.argMessageItem != other.argMessageItem) {
            return false
        }
    }
    return true
}

fun levelOf(levelInt: Int): Level {
    return Level.values().first { it.toInt() == levelInt }
}

@Deprecated("")
interface MdcScope {
    fun KLogger.flushLast() {
        KLoggerContext.last.get().forEach {
            write(it)
        }
    }

    fun KLogger.flushBuffer() {
        flushLast()

        val buffer = KLoggerContext.buffer.get()
        for (idx in buffer.indices) {
            val item = buffer[idx]
            val result = if (item is LogBuffer) {
                item.asResult().also { buffer[idx] = it }
            } else {
                item as LogResult
            }
            write(result)
        }
    }

    @Deprecated("")
    companion object : MdcScope
}

inline fun KLogger.withMdc(block: MdcScope.() -> Unit) {
    MDC.put(RepeatTimesMdcKey, "[x${KLoggerContext.countBuffer.get().get()}]")
    try {
        MdcScope.block()
    } finally {
        MDC.remove(RepeatTimesMdcKey)
    }
}


fun KLogger.write(result: LogResult) {
    val throwable = result.asThrowable()
    if (result.hasLazyLogResult()) {
        val msg = result.lazyLogResult.message
        when (levelOf(result.level)) {
            Level.TRACE -> if (underlyingLogger.isTraceEnabled) {
                underlyingLogger.trace(msg, throwable)
            }

            Level.DEBUG -> if (underlyingLogger.isDebugEnabled) {
                underlyingLogger.debug(msg, throwable)
            }

            Level.INFO -> if (underlyingLogger.isInfoEnabled) {
                underlyingLogger.info(msg, throwable)
            }

            Level.WARN -> if (underlyingLogger.isWarnEnabled) {
                underlyingLogger.warn(msg, throwable)
            }

            Level.ERROR -> if (underlyingLogger.isErrorEnabled) {
                underlyingLogger.error(msg, throwable)
            }
        }
    } else {
        val format = result.argLogResult.formatMessage
        val args =
            result.argLogResult.argMessagesList.map { if (it.hasArgMessageItem()) it.argMessageItem else null }
        if (throwable != null) {
            when (levelOf(result.level)) {
                Level.TRACE -> if (underlyingLogger.isTraceEnabled) {
                    underlyingLogger.trace(format, args.toTypedArray(), throwable)
                }

                Level.DEBUG -> if (underlyingLogger.isDebugEnabled) {
                    underlyingLogger.debug(format, args.toTypedArray(), throwable)
                }

                Level.INFO -> if (underlyingLogger.isInfoEnabled) {
                    underlyingLogger.info(format, args.toTypedArray(), throwable)
                }

                Level.WARN -> if (underlyingLogger.isWarnEnabled) {
                    underlyingLogger.warn(format, args.toTypedArray(), throwable)
                }

                Level.ERROR -> if (underlyingLogger.isErrorEnabled) {
                    underlyingLogger.error(format, args.toTypedArray(), throwable)
                }
            }
        } else {
            when (levelOf(result.level)) {
                Level.TRACE -> if (underlyingLogger.isTraceEnabled) {
                    underlyingLogger.trace(format, args.toTypedArray())
                }

                Level.DEBUG -> if (underlyingLogger.isDebugEnabled) {
                    underlyingLogger.debug(format, args.toTypedArray())
                }

                Level.INFO -> if (underlyingLogger.isInfoEnabled) {
                    underlyingLogger.info(format, args.toTypedArray())
                }

                Level.WARN -> if (underlyingLogger.isWarnEnabled) {
                    underlyingLogger.warn(format, args.toTypedArray())
                }

                Level.ERROR -> if (underlyingLogger.isErrorEnabled) {
                    underlyingLogger.error(format, args.toTypedArray())
                }
            }
        }
    }
}


fun KLogger.matchCache(logBuffer: LogBuffer): Boolean {
    val matchResult = KLoggerContext.matchResult.get()
    if (matchResult.failed) {
        KLoggerContext.buffer.get().add(logBuffer)
        return false
    }
    val exists = KLoggerContext.last.get().getOrNull(matchResult.idx)
    if (exists == null) {
        withMdc {
            flushBuffer()
        }
        KLoggerContext.buffer.get().add(logBuffer)
        matchResult.failed = true
        return false
    }
    val result = logBuffer.asResult()
    KLoggerContext.buffer.get().add(result)
    if (!exists.sameTo(result)) {
        withMdc {
            flushBuffer()
        }
        matchResult.failed = true
        return false
    }
    matchResult.idx++
    return true
}


class ArrayWrapper(val array: Array<out Any?>, val newSize: Int)
