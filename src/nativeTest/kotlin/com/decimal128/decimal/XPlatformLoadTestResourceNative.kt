package com.decimal128.decimal

import kotlinx.cinterop.refTo
import platform.posix.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun loadTestResourceAsString(fileName: String): String? {
    val path = "src/jvmTest/resources/$fileName"
    println(" loadTestResourceAsString path:$path")
    val file = fopen(path, "r") ?: return null
    try {
        val sb = StringBuilder()
        val buffer = ByteArray(4096)
        while (true) {
            val read = fread(buffer.refTo(0), 1u, buffer.size.toULong(), file)
            if (read == 0UL) break
            sb.append(buffer.decodeToString(0, read.toInt()))
        }
        return sb.toString()
    } finally {
        fclose(file)
    }
}