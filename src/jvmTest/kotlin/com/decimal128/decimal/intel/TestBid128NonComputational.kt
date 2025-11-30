package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import kotlin.test.Test

class TestBid128NonComputational {

    val verbose = false

    /*****************************************************************************
     *
     *    BID128 non-computational functions:
     *         - bid128_isSigned
     *         - bid128_isNormal
     *         - bid128_isSubnormal
     *         - bid128_isFinite
     *         - bid128_isZero
     *         - bid128_isInf
     *         - bid128_isSignaling
     *         - bid128_isCanonical
     *         - bid128_isNaN
     *         - bid128_copy
     *         - bid128_negate
     *         - bid128_abs
     *         - bid128_copySign
     *         - bid128_class
     *         - bid128_totalOrder
     *         - bid128_totalOrderMag
     *         - bid128_sameQuantum
     *         - bid128_radix
     ****************************************************************************/

    @Test
    fun testIsSigned() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSigned",
        Decimal::isNegative,
        verbose = verbose
    )

    @Test
    fun testIsNormal() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isNormal",
        Decimal::isNormal,
        verbose = verbose
    )

    @Test
    fun testIsNormalCases() = IntelRunner.runUnaryBooleanOp(
        Decimal::isNormal,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isNormal 0 [0001ed09bead87c0378d8e64ffffffff] 0 00",
        )
    )


    @Test
    fun testIsSubnormal() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSubnormal",
        Decimal::isSubnormal,
        verbose = verbose
    )

    @Test
    fun testIsFinite() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isFinite",
        Decimal::isFinite,
        verbose = verbose
    )

    @Test
    fun testIsZero() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isZero",
        Decimal::isCanonicalZero,
        verbose = verbose
    )

    @Test
    fun testIsZeroCases() = IntelRunner.runUnaryBooleanOp(
        Decimal::isCanonicalZero,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isZero 0 [789b88be70d10384,ffffffffffffffff] 0 00"
        )
    )

    @Test
    fun testIsInf() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isInf",
        Decimal::isInfinite,
        verbose = verbose
    )

    @Test
    fun testIsSignaling() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSignaling",
        Decimal::isSignaling,
        verbose = verbose
    )

    @Test
    fun testIsCanonical() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isCanonical",
        Decimal::isCanonical,
        verbose = verbose
    )


    @Test
    fun testIsCanonicalCases() = IntelRunner.runUnaryBooleanOp(
        Decimal::isCanonical,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isCanonical 0 [f800000001000000,0000000000000000] 0 00",
            "bid128_isCanonical 0 [f800000000000000,0000000000000000] 1 00",
            "bid128_isCanonical 0 [7c0013e87ada0359,835044d68d872147] 1 00",
        )
    )

    @Test
    fun testIsNaN() = IntelRunner.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isNaN",
        Decimal::isNaN,
        verbose = verbose
    )

    @Test
    fun testAbs() = IntelRunner.runUnaryDecimalOp(
        "/intel/readtest.in",
        "bid128_abs",
        Decimal::abs,
        verbose = verbose
    )



}