package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.addImpl
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryBooleanOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalOp
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryIntOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp

class TestDectestBasicArithmetic {

    val verbose = true

    @Test
    fun testAdd() = runBinaryDecimalCtxOp(
        "dqAdd.dectest",
        "add",
        ::addImpl,
        verbose = verbose
    )

    @Test
    fun testAddCases() = runBinaryDecimalCtxOp(
        ::addImpl,
        verbose = verbose,
        cases = arrayOf(
            "rounding: half_up",
            "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded"
        )
    )

}

