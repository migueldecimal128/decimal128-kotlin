package com.decimal128.decimal.intel

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.Ieee754Class
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

object IntelRunner {

    fun runUnaryDecimalOp(cases: List<IntelCase>,
                          funcStr: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray(),
                          verbose: Boolean = false,
                          targetOnly: Boolean = false,
                          targetCases: Array<String> = emptyArray()) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val targetSet: Set<String> = if (targetOnly) targetCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr }
        filtered.forEach { tc ->
            val isTarget = targetSet.contains(tc.text)
            if (!isTarget && skipSet.contains(tc.text))
                return@forEach
            if (targetOnly && !isTarget)
                return@forEach
            if (verbose)
                println(tc.text)
            val observed = tc.op1Bid128.unaryDecimalOp()
            val expected = tc.resBid128
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
        }
    }

    fun runUnaryBooleanOp(cases: List<IntelCase>,
                          funcStr: String,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray(),
                          verbose: Boolean = false,
                          targetOnly: Boolean = false,
                          targetCases: Array<String> = emptyArray()) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val targetSet: Set<String> = if (targetOnly) targetCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr }
        var caseRunCount = 0
        filtered.forEach { tc ->
            val isTarget = targetSet.contains(tc.text)
            if (!isTarget && skipSet.contains(tc.text))
                return@forEach
            if (targetOnly && !isTarget)
                return@forEach
            if (verbose) {
                println(tc.text)
                println("  op1Bid128:${tc.op1Bid128}")
            }
            val observed = tc.op1Bid128.unaryBooleanOp()
            val expected = tc.resBoolean
            assertEquals(expected, observed)
            ++caseRunCount
        }
        if (verbose)
            println("$funcStr caseRunCount:$caseRunCount")
        assert(caseRunCount > 0)
    }

}