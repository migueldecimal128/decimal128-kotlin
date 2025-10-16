package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
internal inline fun packLengths(digitLen: Int, bitLen: Int) =
    ((digitLen shl 9) or bitLen).toShort()

@Suppress("NOTHING_TO_INLINE")
internal inline fun packSignExp(sign: Boolean, qExp: Int): Short = ((if (sign) 0x8000 else 0) or (qExp and 0x7FFF)).toShort()

@Suppress("NOTHING_TO_INLINE")
internal inline fun unpackBitLen(packedLengths: Short) = packedLengths.toInt() and 0x1FF

@Suppress("NOTHING_TO_INLINE")
internal inline fun unpackDigitLen(packedLengths: Short) = (packedLengths.toInt() shr 9) and 0x7F

@Suppress("NOTHING_TO_INLINE")
internal inline fun unpackSign(signExp: Short) = signExp < 0

@Suppress("NOTHING_TO_INLINE")
internal inline fun unpackExp(signExp: Short) = (signExp.toInt() shl 1) shr 1


@Suppress("NOTHING_TO_INLINE")
internal inline fun calcPackedLengths(dw0: Long): Short {
    val bitLen = calcBitLen64(dw0)
    val digitLen = U256Pow10.calcDigitLen64(bitLen, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun calcPackedLengths(dw1: Long, dw0: Long): Short {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = U256Pow10.calcDigitLen128(bitLen, dw1, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

