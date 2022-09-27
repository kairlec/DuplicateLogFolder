package com.kairlec

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val log = KotlinLogging.foldLogger {}

suspend fun main() {
    repeat(5) {
        log.suspendFolder(33) {
            val j = coroutineScope {
                val job = launch {
                    log.warn { "abcd" }
                }
                if (it > 3) {
                    log.error("err")
                }
                job
            }
            j.join()
        }
    }
}

private val log2 = KotlinLogging.logger { }