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
    fun testAbs() = IntelRunner1.runBinaryDecimalOp(
        "/intel/readtest.in",
        "bid128_add",
        Decimal::plus,

        verbose = verbose
    )



}