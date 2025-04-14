package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.decimal128.ResidualOld.Companion.EXACT
import com.decimal128.ResidualOld.Companion.LT_HALF
import com.decimal128.ResidualOld.Companion.HALF
import com.decimal128.ResidualOld.Companion.GT_HALF

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE



class TestResidualOldRounding {

    class RoundingDirectionCase(
        val roundingDirection: RoundingDirection, val residual: ResidualOld, val lsb: Long, val sign: Boolean,
        val expectedInexact: Boolean, val expectedRoundUp: Long
    ) {
        override fun toString() : String {
            return "$roundingDirection $residual sign:$sign lsb:$lsb expectedInexact:$expectedInexact expectedRoundup:$expectedRoundUp"
        }
    }

    val roundingCases = arrayOf(

        // roundTiesToEven
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 0, false, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 0, true, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 1, false, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 1, true, false, 0),

        RoundingDirectionCase(ROUND_TIES_TO_EVEN, LT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, LT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, LT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, LT_HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 1, true, true, 1),

        RoundingDirectionCase(ROUND_TIES_TO_EVEN, GT_HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, GT_HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, GT_HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, GT_HALF, 1, true, true, 1),

        // roundTiesToAway
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 0, false, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 0, true, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 1, false, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 1, true, false, 0),

        RoundingDirectionCase(ROUND_TIES_TO_AWAY, LT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, LT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, LT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, LT_HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 1, true, true, 1),

        RoundingDirectionCase(ROUND_TIES_TO_AWAY, GT_HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, GT_HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, GT_HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, GT_HALF, 1, true, true, 1),

        // roundTowardZero
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 0, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 0, true, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 1, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 1, true, false, 0),

        RoundingDirectionCase(ROUND_TOWARD_ZERO, LT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, LT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, LT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, LT_HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_ZERO, GT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, GT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, GT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, GT_HALF, 1, true, true, 0),

        // roundTowardPositive
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 0, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 0, true, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 1, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 1, true, false, 0),

        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, LT_HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, LT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, LT_HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, LT_HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 1, true, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, GT_HALF, 0, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, GT_HALF, 0, true, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, GT_HALF, 1, false, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, GT_HALF, 1, true, true, 0),

        // roundTowardNegative
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, true, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 1, false, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 1, true, false, 0),

        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, LT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, LT_HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, LT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, LT_HALF, 1, true, true, 1),

        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 1, true, true, 1),

        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, GT_HALF, 0, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, GT_HALF, 0, true, true, 1),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, GT_HALF, 1, false, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, GT_HALF, 1, true, true, 1),

        )

    @Test
    fun test() {
        for (roundingCase in roundingCases)
            test1(roundingCase)
    }

    fun test1(tc: RoundingDirectionCase) {
        println("$tc")
        val effectiveRoundingDirection = tc.roundingDirection.negate(tc.sign)
        val observedBits = ResidualOld.inexactAndRoundupFrom(effectiveRoundingDirection, tc.residual, tc.lsb)
        val observedInexact = (observedBits and 2) != 0L
        val observedRoundUp = observedBits and 1

        assertEquals(tc.expectedInexact, observedInexact)
        assertEquals(tc.expectedRoundUp, observedRoundUp)
    }

}

