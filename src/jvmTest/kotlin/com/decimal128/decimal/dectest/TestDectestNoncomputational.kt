package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner.runUnaryDecimalOp

class TestDectestNoncomputational {


    @Test
    fun testAbs() = runUnaryDecimalOp("dqAbs.dectest", Decimal::abs,
        verbose = false,
        skip = true,
        skipCases = arrayOf(
            // IEEE definition of abs() differs from Colishaw
            "dqabs523 abs  sNaN    ->  NaN   Invalid_operation",
            "dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation",
            "dqabs526 abs  -NaN22  -> -NaN22",
            "dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation",

            "dqabs900 abs  # -> NaN Invalid_operation",
        ),
        targetOnly = false,
        targetCases = arrayOf(
            "dqabs121 abs   2682682682682682682682682682682682    ->  2682682682682682682682682682682682",
            "dqabs038 abs '+0.000000000001' -> '1E-12'",
        )
    )


}