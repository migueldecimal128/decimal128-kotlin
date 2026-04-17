package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Int
import com.decimal128.decimal.intel.IntelRunner1.intelMethod_Long
import kotlin.test.Test

class TestBid128Convert {

    val verbose = false

    @Test
    fun testConvertToLongTiesToEven(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_rnint",
        Decimal::toLongTiesToEven,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTiesToEvenCases(): Unit = intelMethod_Long(
        arrayOf (
            "bid128_to_int64_rnint 0 [0000000000000000,0000000000000000] 0 00",
            "bid128_to_int64_rnint 0 [AFFE314DC6448D9338C15B0A00000000] -1 00 -- -(1)",
            "bid128_to_int64_rnint 0 [3023C6BF52633FFFFFFE3940AD9CC000] -9223372036854775808 01 -- 2^63-0.5",
            "bid128_to_int64_rnint 0 [2FFE49F4A966D45CD522088F00000000] 2 00 -- 1.5",
        ),
        Decimal::toLongTiesToEven,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTiesToAway(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_rninta",
        Decimal::toLongTiesToAway,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardZero(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_int",
        Decimal::toLongTowardZero,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardPositive(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_ceil",
        Decimal::toLongTowardPositive,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardNegative(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_floor",
        Decimal::toLongTowardNegative,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToEven(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_xrnint",
        Decimal::toLongTiesToEvenSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToEvenCases(): Unit = intelMethod_Long(
        arrayOf (
            "bid128_to_int64_xrnint 0 1.0 1 00",
        ),
        Decimal::toLongTiesToEvenSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToAway(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_xrninta",
        Decimal::toLongTiesToAwaySignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardZero(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_xint",
        Decimal::toLongTowardZeroSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardPositive(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_xceil",
        Decimal::toLongTowardPositiveSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardNegative(): Unit = intelMethod_Long(
        "/intel/readtest.in",
        "bid128_to_int64_xfloor",
        Decimal::toLongTowardNegativeSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToEven(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_rnint",
        Decimal::toIntTiesToEven,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToEvenCases(): Unit = intelMethod_Int(
        arrayOf (
            "bid128_to_int32_rnint 0 [30520000000000000000000000000005] -2147483648 01 -- 5e9",
            "bid128_to_int32_rnint 0 [301069E10DE628D3A6C9CC9B8E800001] 2147483647 00 -- 2^31-1.5+ulp",
            "bid128_to_int32_rnint 0 [2FFE314DC6448D9338C15B0A00000000] 1 00 -- 1",
        ),
        Decimal::toIntTiesToEven,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToAway(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_rninta",
        Decimal::toIntTiesToAway,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardZero(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_int",
        Decimal::toIntTowardZero,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardPositive(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_ceil",
        Decimal::toIntTowardPositive,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardNegative(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_floor",
        Decimal::toIntTowardNegative,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToEven(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_xrnint",
        Decimal::toIntTiesToEvenSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToEvenCases(): Unit = intelMethod_Int(
        arrayOf (
            "bid128_to_int32_xrnint 0 1.0 1 00",
        ),
        Decimal::toIntTiesToEvenSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToAway(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_xrninta",
        Decimal::toIntTiesToAwaySignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardZero(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_xint",
        Decimal::toIntTowardZeroSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardPositive(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_xceil",
        Decimal::toIntTowardPositiveSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardNegative(): Unit = intelMethod_Int(
        "/intel/readtest.in",
        "bid128_to_int32_xfloor",
        Decimal::toIntTowardNegativeSignalInexact,
        DecContext.decimal128Kotlin(),
        verbose = verbose,
    )


}