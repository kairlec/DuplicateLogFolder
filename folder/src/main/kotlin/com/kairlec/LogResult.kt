@file:Suppress(
    "UnusedReceiverParameter", "RemoveRedundantQualifierName", "unused",
    "MemberVisibilityCanBePrivate"
)

package com.kairlec

import com.kairlec.FoldMdcKeys.clearFoldMdc
import com.kairlec.FoldMdcKeys.setFoldMdc
import com.kairlec.FolderKLoggerContexts.FolderKLoggerContext
import com.kairlec.FolderKLoggerContexts.currentLogFolderId
import com.kairlec.log.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import kotlin.DeprecationLevel.ERROR


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

object FolderKLoggerContexts {
    private val contextMap by lazy {
        ConcurrentHashMap<Long, FolderKLoggerContext>().also {
            Runtime.getRuntime().addShutdownHook(Thread({
                it.values.forEach { context ->
                    context.withMdc {
                        log.flushBuffer()
                    }
                }
            }, "log-folder-clear"))
        }
    }
    private val log = KotlinLogging.foldLogger { }

    class FolderKLoggerContext(
        val id: Long,
        val config: FolderKLoggerConfig
    ) {

        var last: List<RuntimeLogResultWrapper> = emptyList()
        val buffer: MutableList<Any> = mutableListOf()
        var countBuffer: Int = 0
        var matchIndex: Int = 0
        var matchFastFailed: Boolean? = true

        fun resetMatch() {
            matchIndex = 0
            matchFastFailed = null
        }

        inline fun clearFinal(flush: FolderKLoggerContext.() -> Unit) {
            when (matchFastFailed) {
                true -> {
                    last = buffer.map {
                        if (it is LogBuffer) {
                            it.asResult()
                        } else {
                            it as RuntimeLogResultWrapper
                        }
                    }
                    countBuffer = 0
                }

                false -> {
                    countBuffer++
                    last.forEach {
                        it.written = false
                    }
                    if (countBuffer >= config.folderMaxLimit) {
                        flush()
                    }
                    if (countBuffer >= config.persistenceBuffer) {
                        with(config.persistenceStrategy) {
                            persistence()
                        }
                    }
                }

                else -> {
                    // 这一轮没有数据
                    flush()
                }
            }
            buffer.clear()
        }
    }

    operator fun set(id: Long, context: FolderKLoggerContext) {
        contextMap[id] = context
    }

    operator fun get(id: Long): FolderKLoggerContext? {
        return contextMap[id]
    }

    @PublishedApi
    internal val currentLogFolderId = InheritableThreadLocal<Long>()

    val currentThreadFolderContext get() = currentLogFolderId.get()?.let { contextMap[it] }
}

object FoldMdcKeys {
    const val foldTimesMdcKey = "FOLD_TIMES"
    const val foldIdMdcKey = "FOLD_ID"
    const val foldFormatKey = "FOLD_FORMAT"

    fun FolderKLoggerContext.setFoldMdc() {
        MDC.put(foldIdMdcKey, this.id.toString())
        MDC.put(foldTimesMdcKey, this.countBuffer.toString())
        MDC.put(foldFormatKey, " [${this.id}, x${this.countBuffer}]")
    }

    fun setFoldMdc(id: Long, times: Int) {
        MDC.put(foldIdMdcKey, id.toString())
        MDC.put(foldTimesMdcKey, times.toString())
        MDC.put(foldFormatKey, " [${id}, x${times}]")
    }

    fun clearFoldMdc() {
        MDC.remove(foldIdMdcKey)
        MDC.remove(foldTimesMdcKey)
        MDC.remove(foldFormatKey)
    }
}

inline fun FolderKLogger.folder(
    id: Long,
    configuration: FolderKLoggerConfig.() -> Unit = { },
    block: () -> Unit
) {
    currentLogFolderId.set(id)
    val c = FolderKLoggerContexts[id]?.apply { resetMatch() }
        ?: FolderKLoggerContext(id, FolderKLoggerConfig().apply { configuration() }.check()).also {
            FolderKLoggerContexts[id] = it
        }
    try {
        block()
    } finally {
        c.clearFinal {
            withMdc {
                flushLast()
            }
        }
        currentLogFolderId.remove()
    }
}
@Deprecated(
    "如果不在同一个线程,当前线程的id不一致,请尽可能指定id",
    ReplaceWith("this.folder(TODO(\"id here\") as Long,configuration,block)"),
    ERROR
)
inline fun FolderKLogger.folder(
    configuration: FolderKLoggerConfig.() -> Unit = { },
    block: () -> Unit
) {
    folder(Thread.currentThread().id, configuration, block)
}

@Deprecated(
    "如果发生一次协程切换,则可能获取到的名称或当前线程的id不一致,请尽可能指定id",
    ReplaceWith("this.suspendFolder(TODO(\"id here\") as Long,configuration,block)"),
    ERROR
)
suspend inline fun FolderKLogger.suspendFolder(
    noinline configuration: suspend FolderKLoggerConfig.() -> Unit = { },
    crossinline block: suspend () -> Unit
) {
    suspendFolder(
        currentCoroutineContext()[CoroutineName.Key]?.name?.hashCode()?.toLong() ?: Thread.currentThread().id,
        configuration,
        block
    )
}

suspend inline fun FolderKLogger.suspendFolder(
    noinline idProvider: suspend () -> Long,
    noinline configuration: suspend FolderKLoggerConfig.() -> Unit = { },
    crossinline block: suspend () -> Unit
) {
    suspendFolder(idProvider(), configuration, block)
}

suspend inline fun FolderKLogger.suspendFolder(
    id: Long,
    noinline configuration: suspend FolderKLoggerConfig.() -> Unit = { },
    crossinline block: suspend () -> Unit
) {
    withContext(currentLogFolderId.asContextElement(id) + MDCContext()) {
        val c = FolderKLoggerContexts[id]?.apply { resetMatch() }
            ?: FolderKLoggerContext(id, FolderKLoggerConfig().apply { configuration() }.check()).also {
                FolderKLoggerContexts[id] = it
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
}


class MdcScope(private val c: FolderKLoggerContext) {
    /**
     * 强制刷新上一次的记录信息
     */
    @OptIn(ExperimentalApi::class)
    fun FolderKLogger.flushLast() {
        c.setFoldMdc()
        c.last.forEach {
            write(it)
        }
        c.last = emptyList()
        c.countBuffer = 0
        with(c) {
            with(config.persistenceStrategy) {
                clearPersistence()
            }
        }
        clearFoldMdc()
    }

    /**
     * 强制刷新掉buffer, 并且清空buffer
     */
    fun FolderKLogger.flushBuffer() {
        flushLast()
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
}

inline fun FolderKLoggerContext.withMdc(block: MdcScope.() -> Unit) {
    try {
        MdcScope(this).block()
    } finally {
        clearFoldMdc()
    }
}

@PublishedApi
internal fun FolderKLogger.matchCache(logBuffer: LogBuffer): Boolean {
    val c = FolderKLoggerContexts.currentThreadFolderContext ?: return false
    if (c.matchFastFailed == true) {
        // fast fail 是前面已经flush过了,这里不需要flush
        c.buffer.add(logBuffer)
        logBuffer.written = true
        return false
    }
    c.matchFastFailed = false
    val exists = c.last.getOrNull(c.matchIndex)
    if (exists == null) {
        // 超出了原来的匹配界限,这里直接刷新缓冲区
        c.withMdc {
            flushBuffer()
        }
        // 老的缓冲区已经写出了,这里返回false的匹配,日志会直接输出的,所以这里需要设置written标记为true,防止下次刷新缓冲区的时候多次输出
        c.buffer.add(logBuffer)
        logBuffer.written = true
        return false
    }
    val result = logBuffer.asResult()
    if (!exists.logResult.sameTo(result.logResult)) {
        c.withMdc {
            flushBuffer()
        }
        c.buffer.add(result)
        logBuffer.written = true
        result.written = true
        return false
    }
    c.buffer.add(result)
    c.matchIndex++
    return true
}


class ArrayWrapper(val array: Array<out Any?>, val newSize: Int)
