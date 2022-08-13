@file:Suppress("NOTHING_TO_INLINE")

package com.kairlec

import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.slf4j.Logger

inline fun KLogger.asFolderLogger(): FolderKLogger {
    return if (this is FolderKLogger) {
        this
    } else {
        FolderKLogger(this)
    }
}

inline fun KotlinLogging.foldLogger(noinline func: () -> Unit): FolderKLogger {
    return logger(func).asFolderLogger()
}

inline fun KotlinLogging.foldLogger(name: String): FolderKLogger {
    return logger(name).asFolderLogger()
}

inline fun KotlinLogging.foldLogger(underlyingLogger: Logger): FolderKLogger {
    return logger(underlyingLogger).asFolderLogger()
}

inline fun Logger.toFolderKLogger(): FolderKLogger {
    return toKLogger().asFolderLogger()
}