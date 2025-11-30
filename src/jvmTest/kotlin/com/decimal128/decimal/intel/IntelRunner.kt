package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.Ieee754Class
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

object IntelRunner {

    fun runUnaryDecimalOp(fileName: String,
                          funcStr: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryDecimalOp(filtered, unaryDecimalOp, verbose)
    }

    fun runUnaryDecimalOp(cases: List<IntelCase>,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose)
                println(tc.text)
            val observed = tc.op1Bid128.unaryDecimalOp()
            val expected = tc.resBid128
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }

    fun runUnaryBooleanOp(fileName: String,
                          funcStr: String,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryBooleanOp(filtered, unaryBooleanOp, verbose)
    }

    fun runUnaryBooleanOp(cases: List<IntelCase>,
                          funcStr: String,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryBooleanOp(filtered, unaryBooleanOp, verbose)
    }

    fun runUnaryBooleanOp(unaryBooleanOp: Decimal.() -> Boolean,
                          verbose: Boolean = false,
                          cases: Array<String> ) =
        runUnaryBooleanOp(IntelParser.parseCases(cases), unaryBooleanOp, verbose)

    fun runUnaryBooleanOp(cases: List<IntelCase>,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println(tc.text)
                println("op1=${tc.op1Bid128}")
            }
            val observed = tc.op1Bid128.unaryBooleanOp()
            val expected = tc.resBoolean
            assertEquals(expected, observed)
        }
    }

}