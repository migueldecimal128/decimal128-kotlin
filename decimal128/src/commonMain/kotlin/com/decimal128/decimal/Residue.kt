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

    fun ulpRoundUp(roundingDirection: RoundingDirection, lsdwIsOdd: Long): Boolean

    fun ulpRoundUp01L(roundingDirection: RoundingDirection, lsdwIsOdd: Long): Long

    fun ulpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long): Long

    fun ulpBiasY(roundingDirection: RoundingDirection, lsdwIsOdd: Long): Long

    // used in add case when there is no overlap
    fun ulpBiasX(roundingDirection: RoundingDirection, lsdwIsOdd: Long): Long

    fun merge(stickyResidue: Residue): Residue

    fun subtractionInverse(): Residue

}
