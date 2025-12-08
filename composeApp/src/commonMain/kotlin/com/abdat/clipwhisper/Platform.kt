package com.abdat.clipwhisper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform