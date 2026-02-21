package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.DecContext.Companion.DECIMAL128
import com.decimal128.decimal.DecFlags
import com.decimal128.decimal.DecPrefs
import com.decimal128.decimal.Decimal
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.math.exp
import kotlin.test.assertTrue

object IntelRunner1 {

    fun runUnaryDecimalOp(fileName: String,
                          funcStr: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          allowNonCanonical: Boolean,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryDecimalOp(filtered, unaryDecimalOp, allowNonCanonical, verbose)
    }

    fun runUnaryDecimalOp(cases: List<IntelCase1>,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          allowNonCanonical: Boolean = false,
                          verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose)
                println(tc.text)
            val decPrefs = DecPrefs().copy(decodeBidAllowNonCanonical = allowNonCanonical)
            val ctx = DECIMAL128.with(decPrefs).withNewFlags().with(tc.decRounding())
            val op1 = tc.op1Bid128(ctx)
            if (verbose)
                println("op1:$op1 decFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.unaryDecimalOp()
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runUnaryBooleanOp(fileName: String,
                          funcStr: String,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          allowNonCanonical: Boolean,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryBooleanOp(filtered, unaryBooleanOp, allowNonCanonical, verbose)
    }

    fun runUnaryBooleanOp(cases: List<IntelCase1>,
                          funcStr: String,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          allowNonCanonical: Boolean = false,
                          verbose: Boolean = false,
                          skip: Boolean = true,
                          skipCases: Array<String> = emptyArray() ) {
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryBooleanOp(filtered, unaryBooleanOp, allowNonCanonical, verbose)
    }

    fun runUnaryBooleanOp(unaryBooleanOp: Decimal.() -> Boolean,
                          allowNonCanonical: Boolean,
                          verbose: Boolean = false,
                          cases: Array<String> ) =
        runUnaryBooleanOp(IntelParser1.parseCases(cases), unaryBooleanOp, allowNonCanonical, verbose)

    fun runUnaryBooleanOp(cases: List<IntelCase1>,
                          unaryBooleanOp: Decimal.() -> Boolean,
                          allowNonCanonical: Boolean,
                          verbose: Boolean = false ) {
        cases.forEach { tc ->
            val decPrefs = DecPrefs().copy(decodeBidAllowNonCanonical = allowNonCanonical)
            val ctx = DECIMAL128.with(decPrefs).withNewFlags()
            val op1 = tc.op1Bid128(ctx)
            if (verbose) {
                println(tc.text)
                println("op1=$op1")
            }
            val observed = op1.unaryBooleanOp()
            val expected = tc.resBoolean
            assertEquals(expected, observed)
        }
    }

    fun runBinaryDecimalOp(fileName: String,
                           funcStr: String,
                           binaryDecimalOp: (Decimal, Decimal, DecContext) -> Decimal,
                           decContext: DecContext,
                           verbose: Boolean = false,
                           skip: Boolean = true,
                           skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryDecimalOp(filtered, binaryDecimalOp, decContext, verbose)
    }

    fun runBinaryDecimalOp(caseStrings: Array<String>,
                           binaryDecimalOp: (Decimal, Decimal, DecContext) -> Decimal,
                           decContext: DecContext,
                           verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryDecimalOp(cases, binaryDecimalOp, decContext, verbose)
    }


    fun runBinaryDecimalOp(cases: List<IntelCase1>,
                           binaryDecimalOp: (Decimal, Decimal, DecContext) -> Decimal,
                           decContext: DecContext,
                           verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = decContext.with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(decContext)
            val op2 = tc.op2Bid128(decContext)
            if (verbose)
                println("op1:$op1 op2:$op2 parsingFlags:${decContext.decFlags}")
            ctx.decFlags.clearAll()
            val observed = binaryDecimalOp(op1, op2, ctx)
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }


}