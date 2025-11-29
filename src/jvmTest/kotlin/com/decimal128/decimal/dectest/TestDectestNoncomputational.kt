package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner.runUnaryDecimalOp

class TestDectestNoncomputational {


    @Test
    fun testAbs() = runUnaryDecimalOp("dqAbs.dectest", Decimal::abs,
        verbose = false,
        skip = true,
        skipIds = arrayOf(
            // IEEE definition of abs() differs from Colishaw
            "dqabs523", // dqabs523 abs  sNaN    ->  NaN   Invalid_operation
            "dqabs525", // dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation
            "dqabs526", // dqabs526 abs  -NaN22  -> -NaN22
            "dqabs527", // dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation

            "dqabs900", // dqabs900 abs  # -> NaN Invalid_operation
        ),
        targetOnly = false,
        targetIds = arrayOf(
            "dqabs526", // all NaNs compare equals
        )
    )


}