package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp

class TestDecNoncomputational {

    val verbose = false

    @Test
    fun testAbs() = runUnaryDecimalOp(
        "dqAbs.decTest",
        "abs",
        Decimal::abs,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            // IEEE definition of abs() differs from Colishaw
            "dqabs523 abs  sNaN    ->  NaN   Invalid_operation",
            "dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation",
            "dqabs526 abs  -NaN22  -> -NaN22",
            "dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation",

            "dqabs900 abs  # -> NaN Invalid_operation",
        )
    )

    @Test
    fun testAbsCases() = runUnaryDecimalOp(
        Decimal::abs,
        verbose = verbose,
        cases = arrayOf(
            "dqabs121 abs   2682682682682682682682682682682682    ->  2682682682682682682682682682682682",
            "dqabs038 abs '+0.000000000001' -> '1E-12'",
        )
    )

    @Test
    fun testMinus() = runUnaryDecimalOp(
        "dqMinus.decTest",
        "minus",
        Decimal::negate,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqmns021 minus         NaN  -> NaN",
            "dqmns022 minus        -NaN  -> -NaN",
            "dqmns023 minus        sNaN  -> NaN  Invalid_operation",
            "dqmns024 minus       -sNaN  -> -NaN Invalid_operation",
            "dqmns031 minus       NaN13  -> NaN13",
            "dqmns032 minus      -NaN13  -> -NaN13",
            "dqmns033 minus      sNaN13  -> NaN13   Invalid_operation",
            "dqmns034 minus     -sNaN13  -> -NaN13  Invalid_operation",
            "dqmns035 minus       NaN70  -> NaN70",
            "dqmns036 minus      -NaN70  -> -NaN70",
            "dqmns037 minus      sNaN101 -> NaN101  Invalid_operation",
            "dqmns038 minus     -sNaN101 -> -NaN101 Invalid_operation",
            "dqmns111 minus          0   -> 0",
            "dqmns113 minus       0E+4   -> 0E+4",
            "dqmns115 minus     0.0000   -> 0.0000",
            "dqmns117 minus      0E-141  -> 0E-141",
        )
    )

    @Test
    fun testCopyNegate() = runUnaryDecimalOp(
        "dqCopyNegate.decTest",
        "copynegate",
        Decimal::negate,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCopySign() = runBinaryDecimalOp(
        "dqCopySign.decTest",
        "copysign",
        Decimal::copySign,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCopy() = runUnaryDecimalOp(
        "dqCopy.decTest",
        "copy",
        Decimal::copy,
        verbose = verbose,
    )

    @Test
    fun testTotalOrder() = runBinaryIntOp(
        "dqCompareTotal.decTest",
        "comparetotal",
        Decimal::compareTotalOrderTo,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqcot9990 comparetotal 10  # -> NaN Invalid_operation",
            "dqcot9991 comparetotal  # 10 -> NaN Invalid_operation",
        )
    )

    @Test
    fun testTotalOrderMag() = runBinaryIntOp(
        "dqCompareTotalMag.decTest",
        "comparetotalMag",
        Decimal::compareTotalOrderMagTo,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqctm9990 comparetotmag 10  # -> NaN Invalid_operation",
            "dqctm9991 comparetotmag  # 10 -> NaN Invalid_operation",
        )
    )

    @Test
    fun testSameQuantum() = runBinaryBooleanOp(
        "dqSameQuantum.decTest",
        "samequantum",
        Decimal::isSameQuantum,
        verbose = verbose
    )

    @Test
    fun testEncode() = runUnaryDecimalOp(
        "dqEncode.decTest",
        "apply",
        Decimal::copy,
        verbose = verbose,
    )
}

