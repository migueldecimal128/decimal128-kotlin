@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal const val FNZ_FNZ = 0b0000
internal const val FNZ_INF = 0b0001
internal const val FNZ_ZER = 0b0010
internal const val FNZ_NAN = 0b0011

internal const val INF_FNZ = 0b0100
internal const val INF_INF = 0b0101
internal const val INF_ZER = 0b0110
internal const val INF_NAN = 0b0111

internal const val ZER_FNZ = 0b1000
internal const val ZER_INF = 0b1001
internal const val ZER_ZER = 0b1010
internal const val ZER_NAN = 0b1011

internal const val NAN_FNZ = 0b1100
internal const val NAN_INF = 0b1101
internal const val NAN_ZER = 0b1110
internal const val NAN_NAN = 0b1111

internal inline fun binopSignatureOf(stealX: Int, stealY: Int): Int =
    ((stealX shl 2) or (stealY and 0x03)) and 0x0F

/*
    //typical example

    val xSteal = x.steal
    val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    when (signature) {
        FNZ_FNZ,
        FNZ_INF,
        FNZ_ZER,

        INF_FNZ,
        INF_INF,
        INF_ZER,

        ZER_FNZ,
        ZER_INF,
        ZER_ZER -> TODO()

        else -> nanOperandFound(x, y)
    }
 */