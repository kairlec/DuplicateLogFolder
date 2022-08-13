package com.kairlec

import mu.KotlinLogging

private val log = KotlinLogging.foldLogger {}

fun main() {
    repeat(5) {
        log.withFolder {
            repeat(3) {
                log.warn { "abcd" }
            }
            log.info { "aaa" }
        }
    }
}