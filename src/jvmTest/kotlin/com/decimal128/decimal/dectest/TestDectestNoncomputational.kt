package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal2
import com.decimal128.decimal.dectest.DectestRunner.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner.runUnaryDecimalOp

class TestDectestNoncomputational {

    val verbose = true

    @Test
    fun testAbs() = runUnaryDecimalOp(
        "dqAbs.dectest",
        Decimal2::abs,
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
        Decimal2::abs,
        verbose = verbose,
        cases = arrayOf(
            "dqabs121 abs   2682682682682682682682682682682682    ->  2682682682682682682682682682682682",
            "dqabs038 abs '+0.000000000001' -> '1E-12'",
        )
    )

    @Test
    fun testMinus() = runUnaryDecimalOp(
        "dqMinus.dectest",
        Decimal2::negate,
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
    fun testCopy() = runUnaryDecimalOp(
        "dqCopy.dectest",
        Decimal2::copy,
        verbose = verbose,
    )

    @Test
    fun testTotalOrder() = runBinaryIntOp(
        "dqCompareTotal.dectest",
        Decimal2::compareTotalOrderTo,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqcot9990 comparetotal 10  # -> NaN Invalid_operation",
            "dqcot9991 comparetotal  # 10 -> NaN Invalid_operation",
        )
    )

    @Test
    fun testTotalOrderMag() = runBinaryIntOp(
        "dqCompareTotalMag.dectest",
        Decimal2::compareTotalOrderMagTo,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqctm9990 comparetotmag 10  # -> NaN Invalid_operation",
            "dqctm9991 comparetotmag  # 10 -> NaN Invalid_operation",
        )
    )

    @Test
    fun testSameQuantum() = runBinaryBooleanOp(
        "dqSameQuantum.dectest",
        Decimal2::sameQuantum,
        verbose = verbose
    )

}

