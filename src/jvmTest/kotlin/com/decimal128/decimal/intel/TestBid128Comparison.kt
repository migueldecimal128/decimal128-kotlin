package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Test

class TestBid128Comparison {

    val verbose = true

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
}