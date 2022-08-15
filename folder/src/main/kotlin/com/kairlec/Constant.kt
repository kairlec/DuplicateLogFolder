package com.kairlec

import com.kairlec.log.ArgLogItem
import com.kairlec.log.ArgLogResult
import com.kairlec.log.LazyLogResult
import com.kairlec.log.LogResult
import mu.internal.ErrorMessageProducer
import org.slf4j.event.Level


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

@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).toStringSafe(): String {
    return try {
        invoke().toString()
    } catch (e: Exception) {
        ErrorMessageProducer.getErrorLog(e)
    }
}