package com.kairlec

import mu.KotlinLogging

private val log = KotlinLogging.foldLogger {}

fun main() {
    repeat(5) {
        log.withRepeatTimes(1) {
            repeat(3) {
                log.warn { "abcd" }
            }
            log.info { "aaa" }
        }
    }
}