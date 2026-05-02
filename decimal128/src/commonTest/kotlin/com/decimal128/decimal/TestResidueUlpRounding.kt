package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.EXACT

import com.decimal128.decimal.RoundingDirection.Companion.TIES_TO_EVEN
import com.decimal128.decimal.RoundingDirection.Companion.TIES_TO_AWAY
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_ZERO
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_POSITIVE
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_NEGATIVE


class TestResidueUlpRounding {

    val verbose = false

    class TC(
        val roundingDirection: RoundingDirection, val residue: Residue, val lsdw: Long, val sign: Boolean,
        val expectedBias: Long) {

            override fun toString() : String {
            return "$roundingDirection $residue sign:$sign lsb:$lsdw expectedBias:$expectedBias"
        }
    }

    val roundingCases = arrayOf(

        // roundTiesToEven
        TC(TIES_TO_EVEN, HALF, 0, false, 0),
        TC(TIES_TO_EVEN, HALF, 2, true, 0),
        TC(TIES_TO_EVEN, HALF, 5, false, 1),
        TC(TIES_TO_EVEN, HALF, 7, true, 1),

        TC(TIES_TO_EVEN, LT_HALF, 4, false, 0),
        TC(TIES_TO_EVEN, LT_HALF, 6, true, 0),
        TC(TIES_TO_EVEN, LT_HALF, 9, false, 0),
        TC(TIES_TO_EVEN, LT_HALF, 1, true, 0),

        TC(TIES_TO_EVEN, GT_HALF, 0, false, 1),
        TC(TIES_TO_EVEN, GT_HALF, 2, true, 1),
        TC(TIES_TO_EVEN, GT_HALF, 9, false, 1),
        TC(TIES_TO_EVEN, GT_HALF, 1, true, 1),

        TC(TIES_TO_EVEN, EXACT, 4, false, 0),
        TC(TIES_TO_EVEN, EXACT, 6, true, 0),
        TC(TIES_TO_EVEN, EXACT, 1, false, 0),
        TC(TIES_TO_EVEN, EXACT, 3, true, 0),

        // roundTiesToAway
        TC(TIES_TO_AWAY, HALF, 8, false, 1),
        TC(TIES_TO_AWAY, HALF, 0, true, 1),
        TC(TIES_TO_AWAY, HALF, 7, false, 1),
        TC(TIES_TO_AWAY, HALF, 9, true, 1),

        TC(TIES_TO_AWAY, LT_HALF, 2, false,0),
        TC(TIES_TO_AWAY, LT_HALF, 4, true,0),
        TC(TIES_TO_AWAY, LT_HALF, 1, false,0),
        TC(TIES_TO_AWAY, LT_HALF, 3, true, 0),

        TC(TIES_TO_AWAY, GT_HALF, 6, false,1),
        TC(TIES_TO_AWAY, GT_HALF, 8, true,1),
        TC(TIES_TO_AWAY, GT_HALF, 1, false,1),
        TC(TIES_TO_AWAY, GT_HALF, 3, true, 1),

        TC(TIES_TO_AWAY, EXACT, 2, false, 0),
        TC(TIES_TO_AWAY, EXACT, 4, true, 0),
        TC(TIES_TO_AWAY, EXACT, 3, false, 0),
        TC(TIES_TO_AWAY, EXACT, 5, true, 0),

        // roundTowardZero
        TC(TOWARD_ZERO, HALF, 6, false, 0),
        TC(TOWARD_ZERO, HALF, 8, true, 0),
        TC(TOWARD_ZERO, HALF, 9, false, 0),
        TC(TOWARD_ZERO, HALF, 1, true, 0),

        TC(TOWARD_ZERO, LT_HALF, 0, false, 0),
        TC(TOWARD_ZERO, LT_HALF, 2, true, 0),
        TC(TOWARD_ZERO, LT_HALF, 3, false, 0),
        TC(TOWARD_ZERO, LT_HALF, 5, true, 0),

        TC(TOWARD_ZERO, GT_HALF, 4, false, 0),
        TC(TOWARD_ZERO, GT_HALF, 6, true, 0),
        TC(TOWARD_ZERO, GT_HALF, 3, false, 0),
        TC(TOWARD_ZERO, GT_HALF, 5, true, 0),

        TC(TOWARD_ZERO, EXACT, 8, false, 0),
        TC(TOWARD_ZERO, EXACT, 0, true, 0),
        TC(TOWARD_ZERO, EXACT, 5, false, 0),
        TC(TOWARD_ZERO, EXACT, 7, true, 0),

        // roundTowardPositive
        TC(TOWARD_POSITIVE, HALF, 2, false, 1),
        TC(TOWARD_POSITIVE, HALF, 4, true, 0),
        TC(TOWARD_POSITIVE, HALF, 1, false, 1),
        TC(TOWARD_POSITIVE, HALF, 3, true, 0),

        TC(TOWARD_POSITIVE, LT_HALF, 6, false, 1),
        TC(TOWARD_POSITIVE, LT_HALF, 8, true, 0),
        TC(TOWARD_POSITIVE, LT_HALF, 5, false, 1),
        TC(TOWARD_POSITIVE, LT_HALF, 7, true, 0),

        TC(TOWARD_POSITIVE, GT_HALF, 0, false, 1),
        TC(TOWARD_POSITIVE, GT_HALF, 0, true, 0),
        TC(TOWARD_POSITIVE, GT_HALF, 5, false, 1),
        TC(TOWARD_POSITIVE, GT_HALF, 7, true, 0),

        TC(TOWARD_POSITIVE, EXACT, 0, false, 0),
        TC(TOWARD_POSITIVE, EXACT, 0, true, 0),
        TC(TOWARD_POSITIVE, EXACT, 7, false, 0),
        TC(TOWARD_POSITIVE, EXACT, 9, true, 0),

        // roundTowardNegative
        TC(TOWARD_NEGATIVE, HALF, 0, false, 0),
        TC(TOWARD_NEGATIVE, HALF, 0, true, 1),
        TC(TOWARD_NEGATIVE, HALF, 3, false, 0),
        TC(TOWARD_NEGATIVE, HALF, 5, true, 1),

        TC(TOWARD_NEGATIVE, LT_HALF, 0, false, 0),
        TC(TOWARD_NEGATIVE, LT_HALF, 0, true, 1),
        TC(TOWARD_NEGATIVE, LT_HALF, 7, false, 0),
        TC(TOWARD_NEGATIVE, LT_HALF, 9, true, 1),

        TC(TOWARD_NEGATIVE, GT_HALF, 0, false, 0),
        TC(TOWARD_NEGATIVE, GT_HALF, 0, true, 1),
        TC(TOWARD_NEGATIVE, GT_HALF, 7, false, 0),
        TC(TOWARD_NEGATIVE, GT_HALF, 9, true, 1),

        TC(TOWARD_NEGATIVE, EXACT, 0, false, 0),
        TC(TOWARD_NEGATIVE, EXACT, 0, true, 0),
        TC(TOWARD_NEGATIVE, EXACT, 9, false, 0),
        TC(TOWARD_NEGATIVE, EXACT, 1, true, 0),

        )

    @Test
    fun test() {
        for (roundingCase in roundingCases)
            test1(roundingCase)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("$tc")
        val effectiveRoundingDirection = tc.roundingDirection.negated(tc.sign)
        val observedBias = tc.residue.ulpBias(effectiveRoundingDirection, tc.lsdw)
        assertEquals(tc.expectedBias, observedBias)
    }

}