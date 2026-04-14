package com.decimal128.decimal

import com.sun.jna.Library
import com.sun.jna.Native

interface JnaBidDpdShim : Library {
    companion object {
        // "jna_bid_dpd_shim.dylib" => "JnaBidDpdShim" here
        val INSTANCE: JnaBidDpdShim =
            Native.load("jna_bid_dpd_shim", JnaBidDpdShim::class.java)
    }

    // decimal128 (16 bytes)
    fun d128_dpd_le_from_string(s: String, out16: ByteArray): Int
    fun d128_dpd_le_to_string(in16: ByteArray, out: ByteArray, outCap: Int): Int
    fun d128_bid_le_from_string(s: String, out16: ByteArray): Int
    fun d128_bid_le_to_string(in16: ByteArray, out: ByteArray, outCap: Int): Int

    // decimal64 (8 bytes)
    fun d64_dpd_le_from_string(s: String, out8: ByteArray): Int
    fun d64_dpd_le_to_string(in8: ByteArray, out: ByteArray, outCap: Int): Int
    fun d64_bid_le_from_string(s: String, out8: ByteArray): Int
    fun d64_bid_le_to_string(in8: ByteArray, out: ByteArray, outCap: Int): Int
}

// tiny helpers
fun ByteArray.spacedHex() = joinToString(" ") { "%02X".format(it.toUByte().toInt()) }
fun ByteArray.cString(): String {
    var end = 0
    while (end < size && this[end] != 0.toByte()) end++
    return String(this, 0, end, Charsets.UTF_8)
}

fun LongArray.spacedHex() = joinToString(" ") { "%016X".format(it) }
