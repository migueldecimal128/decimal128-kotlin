package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.intel.IntelRunner1.runUnaryIntCtxMethodOp
import com.decimal128.decimal.intel.IntelRunner1.runUnaryLongCtxMethodOp
import org.junit.jupiter.api.Test

class TestBid128Convert {

    val verbose = true

    @Test
    fun testConvertToLongTiesToEven(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_rnint",
        Decimal::convertToLongTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTiesToEvenCases(): Unit = runUnaryLongCtxMethodOp(
        arrayOf (
            "bid128_to_int64_rnint 0 [AFFE314DC6448D9338C15B0A00000000] -1 00 -- -(1)",
            "bid128_to_int64_rnint 0 [3023C6BF52633FFFFFFE3940AD9CC000] -9223372036854775808 01 -- 2^63-0.5",
            "bid128_to_int64_rnint 0 [2FFE49F4A966D45CD522088F00000000] 2 00 -- 1.5",
        ),
        Decimal::convertToLongTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTiesToAway(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_rninta",
        Decimal::convertToLongTiesToAway,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardZero(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_int",
        Decimal::convertToLongTowardZero,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardPositive(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_ceil",
        Decimal::convertToLongTowardPositive,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongTowardNegative(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_floor",
        Decimal::convertToLongTowardNegative,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToEven(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_xrnint",
        Decimal::convertToLongExactTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToEvenCases(): Unit = runUnaryLongCtxMethodOp(
        arrayOf (
            "bid128_to_int64_xrnint 0 1.0 1 00",
        ),
        Decimal::convertToLongExactTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTiesToAway(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_xrninta",
        Decimal::convertToLongExactTiesToAway,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardZero(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_xint",
        Decimal::convertToLongExactTowardZero,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardPositive(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_xceil",
        Decimal::convertToLongExactTowardPositive,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToLongExactTowardNegative(): Unit = runUnaryLongCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int64_xfloor",
        Decimal::convertToLongExactTowardNegative,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToEven(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_rnint",
        Decimal::convertToIntTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToEvenCases(): Unit = runUnaryIntCtxMethodOp(
        arrayOf (
            "bid128_to_int32_rnint 0 [30520000000000000000000000000005] -2147483648 01 -- 5e9",
            "bid128_to_int32_rnint 0 [301069E10DE628D3A6C9CC9B8E800001] 2147483647 00 -- 2^31-1.5+ulp",
            "bid128_to_int32_rnint 0 [2FFE314DC6448D9338C15B0A00000000] 1 00 -- 1",
        ),
        Decimal::convertToIntTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTiesToAway(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_rninta",
        Decimal::convertToIntTiesToAway,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardZero(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_int",
        Decimal::convertToIntTowardZero,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardPositive(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_ceil",
        Decimal::convertToIntTowardPositive,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntTowardNegative(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_floor",
        Decimal::convertToIntTowardNegative,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToEven(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_xrnint",
        Decimal::convertToIntExactTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToEvenCases(): Unit = runUnaryIntCtxMethodOp(
        arrayOf (
            "bid128_to_int32_xrnint 0 1.0 1 00",
        ),
        Decimal::convertToIntExactTiesToEven,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTiesToAway(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_xrninta",
        Decimal::convertToIntExactTiesToAway,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardZero(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_xint",
        Decimal::convertToIntExactTowardZero,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardPositive(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_xceil",
        Decimal::convertToIntExactTowardPositive,
        DecContext.DECIMAL128,
        verbose = verbose,
    )

    @Test
    fun testConvertToIntExactTowardNegative(): Unit = runUnaryIntCtxMethodOp(
        "/intel/readtest.in",
        "bid128_to_int32_xfloor",
        Decimal::convertToIntExactTowardNegative,
        DecContext.DECIMAL128,
        verbose = verbose,
    )


}