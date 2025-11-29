package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import kotlin.test.Test

class TestBid128NonComputational {

    val verbose = true

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
        getAllCases(),
        "bid128_isSigned",
        Decimal::isNegative,
        verbose = verbose
    )

    @Test
    fun testIsNormal() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isNormal",
        Decimal::isNormal,
        verbose = verbose
    )

    @Test
    fun testIsSubnormal() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isSubnormal",
        Decimal::isSubnormal,
        verbose = verbose
    )

    @Test
    fun testIsFinite() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isFinite",
        Decimal::isFinite,
        verbose = verbose
    )

    @Test
    fun testIsZero() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isZero",
        Decimal::isZero,
        verbose = verbose
    )

    @Test
    fun testIsInf() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isInf",
        Decimal::isInfinite,
        verbose = verbose
    )

    @Test
    fun testIsSignaling() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isSignaling",
        Decimal::isSignaling,
        verbose = verbose
    )

    @Test
    fun testIsCanonical() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isCanonical",
        Decimal::isCanonical,
        verbose = verbose
    )

    @Test
    fun testIsNaN() = IntelRunner.runUnaryBooleanOp(
        getAllCases(),
        "bid128_isNaN",
        Decimal::isNaN,
        verbose = verbose
    )

    @Test
    fun testAbs() = IntelRunner.runUnaryDecimalOp(
        getAllCases(),
        "bid128_abs",
        Decimal::abs,
        verbose = verbose
    )

    private var allCases: List<IntelCase>? = null

    fun getAllCases(): List<IntelCase> {
        var cases = allCases
        if (cases == null) {
            val fileText = IntelParser::class.java.getResource("/intel/readtest.in")!!.readText()
            cases = IntelParser.parseAllCases(fileText)
            allCases = cases
        }
        return cases
    }


}