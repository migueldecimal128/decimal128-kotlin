package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestMutDecLn {

    val verbose = true

    @Test
    fun lnOfE_under() {
        val ctx = DecContext.decimal128IEEE()
        // last 4 digits get truncated
        val e = MutDec().set("2.7182818284590452353602874713526624978", ctx)
        if (verbose)
            println("e:$e")
        val result = MutDec().setLn(e, ctx)
        val expected = MutDec().set("0.9999999999999999999999999999999998")
        if (verbose) {
            println("ln(e): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

    @Test
    fun lnOfE_over() {
        val ctx34 = DecContext.decimal128IEEE()
        // e was rounded up in the last digit ... 3 instead of 2
        val e = MutDec().set("2.718281828459045235360287471352663", ctx34)
        if (verbose)
            println("e:$e")
        val result = MutDec().setLn(e, ctx34)
        val expected = MutDec().set("1.000000000000000000000000000000000")
        if (verbose) {
            println("ln(e): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

    @Test
    fun lnOf10() {
        val ctx = DecContext.decimal128IEEE()
        val ten = MutDec().set(10)
        if (verbose)
            println("ten:$ten")
        val result = MutDec().setLn(ten, ctx)
        val expected = MutDec().set("2.302585092994045684017991454684364", ctx)
        if (verbose) {
            println("ln(10): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

    @Test
    fun lnOf2() {
        val ctx = DecContext.decimal128IEEE()
        val two = MutDec().set(2)
        if (verbose)
            println("two:$two")
        val result = MutDec().setLn(two, ctx)
        val expected = MutDec().set("0.6931471805599453094172321214581765681", ctx)
        if (verbose) {
            println("ln(2): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

    @Test
    fun lnOf100() {
        val ctx = DecContext.decimal128IEEE()
        val hundred = MutDec().set(100)
        if (verbose)
            println("hundred:$hundred")
        val result = MutDec().setLn(hundred, ctx)
        val expected = MutDec().set("4.605170185988091368035982909368728416", ctx)

        if (verbose) {
            println("ln($hundred): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

    @Test
    fun lnOfHalf() {
        val ctx = DecContext.decimal128IEEE()
        val half = MutDec().set("0.5")
        if (verbose)
            println("half:$half")
        val result = MutDec().setLn(half, ctx)
        val expected = MutDec().set("-0.6931471805599453094172321214581765681", ctx)

        if (verbose) {
            println("ln($half): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }

}
