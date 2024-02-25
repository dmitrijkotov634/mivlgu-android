package com.wavecat.mivlgu

import com.wavecat.mivlgu.client.HttpClient
import kotlinx.coroutines.runBlocking


class ClientTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                println(HttpClient().scheduleGetJson("Р-121", "1", "2022"))
            }
        }
    }
}