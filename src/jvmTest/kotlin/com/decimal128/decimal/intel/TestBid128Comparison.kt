package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Decimal_Boolean
import org.junit.jupiter.api.Test

class TestBid128Comparison {

    val verbose = false

    @Test
    fun testCompareQuietEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_equal",
        Decimal::compareQuietEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietGreater() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_greater",
        Decimal::compareQuietGreater,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietGreaterEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_greater_equal",
        Decimal::compareQuietGreaterEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietLess() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_less",
        Decimal::compareQuietLess,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietLessEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_less_equal",
        Decimal::compareQuietLessEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietLessUnordered() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_less_unordered",
        Decimal::compareQuietLessUnordered,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_not_equal",
        Decimal::compareQuietNotEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotGreater() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_not_greater",
        Decimal::compareQuietNotGreater,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotLess() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_not_less",
        Decimal::compareQuietNotLess,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietOrdered() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_ordered",
        Decimal::compareQuietOrdered,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareQuietUnordered() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_quiet_unordered",
        Decimal::compareQuietUnordered,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreater() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_greater",
        Decimal::compareSignalingGreater,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreaterEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_greater_equal",
        Decimal::compareSignalingGreaterEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreaterUnordered() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_greater_unordered",
        Decimal::compareSignalingGreaterUnordered,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLess() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_less",
        Decimal::compareSignalingLess,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLessEqual() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_less_equal",
        Decimal::compareSignalingLessEqual,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLessUnordered() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_less_unordered",
        Decimal::compareSignalingLessUnordered,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingNotGreater() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_not_greater",
        Decimal::compareSignalingNotGreater,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )

    @Test
    fun testCompareSignalingNotLess() = intelMethod_Decimal_Boolean(
        "/intel/readtest.in",
        "bid128_signaling_not_less",
        Decimal::compareSignalingNotLess,
        DecContext.decimal128Kotlin(),
        verbose = verbose
    )


}