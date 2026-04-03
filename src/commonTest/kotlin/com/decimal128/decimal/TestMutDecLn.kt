package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestMutDecLn {

    val verbose = true

    @Test
    fun lnOfE() {
        val ctx = DecContext.decimal128IEEE()
        val e = MutDec().set("2.7182818284590452353602874713526624978")
        val result = MutDec().setLn(e, ctx)
        val expected = MutDec().set("1")
        if (verbose) {
            println("ln(e): $result")
        }
        assertTrue(result.bitwiseEQ(expected))
    }
}