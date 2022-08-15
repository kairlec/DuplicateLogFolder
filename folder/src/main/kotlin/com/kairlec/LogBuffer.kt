package com.kairlec

import com.kairlec.log.*
import org.slf4j.event.Level

sealed class LogBuffer(var written: Boolean) {
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
    written: Boolean,
    val logLevel: Level,
    val t: Throwable?,
    val msg: () -> Any?
) : LogBuffer(written) {
    override fun asResult(): RuntimeLogResultWrapper {
        return RuntimeLogResultWrapper.from(logResult {
            this.level = logLevel.toInt()
            this.lazyLogResult = lazyLogResult {
                t?.delegate()?.let {
                    this.throwableDelegate = it
                }
                this.message = msg.toStringSafe()
            }
        }, t).also { it.written = this.written }
    }
}

class ArgLogBuffer(
    written: Boolean,
    val logLevel: Level,
    val t: Throwable?,
    val format: String?,
    var argArray: ArrayWrapper?
) : LogBuffer(written) {
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
        }, t).also { it.written = this.written }
    }
}