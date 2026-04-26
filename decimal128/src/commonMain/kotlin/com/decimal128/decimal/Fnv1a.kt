package com.decimal128.decimal

private const val FNV_OFFSET_BASIS = -0x7ee3ad4b // 2166136261 as signed Int
private const val FNV_PRIME = 16777619

internal fun fnv1aHash(bytes: ByteArray): Int {
    var hash = FNV_OFFSET_BASIS
    for (b in bytes) {
        hash = hash xor (b.toInt() and 0xFF)
        hash *= FNV_PRIME
    }
    return hash
}

internal fun fnv1aHash(longs: LongArray): Int {
    var hash = FNV_OFFSET_BASIS
    for (value in longs) {
        // Process the 8 bytes within the Long
        for (i in 0..7) {
            val byte = ((value shr (i * 8)) and 0xFF).toInt()
            hash = hash xor byte
            hash *= FNV_PRIME
        }
    }
    return hash
}

