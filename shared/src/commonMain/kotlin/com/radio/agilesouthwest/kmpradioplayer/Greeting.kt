package com.radio.agilesouthwest.kmpradioplayer

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}