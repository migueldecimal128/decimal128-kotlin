package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal2
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object DectestRunner {

    fun runUnaryDecimalOp(fileName: String,
                          unaryDecimalOp: Decimal2.() -> Decimal2,
                          verbose: Boolean = true,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runUnaryDecimalOp(allTests, unaryDecimalOp, skip, skipCases, verbose)
    }

    fun runUnaryDecimalOp(unaryDecimalOp: Decimal2.() -> Decimal2,
                          verbose: Boolean = true,
                          cases: Array<String> = emptyArray(),
                          ) {
        val cases2 = DectestParser.parse(cases)
        runUnaryDecimalOp(cases2, unaryDecimalOp, verbose = verbose)
    }

    fun runUnaryDecimalOp(cases: List<DectestCase>,
                          unaryDecimalOp: Decimal2.() -> Decimal2,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
                          verbose: Boolean = true,
                          ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        cases.forEach { tc ->
            if (skipSet.contains(tc.text))
                return@forEach
            if (verbose)
                println(tc.text)
            val operand1 = tc.operand1
            val observed = operand1.unaryDecimalOp()
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }

    fun runUnaryIntOp(fileName: String,
                      unaryIntOp: Decimal2.() -> Int,
                      verbose: Boolean = true,
                      skip: Boolean = true,
                      skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runUnaryIntOp(allTests, unaryIntOp, skip, skipCases, verbose)
    }

    fun runUnaryIntOp(unaryIntOp: Decimal2.() -> Int,
                      verbose: Boolean = true,
                      cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser.parse(cases)
        runUnaryIntOp(cases2, unaryIntOp, verbose = verbose)
    }

    fun runUnaryIntOp(cases: List<DectestCase>,
                      unaryIntOp: Decimal2.() -> Int,
                      skip: Boolean = true,
                      skipCases: Array<String> = arrayOf(),
                      verbose: Boolean = true,
    ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        cases.forEach { tc ->
            if (skipSet.contains(tc.text))
                return@forEach
            if (verbose)
                println(tc.text)
            val operand1 = tc.operand1
            val observed = operand1.unaryIntOp()
            val expected = tc.resultInt
            assertEquals(expected, observed)
        }
    }

    fun runBinaryDecimalOp(fileName: String,
                           binaryDecimalOp: Decimal2.(Decimal2) -> Decimal2,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runBinaryDecimalOp(allTests, binaryDecimalOp, skip, skipCases, verbose)
    }

    fun runBinaryDecimalOp(binaryDecimalOp: Decimal2.(Decimal2) -> Decimal2,
                           verbose: Boolean = true,
                           cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser.parse(cases)
        runBinaryDecimalOp(cases2, binaryDecimalOp, verbose = verbose)
    }

    fun runBinaryDecimalOp(cases: List<DectestCase>,
                           binaryDecimalOp: Decimal2.(Decimal2) -> Decimal2,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
                           verbose: Boolean = true,
    ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        cases.forEach { tc ->
            if (skipSet.contains(tc.text))
                return@forEach
            if (verbose)
                println(tc.text)
            val operand1 = tc.operand1
            val operand2 = tc.operand2
            val observed = operand1.binaryDecimalOp(operand2)
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }

    fun runBinaryIntOp(fileName: String,
                       binaryIntOp: Decimal2.(Decimal2) -> Int,
                       verbose: Boolean = true,
                       skip: Boolean = true,
                       skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runBinaryIntOp(allTests, binaryIntOp, skip, skipCases, verbose)
    }

    fun runBinaryIntOp(binaryIntOp: Decimal2.(Decimal2) -> Int,
                       verbose: Boolean = true,
                       cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser.parse(cases)
        runBinaryIntOp(cases2, binaryIntOp, verbose = verbose)
    }

    fun runBinaryIntOp(cases: List<DectestCase>,
                       binaryIntOp: Decimal2.(Decimal2) -> Int,
                       skip: Boolean = true,
                       skipCases: Array<String> = arrayOf(),
                       verbose: Boolean = true,
    ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        cases.forEach { tc ->
            if (skipSet.contains(tc.text))
                return@forEach
            if (verbose)
                println(tc.text)
            val operand1 = tc.operand1
            val operand2 = tc.operand2
            val observed = operand1.binaryIntOp(operand2)
            val expected = tc.resultInt
            assertEquals(expected, observed)
        }
    }

    fun runBinaryBooleanOp(fileName: String,
                           binaryBooleanOp: Decimal2.(Decimal2) -> Boolean,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runBinaryBooleanOp(allTests, binaryBooleanOp, skip, skipCases, verbose)
    }

    fun runBinaryBooleanOp(binaryBooleanOp: Decimal2.(Decimal2) -> Boolean,
                           verbose: Boolean = true,
                           cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser.parse(cases)
        runBinaryBooleanOp(cases2, binaryBooleanOp, verbose = verbose)
    }

    fun runBinaryBooleanOp(cases: List<DectestCase>,
                           binaryBooleanOp: Decimal2.(Decimal2) -> Boolean,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
                           verbose: Boolean = true,
    ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        cases.forEach { tc ->
            if (skipSet.contains(tc.text))
                return@forEach
            if (verbose)
                println(tc.text)
            val operand1 = tc.operand1
            val operand2 = tc.operand2
            val observed = operand1.binaryBooleanOp(operand2)
            val expected = tc.resultBoolean
            assertEquals(expected, observed)
        }
    }


}