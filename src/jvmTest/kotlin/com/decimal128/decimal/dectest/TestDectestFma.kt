package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.fmaImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.subImpl

class TestDectestFma {

    val verbose = false

    @Test
    fun testFma() = runTernaryDecimalCtxOp(
        "dqFMA.dectest",
        "fma",
        ::fmaImpl,
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
    fun testFmaCases() = runTernaryDecimalCtxOp(
        ::fmaImpl,
        verbose = verbose,
        cases = arrayOf(
            "rounding: floor",
            "dqadd371720 fma  1   0        0E-19  ->  0E-19",
            "dqadd371721 fma  1  -0        0E-19  -> -0E-19",

            //"rounding:    up",
            //"dqadd371674 fma -9E6144 10   1     -> -Infinity Overflow Inexact Rounded",

            "rounding: half_even",
            "dqadd37787 fma  1  -1000 -Inf   -> -Infinity",
            "dqadd3138 fma  1  -1  '0E-39'      ->  '-1.000000000000000000000000000000000' Rounded",
            "dqadd3139 fma  1  '0E-39' 1        ->  '1.000000000000000000000000000000000'  Rounded",
        )
    )

}

