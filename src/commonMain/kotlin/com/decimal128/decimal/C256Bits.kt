@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal inline fun calcBitLen64(dw0: Long): Int {
    val nlz0 = dw0.countLeadingZeroBits()
    val bitLen = 64 - nlz0
    return bitLen
}

internal inline fun calcBitLen128(dw1: Long, dw0: Long): Int {
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz1 = dw1.countLeadingZeroBits()
    val nlz0 = dw0.countLeadingZeroBits()
    val bitLen = 128 - nlz1 - (nlz0 and dw1IsZeroMask)
    return bitLen
}

internal inline fun calcBitLen192(dw2: Long, dw1: Long, dw0: Long): Int {
    val dw2IsZeroMask = ((dw2 or -dw2) shr 63).inv().toInt()
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz2 = dw2.countLeadingZeroBits()
    val nlz1 = dw1.countLeadingZeroBits()
    val nlz0 = dw0.countLeadingZeroBits()
    val nlz10 = nlz1 + (nlz0 and dw1IsZeroMask)
    val bitLen = 192 - nlz2 - (nlz10 and dw2IsZeroMask)
    return bitLen
}

internal fun calcBitLen256(dw3: Long, dw2: Long, dw1: Long, dw0: Long): Int {
    val dw3IsZeroMask = ((dw3 or -dw3) shr 63).inv().toInt()
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val dw23 = dw2 or dw3
    val dw23IsZeroMask = ((dw23 or -dw23) shr 63).inv().toInt()

    val nlz3 = dw3.countLeadingZeroBits()
    val nlz2 = dw2.countLeadingZeroBits()
    val nlz1 = dw1.countLeadingZeroBits()
    val nlz0 = dw0.countLeadingZeroBits()
    val nlz23 = nlz3 + (nlz2 and dw3IsZeroMask)
    val nlz10 = nlz1 + (nlz0 and dw1IsZeroMask)
    val bitLen = 256 - nlz23 - (nlz10 and dw23IsZeroMask)
    return bitLen
}

private const val MASK_BITS_0_MOD_4 = 0x1111111111111111L
private const val MASK_BITS_1_MOD_4 = MASK_BITS_0_MOD_4 shl 1
private const val MASK_BITS_2_MOD_4 = MASK_BITS_0_MOD_4 shl 2
private const val MASK_BITS_3_MOD_4 = MASK_BITS_0_MOD_4 shl 3

internal fun c256Ctz(x: C256): Int {
    val ntz0 = x.dw0.countTrailingZeroBits()
    val ntz1 = 64 + x.dw1.countTrailingZeroBits()
    val ntz2 = 128 + x.dw2.countTrailingZeroBits()
    val ntz3 = 192 + x.dw3.countTrailingZeroBits()
    val ntz01 = if (x.dw0 != 0L) ntz0 else ntz1
    val ntz23 = if (x.dw2 != 0L) ntz2 else ntz3
    val ntz0123 = if ((x.dw0 or x.dw1) != 0L) ntz01 else ntz23
    return ntz0123
}

internal fun getDwordAtBitIndex(x: C256, bitIndex: Int): Long {
    val dwordShift = bitIndex ushr 6
    val innerShift = bitIndex and 0x3F
    val nonZeroMask = (-innerShift shr 31).toLong()
    return when (dwordShift) {
        0 -> (nonZeroMask and (x.dw1 shl -innerShift)) or (x.dw0 ushr innerShift)
        1 -> (nonZeroMask and (x.dw2 shl -innerShift)) or (x.dw1 ushr innerShift)
        2 -> (nonZeroMask and (x.dw3 shl -innerShift)) or (x.dw2 ushr innerShift)
        3 -> (x.dw3 ushr innerShift)
        else -> 0L
    }
}

internal fun isMultipleOfFive64(dw0: Long): Boolean {
    val m0 = MASK_BITS_0_MOD_4
    val m1 = MASK_BITS_1_MOD_4
    val m2 = MASK_BITS_2_MOD_4
    val m3 = MASK_BITS_3_MOD_4

    val count0 = (dw0 and m0).countOneBits()
    val count1 = (dw0 and m1).countOneBits()
    val count2 = (dw0 and m2).countOneBits()
    val count3 = (dw0 and m3).countOneBits()

    val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
    val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
    return ret
}

internal fun isMultipleOfFive128(dw1: Long, dw0: Long): Boolean {
    val m0 = MASK_BITS_0_MOD_4
    val m1 = MASK_BITS_1_MOD_4
    val m2 = MASK_BITS_2_MOD_4
    val m3 = MASK_BITS_3_MOD_4

    val count0 = (dw1 and m0).countOneBits() + (dw0 and m0).countOneBits()
    val count1 = (dw1 and m1).countOneBits() + (dw0 and m1).countOneBits()
    val count2 = (dw1 and m2).countOneBits() + (dw0 and m2).countOneBits()
    val count3 = (dw1 and m3).countOneBits() + (dw0 and m3).countOneBits()

    val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
    val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
    return ret
}

internal fun isMultipleOfFive192(dw2: Long, dw1: Long, dw0: Long): Boolean {
    val m0 = MASK_BITS_0_MOD_4
    val m1 = MASK_BITS_1_MOD_4
    val m2 = MASK_BITS_2_MOD_4
    val m3 = MASK_BITS_3_MOD_4

    val count0 = (dw2 and m0).countOneBits() + (dw1 and m0).countOneBits() + (dw0 and m0).countOneBits()
    val count1 = (dw2 and m1).countOneBits() + (dw1 and m1).countOneBits() + (dw0 and m1).countOneBits()
    val count2 = (dw2 and m2).countOneBits() + (dw1 and m2).countOneBits() + (dw0 and m2).countOneBits()
    val count3 = (dw2 and m3).countOneBits() + (dw1 and m3).countOneBits() + (dw0 and m3).countOneBits()

    val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
    val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
    return ret
}

internal fun isMultipleOfFive256(dw3: Long, dw2: Long, dw1: Long, dw0: Long): Boolean {
    val m0 = MASK_BITS_0_MOD_4
    val m1 = MASK_BITS_1_MOD_4
    val m2 = MASK_BITS_2_MOD_4
    val m3 = MASK_BITS_3_MOD_4

    val count0 = (dw3 and m0).countOneBits() + (dw2 and m0).countOneBits() +
            (dw1 and m0).countOneBits() + (dw0 and m0).countOneBits()
    val count1 = (dw3 and m1).countOneBits() + (dw2 and m1).countOneBits() +
            (dw1 and m1).countOneBits() + (dw0 and m1).countOneBits()
    val count2 = (dw3 and m2).countOneBits() + (dw2 and m2).countOneBits() +
            (dw1 and m2).countOneBits() + (dw0 and m2).countOneBits()
    val count3 = (dw3 and m3).countOneBits() + (dw2 and m3).countOneBits() +
            (dw1 and m3).countOneBits() + (dw0 and m3).countOneBits()

    val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
    val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
    return ret
}

internal fun c256IsMultipleOf10(x: C256): Boolean {
    if (x.bitLen < 4 || (x.dw0 and 1L) != 0L)
        return false
    return c256IsMultipleOf5(x)
}

internal fun c256IsMultipleOf5(x: C256): Boolean {
    val bitLen = x.bitLen
    return when {
        bitLen <= 64 -> isMultipleOfFive64(x.dw0)
        bitLen <= 128 -> isMultipleOfFive128(x.dw1, x.dw0)
        bitLen <= 192 -> isMultipleOfFive192(x.dw2, x.dw1, x.dw0)
        else -> isMultipleOfFive256(x.dw3, x.dw2, x.dw1, x.dw0)
    }
}

internal fun c256SetPow2(z: C256, pow2: Int) {
    if (pow2 !in 0..255)
        throw IllegalArgumentException()
    val shifted = 1L shl pow2
    val i = pow2 ushr 6
    val j = 1L shl (60 + i)
    z.dw0 = shifted and ((j shl 3) shr 63)
    z.dw1 = shifted and ((j shl 2) shr 63)
    z.dw2 = shifted and ((j shl 1) shr 63)
    z.dw3 = shifted and ((j) shr 63)
    val bitLen = pow2 + 1
    val digitLen = ((pow2 * 1233) ushr 12) + 1
    z.updateDigitLenBitLen(digitLen, bitLen)
}

internal fun c256ToFloorDouble(x: C256): Double {
    val hiBitLen = min(53, x.bitLen)
    val hiBitIndex = x.bitLen - hiBitLen
    val hiBits = getDwordAtBitIndex(x, hiBitIndex)
    val dHiBits = scalb(hiBits.toDouble(), hiBitIndex)
    return dHiBits
}

internal fun c256Set(z: C256, d: Double) {
    val dRaw = d.toRawBits()
    val exp = ((dRaw ushr 52).toInt() and 0x7FF) - 1023
    if (exp <= 63) {
        z.c256Set64(Math.abs(d).toLong())
        return
    }
    if (exp > 255) {
        throw RuntimeException("coefficient overflow")
    }
    val significand = ((dRaw and ((1L shl 52) - 1)) or (1L shl 52))
    z.c256Set64(significand)
    c256SetShiftLeft(z, z, exp - 52)
}

internal fun c256ToNewDoubleDouble(x: C256): DoubleDouble {
    val hiBitsLen = min(53, x.bitLen)
    val hiBitsIndex = x.bitLen - hiBitsLen
    val hiBits = getDwordAtBitIndex(x, hiBitsIndex)
    val dHiBits = scalb(hiBits.toDouble(), hiBitsIndex)
    if (hiBitsIndex == 0)
        return DoubleDouble(dHiBits, 0.0)
    var loBits64Index: Int = max(0, hiBitsIndex - 64)
    var loBitsMask = -1L ushr max(0, 64 - hiBitsIndex)
    var loBits: Long
    var nlz: Int
    while (true) {
        loBits = getDwordAtBitIndex(x, loBits64Index) and loBitsMask
        nlz = loBits.countLeadingZeroBits()
        if (loBits64Index == 0 || nlz <= 11)
            break
        loBits64Index = max(loBits64Index - nlz, 0)
        loBitsMask = -1
    }
    val extraBits = max(0, 11 - nlz)
    loBits = loBits ushr extraBits
    val loBits53Index = loBits64Index + extraBits
    val dLoBits = scalb(loBits.toDouble(), loBits53Index)
    return DoubleDouble(dHiBits, dLoBits)
}

internal fun c256Set(z: C256, dd: DoubleDouble, pentad: Pentad) {
    c256Set(z, dd.hi)
    if (dd.lo == 0.0)
        return
    val uLo = C256()
    uLo.c256Set(dd.lo)
    if (dd.lo > 0)
        c256SetAddUnscaled(z, z, uLo, pentad)
    else
        c256SetSubUnscaled(z, z, uLo)
}
