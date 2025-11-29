package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDectestNoncomputational {


    @Test
    fun testAbs() = testUnaryDecimalOp("dqAbs.dectest", Decimal::abs,
        verbose = true,
        skip = true,
        skipIds = arrayOf(
            // IEEE definition of abs() differs from Colishaw
            "dqabs523", // dqabs523 abs  sNaN    ->  NaN   Invalid_operation
            "dqabs525", // dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation
            "dqabs526", // dqabs526 abs  -NaN22  -> -NaN22
            "dqabs527", // dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation

            "dqabs900", // dqabs900 abs  # -> NaN Invalid_operation
        ),
        targetOnly = false,
        targetIds = arrayOf(
            "dqabs526", // all NaNs compare equals
        )
    )

    fun testUnaryDecimalOp(fileName: String,
                           unaryDecimalOp: Decimal.() -> Decimal,
                           skip: Boolean = true,
                           skipIds: Array<String> = arrayOf(),
                           verbose: Boolean = true,
                           targetOnly: Boolean = false,
                           targetIds: Array<String> = arrayOf()) {
        val fileText: String = DectestParser::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser.parse(fileText)
        val skipSet: Set<String> = if (skip) skipIds.toSet() else emptySet()
        val targetSet: Set<String> = if (targetOnly) targetIds.toSet() else emptySet()
        allTests.forEach { tc ->
            val operand1Str = tc.operand1Str
            val isTarget = targetSet.contains(tc.id)
            if (!isTarget && skipSet.contains(tc.id))
                return@forEach
            if (targetOnly && !isTarget)
                return@forEach
            if (verbose)
                println(tc.text)
            val observed = tc.operand1.unaryDecimalOp()
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }
}