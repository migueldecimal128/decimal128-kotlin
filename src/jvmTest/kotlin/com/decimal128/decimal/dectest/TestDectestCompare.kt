package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.cmpImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.maxImpl
import com.decimal128.decimal.maxNumImpl
import com.decimal128.decimal.minImpl
import com.decimal128.decimal.minNumImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.subImpl

class TestDectestCompare {

    val verbose = true

    @Test
    fun testCompare() = runBinaryDecimalCtxOp(
        "dqCompare.dectest",
        "compare",
        ::cmpImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareCases() = runBinaryDecimalCtxOp(
        ::cmpImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqcom002 compare  -2  -1  -> -1",
            "dqcom101 compare   7.0    7      -> 0",
        )
    )

}

