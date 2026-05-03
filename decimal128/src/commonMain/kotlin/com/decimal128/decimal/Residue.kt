// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal expect value class Residue internal constructor(val value:Int) {

    companion object {
        val EXACT: Residue
        val LT_HALF: Residue
        val HALF: Residue
        val GT_HALF: Residue

        operator fun invoke(res: Int): Residue

    }

}

private const val DIGIT_MAP = 0b11_11_11_11_10_01_01_01_01_00

internal inline fun Residue.Companion.fromDecimalDigit(digit: Int): Residue = Residue((DIGIT_MAP shr (digit shl 1)) and 0x03)

// FIXME - this method is fine, but it needs a better name
//  ... and perhaps a better implementation
internal fun Residue.Companion.fromValueDecade(c:C256): Residue {
    val digitLen = c.digitLen
    if (digitLen == 0)
        return EXACT
    val c0 = c.dw0
    val c1 = c.dw1
    val cmp = when {
        digitLen < MIN_POW10_DIGIT_LEN_128 -> compareWithHalfPow10_1(c0, digitLen)
        digitLen < MIN_POW10_DIGIT_LEN_192 -> compareWithHalfPow10_2(c1, c0, digitLen)
        digitLen < MIN_POW10_DIGIT_LEN_256 -> compareWithHalfPow10_3(c.dw2, c1, c0, digitLen)
        else -> compareWithHalfPow10_4(c.dw3, c.dw2, c1, c0, digitLen)
    }
    val residueValue = (cmp + 2) and 0x03
    val residue = Residue(residueValue)
    return residue
}

internal fun Residue.Companion.fromValueDecade(x: Decimal): Residue {
    val digitLen = stealDigitLen(x.steal)
    if (digitLen == 0)
        return EXACT
    val x0 = x.dw0
    val x1 = x.dw1
    val cmp = when {
        digitLen < MIN_POW10_DIGIT_LEN_128 -> compareWithHalfPow10_1(x0, digitLen)
        else -> compareWithHalfPow10_2(x1, x0, digitLen)
    }
    val residueValue = (cmp + 2) and 0x03
    val residue = Residue(residueValue)
    return residue
}

internal fun Residue.Companion.fromValuePow10(dw1: Long, dw0: Long, pow10: Int): Residue {
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val dw1P = POW10[pow10Offset + 1]
    val dw0P = POW10[pow10Offset    ]
    val dw1H = dw1P ushr 1
    val dw0H = (dw1P shl 63) or (dw0P ushr 1)
    val cmp = ucmp128(dw1, dw0, dw1H, dw0H)
    return Residue(cmp + 2)
}


internal fun Residue.Companion.fromRoundBitStickyBitsStickyBits(isolatedRoundBit: Long, stickyBitsFracCompare: Int, stickyBitsPow2: Long) : Residue {
    val stickyBit = if (stickyBitsFracCompare >= 0 || stickyBitsPow2 != 0L) 1 else 0
    val roundBit = if (isolatedRoundBit == 0L) 0 else 1
    val residueValue = ((roundBit shl 1) or stickyBit) and 0x03
    val residueX = Residue(residueValue)
    val residueY =
        if (stickyBitsPow2 == 0L) {
            if (stickyBitsFracCompare < 0) {
                if (isolatedRoundBit == 0L) EXACT else HALF
            } else {
                if (isolatedRoundBit == 0L) LT_HALF else GT_HALF
            }
        } else {
            if (isolatedRoundBit == 0L) LT_HALF else GT_HALF
        }
    if (residueX != residueY)
        println("residueX:$residueX residueY:$residueY")
    verify { residueX == residueY }
    return residueX
}

internal fun Residue.Companion.fromRoundBitStickBit(roundBit: Int, stickyBit: Int) : Residue {
    verify { roundBit in 0..1 }
    verify { stickyBit in 0..1 }
    val residueValue = (roundBit shl 1) or stickyBit
    val residueX = Residue(residueValue)
    return residueX
}

internal fun Residue.Companion.fromRoundBitStickyBits(isolatedRoundBit: Long, stickyBits: Long) : Residue {
    val roundBit = ((isolatedRoundBit or -isolatedRoundBit) ushr 63).toInt()
    val stickyBit = ((stickyBits or -stickyBits) ushr 63).toInt()
    val residueValue = (roundBit shl 1) or stickyBit
    val residueX = Residue(residueValue)
    return residueX
}

internal fun Residue.Companion.fromRemainderDivisor(r: C256, d: C256): Residue {
    if (r.dw3 < 0L) {
        // high bit of residue is set
        // doubling is certainly larger
        return GT_HALF
    }
    val s3 = (r.dw3 shl 1) or (r.dw2 ushr -1)
    if (s3 != d.dw3) {
        val cmp = unsignedCmp(s3, d.dw3)
        return if (cmp < 0) LT_HALF else GT_HALF
    }
    val s2 = (r.dw2 shl 1) or (r.dw1 ushr -1)
    if (s2 != d.dw2) {
        val cmp = unsignedCmp(s2, d.dw2)
        return if (cmp < 0) LT_HALF else GT_HALF
    }
    val s1 = (r.dw1 shl 1) or (r.dw0 ushr -1)
    if (s1 != d.dw1) {
        val cmp = unsignedCmp(s1, d.dw1)
        return if (cmp < 0) LT_HALF else GT_HALF
    }
    val s0 = (r.dw0 shl 1)
    if (s0 != d.dw0) {
        val cmp = unsignedCmp(s0, d.dw0)
        return if (cmp < 0) LT_HALF else GT_HALF
    }
    return EXACT
}

internal fun Residue.Companion.fromRemainderDivisor(remainder: Long, divisor: Long): Residue {
    val residue = when {
        remainder == 0L -> EXACT
        remainder < 0L -> GT_HALF // hi bit set .. so doubling would be 65 bits ... GT y0
        unsignedLT(2 * remainder, divisor) -> LT_HALF // we are doubling the remainder here
        unsignedCmp(2 * remainder, divisor) > 0 -> GT_HALF
        else -> HALF
    }
    return residue
}

internal fun Residue.Companion.residueFromRemainderPow10(remainder: Long, pow10: Int): Residue {
    val nonZeroMask = ((remainder or -remainder) shr 63).toInt()
    val pow10div2 = pow10_64(pow10) ushr 1
    val cmp = unsignedCmp(remainder, pow10div2)
    val index = ((cmp + 2) and nonZeroMask) and 0x03
    val residue = Residue(index)
    return residue
}


internal fun Residue.ulpRoundUp(roundingDirection: RoundingDirection, isOdd: Long) : Boolean =
    ulpBias(roundingDirection, isOdd) != 0L

internal fun Residue.ulpRoundUp01L(roundingDirection: RoundingDirection, isOdd: Long) : Long =
    -ulpBias(roundingDirection, isOdd) ushr 63

internal fun Residue.ulpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
    val ULP_BIAS_MAP = 0b0_00000000_00001110_00000000_00001100_00001000L

    val biasMapEvenOdd = ULP_BIAS_MAP or ((lsdwIsOdd and 1) shl 2)
    val bitIndex = (roundingDirection.value shl 3) + value
    val roundingMapShifted = biasMapEvenOdd shr bitIndex
    val bias = roundingMapShifted and 1
    return bias
}

/*
            previous
            exact lt_half half    gt_half
exact       exact lt_half lt_half lt_half
lt_half     lt_half lt_half lt_half lt_half
half        half, gt_half, gt_half, gtHalf
gt_half     gt_half gt_half gt_half gt_half

 */

/**
 * Merges previous residue with new stickResidue in left-to-right fashion ...
 * as though parsing digits left to right.
 */
internal fun Residue.merge(stickyResidue: Residue): Residue {
    /*
    val mergedResidue = when (this.value) {
        EXACT.value -> if (stickyResidue.value == EXACT.value) EXACT else LT_HALF
        LT_HALF.value -> LT_HALF
        HALF.value -> if (stickyResidue.value == EXACT.value) HALF else GT_HALF
        GT_HALF.value -> GT_HALF

        else -> throw RuntimeException("unrecognized Residue.value")
    }
     */
    val s = (stickyResidue.value and 1) or (stickyResidue.value ushr 1)
    val r = (this.value or s) and 0x03
    val mergedResidue = Residue(r)
    return mergedResidue
}


internal fun Residue.subtractionInverse(): Residue {
    // Subtraction inverse map, packed as nybbles into an Int.
    //   0 (EXACT)   -> 0 (EXACT)
    //   1 (LT_HALF) -> 3 (GT_HALF)
    //   2 (HALF)    -> 2 (HALF)
    //   3 (GT_HALF) -> 1 (LT_HALF)
    //
    // Nybble layout (low nybble = index 0):
    //   nybble[0]=0, nybble[1]=3, nybble[2]=2, nybble[3]=1
    //   = 0x1230
    val INVERSE_MAP = 0x1230
    val shift = value shl 2  // value * 4
    val result = (INVERSE_MAP shr shift) and 0x3
    return Residue(result)
}

private val STRING_NAMES = arrayOf("EXACT", "LT_HALF", "HALF", "GT_HALF")

internal fun Residue.toDebugString(): String =
    if (this.value in STRING_NAMES.indices) STRING_NAMES[this.value] else "invalid Residue:$value"

