package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Decimal
import org.junit.jupiter.api.Test

class TestBid128Log {

    val verbose = false

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

    @Test
    fun testExp(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_exp",
        Decimal::exp,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            // Intel thinks these cases are exact.
            // Observe that ulp=0.0 and flags are 00
            // However, for all finite non-zero values x exp(x) is INEXACT.
            // Looks like a lot of dups, but rounding mode is different.
            "bid128_exp 0 [00000000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 0 [00000000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 0 [0000106F4216D9DBBD95C90355555555] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 0 [00420000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 0 [00420000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 1 [00000000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 1 [00000000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 1 [0000106F4216D9DBBD95C90355555555] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 1 [00420000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 1 [00420000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 2 [00000000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 2 [00000000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 2 [0000106F4216D9DBBD95C90355555555] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 2 [00420000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 2 [00420000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 3 [00000000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 3 [00000000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 3 [0000106F4216D9DBBD95C90355555556] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 3 [00420000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
            "bid128_exp 3 [00420000000000000000000000000003] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
        )
    )

    @Test
    fun testExpCases(): Unit = intelMethod_Decimal(
        arrayOf(
            "bid128_exp 0 [B00645BC590568678A9BCA554875C90B] [0000629B8C891B267182B614000008FD] 20 ulp=4.5098188902e-01",
            "bid128_exp 0 [5FFFED09BEAD87C0378D8E63FFFFFFFF] [78000000000000000000000000000000] 28 ulp=0.0000000000e-01",
            // skipped ... see above
            //"bid128_exp 0 [00000000000000000000000000000001] [2FFE314DC6448D9338C15B0A00000000] 00 ulp=0.0000000000e-01",
        ),
        Decimal::exp,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testExp10(): Unit = intelMethod_Decimal(
        "/intel/readtest.in",
        "bid128_exp10",
        Decimal::exp10,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            // Intel signals INEXACT, but they should also be signaling UNDERFLOW
            // see IEEE754-2019 7.5
            "bid128_exp10 0 [b0058bf72fa4eea6e98489819368194d] [00000000000000000000000000000000] 20",
            "bid128_exp10 0 [b005db5e877e758bfe53dd5e06733086] [00000000000000000000000000000000] 20",
            "bid128_exp10 0 [b005b1d75299a5aa378acaa543a24b82] [00000000000000000000000000000000] 20",
            "bid128_exp10 0 [b0053af01b8a0125ac5437185b9f74bb] [00000000000000000000000000000000] 20",
            "bid128_exp10 0 [b0057e657e0fef7b8b7572acc164e1f3] [00000000000000000000000000000000] 20",
            "bid128_exp10 0 [b00552f25788a4e163d781ec08d2be04] [00000000000000000000000000000000] 20",
            // Intel should signaling OVERFLOW in addition to INEXACT
            "bid128_exp10 0 [3005da8d9306b37a08cd74840506b073] [78000000000000000000000000000000] 20",
            "bid128_exp10 0 [30059144adac675798f2764aabb361f2] [78000000000000000000000000000000] 20",
            "bid128_exp10 0 [3005967f3b6dc2b69607e01ce2a77a34] [78000000000000000000000000000000] 20",
        )
    )

    @Test
    fun testExp10Cases(): Unit = intelMethod_Decimal(
        arrayOf(
            "bid128_exp10 0 [b000aa9eeb9637ce97ce137590ff2e75] [2fb87a25091fa9548163398ab6dadb5e] 20",
        ),
        Decimal::exp10,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )
}