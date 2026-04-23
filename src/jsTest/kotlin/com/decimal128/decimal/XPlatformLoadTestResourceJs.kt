package com.decimal128.decimal

actual fun loadTestResourceAsString(fileName: String): String? {
    val cwd = js("process.cwd()") as String
    val path = "$cwd/../../../../src/jvmTest/resources$fileName"
    return try {
        val fs = js("require('fs')")
        fs.readFileSync(path, "utf8") as String
    } catch (_: Throwable) {
        null
    }
}

