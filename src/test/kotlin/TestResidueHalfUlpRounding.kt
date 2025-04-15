package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.HALF
import com.decimal128.Residue.Companion.BIAS_TRUNC

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE



class TestResidueHalfUlpRounding {

    val verbose = false

    class HalfUlpRoundingCase(
        val roundingDirection: RoundingDirection, val residue: Residue, val lsdw: Long, val sign: Boolean,
        val expectedBias: Long) {
        override fun toString() : String {
            return "$roundingDirection $residue sign:$sign lsb:$lsdw expectedBias:$expectedBias"
        }
    }

    val roundingCases = arrayOf(

        // roundTiesToEven
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, HALF, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, HALF, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, HALF, 5, false, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, HALF, 7, true, 1),

        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 0, false, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 0, true, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 9, false, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 1, true, 1),

        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, EXACT, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, EXACT, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, EXACT, 1, false, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_EVEN, EXACT, 3, true, 0),

        // roundTiesToAway
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, HALF, 0, false, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, HALF, 0, true, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, HALF, 7, false, 1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, HALF, 9, true, 1),

        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 0, false,1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 0, true,1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 1, false,1),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 3, true, 1),

        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, EXACT, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, EXACT, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, EXACT, 3, false, 0),
        HalfUlpRoundingCase(ROUND_TIES_TO_AWAY, EXACT, 5, true, 0),

        // roundTowardZero
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, HALF, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, HALF, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, HALF, 9, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, HALF, 1, true, 0),

        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 3, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 5, true, 0),

        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, EXACT, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, EXACT, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, EXACT, 5, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_ZERO, EXACT, 7, true, 0),

        // roundTowardPositive
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, HALF, 0, false, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, HALF, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, HALF, 1, false, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, HALF, 3, true, 0),

        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 0, false, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 5, false, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 7, true, 0),

        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, EXACT, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, EXACT, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, EXACT, 7, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_POSITIVE, EXACT, 9, true, 0),

        // roundTowardNegative
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, HALF, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, HALF, 0, true, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, HALF, 3, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, HALF, 5, true, 2),

        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 0, true, 2),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 7, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 9, true, 2),

        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, true, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, EXACT, 9, false, 0),
        HalfUlpRoundingCase(ROUND_TOWARD_NEGATIVE, EXACT, 1, true, 0),

        )

    @Test
    fun test() {
        for (roundingCase in roundingCases)
            test1(roundingCase)
    }

    fun test1(tc: HalfUlpRoundingCase) {
        if (verbose)
            println("$tc")
        val effectiveRoundingDirection = tc.roundingDirection.negate(tc.sign)
        val observedBias = tc.residue.halfUlpBias(effectiveRoundingDirection, tc.lsdw)
        assertEquals(tc.expectedBias, observedBias)
    }

}

