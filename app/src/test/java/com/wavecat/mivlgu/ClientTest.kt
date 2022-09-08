package com.wavecat.mivlgu

import kotlinx.coroutines.runBlocking


class ClientTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                println(Client().scheduleGetJson("Р-121", "1", "2022"))
            }
        }
    }
}