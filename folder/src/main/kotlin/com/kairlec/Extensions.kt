@file:Suppress("NOTHING_TO_INLINE")

package com.kairlec

import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.slf4j.Logger

inline fun KLogger.asFolderLogger(): KLogger {
    return if (this is KLoggerAdapter) {
        this
    } else {
        KLoggerAdapter(this)
    }
}

inline fun KotlinLogging.foldLogger(noinline func: () -> Unit): KLogger {
    return logger(func).asFolderLogger()
}

inline fun KotlinLogging.foldLogger(name: String): KLogger {
    return logger(name).asFolderLogger()
}

inline fun KotlinLogging.foldLogger(underlyingLogger: Logger): KLogger {
    return logger(underlyingLogger).asFolderLogger()
}

inline fun Logger.toFolderKLogger(): KLogger {
    return toKLogger().asFolderLogger()
}