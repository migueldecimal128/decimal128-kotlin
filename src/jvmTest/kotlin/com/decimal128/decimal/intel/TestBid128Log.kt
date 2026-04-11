package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Decimal
import org.junit.jupiter.api.Test

class TestBid128Log {

    val verbose = true

    @Test
    fun testLn(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_log",
        Decimal::ln,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testLog10(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_log10",
        Decimal::log10,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testLog10Cases(): Unit = intelMethod_Decimal(
        arrayOf(
            "bid128_log10 0 [5FFFED09BEAD87C0378D8E63FFFFFFFF] [30052EF8CDE0236B3CC2E3EA40000000] 20 ulp=-4.3429448190e-05",
        ),
        Decimal::log10,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )


}