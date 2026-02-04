package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalCtxOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.fmaImpl

class TestDecMisc {

    val verbose = false

    @Test
    fun testLogB() = runUnaryDecimalCtxOp(
        "dqLogB.dectest",
        "logb",
        Decimal::logB,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            // This test case is wrong
            // under roundTowardPositive the result is MIN_FINITE, not -Infinity
            //"rounding:    up",
            "dqadd371674 fma -9E6144 10   1     -> -Infinity Overflow Inexact Rounded",
            )
    )

    @Test
    fun testFmaCases() = runUnaryDecimalCtxOp(
        Decimal::logB,
        verbose = verbose,
        cases = arrayOf(
        )
    )

}

