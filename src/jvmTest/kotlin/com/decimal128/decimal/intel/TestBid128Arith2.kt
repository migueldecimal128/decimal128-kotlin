package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Int_Decimal
import com.decimal128.decimal.intel.IntelRunner1.runDecimalIntMethodOp
import com.decimal128.decimal.intel.IntelRunner1.runMethod_Int
import com.decimal128.decimal.intel.IntelRunner1.runUnaryDecimalCtxMethodOp
import org.junit.jupiter.api.Test

class TestBid128Arith2 {

    val verbose = false

    @Test
    fun testScaleB(): Unit = intelMethod_Int_Decimal(
        "/intel/readtest.in",
        "bid128_scalbn",
        Decimal::scaleB,
        verbose = verbose,
    )

    @Test
    fun testScaleBCases(): Unit = intelMethod_Int_Decimal(
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
    fun testRoundToIntegralTiesToEven(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_nearest_even",
        Decimal::roundToIntegralTiesToEven,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTiesToAway(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_nearest_away",
        Decimal::roundToIntegralTiesToAway,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardNegative(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_negative",
        Decimal::roundToIntegralTowardNegative,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardPositive(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_positive",
        Decimal::roundToIntegralTowardPositive,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralTowardZero(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_zero",
        Decimal::roundToIntegralTowardZero,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testRoundToIntegralExact(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_round_integral_exact",
        Decimal::roundToIntegralExact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testNextUp(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_nextup",
        Decimal::nextUp,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testNextUpCases(): Unit = intelMethod_Decimal(
        arrayOf(
            "bid128_nextup 0 [782d2f94d69006ec,2196c0c64c5c5d60] [78000000000000000000000000000000] 00",
        ),
        Decimal::nextUp,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testNextDown(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_nextdown",
        Decimal::nextDown,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testQuantumExponent(): Unit = runMethod_Int(
        "/intel/readtest.in",
        "bid128_quantexp",
        Decimal::quantumExponent,
        verbose = verbose,
    )


}