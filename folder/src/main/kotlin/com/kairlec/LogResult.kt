@file:Suppress(
    "DEPRECATION", "UnusedReceiverParameter", "RemoveRedundantQualifierName", "unused",
    "MemberVisibilityCanBePrivate"
)

package com.kairlec

import com.kairlec.FolderKLoggerContext.FolderKLoggerThreadContext
import com.kairlec.log.*
import mu.KLogger
import mu.internal.ErrorMessageProducer
import org.slf4j.MDC
import org.slf4j.event.Level


data class MatchResult(
    var idx: Int = 0,
    var failed: Boolean = false
) {
    fun reset() {
        idx = 0
        failed = false
    }
}

class FolderKLoggerConfig {
    var persistenceStrategy: LogPersistenceStrategy = LogPersistenceStrategy.Default
    var folderMaxLimit: Int = Int.MAX_VALUE
    var persistenceBuffer: Int = 50

    @PublishedApi
    internal fun check() = apply {
        require(folderMaxLimit > 1) { "folderMaxLimit must be greater than 1" }
    }
}

object FolderKLoggerContext {
    class FolderKLoggerThreadContext(
        val id: String,
        val config: FolderKLoggerConfig
    ) {

        var last: List<RuntimeLogResultWrapper> = emptyList()
        val buffer: MutableList<Any> = mutableListOf()
        var countBuffer: Int = 0
        var matchIndex: Int = 0
        var matchFastFailed: Boolean = true

        fun resetMatch() {
            matchIndex = 0
            matchFastFailed = false
        }

        inline fun clearFinal(flush: () -> Unit) {
            if (matchFastFailed) {
                last = buffer.map {
                    if (it is LogBuffer) {
                        it.asResult()
                    } else {
                        it as RuntimeLogResultWrapper
                    }
                }
                countBuffer = 0
            } else {
                countBuffer++
                if (countBuffer >= config.folderMaxLimit) {
                    flush()
                }
                if (countBuffer >= config.persistenceBuffer) {
                    with(config.persistenceStrategy) {
                        persistence()
                    }
                }
            }
            buffer.clear()
        }
    }

    val contextThreadLocal = InheritableThreadLocal<FolderKLoggerThreadContext>()
}

inline val FolderKLogger.threadFolderContext: FolderKLoggerThreadContext
    get() = FolderKLoggerContext.contextThreadLocal.get()

const val foldTimesMdcKey = "FOLD_TIMES"
const val foldIdMdcKey = "FOLD_ID"

inline fun FolderKLogger.folder(
    id: String = Thread.currentThread().name,
    configuration: FolderKLoggerConfig.() -> Unit = { },
    block: () -> Unit
) {
    val c0 = FolderKLoggerContext.contextThreadLocal.get()
    val c = if (c0 == null) {
        val c1 = FolderKLoggerThreadContext(id, FolderKLoggerConfig().apply(configuration).check())
        FolderKLoggerContext.contextThreadLocal.set(c1)
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
        c.clearFinal {
            withMdc {
                flushLast()
            }
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
    abstract fun asResult(): RuntimeLogResultWrapper
}

internal fun LogResult.asThrowable(): RuntimeThrowableDelegateWrapper? {
    if (this.hasLazyLogResult() && this.lazyLogResult.hasThrowableDelegate()) {
        return RuntimeThrowableDelegateWrapper.from(lazyLogResult.throwableDelegate)
    }
    if (this.hasArgLogResult() && this.argLogResult.hasThrowableDelegate()) {
        return RuntimeThrowableDelegateWrapper.from(argLogResult.throwableDelegate)
    }
    return null
}

class LazyLogBuffer(
    val logLevel: Level,
    val t: Throwable?,
    val msg: () -> Any?
) : LogBuffer() {
    override fun asResult(): RuntimeLogResultWrapper {
        return RuntimeLogResultWrapper.from(logResult {
            this.level = logLevel.toInt()
            this.lazyLogResult = lazyLogResult {
                t?.delegate()?.let {
                    this.throwableDelegate = it
                }
                this.message = msg.toStringSafe()
            }
        }, t)
    }
}

class ArgLogBuffer(
    val logLevel: Level,
    val t: Throwable?,
    val format: String?,
    var argArray: ArrayWrapper?
) : LogBuffer() {
    override fun asResult(): RuntimeLogResultWrapper {
        return RuntimeLogResultWrapper.from(logResult {
            this.level = logLevel.toInt()
            this.argLogResult = argLogResult {
                t?.delegate()?.let {
                    this.throwableDelegate = it
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
        }, t)
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
    if (this.hasThrowableDelegate()) {
        if (!other.hasThrowableDelegate()) {
            return false
        }
        return this.throwableDelegate == other.throwableDelegate
    }
    return this.message == other.message
}

fun ArgLogResult.sameTo(other: ArgLogResult): Boolean {
    if (this.hasThrowableDelegate()) {
        if (!other.hasThrowableDelegate()) {
            return false
        }
        if (this.throwableDelegate != other.throwableDelegate) {
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
    /**
     * 强制刷新上一次的记录信息
     */
    @OptIn(ExperimentalApi::class)
    fun FolderKLogger.flushLast() {
        val c = threadFolderContext
        c.last.forEach {
            write(it)
        }
        c.last = emptyList()
        c.countBuffer = 0
        with(threadFolderContext) {
            with(config.persistenceStrategy) {
                clearPersistence()
            }
        }
    }

    /**
     * 强制刷新掉buffer, 并且清空buffer
     */
    fun FolderKLogger.flushBuffer() {
        flushLast()
        val c = threadFolderContext
        val buffer = c.buffer
        for (idx in buffer.indices) {
            val item = buffer[idx]
            val result = if (item is LogBuffer) {
                item.asResult().also { buffer[idx] = it }
            } else {
                item as RuntimeLogResultWrapper
            }
            write(result)
        }

        buffer.clear()

        c.matchFastFailed = true
        c.matchIndex = 0
    }

    @Deprecated("")
    companion object : MdcScope
}

fun FolderKLogger.withMdc(block: MdcScope.() -> Unit) {
    MDC.put(foldTimesMdcKey, threadFolderContext.countBuffer.toString())
    MDC.put(foldIdMdcKey, threadFolderContext.id)
    try {
        MdcScope.block()
    } finally {
        MDC.remove(foldTimesMdcKey)
        MDC.remove(foldIdMdcKey)
    }
}

@PublishedApi
internal fun KLogger.write(resultWrapper: RuntimeLogResultWrapper) {
    val result = resultWrapper.logResult
    val throwable = resultWrapper.rawThrowable ?: result.asThrowable()?.asThrowable()
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

@PublishedApi
internal fun FolderKLogger.matchCache(logBuffer: LogBuffer): Boolean {
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
        return false
    }
    val result = logBuffer.asResult()
    c.buffer.add(result)
    if (!exists.logResult.sameTo(result.logResult)) {
        withMdc {
            flushBuffer()
        }
        return false
    }
    c.matchIndex++
    return true
}


class ArrayWrapper(val array: Array<out Any?>, val newSize: Int)
