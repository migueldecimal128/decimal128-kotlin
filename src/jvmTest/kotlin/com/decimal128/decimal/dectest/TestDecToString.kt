package com.decimal128.decimal.dectest

import com.decimal128.decimal.dectest.DectestRunner1.runUnaryStringCtxOp
import com.decimal128.decimal.fmaImpl
import org.junit.jupiter.api.Test

class TestDecToString {

    val verbose = false

    /*
    @Test
    fun testToSci() = runUnaryStringCtxOp(
        "dqBase.decTest",
        "toSci",
        ::fmaImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testToSciCases() = runUnaryStringCtxOp(
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
*/
}