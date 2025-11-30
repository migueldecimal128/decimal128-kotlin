package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import kotlin.test.assertTrue

object DectestRunner {

    fun runUnaryDecimalOp(fileName: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = true,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        runUnaryDecimalOp(allTests, unaryDecimalOp, skip, skipCases, verbose)
    }

    fun runUnaryDecimalOp(unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = true,
                          cases: Array<String> = emptyArray(),
                          ) {
        val cases2 = DectestParser.parse(cases)
        runUnaryDecimalOp(cases2, unaryDecimalOp, verbose = verbose)
    }

    fun runUnaryDecimalOp(cases: List<DectestCase>,
                          unaryDecimalOp: Decimal.() -> Decimal,
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
}