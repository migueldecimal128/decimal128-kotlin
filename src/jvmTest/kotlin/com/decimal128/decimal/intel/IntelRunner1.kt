package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertTrue

object IntelRunner1 {

    fun runUnaryDecimalOp(fileName: String,
                          funcStr: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryDecimalOp(filtered, unaryDecimalOp, verbose)
    }

    fun runUnaryDecimalOp(cases: List<IntelCase1>,
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
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryBooleanOp(filtered, unaryBooleanOp, verbose)
    }

    fun runUnaryBooleanOp(cases: List<IntelCase1>,
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
        runUnaryBooleanOp(IntelParser1.parseCases(cases), unaryBooleanOp, verbose)

    fun runUnaryBooleanOp(cases: List<IntelCase1>,
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

    fun runBinaryDecimalOp(fileName: String,
                           funcStr: String,
                           binaryDecimalOp: Decimal.(Decimal) -> Decimal,
                           verbose: Boolean = false,
                           skip: Boolean = true,
                           skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryDecimalOp(filtered, binaryDecimalOp, verbose)
    }

    fun runBinaryDecimalOp(caseStrings: Array<String>,
                           binaryDecimalOp: Decimal.(Decimal) -> Decimal,
                           verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryDecimalOp(cases, binaryDecimalOp, verbose)
    }


    fun runBinaryDecimalOp(cases: List<IntelCase1>,
    binaryDecimalOp: Decimal.(Decimal) -> Decimal,
    verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val observed = tc.op1Bid128.binaryDecimalOp(tc.op2Bid128)
            val expected = tc.resBid128
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }


}