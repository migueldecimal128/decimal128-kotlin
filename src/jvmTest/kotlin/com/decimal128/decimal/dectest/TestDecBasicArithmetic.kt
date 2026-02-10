package com.decimal128.decimal.dectest

import com.decimal128.decimal.addImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.divIntImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.remNearImpl
import com.decimal128.decimal.remTruncImpl
import com.decimal128.decimal.subImpl

class TestDecBasicArithmetic {

    val verbose = true

    @Test
    fun testAdd() = runBinaryDecimalCtxOp(
        "dqAdd.decTest",
        "add",
        ::addImpl,
        verbose = verbose
    )

    @Test
    fun testAddCases() = runBinaryDecimalCtxOp(
        ::addImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqadd71340 add 1E34  -5000000.000010001   ->  9999999999999999999999999995000000      Inexact Rounded",
            "dqadd7976 add      9999999999999999999999999999999999E+6111  9E+6110  -> Infinity Overflow Inexact Rounded",
            "dqadd7975 add      9999999999999999999999999999999999E+6111  1E+6111  -> Infinity Overflow Inexact Rounded",
            "dqadd011 add '0.4444444444444444444444444444444446'  '0.5555555555555555555555555555555555' -> '1.000000000000000000000000000000000' Inexact Rounded",
            "dqadd012 add '0.4444444444444444444444444444444445'  '0.5555555555555555555555555555555555' -> '1.000000000000000000000000000000000' Rounded",
            "dqadd7794 add  Inf  -Inf   ->  NaN  Invalid_operation",
//            "rounding: half_up",
//            "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded"
        )
    )

    @Test
    fun testSubtract() = runBinaryDecimalCtxOp(
        "dqSubtract.decTest",
        "subtract",
        ::subImpl,
        verbose = verbose
    )

    @Test
    fun testSubtractCases() = runBinaryDecimalCtxOp(
        ::subImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqsub063 subtract '70000'    '10000e+34' -> '-9.999999999999999999999999999999993E+37' Rounded",
            "dqsub472 subtract '0.4444444444444444444444444444444444'  '-0.5555555555555555555555555555555561' -> '1.000000000000000000000000000000000' Inexact Rounded",
            "dqsub470 subtract '0.4444444444444444444444444444444444'  '-0.5555555555555555555555555555555563' -> '1.000000000000000000000000000000001' Inexact Rounded",
            "dqsub463 subtract '1.111'  '-1E+12'  -> '1000000000001.111'",
            "dqsub372 subtract 1  .0 -> 1.0",
            "dqsub040 subtract '5.75' '3.3'  -> '2.45'",
        )
    )

    @Test
    fun testMultiply() = runBinaryDecimalCtxOp(
        "dqMultiply.decTest",
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
        "dqDivide.decTest",
        "divide",
        ::divImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testDivideCases() = runBinaryDecimalCtxOp(
        ::divImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqdiv732 divide 00.00 0.000  -> NaN Division_undefined",
            "dqdiv791 divide -0     Inf   -> -0E-6176 Clamped",
            "dqdiv788 divide -1000  Inf   -> -0E-6176 Clamped",
            "dqdiv783 divide  Inf  -0     -> -Infinity",
            "dqdiv781 divide  Inf  -1000  -> -Infinity",
        )
    )

    @Test
    fun testDivideInt() = runBinaryDecimalCtxOp(
        "dqDivideInt.decTest",
        "divideint",
        ::divIntImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testDivideIntCases() = runBinaryDecimalCtxOp(
        ::divIntImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqdvi274 divideint 9e384    1       -> NaN Division_impossible",
        )
    )

    @Test
    fun testRemainder() = runBinaryDecimalCtxOp(
        "dqRemainder.decTest",
        "remainder",
        ::remTruncImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqrem421 remainder   1E+6144        1  ->   NaN Division_impossible",
            "dqrem772  remainder  1234568888888887777777777890123456   0.1  ->  NaN Division_impossible",
            "dqrem773  remainder  1234568888888887777777777890123456   0.01 ->  NaN Division_impossible",
            "dqrem1051 remainder  1e+277  1e-311 ->  NaN Division_impossible",
            "dqrem1052 remainder  1e+277 -1e-311 ->  NaN Division_impossible",
            "dqrem1053 remainder -1e+277  1e-311 ->  NaN Division_impossible",
            "dqrem1054 remainder -1e+277 -1e-311 ->  NaN Division_impossible",
        )
    )

    @Test
    fun testRemainderCases() = runBinaryDecimalCtxOp(
        ::remTruncImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqrem131 remainder  0     -1   ->  0",
            "dqrem107 remainder  0.0001  0   -> NaN Invalid_operation",
            "dqrem083 remainder  0.00E+9       1  -> 0",
        )
    )

    @Test
    fun testRemainderNear() = runBinaryDecimalCtxOp(
        "dqRemainderNear.decTest",
        "remaindernear",
        ::remNearImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqrmn421 remaindernear   1E+6144        1  ->   NaN Division_impossible",
            "dqrmn772  remaindernear  1234500000000000000000067890123456   0.1  ->  NaN Division_impossible",
            "dqrmn773  remaindernear  1234500000000000000000067890123456   0.01 ->  NaN Division_impossible",
            "dqrmn1051 remaindernear  1e+277  1e-311 ->  NaN Division_impossible",
            "dqrmn1052 remaindernear  1e+277 -1e-311 ->  NaN Division_impossible",
            "dqrmn1053 remaindernear -1e+277  1e-311 ->  NaN Division_impossible",
            "dqrmn1054 remaindernear -1e+277 -1e-311 ->  NaN Division_impossible",
        )
    )

    @Test
    fun testRemainderNearCases() = runBinaryDecimalCtxOp(
        ::remNearImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

}

