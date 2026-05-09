package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestRoundAndFinalize {

    val verbose = true

    @Test
    fun testProblem1() {
        //        TC("ub_half_tta", false, -6177, 5, .exact, .tiesToAway, "1E-6176"),
        val sign = false
        val qExpIn = -6177
        val dw1 = 0L
        val dw0 = 5L
        val residueIn = Residue.EXACT
        val roundingDirection = RoundingDirection.TIES_TO_AWAY
        val ctx = DecContext.decimal128Kotlin()

        val observed = decRoundAndFinalizeFinite(sign, qExpIn, dw1, dw0, residueIn, roundingDirection, ctx)
        val observedStr = observed.toString()
        if (verbose) {
            println("observed:$observedStr")
        }
        assertEquals("1E-6176", observedStr)
    }

    @Test
    fun testProblem2() {
        //        TC("sub_rollover", false, -6177, 99, .exact, .tiesToEven, "1E-6175"),
        val sign = false
        val qExpIn = -6177
        val dw1 = 0L
        val dw0 = 99L
        val residueIn = Residue.EXACT
        val roundingDirection = RoundingDirection.TIES_TO_EVEN
        val ctx = DecContext.decimal128Kotlin()

        val observed = decRoundAndFinalizeFinite(sign, qExpIn, dw1, dw0, residueIn, roundingDirection, ctx)
        val observedStr = observed.toString()
        if (verbose) {
            println("observed:$observedStr")
        }
        assertEquals("1.0E-6175", observedStr)

    }

    @Test
    fun testProblem3() {
        val sign = false
        val qExpIn = 6112
        val dw1 = 0L
        val dw0 = 1L
        val residueIn = Residue.EXACT
        val roundingDirection = RoundingDirection.TIES_TO_EVEN
        val ctx = DecContext.decimal128Kotlin()

        val observed = decRoundAndFinalizeFinite(sign, qExpIn, dw1, dw0, residueIn, roundingDirection, ctx)
        val observedStr = observed.toString()
        if (verbose) {
            println("observed:$observedStr")
        }
        assertEquals("1.0E+6112", observedStr)
    }

    @Test
    fun testProblem4() {
        val sign = false
        val qExpIn = 6144
        val dw1 = 0L
        val dw0 = 1L
        val residueIn = Residue.EXACT
        val roundingDirection = RoundingDirection.TIES_TO_EVEN
        val ctx = DecContext.decimal128Kotlin()

        val observed = decRoundAndFinalizeFinite(sign, qExpIn, dw1, dw0, residueIn, roundingDirection, ctx)
        val observedStr = observed.toString()
        if (verbose) {
            println("observed:$observedStr")
        }
        assertEquals("1.000000000000000000000000000000000E+6144", observedStr)
    }

    @Test
    fun testProblem5() {
        val sign = false
        val qExpIn = 6145
        val dw1 = 0L
        val dw0 = 1L
        val residueIn = Residue.EXACT
        val roundingDirection = RoundingDirection.TIES_TO_EVEN
        val ctx = DecContext.decimal128Kotlin()

        val observed = decRoundAndFinalizeFinite(sign, qExpIn, dw1, dw0, residueIn, roundingDirection, ctx)
        val observedStr = observed.toString()
        if (verbose) {
            println("observed:$observedStr")
        }
        assertEquals("Infinity", observedStr)
    }

}