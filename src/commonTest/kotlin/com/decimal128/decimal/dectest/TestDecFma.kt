package com.decimal128.decimal.dectest

import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import kotlin.test.Test
import com.decimal128.decimal.fmaImpl

class TestDecFma {

    val verbose = false

    @Test
    fun testFma() = runTernaryDecimalCtxOp(
        "dqFMA.decTest",
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
            "dqfma0307  fma   1e-6176    0.1  1e-6176   -> 1E-6176 Underflow Subnormal Inexact Rounded",
            "dqadd3139 fma  1  '0E-39' 1        ->  '1.000000000000000000000000000000000'  Rounded",

            "dqfma0900 fma  NaN2  NaN3  NaN5   ->  NaN2",

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

