package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import com.decimal128.decimal.divImpl
import com.decimal128.decimal.maxImpl
import com.decimal128.decimal.mulImpl
import com.decimal128.decimal.subImpl

class TestDectestMinMax {

    val verbose = true

    @Test
    fun testMax() = runBinaryDecimalCtxOp(
        "dqMax.dectest",
        "max",
        ::maxImpl,
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
        ::maxImpl,
        verbose = verbose,
        cases = arrayOf(
            "dqmax407 max  0.10   0       ->  0.10",
        )
    )


}

