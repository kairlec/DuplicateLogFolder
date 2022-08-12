package com.kairlec

import mu.KLogger
import mu.KotlinLogging

val KLogger.folder: KLogger
    get() = if (this is KLoggerAdapter) {
        this
    } else {
        KLoggerAdapter(this)
    }

fun KotlinLogging.foldLogger(func: () -> Unit): KLogger {
    return logger(func).folder
}