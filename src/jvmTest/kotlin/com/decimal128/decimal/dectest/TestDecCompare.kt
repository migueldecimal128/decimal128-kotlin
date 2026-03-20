package com.decimal128.decimal.dectest

import com.decimal128.decimal.cmpImpl
import com.decimal128.decimal.cmpSignalingImpl
import com.decimal128.decimal.cmpTotalOrderImpl
import com.decimal128.decimal.cmpTotalOrderMagnitudeImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test

class TestDecCompare {

    val verbose = false

    @Test
    fun testCompare() = runBinaryDecimalCtxOp(
        "dqCompare.decTest",
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
        "dqCompareSig.decTest",
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
            "dqcms781 comparesig  Inf  -1000  ->  1",
        )
    )

    @Test
    fun testCompareTotalOrder() = runBinaryIntOp(
        "dqCompareTotal.decTest",
        "comparetotal",
        ::cmpTotalOrderImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareTotalOrderCases() = runBinaryIntOp(
        ::cmpTotalOrderImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqcot850 comparetotal  sNaN  NaN   ->  -1",
        )
    )

    @Test
    fun testCompareTotalOrderMagnitude() = runBinaryIntOp(
        "dqCompareTotalMag.decTest",
        "comparetotmag",
        ::cmpTotalOrderMagnitudeImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testCompareTotalOrderMagnitudeCases() = runBinaryIntOp(
        ::cmpTotalOrderImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

}

