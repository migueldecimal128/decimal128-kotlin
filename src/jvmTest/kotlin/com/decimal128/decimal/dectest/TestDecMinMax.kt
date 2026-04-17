package com.decimal128.decimal.dectest

import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import kotlin.test.Test
import com.decimal128.decimal.d128MaxImpl
import com.decimal128.decimal.d128MaxNumImpl
import com.decimal128.decimal.d128MinImpl
import com.decimal128.decimal.d128MinNumImpl

class TestDecMinMax {

    val verbose = false

    @Test
    fun testMax() = runBinaryDecimalCtxOp(
        "dqMax.decTest",
        "max",
        ::d128MaxImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqmax141 max  NaN -Inf    -> -Infinity",
            "dqmax142 max  NaN -1000   -> -1000",
            "dqmax143 max  NaN -1      -> -1",
            "dqmax144 max  NaN -0      -> -0",
            "dqmax145 max  NaN  0      ->  0",
            "dqmax146 max  NaN  1      ->  1",
            "dqmax147 max  NaN  1000   ->  1000",
            "dqmax148 max  NaN  Inf    ->  Infinity",
            "dqmax150 max -Inf  NaN    -> -Infinity",
            "dqmax151 max -1000 NaN    -> -1000",
            "dqmax152 max -1    NaN    -> -1",
            "dqmax153 max -0    NaN    -> -0",
            "dqmax154 max  0    NaN    ->  0",
            "dqmax155 max  1    NaN    ->  1",
            "dqmax156 max  1000 NaN    ->  1000",
            "dqmax157 max  Inf  NaN    ->  Infinity",
            "dqmax181 max  NaN9  -Inf   -> -Infinity",
            "dqmax182 max  NaN8     9   ->  9",
            "dqmax183 max -NaN7   Inf   ->  Infinity",
            "dqmax188 max -Inf    NaN4  -> -Infinity",
            "dqmax189 max -9     -NaN3  -> -9",
            "dqmax190 max  Inf    NaN2  ->  Infinity",
            "dqmax284 max '7' 'NaN'  ->  '7'",
        )
    )

    @Test
    fun testMaxCases() = runBinaryDecimalCtxOp(
        ::d128MaxImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqmax407 max  0.10   0       ->  0.10",
        )
    )

    @Test
    fun testMaxNumber() = runBinaryDecimalCtxOp(
        "dqMax.decTest",
        "max",
        ::d128MaxNumImpl,
        verbose = verbose,
    )

    @Test
    fun testMaxNumCases() = runBinaryDecimalCtxOp(
        ::d128MaxNumImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

    @Test
    fun testMin() = runBinaryDecimalCtxOp(
        "dqMin.decTest",
        "min",
        ::d128MinImpl,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqmin141 min  NaN -Inf    ->  -Infinity",
            "dqmin142 min  NaN -1000   ->  -1000",
            "dqmin143 min  NaN -1      ->  -1",
            "dqmin144 min  NaN -0      ->  -0",
            "dqmin145 min  NaN  0      ->  0",
            "dqmin146 min  NaN  1      ->  1",
            "dqmin147 min  NaN  1000   ->  1000",
            "dqmin148 min  NaN  Inf    ->  Infinity",
            "dqmin150 min -Inf  NaN    -> -Infinity",
            "dqmin151 min -1000 NaN    -> -1000",
            "dqmin152 min -1   -NaN    -> -1",
            "dqmin153 min -0    NaN    -> -0",
            "dqmin154 min  0   -NaN    ->  0",
            "dqmin155 min  1    NaN    ->  1",
            "dqmin156 min  1000 NaN    ->  1000",
            "dqmin157 min  Inf  NaN    ->  Infinity",
            "dqmin181 min  NaN9   -Inf   -> -Infinity",
            "dqmin182 min -NaN8    9990  ->  9990",
            "dqmin183 min  NaN71   Inf   ->  Infinity",
            "dqmin188 min -Inf     NaN41 -> -Infinity",
            "dqmin189 min -9999   -NaN33 -> -9999",
            "dqmin190 min  Inf     NaN2  ->  Infinity",
            "dqmin284 min '7' 'NaN'  ->  '7'",
        )
    )

    @Test
    fun testMinCases() = runBinaryDecimalCtxOp(
        ::d128MinImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )

    @Test
    fun testMinNumber() = runBinaryDecimalCtxOp(
        "dqMin.decTest",
        "min",
        ::d128MinNumImpl,
        verbose = verbose,
    )

    @Test
    fun testMinNumCases() = runBinaryDecimalCtxOp(
        ::d128MinNumImpl,
        verbose = verbose,
        cases = arrayOf(
        )
    )


}

