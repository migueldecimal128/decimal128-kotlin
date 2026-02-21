package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.DecPrefs
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import kotlin.test.Test

class TestBid128BasicArithmetic {

    val verbose = true

    val decPrefs = DecPrefs().copy(propagatePreferSnan = false)
    val decContext = DecContext(decPrefs = decPrefs)

    /*****************************************************************************
     *
     *    BID128 basic arithmetic functions:
     *         - bid128_add
     ****************************************************************************/

    @Test
    fun testAdd() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_add",
        ::addImpl,
        skip = true,
        skipCases = arrayOf(
        ),
        decContext = decContext,
        verbose = verbose
    )

    @Test
    fun testAddCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
            "bid128_add 1 [0001ed09bead87c0378d8e62ffffffff] [0001ed09bead87c0378d8e62ffffffff] [0002629b8c891b267182b613cccccccc] 20",
            "bid128_add 0 -67742893945653349875463748543548.9E-6184 +1100.0100110001101010E-6045 [00ca363c140ab6aa266b6f4aea488000] 20",
            "bid128_add 0 +101001100000101.000000E6138 -7695957767658598867966685688.99E6120 [7c000000000000000000000000000000] 01",
            "bid128_add 0 [0001ed09bead87c0378d8e62ffffffff] [7c003fffffffffff38c15b08ffffffff] [7c000000000000000000000000000000] 00",
            "bid128_add 0 [0000000000000000,5dfecf59bad3acaa] [4014d000d4008a04,ffffffddfdfdfeff] [4014d000d4008a04ffffffddfdfdfeff] 20",
        ),
        ::addImpl,
        decContext = decContext,
        verbose = verbose,
    )



}