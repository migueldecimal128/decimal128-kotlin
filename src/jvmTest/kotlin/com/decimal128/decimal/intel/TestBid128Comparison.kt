package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Test

class TestBid128Comparison {

    val verbose = false

    @Test
    fun testCompareQuietEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_equal",
        Decimal::compareQuietEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietGreater() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_greater",
        Decimal::compareQuietGreater,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietGreaterEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_greater_equal",
        Decimal::compareQuietGreaterEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietLess() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_less",
        Decimal::compareQuietLess,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietLessEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_less_equal",
        Decimal::compareQuietLessEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietLessUnordered() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_less_unordered",
        Decimal::compareQuietLessUnordered,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_not_equal",
        Decimal::compareQuietNotEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotGreater() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_not_greater",
        Decimal::compareQuietNotGreater,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietNotLess() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_not_less",
        Decimal::compareQuietNotLess,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietOrdered() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_ordered",
        Decimal::compareQuietOrdered,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareQuietUnordered() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_quiet_unordered",
        Decimal::compareQuietUnordered,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreater() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_greater",
        Decimal::compareSignalingGreater,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreaterEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_greater_equal",
        Decimal::compareSignalingGreaterEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingGreaterUnordered() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_greater_unordered",
        Decimal::compareSignalingGreaterUnordered,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLess() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_less",
        Decimal::compareSignalingLess,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLessEqual() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_less_equal",
        Decimal::compareSignalingLessEqual,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingLessUnordered() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_less_unordered",
        Decimal::compareSignalingLessUnordered,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingNotGreater() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_not_greater",
        Decimal::compareSignalingNotGreater,
        DecContext.DECIMAL128,
        verbose = verbose
    )

    @Test
    fun testCompareSignalingNotLess() = IntelRunner1.runBinaryBooleanCtxMethodOp(
        "/intel/readtest.in",
        "bid128_signaling_not_less",
        Decimal::compareSignalingNotLess,
        DecContext.DECIMAL128,
        verbose = verbose
    )


}