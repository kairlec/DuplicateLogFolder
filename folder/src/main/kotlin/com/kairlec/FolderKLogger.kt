package com.kairlec

import mu.KLogger
import mu.Marker
import org.slf4j.event.Level

@PublishedApi
internal fun FolderKLogger.repeatLog(level: Level, t: Throwable?, msg: () -> Any?): Boolean {
    return matchCache(LazyLogBuffer(level, t, msg))
}

@PublishedApi
internal fun FolderKLogger.repeatLog(level: Level, t: Throwable?, format: String?, argArray: ArrayWrapper): Boolean {
    return matchCache(ArgLogBuffer(level, t, format, argArray))
}

@PublishedApi
internal fun FolderKLogger.repeatLog(level: Level, t: Throwable?, format: String?, argArray: Array<out Any?>? = null): Boolean {
    return matchCache(ArgLogBuffer(level, t, format, argArray?.let { ArrayWrapper(it, it.size) }))
}

class FolderKLogger(private val kLogger: KLogger) : KLogger by kLogger {
    override fun debug(msg: () -> Any?) {
        if (repeatLog(Level.DEBUG, null, msg)) {
            return
        }
        kLogger.debug(msg)
    }

    override fun debug(t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.DEBUG, t, msg)) {
            return
        }
        kLogger.debug(t, msg)
    }

    override fun debug(marker: Marker?, msg: () -> Any?) {
        if (repeatLog(Level.DEBUG, null, msg)) {
            return
        }
        kLogger.debug(marker, msg)
    }

    override fun debug(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.DEBUG, t, msg)) {
            return
        }
        kLogger.debug(marker, t, msg)
    }

    override fun debug(msg: String?) {
        if (repeatLog(Level.DEBUG, null, msg)) {
            return
        }
        kLogger.debug(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.DEBUG, arg, format)) {
                return
            }
            kLogger.debug(format, arg)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arrayOf(arg))) {
                return
            }
            kLogger.debug(format, arg)
            return
        }
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.DEBUG, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.debug(format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.debug(format, arg1, arg2)
            return
        }
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.DEBUG, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.debug(format, *arguments)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arguments)) {
                return
            }
            kLogger.debug(format, *arguments)
            return
        }
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (repeatLog(Level.DEBUG, t, msg)) {
            return
        }
        kLogger.debug(msg, t)
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?) {
        if (repeatLog(Level.DEBUG, null, msg)) {
            return
        }
        kLogger.debug(marker, msg)
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.DEBUG, arg, format)) {
                return
            }
            kLogger.debug(marker, format, arg)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arrayOf(arg))) {
                return
            }
            kLogger.debug(marker, format, arg)
            return
        }
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.DEBUG, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.debug(marker, format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.debug(marker, format, arg1, arg2)
            return
        }
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.DEBUG, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.debug(marker, format, *arguments)
            return
        } else {
            if (repeatLog(Level.DEBUG, null, format, arguments)) {
                return
            }
            kLogger.debug(marker, format, *arguments)
            return
        }
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        if (repeatLog(Level.DEBUG, t, msg)) {
            return
        }
        kLogger.debug(marker, msg, t)
    }

    override fun error(msg: () -> Any?) {
        if (repeatLog(Level.ERROR, null, msg)) {
            return
        }
        kLogger.error(msg)
    }

    override fun error(t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.ERROR, t, msg)) {
            return
        }
        kLogger.error(t, msg)
    }

    override fun error(marker: Marker?, msg: () -> Any?) {
        if (repeatLog(Level.ERROR, null, msg)) {
            return
        }
        kLogger.error(marker, msg)
    }

    override fun error(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.ERROR, t, msg)) {
            return
        }
        kLogger.error(marker, t, msg)
    }

    override fun error(msg: String?) {
        if (repeatLog(Level.ERROR, null, msg)) {
            return
        }
        kLogger.error(msg)
    }

    override fun error(format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.ERROR, arg, format)) {
                return
            }
            kLogger.error(format, arg)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arrayOf(arg))) {
                return
            }
            kLogger.error(format, arg)
            return
        }
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.ERROR, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.error(format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.error(format, arg1, arg2)
            return
        }
    }

    override fun error(format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.ERROR, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.error(format, *arguments)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arguments)) {
                return
            }
            kLogger.error(format, *arguments)
            return
        }
    }

    override fun error(msg: String?, t: Throwable?) {
        if (repeatLog(Level.ERROR, t, msg)) {
            return
        }
        kLogger.error(msg, t)
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?) {
        if (repeatLog(Level.ERROR, null, msg)) {
            return
        }
        kLogger.error(marker, msg)
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.ERROR, arg, format)) {
                return
            }
            kLogger.error(marker, format, arg)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arrayOf(arg))) {
                return
            }
            kLogger.error(marker, format, arg)
            return
        }
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.ERROR, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.error(marker, format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.error(marker, format, arg1, arg2)
            return
        }
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.ERROR, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.error(marker, format, *arguments)
            return
        } else {
            if (repeatLog(Level.ERROR, null, format, arguments)) {
                return
            }
            kLogger.error(marker, format, *arguments)
            return
        }
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        if (repeatLog(Level.ERROR, t, msg)) {
            return
        }
        kLogger.error(marker, msg, t)
    }

    override fun info(msg: () -> Any?) {
        if (repeatLog(Level.INFO, null, msg)) {
            return
        }
        kLogger.info(msg)
    }

    override fun info(t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.INFO, t, msg)) {
            return
        }
        kLogger.info(t, msg)
    }

    override fun info(marker: Marker?, msg: () -> Any?) {
        if (repeatLog(Level.INFO, null, msg)) {
            return
        }
        kLogger.info(marker, msg)
    }

    override fun info(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.INFO, t, msg)) {
            return
        }
        kLogger.info(marker, t, msg)
    }

    override fun info(msg: String?) {
        if (repeatLog(Level.INFO, null, msg)) {
            return
        }
        kLogger.info(msg)
    }

    override fun info(format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.INFO, arg, format)) {
                return
            }
            kLogger.info(format, arg)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arrayOf(arg))) {
                return
            }
            kLogger.info(format, arg)
            return
        }
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.INFO, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.info(format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.info(format, arg1, arg2)
            return
        }
    }

    override fun info(format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.INFO, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.info(format, *arguments)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arguments)) {
                return
            }
            kLogger.info(format, *arguments)
            return
        }
    }

    override fun info(msg: String?, t: Throwable?) {
        if (repeatLog(Level.INFO, t, msg)) {
            return
        }
        kLogger.info(msg, t)
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?) {
        if (repeatLog(Level.INFO, null, msg)) {
            return
        }
        kLogger.info(marker, msg)
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.INFO, arg, format)) {
                return
            }
            kLogger.info(marker, format, arg)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arrayOf(arg))) {
                return
            }
            kLogger.info(marker, format, arg)
            return
        }
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.INFO, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.info(marker, format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.info(marker, format, arg1, arg2)
            return
        }
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.INFO, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.info(marker, format, *arguments)
            return
        } else {
            if (repeatLog(Level.INFO, null, format, arguments)) {
                return
            }
            kLogger.info(marker, format, *arguments)
            return
        }
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        if (repeatLog(Level.INFO, t, msg)) {
            return
        }
        kLogger.info(marker, msg, t)
    }

    override fun trace(msg: () -> Any?) {
        if (repeatLog(Level.TRACE, null, msg)) {
            return
        }
        kLogger.trace(msg)
    }

    override fun trace(t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.TRACE, t, msg)) {
            return
        }
        kLogger.trace(t, msg)
    }

    override fun trace(marker: Marker?, msg: () -> Any?) {
        if (repeatLog(Level.TRACE, null, msg)) {
            return
        }
        kLogger.trace(marker, msg)
    }

    override fun trace(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.TRACE, t, msg)) {
            return
        }
        kLogger.trace(marker, t, msg)
    }

    override fun trace(msg: String?) {
        if (repeatLog(Level.TRACE, null, msg)) {
            return
        }
        kLogger.trace(msg)
    }

    override fun trace(format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.TRACE, arg, format)) {
                return
            }
            kLogger.trace(format, arg)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, arrayOf(arg))) {
                return
            }
            kLogger.trace(format, arg)
            return
        }
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.TRACE, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.trace(format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.trace(format, arg1, arg2)
            return
        }
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.TRACE, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.trace(format, *arguments)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, arguments)) {
                return
            }
            kLogger.trace(format, *arguments)
            return
        }
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (repeatLog(Level.TRACE, t, msg)) {
            return
        }
        kLogger.trace(msg, t)
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?) {
        if (repeatLog(Level.TRACE, null, msg)) {
            return
        }
        kLogger.trace(marker, msg)
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.TRACE, arg, format)) {
                return
            }
            kLogger.trace(marker, format, arg)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, arrayOf(arg))) {
                return
            }
            kLogger.trace(marker, format, arg)
            return
        }
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.TRACE, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.trace(marker, format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.trace(marker, format, arg1, arg2)
            return
        }
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, vararg argArray: Any?) {
        val last = argArray.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.TRACE, last, format, ArrayWrapper(argArray, argArray.size - 1))) {
                return
            }
            kLogger.trace(marker, format, *argArray)
            return
        } else {
            if (repeatLog(Level.TRACE, null, format, argArray)) {
                return
            }
            kLogger.trace(marker, format, *argArray)
            return
        }
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        if (repeatLog(Level.TRACE, t, msg)) {
            return
        }
        kLogger.trace(marker, msg, t)
    }

    override fun warn(msg: () -> Any?) {
        if (repeatLog(Level.WARN, null, msg)) {
            return
        }
        kLogger.warn(msg)
    }

    override fun warn(t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.WARN, t, msg)) {
            return
        }
        kLogger.warn(t, msg)
    }

    override fun warn(marker: Marker?, msg: () -> Any?) {
        if (repeatLog(Level.WARN, null, msg)) {
            return
        }
        kLogger.warn(marker, msg)
    }

    override fun warn(marker: Marker?, t: Throwable?, msg: () -> Any?) {
        if (repeatLog(Level.WARN, t, msg)) {
            return
        }
        kLogger.warn(marker, t, msg)
    }

    override fun warn(msg: String?) {
        if (repeatLog(Level.WARN, null, msg)) {
            return
        }
        kLogger.warn(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.WARN, arg, format)) {
                return
            }
            kLogger.warn(format, arg)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arrayOf(arg))) {
                return
            }
            kLogger.warn(format, arg)
            return
        }
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.WARN, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.warn(format, *arguments)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arguments)) {
                return
            }
            kLogger.warn(format, *arguments)
            return
        }
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.WARN, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.warn(format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.warn(format, arg1, arg2)
            return
        }
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (repeatLog(Level.WARN, t, msg)) {
            return
        }
        kLogger.warn(msg, t)
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?) {
        if (repeatLog(Level.WARN, null, msg)) {
            return
        }
        kLogger.warn(marker, msg)
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        if (arg is Throwable) {
            if (repeatLog(Level.WARN, arg, format)) {
                return
            }
            kLogger.warn(marker, format, arg)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arrayOf(arg))) {
                return
            }
            kLogger.warn(marker, format, arg)
            return
        }
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (arg2 is Throwable) {
            if (repeatLog(Level.WARN, arg2, format, arrayOf(arg1))) {
                return
            }
            kLogger.warn(marker, format, arg1, arg2)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arrayOf(arg1, arg2))) {
                return
            }
            kLogger.warn(marker, format, arg1, arg2)
            return
        }
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        val last = arguments.lastOrNull()
        if (last is Throwable) {
            if (repeatLog(Level.WARN, last, format, ArrayWrapper(arguments, arguments.size - 1))) {
                return
            }
            kLogger.warn(marker, format, *arguments)
            return
        } else {
            if (repeatLog(Level.WARN, null, format, arguments)) {
                return
            }
            kLogger.warn(marker, format, *arguments)
            return
        }
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        if (repeatLog(Level.WARN, t, msg)) {
            return
        }
        kLogger.warn(marker, msg, t)
    }
}

