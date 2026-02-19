package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import kotlin.test.Test

class TestBid128BasicArithmetic {

    val verbose = true

    /*****************************************************************************
     *
     *    BID128 basic arithmetic functions:
     *         - bid128_add
     ****************************************************************************/

    //@Test
    fun testAdd() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_add",
        Decimal::plus,
        skip = true,
        skipCases = arrayOf(
            // oversized coefficient of 35 digits in operand2
            // but my tests generally run with allowOversizedCoefficient = true
            // in order to catch other non-canonical values
            "bid128_add 0 [0000000000008000,004910c400000000] [5fe5f9ffd9ebcf7f,000404e2000600a0] [0000000000008000004910c400000000] 00",
            "bid128_add 0 [0001ed09bead87c0378d8e62ffffffff] [0001ed09bead87c0378d8e64ffffffff] [0001ed09bead87c0378d8e62ffffffff] 00",
            // oversize NaN payload
            "bid128_add 0 [0001ed09bead87c0378d8e62ffffffff] [7c003fffffffffff38c15b08ffffffff] [7c000000000000000000000000000000] 00",
            "bid128_add 0 [0001ed09bead87c0378d8e62ffffffff] [7c003fffffffffff38c15b0affffffff] [7c000000000000000000000000000000] 00",
        ),
        verbose = verbose
    )

    @Test
    fun testAddCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
        ),
        Decimal::plus,
        verbose = verbose
    )



}