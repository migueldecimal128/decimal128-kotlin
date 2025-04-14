package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.decimal128.Residual.Companion.EXACT
import com.decimal128.Residual.Companion.HALF
import com.decimal128.Residual.Companion.BIAS_TRUNC

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE



class TestResidualRounding {

    class RoundingDirectionCase(
        val roundingDirection: RoundingDirection, val residual: Residual, val lsb: Long, val sign: Boolean,
        val expectedBias: Long) {
        override fun toString() : String {
            return "$roundingDirection $residual sign:$sign lsb:$lsb expectedBias:$expectedBias"
        }
    }

    val roundingCases = arrayOf(

        // roundTiesToEven
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 0, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 0, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 1, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, EXACT, 1, true, 0),

        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 0, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 0, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 1, false, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, HALF, 1, true, 1),

        RoundingDirectionCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 0, false, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 0, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 1, false, 1),
        RoundingDirectionCase(ROUND_TIES_TO_EVEN, BIAS_TRUNC, 1, true, 1),

        // roundTiesToAway
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 0, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 0, true, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 1, false, 0),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, EXACT, 1, true, 0),

        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 0, false, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 0, true, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 1, false, 1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, HALF, 1, true, 1),

        RoundingDirectionCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 0, false,1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 0, true,1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 1, false,1),
        RoundingDirectionCase(ROUND_TIES_TO_AWAY, BIAS_TRUNC, 1, true, 1),

        // roundTowardZero
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, EXACT, 1, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, HALF, 1, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_ZERO, BIAS_TRUNC, 1, true, 0),

        // roundTowardPositive
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, EXACT, 1, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 0, false, 2),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 1, false, 2),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, HALF, 1, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 0, false, 2),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 1, false, 2),
        RoundingDirectionCase(ROUND_TOWARD_POSITIVE, BIAS_TRUNC, 1, true, 0),

        // roundTowardNegative
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 0, true, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, EXACT, 1, true, 0),

        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 0, true, 2),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, HALF, 1, true, 2),

        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 0, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 0, true, 2),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 1, false, 0),
        RoundingDirectionCase(ROUND_TOWARD_NEGATIVE, BIAS_TRUNC, 1, true, 2),

        )

    @Test
    fun test() {
        for (roundingCase in roundingCases)
            test1(roundingCase)
    }

    fun test1(tc: RoundingDirectionCase) {
        println("$tc")
        val effectiveRoundingDirection = tc.roundingDirection.negate(tc.sign)
        val observedBias = Residual.biasFrom(effectiveRoundingDirection, tc.residual, tc.lsb)
        assertEquals(tc.expectedBias, observedBias)
    }

}

