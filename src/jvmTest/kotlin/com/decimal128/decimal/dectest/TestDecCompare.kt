package com.decimal128.decimal.dectest

import com.decimal128.decimal.cmpImpl
import com.decimal128.decimal.cmpSignalingImpl
import com.decimal128.decimal.cmpTotalOrderImpl
import com.decimal128.decimal.cmpTotalOrderMagnitudeImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntCtxOp
import org.junit.jupiter.api.Test

class TestDecCompare {

    val verbose = true

    @Test
    fun testCompare() = runBinaryDecimalCtxOp(
        "dqCompare.dectest",
        "compare",
        ::cmpImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareCases() = runBinaryDecimalCtxOp(
        ::cmpImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqcom002 compare  -2  -1  -> -1",
            "dqcom101 compare   7.0    7      -> 0",
        )
    )

    @Test
    fun testCompareSignaling() = runBinaryDecimalCtxOp(
        "dqCompareSig.dectest",
        "comparesig",
        ::cmpSignalingImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareSignalingCases() = runBinaryDecimalCtxOp(
        ::cmpSignalingImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

    @Test
    fun testCompareTotalOrder() = runBinaryIntCtxOp(
        "dqCompareTotal.dectest",
        "comparetotal",
        ::cmpTotalOrderImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareTotalOrderCases() = runBinaryIntCtxOp(
        ::cmpTotalOrderImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqcot850 comparetotal  sNaN  NaN   ->  -1",
        )
    )

    @Test
    fun testCompareTotalOrderMagnitude() = runBinaryIntCtxOp(
        "dqCompareTotalMag.dectest",
        "comparetotmag",
        ::cmpTotalOrderMagnitudeImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareTotalOrderMagnitudeCases() = runBinaryIntCtxOp(
        ::cmpTotalOrderImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

}

