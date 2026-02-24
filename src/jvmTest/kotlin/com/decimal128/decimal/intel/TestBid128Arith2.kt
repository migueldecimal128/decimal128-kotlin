package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.runDecimalIntMethodOp
import com.decimal128.decimal.intel.IntelRunner1.runUnaryDecimalCtxMethodOp
import org.junit.jupiter.api.Test

class TestBid128Arith2 {

    val verbose = true

    @Test
    fun testScaleB(): Unit = runDecimalIntMethodOp(
        "/intel/readtest.in",
        "bid128_scalbn",
        Decimal::scaleB,
        verbose = verbose,
    )

    @Test
    fun testScaleBCases(): Unit = runDecimalIntMethodOp(
        arrayOf(
            "bid128_scalbn 1 [2FFDED09BEAD87C0378D8E63FFFFFFFF] 2147483647 [5FFFED09BEAD87C0378D8E63FFFFFFFF] 28 ulp=0.00000",
            "bid128_scalbn 0 [00000000000000000000000000000001] -1 [00000000000000000000000000000000] 30 ulp=.1000000000",
            "bid128_scalbn 0 [000000028a080400,8002020024000000] -9 [000000000000000ae7dcfc24a9355432] 30",
            "bid128_scalbn 0 [00000000000000000000000000000000] -1 [00000000000000000000000000000000] 00",
        ),
        Decimal::scaleB,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTiesToEven(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_nearest_even",
        Decimal::roundToIntegralTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTiesToAway(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_nearest_away",
        Decimal::roundToIntegralTiesToAway,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardNegative(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_negative",
        Decimal::roundToIntegralTowardNegative,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardPositive(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_positive",
        Decimal::roundToIntegralTowardPositive,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardZero(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_zero",
        Decimal::roundToIntegralTowardZero,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralExact(): Unit = runUnaryDecimalCtxMethodOp(
        "/intel/readtest.in",
        "bid128_round_integral_exact",
        Decimal::roundToIntegralExact,
        DecContext.DECIMAL128,
        verbose = verbose,
    )


}