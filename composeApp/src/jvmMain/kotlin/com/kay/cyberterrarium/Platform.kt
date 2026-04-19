package com.kay.cyberterrarium

class JVMPlatform {
    val name: String = "Java ${System.getProperty("java.version")}"
}

fun getPlatform() = JVMPlatform()