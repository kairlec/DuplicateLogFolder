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
            log.info { "aaa" }
        }
    }
}

private val log2 = KotlinLogging.logger { }