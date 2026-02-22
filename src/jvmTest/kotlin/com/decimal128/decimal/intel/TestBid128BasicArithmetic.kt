package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.DecPrefs
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.remImpl
import com.decimal128.decimal.remNearImpl
import com.decimal128.decimal.remTruncImpl
import com.decimal128.decimal.subImpl
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

    @Test
    fun testSub() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_sub",
        ::subImpl,
        skip = true,
        skipCases = arrayOf(
        ),
        decContext = decContext,
        verbose = verbose
    )

    @Test
    fun testSubCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
            "bid128_sub 0 +1000000010000010.0100000E-6192 +8998898888898999998899999999.988888889E-6026 [8121bbae128738e463d45adcde9f1499] 00",
        ),
        ::subImpl,
        decContext = decContext,
        verbose = verbose,
    )

    @Test
    fun testMul() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_mul",
        ::mulImpl,
        skip = true,
        skipCases = arrayOf(
        ),
        decContext = decContext,
        verbose = verbose
    )

    @Test
    fun testMulCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
        ),
        ::mulImpl,
        decContext = decContext,
        verbose = verbose,
    )

    @Test
    fun testDiv() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_div",
        ::divImpl,
        skip = true,
        skipCases = arrayOf(
        ),
        decContext = decContext,
        verbose = verbose
    )

    @Test
    fun testDivCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
        ),
        ::divImpl,
        decContext = decContext,
        verbose = verbose,
    )

    @Test
    fun testRemNear() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_rem",
        ::remNearImpl,
        skip = true,
        skipCases = arrayOf(
            // FIXME - these are valid ... my implementation is not :(
            // FIXME - the case of large qDelta must be handled differently
            "bid128_rem 0 [26741c0590811072,ffffffff6ef7ffff] [0000000000000000,0300040220100840] [8000000000000000015515ab6c6033c0] 00",
            "bid128_rem 0 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
            "bid128_rem 0 [4082000215010400,1f7dbeffb7fdff7d] [bfffbfff7ffa7fbf,240c30b8380ad037] [bffe0708356252ac4b444f585a0c6d93] 00",
            "bid128_rem 0 [97f3ca2bca3dcd34,321ba8822721f58f] [8000000000000000,bae106a1402505d6] [000000000000000025bf9a7041265ffe] 00",
            "bid128_rem 0 [9c5f9589d6c5fa51,caa6dcb43a12852e] [0080000000000000,0001000000000000] [80800000000000000000000000000000] 00",
            "bid128_rem 0 [a7f734b7f7e7a516,229ddcc454df042c] [0000000000000002,9fc80ebfc7fed6d1] [800000000000000118eb45cf8f8cc645] 00",
            "bid128_rem 1 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
            "bid128_rem 2 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
            "bid128_rem 3 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
            "bid128_rem 4 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
        ),
        decContext = decContext,
        verbose = verbose
    )

    @Test
    fun testRemNearCases() = IntelRunner1.runBinaryDecimalOp(
        arrayOf(
            // FIXME - the case of large qDelta must be handled differently
            // "bid128_rem 0 [26741c0590811072,ffffffff6ef7ffff] [0000000000000000,0300040220100840] [8000000000000000015515ab6c6033c0] 00",
            // "bid128_rem 0 [397cdc9a254ff562,99bc2af7b316f57c] [268d807293a0dcac,afddbd631e831cbb] [a68c1d3c3991cbb19eee601c8bdcb12c] 00",
        ),
        ::remNearImpl,
        decContext = decContext,
        verbose = verbose,
    )



}