package com.decimal128.decimal.dectest

import com.decimal128.decimal.D128ParsePrint
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryStringCtxOp
import com.decimal128.decimal.fmaImpl
import org.junit.jupiter.api.Test

class TestDecToString {

    val verbose = true

    @Test
    fun testToSci() {
        runUnaryStringCtxOp(
            "dqBase.decTest",
            "toSci",
            D128ParsePrint::toString,
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
        D128ParsePrint::toString,
        verbose = verbose,
        cases = arrayOf(
            "dqbas562 toSci \"NaNq\"            -> NaN Conversion_syntax",
            "dqbas500 toSci '1..2'            -> NaN Conversion_syntax",
            "dqbas071 toSci  .1234567891234567890123456780123456123  -> 0.1234567891234567890123456780123456 Inexact Rounded",
        )
    )

}