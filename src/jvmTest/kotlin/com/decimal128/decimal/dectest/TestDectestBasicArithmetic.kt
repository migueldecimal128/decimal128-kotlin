package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.subImpl

class TestDectestBasicArithmetic {

    val verbose = false

    @Test
    fun testAdd() = runBinaryDecimalCtxOp(
        "dqAdd.dectest",
        "add",
        ::addImpl,
        verbose = verbose
    )

    @Test
    fun testAddCases() = runBinaryDecimalCtxOp(
        ::addImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqadd011 add '0.4444444444444444444444444444444446'  '0.5555555555555555555555555555555555' -> '1.000000000000000000000000000000000' Inexact Rounded",
            "rounding: half_up",
            "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded"
        )
    )

    @Test
    fun testSubtract() = runBinaryDecimalCtxOp(
        "dqSubtract.dectest",
        "subtract",
        ::subImpl,
        verbose = verbose
    )

    @Test
    fun testSubtractCases() = runBinaryDecimalCtxOp(
        ::subImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqsub470 subtract '0.4444444444444444444444444444444444'  '-0.5555555555555555555555555555555563' -> '1.000000000000000000000000000000001' Inexact Rounded",
            "dqsub463 subtract '1.111'  '-1E+12'  -> '1000000000001.111'",
            "dqsub372 subtract 1  .0 -> 1.0",
            "dqsub040 subtract '5.75' '3.3'  -> '2.45'",
        )
    )

    @Test
    fun testMultiply() = runBinaryDecimalCtxOp(
        "dqMultiply.dectest",
        "multiply",
        ::mulImpl,
        verbose = verbose
    )

    @Test
    fun testMultiplyCases() = runBinaryDecimalCtxOp(
        ::mulImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqmul580 multiply  Inf  -Inf   -> -Infinity",
            "dqmul025 multiply -0.0   -0.0   ->  0.00",
            "dqmul008 multiply -1.20  0 -> -0.00",
        )
    )

    @Test
    fun testDivide() = runBinaryDecimalCtxOp(
        "dqDivide.dectest",
        "divide",
        ::divImpl,
        verbose = verbose
    )

    @Test
    fun testDivideCases() = runBinaryDecimalCtxOp(
        ::divImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqdiv791 divide -0     Inf   -> -0E-6176 Clamped",
            "dqdiv788 divide -1000  Inf   -> -0E-6176 Clamped",
            "dqdiv783 divide  Inf  -0     -> -Infinity",
            "dqdiv781 divide  Inf  -1000  -> -Infinity",
        )
    )


}

