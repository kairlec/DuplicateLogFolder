@file:Suppress(
    "DEPRECATION", "UnusedReceiverParameter", "RemoveRedundantQualifierName", "unused",
    "MemberVisibilityCanBePrivate"
)

package com.kairlec

import com.kairlec.KLoggerContext.KLoggerThreadContext
import com.kairlec.log.*
import mu.KLogger
import mu.internal.ErrorMessageProducer
import org.slf4j.MDC
import org.slf4j.event.Level
import java.io.PrintStream
import java.io.PrintWriter


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
    class KLoggerThreadContext(
        var last: List<LogResult>,
        val buffer: MutableList<Any>,
        var countBuffer: Int,
        var matchIndex: Int,
        var matchFastFailed: Boolean
    ) {
        fun resetMatch() {
            matchIndex = 0
            matchFastFailed = false
        }
    }

    val contextThreadLocal = InheritableThreadLocal<KLoggerThreadContext>()
}

inline val KLogger.threadFolderContext: KLoggerThreadContext
    get() = KLoggerContext.contextThreadLocal.get()

const val foldTimesMdcKey = "FOLD_TIMES"

inline fun KLogger.withFolder(block: () -> Unit) {
    val c0 = KLoggerContext.contextThreadLocal.get()
    val c = if (c0 == null) {
        val c1 = KLoggerThreadContext(emptyList(), mutableListOf(), 0, 0, true)
        KLoggerContext.contextThreadLocal.set(c1)
        Runtime.getRuntime().addShutdownHook(Thread({
            withMdc {
                flushBuffer()
            }
        }, "${Thread.currentThread().name}-log-folder"))
        c1
    } else {
        c0.resetMatch()
        c0
    }

    try {
        block()
    } finally {
        if (c.matchFastFailed) {
            c.last = c.buffer.map {
                if (it is LogBuffer) {
                    it.asResult()
                } else {
                    it as LogResult
                }
            }
            c.countBuffer = 0
        } else {
            c.countBuffer++
        }
        c.buffer.clear()
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
        threadFolderContext.last.forEach {
            write(it)
        }
    }

    fun KLogger.flushBuffer() {
        flushLast()

        val buffer = threadFolderContext.buffer
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

fun KLogger.withMdc(block: MdcScope.() -> Unit) {
    MDC.put(foldTimesMdcKey, "[x${threadFolderContext.countBuffer}]")
    try {
        MdcScope.block()
    } finally {
        MDC.remove(foldTimesMdcKey)
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
    val c = threadFolderContext
    if (c.matchFastFailed) {
        c.buffer.add(logBuffer)
        return false
    }
    val exists = c.last.getOrNull(c.matchIndex)
    if (exists == null) {
        withMdc {
            flushBuffer()
        }
        c.buffer.add(logBuffer)
        c.matchFastFailed = true
        return false
    }
    val result = logBuffer.asResult()
    c.buffer.add(result)
    if (!exists.sameTo(result)) {
        withMdc {
            flushBuffer()
        }
        c.matchFastFailed = true
        return false
    }
    c.matchIndex++
    return true
}


class ArrayWrapper(val array: Array<out Any?>, val newSize: Int)
