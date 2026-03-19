@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.max



internal inline fun pow10BitLen(pow10: Int): Int {
    return (POW10_BITLEN[pow10 and POW10_BITLEN_BCE].toInt() and 0xFF)
}

internal fun pow10Offset(pow10: Int): Int {
    val isHiTierMask = (MIN_POW10_DIGIT_LEN_192 - 1 - pow10) shr 31
    val loTier = pow10 shl 1
    val hiTierDelta = (POW10_192_BASE - 4 * MIN_POW10_DIGIT_LEN_192) + loTier
    return loTier + (hiTierDelta and isHiTierMask)
        /*
    val isHiMask = (MIN_POW10_DIGIT_LEN_192 - 1 - pow10) shr 31
    val base = (POW10_192_BASE - 4 * MIN_POW10_DIGIT_LEN_192) and isHiMask
    val shift = 1 - isHiMask
    return 0xFF and (base + (pow10 shl shift))
     */
    /*
    return (0xFF) and // hint to the JIT ... actually POW10_DWORD_COUNT == 234
            if (pow10 < MIN_POW10_DIGIT_LEN_192) pow10 shl 1
            else (POW10_192_BASE - 4 * MIN_POW10_DIGIT_LEN_192) + (pow10 shl 2)
     */
}

internal inline fun pow10_64(pow10: Int): Long {
    return POW10[(pow10 shl 1) and POW10_BCE]
}

internal inline fun pow10_128_dw0(pow10: Int): Long {
    return POW10[(pow10 shl 1) and POW10_BCE]
}

internal inline fun pow10_128_dw1(pow10: Int): Long {
    return POW10[((pow10 shl 1) + 1) and POW10_BCE]
}

internal inline fun pow10_128(pow10: Int): Pair<Long, Long> {
    verify { pow10 < MIN_POW10_DIGIT_LEN_192 }
    val offset = (2*pow10) and POW10_BCE
    return POW10[offset + 1] to POW10[offset]
}


internal fun calcMinDigitLenForBitLen(bitLen: Int): Int {
    return (((bitLen - 1) * 1233) ushr 12) + 1
}

internal fun calcMaxDigitLenForBitLen(bitLen: Int): Int {
    return ((bitLen * 19729) ushr 16) + 1
}

/**
 * Calcs the number of decimal digits in the unsigned long.
 * note that the value 0L returns 0
 */

internal inline fun calcDigitLen64(dw0: Long): Int {
    val bitLen = 64 - dw0.countLeadingZeroBits()
    return calcDigitLen64(bitLen, dw0)
}

internal inline fun calcDigitLen64(bitLen: Int, dw0: Long): Int {
    // this formula of
    // ((bitLen * 1233) ushr 12)
    // usually underestimates by 1
    // it is simpler than (((bitLen - 1) * 1233) ushr 12) + 1
    // more importantly, it avoids boundary condition issues
    // for 128 and 192 bits where they cross from 2->4 limbs
    val loDigitCount = (bitLen * 1233) ushr 12
    val p0 = POW10[(loDigitCount shl 1) and POW10_BCE]
    return loDigitCount + 1 - (unsignedCmp(dw0, p0) ushr 31)
}

internal fun calcDigitLen128(dw1: Long, dw0: Long): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    return calcDigitLen128(bitLen, dw1, dw0)
}

internal fun calcDigitLen128(bitLen: Int, dw1: Long, dw0: Long): Int {
    val loDigitCount = (bitLen * 1233) ushr 12
    val hiDigitCount = loDigitCount + 1
    val pow10Offset = (loDigitCount shl 1) and POW10_BCE
    val p1 = POW10[pow10Offset + 1]
    val p0 = POW10[pow10Offset    ]
    val cmp1 = unsignedCmp(dw1, p1)
    if (cmp1 != 0)
        return hiDigitCount - (cmp1 ushr 31)
    val cmp0 = unsignedCmp(dw0, p0)
    return hiDigitCount - (cmp0 ushr 31)
}

internal fun calcStealPackedLengths128(dw1: Long, dw0: Long): Int {
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz1 = dw1.countLeadingZeroBits()
    val nlz0 = dw0.countLeadingZeroBits()
    val bitLen = 128 - nlz1 - (nlz0 and dw1IsZeroMask)

    val loDigitCount = (bitLen * 1233) ushr 12
    val hiDigitCount = loDigitCount + 1
    val pow10Offset = (loDigitCount shl 1) and POW10_BCE
    val p1 = POW10[pow10Offset + 1]
    val p0 = POW10[pow10Offset    ]
    val cmp1 = unsignedCmp(dw1, p1)
    val digitLen = hiDigitCount - ((if (cmp1 != 0) cmp1 else unsignedCmp(dw0, p0)) ushr 31)

    return (digitLen shl STEAL_DIGITLEN_SHIFT) or (bitLen shl STEAL_BITLEN_SHIFT)
}

internal fun calcDigitLen192(bitLen: Int, dw2: Long, dw1: Long, dw0: Long): Int {
    if (bitLen > 128) {
        val loDigitCount = max((bitLen * 1233) ushr 12, MIN_POW10_DIGIT_LEN_192)
        val pow10BitLen = pow10BitLen(loDigitCount)
        val bitLenDelta = pow10BitLen - bitLen
        if (bitLenDelta != 0) {
            return loDigitCount + (bitLenDelta ushr 31)
        }
        val pow10Offset = pow10Offset(loDigitCount) and POW10_BCE
        val p2 = POW10[pow10Offset + 2]
        val p1 = POW10[pow10Offset + 1]
        val p0 = POW10[pow10Offset    ]
        val hiDigitCount = loDigitCount + 1

        val cmp2 = unsignedCmp(dw2, p2)
        if (cmp2 != 0)
            return hiDigitCount - (cmp2 ushr 31)
        val cmp1 = unsignedCmp(dw1, p1)
        if (cmp1 != 0)
            return hiDigitCount - (cmp1 ushr 31)
        val cmp0 = unsignedCmp(dw0, p0)
        return hiDigitCount - (cmp0 ushr 31)
    } else {
        return calcDigitLen128(bitLen, dw1, dw0)
    }
}

internal fun calcDigitLen256(bitLen: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): Int {
    if (bitLen > 128) {
        val loDigitCount = max((bitLen * 1233) ushr 12, MIN_POW10_DIGIT_LEN_192)
        val pow10BitLen = pow10BitLen(loDigitCount)
        val bitLenDelta = pow10BitLen - bitLen
        if (bitLenDelta != 0) {
            return loDigitCount + (bitLenDelta ushr 31)
        }

        val pow10Offset = pow10Offset(loDigitCount) and POW10_BCE
        val p3 = POW10[pow10Offset + 3]
        val p2 = POW10[pow10Offset + 2]
        val p1 = POW10[pow10Offset + 1]
        val p0 = POW10[pow10Offset    ]
        val hiDigitCount = loDigitCount + 1
        val cmp3 = unsignedCmp(dw3, p3)
        if (cmp3 != 0)
            return hiDigitCount - (cmp3 ushr 31)
        val cmp2 = unsignedCmp(dw2, p2)
        if (cmp2 != 0)
            return hiDigitCount - (cmp2 ushr 31)
        val cmp1 = unsignedCmp(dw1, p1)
        if (cmp1 != 0)
            return hiDigitCount - (cmp1 ushr 31)
        val cmp0 = unsignedCmp(dw0, p0)
        return hiDigitCount - (cmp0 ushr 31)
    } else {
        return calcDigitLen128(bitLen, dw1, dw0)
    }
}

internal fun compareWithHalfPow10_1(dw0: Long, pow10: Int): Int {
    verify { pow10 >= 0 && pow10 < MIN_POW10_DIGIT_LEN_128 }
    val pow10Dw0 = pow10_64(pow10)
    val halfPow10Dw0 = pow10Dw0 ushr 1
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    return cmp0
}

internal fun compareWithHalfPow10_2(dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= 0 && pow10 < MIN_POW10_DIGIT_LEN_192 }
    val pow10Dw1 = pow10_128_dw1(pow10)
    val halfPow10Dw1 = pow10Dw1 ushr 1
    val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
    if (cmp1 != 0)
        return cmp1
    val pow10Dw0 = pow10_128_dw0(pow10)
    val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    return cmp0
}

internal fun compareWithHalfPow10_3(dw2: Long, dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= MIN_POW10_DIGIT_LEN_192 && pow10 < MIN_POW10_DIGIT_LEN_256 }
    val pow10Offset = pow10Offset(pow10) and POW10_BCE
    val pow10Dw0 = POW10[pow10Offset    ]
    val pow10Dw1 = POW10[pow10Offset + 1]
    val pow10Dw2 = POW10[pow10Offset + 2]
    val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
    val halfPow10Dw1 = (pow10Dw2 shl -1) or (pow10Dw1 ushr 1)
    val halfPow10Dw2 = pow10Dw2 ushr 1
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
    val cmp2 = unsignedCmp(dw2, halfPow10Dw2)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    val cmp210 = if (cmp2 != 0) cmp2 else cmp10
    return cmp210
}

internal fun compareWithHalfPow10_4(dw3: Long, dw2: Long, dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= MIN_POW10_DIGIT_LEN_256 && pow10 <= MAXX_DIGIT_LEN }
    if (pow10 < MAXX_DIGIT_LEN) {
        val pow10Offset = pow10Offset(pow10) and POW10_BCE
        val pow10Dw0 = POW10[pow10Offset    ]
        val pow10Dw1 = POW10[pow10Offset + 1]
        val pow10Dw2 = POW10[pow10Offset + 2]
        val pow10Dw3 = POW10[pow10Offset + 3]
        val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
        val halfPow10Dw1 = (pow10Dw2 shl -1) or (pow10Dw1 ushr 1)
        val halfPow10Dw2 = (pow10Dw3 shl -1) or (pow10Dw2 ushr 1)
        val halfPow10Dw3 = pow10Dw3 ushr 1
        val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
        val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
        val cmp2 = unsignedCmp(dw2, halfPow10Dw2)
        val cmp3 = unsignedCmp(dw3, halfPow10Dw3)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        val cmp32 = if (cmp3 != 0) cmp3 else cmp2
        val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
        return cmp3210
    } else {
        // when there are 78 digits then we are always LT_HALF
        return -1
    }
}

internal fun c256IsPowerOf10(x: C256): Boolean {
    val xBitLen = x.bitLen
    val xDigitLen = x.digitLen
    if (xDigitLen > 0) {
        val pow10Offset = pow10Offset(xDigitLen - 1) and POW10_BCE
        val p0 = POW10[pow10Offset    ]
        val p1 = POW10[pow10Offset + 1] and ((64 - xBitLen) shr 31).toLong()
        val p2 = POW10[pow10Offset + 2] and ((128 - xBitLen) shr 31).toLong()
        val p3 = POW10[pow10Offset + 3] and ((192 - xBitLen) shr 31).toLong()
        return (p0 == x.dw0) and (p1 == x.dw1) and (p2 == x.dw2) and (p3 == x.dw3)
    }
    return false
}

internal fun c256SetPow10(z: C256, pow10: Int) {
    if (pow10 >= 0) {
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10) and POW10_BCE
        val p0 = POW10[pow10Offset    ]
        val p1 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p2 = POW10[pow10Offset + 2] and ((128 - pow10BitLen) shr 31).toLong()
        val p3 = POW10[pow10Offset + 3] and ((192 - pow10BitLen) shr 31).toLong()
        z.c256Set256(p3, p2, p1, p0)
    } else {
        z.c256SetZero()
    }
}







