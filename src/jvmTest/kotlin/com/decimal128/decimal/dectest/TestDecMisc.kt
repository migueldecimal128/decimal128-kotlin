package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalCtxOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.fmaImpl

class TestDecMisc {

    val verbose = false

    @Test
    fun testLogB() = runUnaryDecimalCtxOp(
        "dqLogB.dectest",
        "logb",
        Decimal::logB,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    // not fully implemented
    // @Test
    fun testNextUp() = runUnaryDecimalCtxOp(
        "dqNextPlus.dectest",
        "nextplus",
        Decimal::nextUp,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )


}

