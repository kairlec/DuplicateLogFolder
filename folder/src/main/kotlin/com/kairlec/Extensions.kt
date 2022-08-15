@file:Suppress("NOTHING_TO_INLINE")

package com.kairlec

import mu.KLogger
import mu.KotlinLogging
import mu.toKLogger
import org.slf4j.Logger
import org.slf4j.event.Level

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

@PublishedApi
internal fun KLogger.write(resultWrapper: RuntimeLogResultWrapper) {
    if (resultWrapper.written) {
        return
    }
    resultWrapper.written = true
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
