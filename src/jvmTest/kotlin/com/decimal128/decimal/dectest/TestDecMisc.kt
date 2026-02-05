package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.fmaImpl

class TestDecMisc {

    val verbose = true

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

    @Test
    fun testNextUp() = runUnaryDecimalCtxOp(
        "dqNextPlus.dectest",
        "nextplus",
        Decimal::nextUp,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testNextUpCases() = runUnaryDecimalCtxOp(
        Decimal::nextUp,
        verbose = verbose,
        cases = arrayOf(
            "dqnextp151 nextplus  -Inf    -> -9.999999999999999999999999999999999E+6144",
            "dqnextp104 nextplus  0E+30000    ->  1E-6176",
            "dqnextp026 nextplus -1.000000000000000000000000000000000  ->  -0.9999999999999999999999999999999999",
        )
    )

    @Test
    fun testNextDown() = runUnaryDecimalCtxOp(
        "dqNextMinus.dectest",
        "nextminus",
        Decimal::nextDown,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

}

