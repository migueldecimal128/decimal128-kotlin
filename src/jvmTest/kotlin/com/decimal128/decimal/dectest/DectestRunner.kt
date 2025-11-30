package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import kotlin.test.assertTrue

object DectestRunner {

    fun runUnaryDecimalOp(fileName: String,
                           unaryDecimalOp: Decimal.() -> Decimal,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
                           verbose: Boolean = true,
                           targetOnly: Boolean = false,
                           targetCases: Array<String> = arrayOf()) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val targetSet: Set<String> = if (targetOnly) targetCases.toSet() else emptySet()
        allTests.forEach { tc ->
            val operand1Str = tc.operand1Str
            val isTarget = targetSet.contains(tc.text)
            if (!isTarget && skipSet.contains(tc.text))
                return@forEach
            if (targetOnly && !isTarget)
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
}