package com.kairlec

import mu.KotlinLogging

private val log = KotlinLogging.logger { }

fun main() {
    withRepeatTimes(1) {
        log.info { "test" }
    }
}