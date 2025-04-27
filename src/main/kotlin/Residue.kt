package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.CoeffDigitCount.POW10

import java.lang.Long.compareUnsigned


//@JvmInline
/*value*/ class Residue private constructor(val value:Int) {

    // comment this out when we turn Residue into a value class
    override fun equals(other: Any?): Boolean {
        return other is Residue && this.value == other.value
    }

    fun toggleNegate() = RESIDUE_MAP[value xor 0x04]
    fun withoutNegate() = RESIDUE_MAP[value and 0x03]
    fun isNegated() = (value and 0x04) != 0

    companion object {
        val EXACT = Residue(0)
        val LT_HALF = Residue(1)
        val HALF = Residue(2)
        val GT_HALF = Residue(3)

        val EXACT_NEGATED = Residue(4)
        val LT_HALF_NEGATED = Residue(5)
        val HALF_NEGATED = Residue(6)
        val GT_HALF_NEGATED = Residue(7)


        val RESIDUE_MAP = arrayOf(EXACT, LT_HALF, HALF, GT_HALF, EXACT_NEGATED, LT_HALF_NEGATED, HALF_NEGATED, GT_HALF_NEGATED)

        val STRING_NAMES = arrayOf("EXACT", "LT_HALF", "HALF", "GT_HALF",
            "EXACT_NEGATED", "LT_HALF_NEGATED", "HALF_NEGATED", "GT_HALF_NEGATED")

        fun residueFrom(c:Coeff) :Residue {
            return (
                    if (( c.dw3 or c.dw2) == 0L) {
                        if (c.dw1 == 0L)
                            residueFrom(c.digitCount, c.dw0)
                        else
                            residueFrom(c.digitCount, c.dw1, c.dw0)
                    } else {
                        if (c.dw3 == 0L)
                            residueFrom(c.digitCount, c.dw2, c.dw1, c.dw0)
                        else
                            residueFrom(c.digitCount, c.dw3, c.dw2, c.dw1, c.dw0)
                    }
                    )

        }

        private fun residueFrom(digitCount:Int, dw0: Long) : Residue {
            if (dw0 == 0L) {
                assert(digitCount == 0)
                return EXACT
            } else if (dw0 > 0) {
                val dw0x2 = dw0 shl 1
                val ten0 = POW10[digitCount]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                val residue = if (cmp0 < 0) LT_HALF else if (cmp0 == 0) HALF else GT_HALF
                return residue
            } else {
                //10**19 has the msb set
                //so if we have our msb set then we compare with 1E19
                val oneE19 = POW10[19]
                val cmp0 = compareUnsigned(dw0, oneE19)
                val residue = if (cmp0 < 0) GT_HALF else LT_HALF
                return residue
            }
        }

        private fun residueFrom(digitCount: Int, dw1: Long, dw0: Long) : Residue {
            // 10**39 takes 3 dwords with most significant dword having value 0x02
            if (digitCount == POW10_192_OFFSET)
                return LT_HALF
            assert (dw1 >= 0)
            //if (dw1 >= 0) {
                val index = 2 * (digitCount - POW10_128_OFFSET) + POW10_128_DWORD_INDEX

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            //} else {
                // 10**39 takes 3 dwords with most significant dword having value 0x02
                // therefore, half of that, 5E38, has a 1 in the most significant dword
                // therefore, all 2 dword values with msb set are LT_HALF
            //    return LT_HALF
            //}
        }

        private fun residueFrom(digitCount: Int, dw2: Long, dw1: Long, dw0: Long) : Residue {
            if (dw2 >= 0) {
                val index = 3 * (digitCount - POW10_192_OFFSET) + POW10_192_DWORD_INDEX

                val dw2x2 = (dw2 shl 1) or (dw1 ushr 63)
                val ten2 = POW10[index + 2]
                val cmp2 = compareUnsigned(dw2x2, ten2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            } else {
                assert(digitCount == 58)
                // this case is a problem child because (10**58) has 193 bits (4 dwords)
                // but 10**58/2 has 192 bits == 3 dwords

                // 0xE400000000000000, 0x37E9F14D3EEC8920, 0x97D4DF19D6057673, 0x0000000000000001, // (10**58)
                val fiveE57dw2 = 0x97D4DF19D6057673uL.toLong() shr 1 // shr to replicate msb down from dw3
                val fiveE57dw1 = (0x37E9F14D3EEC8920 shr 1) or Long.MIN_VALUE
                val fiveE57dw0 = (0xE400000000000000uL.toLong()) ushr 1 //

                val cmp2 = compareUnsigned(dw2, fiveE57dw2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val cmp1 = compareUnsigned(dw1, fiveE57dw1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val cmp0 = compareUnsigned(dw0, fiveE57dw0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            }
        }

        private fun residueFrom(digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : Residue {
            if (dw3 >= 0) {
                if (digitCount == 58)
                    return GT_HALF
                val index = 4 * (digitCount - POW10_256_OFFSET) + POW10_256_DWORD_INDEX

                val dw3x2 = (dw3 shl 1) or (dw2 ushr 63)
                val ten3 = POW10[index + 3]
                val cmp3 = compareUnsigned(dw3x2, ten3)
                if (cmp3 < 0)
                    return LT_HALF
                if (cmp3 > 0)
                    return GT_HALF

                val dw2x2 = (dw2 shl 1) or (dw1 ushr 63)
                val ten2 = POW10[index + 2]
                val cmp2 = compareUnsigned(dw2x2, ten2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            } else {
                // 0x0000000000000000uL.toLong(), 0xAA987B6E6FD2A000uL.toLong(), 0x49EF0EB713F39EBEuL.toLong(),
                // 0xDD15FE86AFFAD912uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000000000 (10**77)

                val oneE77dw3 = 0xDD15FE86AFFAD912uL.toLong()
                val oneE77dw2 = 0x49EF0EB713F39EBEuL.toLong()
                val oneE77dw1 = 0xAA987B6E6FD2A000uL.toLong()
                val oneE77dw0 = 0L

                val cmp3 = compareUnsigned(dw3, oneE77dw3)
                if (cmp3 < 0)
                    return GT_HALF
                if (cmp3 > 0)
                    return LT_HALF

                val cmp2 = compareUnsigned(dw2, oneE77dw2)
                if (cmp2 < 0)
                    return GT_HALF
                if (cmp2 > 0)
                    return LT_HALF

                val cmp1 = compareUnsigned(dw1, oneE77dw1)
                if (cmp1 < 0)
                    return GT_HALF
                if (cmp1 > 0)
                    return LT_HALF


                //val cmp0 = compareUnsigned(dw0, oneE77dw0)
                //if (cmp0 < 0)
                //    return GT_HALF
                //if (cmp0 > 0)
                //    return LT_HALF
                if (dw0 != 0L)
                    LT_HALF // LF_HALF compared to 5e77 ... 78 digits ... which we do not support

                //
                return EXACT // exactly 1e77 ... 78 digits ... which we do not support
            }
        }

        fun residueFrom(isolatedRoundBit: Long, stickyBitsFracCompare: Int, stickyBitsPow2: Long) : Residue {
            val stickyBit = if (stickyBitsFracCompare >= 0 || stickyBitsPow2 != 0L) 1 else 0
            val roundBit = if (isolatedRoundBit == 0L) 0 else 1
            val residueValue = (roundBit shl 1) or stickyBit
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
            assert(residueX == residueY)
            return residueX
        }

    }

    @Deprecated("don't use this in the Coeff layer")
    fun ulpRoundUp(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Boolean =
        ulpBias(roundingDirection, lsdwIsOdd) != 0L

    @Deprecated("don't use this in the Coeff layer")
    fun ulpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long) = ulpBiasY(roundingDirection, lsdwIsOdd)

    @Deprecated("don't use this in the Coeff layer")
    fun ulpBiasY(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        val ULP_BIAS_MAP = 0b0_00000000_00001110_00000000_00001100_00001000L

        val biasMapEvenOdd = ULP_BIAS_MAP or ((lsdwIsOdd and 1) shl 2)
        val bitIndex = (roundingDirection.value * 8) + (value and 0x03) // mask off isNegated bit
        val roundingMapShifted = biasMapEvenOdd shr bitIndex
        val bias = roundingMapShifted and 1
        return bias
    }

    // used in add case when there is no overlap
    fun ulpBiasX(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        return when (roundingDirection) {
            ROUND_TIES_TO_EVEN -> when (value and 0x03) { // mask off isNegated bit
                LT_HALF.value -> 0L
                HALF.value -> lsdwIsOdd and 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TIES_TO_AWAY -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_ZERO -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_POSITIVE -> when (value) {
                LT_HALF.value -> 1L
                HALF.value -> 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            // ROUND_TOWARD_NEGATIVE
            else -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
        }
    }

    override fun toString() : String {
        return if (this.value in STRING_NAMES.indices) STRING_NAMES[this.value] else "invalid Residue:$value"
    }
}
