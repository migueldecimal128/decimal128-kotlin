package com.decimal128.decimal.dectest

import com.decimal128.decimal.d128ToString
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryStringCtxOp
import org.junit.jupiter.api.Test

class TestDecToString {

    val verbose = false

    @Test
    fun testToSci() {
        runUnaryStringCtxOp(
            "dqBase.decTest",
            "toSci",
            ::d128ToString,
            verbose = verbose,
            skip = true,
            skipCases = arrayOf<String>(
                "dqbas745 toSci 'sNaN1234567890123456787234561234567890' -> NaN Conversion_syntax",
                "dqbas725 toSci 'NaN1234567890123456781234567890123456' -> NaN Conversion_syntax",
                "dqbas561 toSci \"qNaN\"            -> NaN Conversion_syntax"
            )
        )
    }

    @Test
    fun testToSciCases() = runUnaryStringCtxOp(
        ::d128ToString,
        verbose = verbose,
        cases = arrayOf(
            "dqbas713 toSci 'NaN1'            -> NaN1",
            "dqbas500 toSci '1..2'            -> NaN Conversion_syntax",
            "dqbas562 toSci \"NaNq\"            -> NaN Conversion_syntax",
            "dqbas071 toSci  .1234567891234567890123456780123456123  -> 0.1234567891234567890123456780123456 Inexact Rounded",
        )
    )

    @Test
    fun testToEng() {
        runUnaryStringCtxOp(
            "dqBase.decTest",
            "toEng",
            ::d128ToString,
            printStyleEngineering = true,
            verbose = verbose,
            skip = true,
            skipCases = arrayOf<String>(
            )
        )
    }

    @Test
    fun testToEngCases() = runUnaryStringCtxOp(
        ::d128ToString,
        printStyleEngineering = true,
        verbose = verbose,
        cases = arrayOf<String>(
            "dqbast800 toEng 0e+1              -> \"0.00E+3\"",
            "dqbas330  toEng 10e-2  -> 0.10",
            "dqbas324  toEng 10e1   -> 100",
            "dqbas302  toEng 10e12  -> 10E+12",
        )
    )

}