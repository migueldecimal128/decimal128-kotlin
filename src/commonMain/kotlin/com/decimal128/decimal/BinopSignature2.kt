@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal const val xFNZ_FNZ = 0b0000
internal const val xFNZ_INF = 0b0001
internal const val xFNZ_ZER = 0b0010
internal const val xFNZ_NAN = 0b0011

internal const val xINF_FNZ = 0b0100
internal const val xINF_INF = 0b0101
internal const val xINF_ZER = 0b0110
internal const val xINF_NAN = 0b0111

internal const val xZER_FNZ = 0b1000
internal const val xZER_INF = 0b1001
internal const val xZER_ZER = 0b1010
internal const val xZER_NAN = 0b1011

internal const val xNAN_FNZ = 0b1100
internal const val xNAN_INF = 0b1101
internal const val xNAN_ZER = 0b1110
internal const val xNAN_NAN = 0b1111

internal inline fun signatureOf(stealX: Int, stealY: Int): Int =
    ((stealX shl 2) or (stealY and 0x03)) and 0x0F

