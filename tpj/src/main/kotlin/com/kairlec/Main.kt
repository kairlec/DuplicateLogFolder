package com.kairlec

import mu.KotlinLogging
import java.lang.Exception
import java.lang.RuntimeException

private val log = KotlinLogging.foldLogger {}

fun main() {
    repeat(5) {
        log.folder(
            configuration = {
                this.persistenceBuffer = 1
            }
        ) {
            repeat(3) {
                log.warn { "abcd" }
            }
            if (it == 3) {
                Thread.sleep(3000)
                Runtime.getRuntime().halt(1)
            }
            log.info(Exception("exp", RuntimeException("rcau"))) { "aaa" }
        }
    }
}

private val log2 = KotlinLogging.logger { }