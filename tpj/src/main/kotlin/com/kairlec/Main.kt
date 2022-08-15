package com.kairlec

import mu.KotlinLogging

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
            if (it > 3) {
                log.error("err")
            }
            log.info(Exception("exp", RuntimeException("rcau"))) { "aaa" }
        }
    }
}

private val log2 = KotlinLogging.logger { }