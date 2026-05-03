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

        internal inline fun fromDecimalDigit(digit: Int): Residue

        fun fromValueDecade(c:C256): Residue

        fun fromValueDecade(x: Decimal): Residue

        fun fromValuePow10(dw1: Long, dw0: Long, pow10: Int): Residue

        fun fromRoundBitStickyBitsStickyBits(isolatedRoundBit: Long, stickyBitsFracCompare: Int, stickyBitsPow2: Long): Residue

        fun fromRoundBitStickBit(roundBit: Int, stickyBit: Int): Residue

        fun fromRoundBitStickyBits(isolatedRoundBit: Long, stickyBits: Long): Residue

        fun fromRemainderDivisor(r: C256, d: C256): Residue

        fun fromRemainderDivisor(remainder: Long, divisor: Long): Residue

        fun residueFromRemainderPow10(remainder: Long, pow10: Int): Residue
    }

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

