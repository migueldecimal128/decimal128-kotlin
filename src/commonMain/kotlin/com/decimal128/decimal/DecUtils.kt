@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal inline fun packLengths(digitLen: Int, bitLen: Int) =
    ((digitLen shl 9) or bitLen).toShort()

internal inline fun packSignExp(sign: Boolean, qExp: Int): Short = ((if (sign) 0x8000 else 0) or (qExp and 0x7FFF)).toShort()

internal inline fun unpackBitLen(packedLengths: Short) = packedLengths.toInt() and 0x1FF

internal inline fun unpackDigitLen(packedLengths: Short) = (packedLengths.toInt() shr 9) and 0x7F

internal inline fun unpackSign(signExp: Short) = signExp < 0

internal inline fun unpackExp(signExp: Short) = (signExp.toInt() shl 1) shr 1

internal inline fun calcPackedLengths(dw0: Long): Short {
    val bitLen = calcBitLen64(dw0)
    val digitLen = calcDigitLen64(bitLen, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun calcPackedLengths(dw1: Long, dw0: Long): Short {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}
