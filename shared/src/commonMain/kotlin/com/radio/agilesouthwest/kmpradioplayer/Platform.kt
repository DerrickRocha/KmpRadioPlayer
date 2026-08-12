package com.radio.agilesouthwest.kmpradioplayer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform