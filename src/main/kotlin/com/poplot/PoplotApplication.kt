package com.poplot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PoplotApplication

fun main(args: Array<String>) {
    runApplication<PoplotApplication>(*args)
}
