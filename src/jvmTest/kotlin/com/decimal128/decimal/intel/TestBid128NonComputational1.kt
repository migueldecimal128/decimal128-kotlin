package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import kotlin.test.Test

class TestBid128NonComputational1 {

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
    fun testIsSigned() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSigned",
        Decimal::isNegative,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsNormal() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isNormal",
        Decimal::isNormal,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsNormalCases() = IntelRunner1.runUnaryBooleanOp(
        Decimal::isNormal,
        allowNonCanonical = true,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isNormal 0 [03f4000000000000,0000000000000000] 0 00",
            "bid128_isNormal 0 [0001ed09bead87c0378d8e64ffffffff] 0 00",
        )
    )


    @Test
    fun testIsSubnormal() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSubnormal",
        Decimal::isSubnormal,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsFinite() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isFinite",
        Decimal::isFinite,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsZero() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isZero",
        Decimal::isCanonicalZero,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsZeroCases() = IntelRunner1.runUnaryBooleanOp(
        Decimal::isCanonicalZero,
        allowNonCanonical = true,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isZero 0 [0001ed09bead87c0378d8e64ffffffff] 1 00",
            "bid128_isZero 0 [0001ed09bead87c0378d8e62ffffffff] 0 00",
            "bid128_isZero 0 [789b88be70d10384,ffffffffffffffff] 0 00"
        )
    )

    @Test
    fun testIsInf() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isInf",
        Decimal::isInfinite,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsSignaling() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isSignaling",
        Decimal::isSignaling,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testIsSignalingCases() = IntelRunner1.runUnaryBooleanOp(
        Decimal::isSignaling,
        allowNonCanonical = true,
        verbose = verbose,
        cases = arrayOf(
            "bid128_isSignaling 0 [fe00000000000000,0000000000000000] 1 00",
        )
    )

    @Test
    fun testIsCanonical() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isCanonical",
        Decimal::isCanonical,
        allowNonCanonical = true,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "bid128_isCanonical 0 [ffffffffffffffff,1000000000000000] 0 00",
            "bid128_isCanonical 0 [fe00381d0020a920,ff6f3fff9ff3cd7e] 0 00",
            "bid128_isCanonical 0 [f810000000000000,0000000000000000] 0 00",
            "bid128_isCanonical 0 [7c003fffffffffff38c15b0affffffff] 0 00",
            "bid128_isCanonical 0 [6085008102161490,ffdffeeff7ffbfff] 0 00",
            "bid128_isCanonical 0 [0001ed09bead87c0378d8e64ffffffff] 0 00",
            "bid128_isCanonical 0 [7c003fffffffffff38c15b08ffffffff] 0 00",
            "bid128_isCanonical 0 [fa79d291c68723e9,bf36ffd4dbefc63f] 0 00",
            "bid128_isCanonical 0 [f800000001000000,0000000000000000] 0 00",
        )
    )


    @Test
    fun testIsCanonicalCases() = IntelRunner1.runUnaryBooleanOp(
        Decimal::isCanonical,
        allowNonCanonical = true,
        verbose = verbose,
        cases = arrayOf(
//            "bid128_isCanonical 0 [7c003fffffffffff38c15b08ffffffff] 0 00",
//            "bid128_isCanonical 0 [fa79d291c68723e9,bf36ffd4dbefc63f] 0 00",
//            "bid128_isCanonical 0 [f800000001000000,0000000000000000] 0 00",
            "bid128_isCanonical 0 [f800000000000000,0000000000000000] 1 00",
            "bid128_isCanonical 0 [7c0013e87ada0359,835044d68d872147] 1 00",
        )
    )

    @Test
    fun testIsNaN() = IntelRunner1.runUnaryBooleanOp(
        "/intel/readtest.in",
        "bid128_isNaN",
        Decimal::isNaN,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testAbs() = IntelRunner1.runUnaryDecimalOp(
        "/intel/readtest.in",
        "bid128_abs",
        Decimal::abs,
        allowNonCanonical = true,
        verbose = verbose
    )

    @Test
    fun testCopySign() = IntelRunner1.runBinaryDecimalMethodOp(
        "/intel/readtest.in",
        "bid128_copySign",
        Decimal::copySign,
        verbose = verbose
    )

    @Test
    fun testTotalOrder() = IntelRunner1.runBinaryBooleanMethodOp(
        "/intel/readtest.in",
        "bid128_totalOrder",
        Decimal::isTotalOrder,
        verbose = verbose
    )

    @Test
    fun testTotalOrderMag() = IntelRunner1.runBinaryBooleanMethodOp(
        "/intel/readtest.in",
        "bid128_totalOrderMag",
        Decimal::isTotalOrderMag,
        verbose = verbose
    )

    @Test
    fun testSameQuantum() = IntelRunner1.runBinaryBooleanMethodOp(
        "/intel/readtest.in",
        "bid128_sameQuantum",
        Decimal::isSameQuantum,
        verbose = verbose
    )

}
