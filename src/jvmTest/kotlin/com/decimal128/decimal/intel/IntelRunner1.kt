package com.decimal128.decimal.intel

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import com.decimal128.decimal.with
import org.junit.jupiter.api.Assertions.assertEquals
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
            val ctx = DecContext.decimal128Kotlin().with(tc.decRounding())
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
            val ctx = DecContext.decimal128Kotlin()
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

    fun runBinaryDecimalCtxOp(fileName: String,
                              funcStr: String,
                              binaryDecimalOp: (Decimal, Decimal, DecContext) -> Decimal,
                              decContext: DecContext,
                              verbose: Boolean = false,
                              skip: Boolean = true,
                              skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryDecimalCtxOp(filtered, binaryDecimalOp, decContext, verbose)
    }

    fun runBinaryDecimalCtxOp(caseStrings: Array<String>,
                              binaryDecimalOp: (Decimal, Decimal, DecContext) -> Decimal,
                              decContext: DecContext,
                              verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryDecimalCtxOp(cases, binaryDecimalOp, decContext, verbose)
    }


    fun runBinaryDecimalCtxOp(cases: List<IntelCase1>,
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

    fun runUnaryDecimalCtxMethodOp(fileName: String,
                                   funcStr: String,
                                   unaryCtxMethodOp: Decimal.(DecContext) -> Decimal,
                                   decContext: DecContext,
                                   verbose: Boolean = false,
                                   skip: Boolean = true,
                                   skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryDecimalCtxMethodOp(filtered, unaryCtxMethodOp, decContext, verbose)
    }

    fun runUnaryDecimalCtxMethodOp(caseStrings: Array<String>,
                                   unaryCtxMethodOp: Decimal.(DecContext) -> Decimal,
                                   decContext: DecContext,
                                   verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runUnaryDecimalCtxMethodOp(cases, unaryCtxMethodOp, decContext, verbose)
    }


    fun runUnaryDecimalCtxMethodOp(cases: List<IntelCase1>,
                                   unaryCtxMethodOp: Decimal.(DecContext) -> Decimal,
                                   decContext: DecContext,
                                   verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = decContext.with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(decContext)
            if (verbose)
                println("op1:$op1 parsingFlags:${decContext.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.unaryCtxMethodOp(ctx)
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runUnaryLongCtxMethodOp(fileName: String,
                                funcStr: String,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Long,
                                decContext: DecContext,
                                verbose: Boolean = false,
                                skip: Boolean = true,
                                skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryLongCtxMethodOp(filtered, unaryCtxMethodOp, decContext, verbose)
    }

    fun runUnaryLongCtxMethodOp(caseStrings: Array<String>,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Long,
                                decContext: DecContext,
                                verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runUnaryLongCtxMethodOp(cases, unaryCtxMethodOp, decContext, verbose)
    }


    fun runUnaryLongCtxMethodOp(cases: List<IntelCase1>,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Long,
                                decContext: DecContext,
                                verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = decContext.with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(decContext)
            if (verbose)
                println("op1:$op1 parsingFlags:${decContext.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.unaryCtxMethodOp(ctx)
            val expected = tc.resLong
            assertEquals(expected, observed,
                "mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runUnaryIntCtxMethodOp(fileName: String,
                                funcStr: String,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Int,
                                decContext: DecContext,
                                verbose: Boolean = false,
                                skip: Boolean = true,
                                skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runUnaryIntCtxMethodOp(filtered, unaryCtxMethodOp, decContext, verbose)
    }

    fun runUnaryIntCtxMethodOp(caseStrings: Array<String>,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Int,
                                decContext: DecContext,
                                verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runUnaryIntCtxMethodOp(cases, unaryCtxMethodOp, decContext, verbose)
    }


    fun runUnaryIntCtxMethodOp(cases: List<IntelCase1>,
                                unaryCtxMethodOp: Decimal.(DecContext) -> Int,
                                decContext: DecContext,
                                verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = decContext.with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(decContext)
            if (verbose)
                println("op1:$op1 parsingFlags:${decContext.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.unaryCtxMethodOp(ctx)
            val expected = tc.resInt
            assertEquals(expected, observed,
                "mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runBinaryBooleanCtxMethodOp(fileName: String,
                                   funcStr: String,
                                   binaryCtxMethodOp: Decimal.(Decimal, DecContext) -> Boolean,
                                   decContext: DecContext,
                                   verbose: Boolean = false,
                                   skip: Boolean = true,
                                   skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryBooleanCtxMethodOp(filtered, binaryCtxMethodOp, decContext, verbose)
    }

    fun runBinaryBooleanCtxMethodOp(caseStrings: Array<String>,
                                    binaryCtxMethodOp: Decimal.(Decimal, DecContext) -> Boolean,
                                    decContext: DecContext,
                                   verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryBooleanCtxMethodOp(cases, binaryCtxMethodOp, decContext, verbose)
    }


    fun runBinaryBooleanCtxMethodOp(cases: List<IntelCase1>,
                                    binaryCtxMethodOp: Decimal.(Decimal, DecContext) -> Boolean,
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
            val observed = op1.binaryCtxMethodOp(op2, ctx)
            val expected = tc.resBoolean
            assertEquals(expected, observed,
                "mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runBinaryDecimalOp(fileName: String,
                           funcStr: String,
                           binaryDecimalOp: (Decimal, Decimal) -> Decimal,
                           verbose: Boolean = false,
                           skip: Boolean = true,
                           skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryDecimalOp(filtered, binaryDecimalOp, verbose)
    }

    fun runBinaryDecimalOp(caseStrings: Array<String>,
                           binaryDecimalOp: (Decimal, Decimal) -> Decimal,
                           verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryDecimalOp(cases, binaryDecimalOp, verbose)
    }


    fun runBinaryDecimalOp(cases: List<IntelCase1>,
                           binaryDecimalOp: (Decimal, Decimal) -> Decimal,
                           verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = DecContext.decimal128Kotlin()
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(ctx)
            val op2 = tc.op2Bid128(ctx)
            if (verbose)
                println("op1:$op1 op2:$op2 parsingFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = binaryDecimalOp(op1, op2)
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runBinaryDecimalMethodOp(fileName: String,
                                 funcStr: String,
                                 binaryDecimalMethodOp: Decimal.(Decimal) -> Decimal,
                                 verbose: Boolean = false,
                                 skip: Boolean = true,
                                 skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryDecimalMethodOp(filtered, binaryDecimalMethodOp, verbose)
    }

    fun runBinaryDecimalMethodOp(caseStrings: Array<String>,
                                 binaryDecimalMethodOp: Decimal.(Decimal) -> Decimal,
                                 verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryDecimalMethodOp(cases, binaryDecimalMethodOp, verbose)
    }


    fun runBinaryDecimalMethodOp(cases: List<IntelCase1>,
                                 binaryDecimalMethodOp: Decimal.(Decimal) -> Decimal,
                                 verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = DecContext.decimal128Kotlin()
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(ctx)
            val op2 = tc.op2Bid128(ctx)
            if (verbose)
                println("op1:$op1 op2:$op2 parsingFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.binaryDecimalMethodOp(op2)
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runDecimalIntMethodOp(fileName: String,
                              funcStr: String,
                              decimalIntMethodOp: Decimal.(Int, DecContext) -> Decimal,
                              verbose: Boolean = false,
                              skip: Boolean = true,
                              skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runDecimalIntMethodOp(filtered, decimalIntMethodOp, verbose)
    }

    fun runDecimalIntMethodOp(caseStrings: Array<String>,
                              decimalIntMethodOp: Decimal.(Int, DecContext) -> Decimal,
                              verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runDecimalIntMethodOp(cases, decimalIntMethodOp, verbose)
    }


    fun runDecimalIntMethodOp(cases: List<IntelCase1>,
                              decimalIntMethodOp: Decimal.(Int, DecContext) -> Decimal,
                              verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = DecContext.decimal128Kotlin().with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(ctx)
            val op2 = tc.op2Int()
            if (verbose)
                println("op1:$op1 op2:$op2 parsingFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.decimalIntMethodOp(op2, ctx)
            val expected = tc.resBid128(ctx)
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runIntMethodOp(fileName: String,
                       funcStr: String,
                       intMethodOp: Decimal.(DecContext) -> Int,
                       verbose: Boolean = false,
                       skip: Boolean = true,
                       skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runIntMethodOp(filtered, intMethodOp, verbose)
    }

    fun runIntMethodOp(caseStrings: Array<String>,
                       intMethodOp: Decimal.(DecContext) -> Int,
                       verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runIntMethodOp(cases, intMethodOp, verbose)
    }


    fun runIntMethodOp(cases: List<IntelCase1>,
                       intMethodOp: Decimal.(DecContext) -> Int,
                       verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = DecContext.decimal128Kotlin().with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(ctx)
            if (verbose)
                println("op1:$op1 parsingFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.intMethodOp(ctx)
            val expected = tc.resInt
            assertEquals(expected, observed,
                "mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }

    fun runBinaryBooleanMethodOp(fileName: String,
                                 funcStr: String,
                                 binaryBooleanMethodOp: Decimal.(Decimal) -> Boolean,
                                 verbose: Boolean = false,
                                 skip: Boolean = true,
                                 skipCases: Array<String> = emptyArray() ) {
        val cases = IntelParser1.parseTestsInFile(fileName)
        val skipSet: Set<String> = if (skip) skipCases.toSet() else emptySet()
        val filtered = cases.filter { it.funcStr == funcStr && !skipSet.contains(it.text)}
        runBinaryBooleanMethodOp(filtered, binaryBooleanMethodOp, verbose)
    }

    fun runBinaryBooleanMethodOp(caseStrings: Array<String>,
                                 binaryBooleanMethodOp: Decimal.(Decimal) -> Boolean,
                                 verbose: Boolean = false ) {
        val cases = IntelParser1.parseCases(caseStrings)
        runBinaryBooleanMethodOp(cases, binaryBooleanMethodOp, verbose)
    }


    fun runBinaryBooleanMethodOp(cases: List<IntelCase1>,
                                 binaryBooleanMethodOp: Decimal.(Decimal) -> Boolean,
                                 verbose: Boolean = false ) {
        cases.forEach { tc ->
            if (verbose) {
                println("test:${tc.text}")
            }
            val ctx = DecContext.decimal128Kotlin().with(tc.decRounding())
            ctx.decFlags.clearAll()
            val op1 = tc.op1Bid128(ctx)
            val op2 = tc.op2Bid128(ctx)
            if (verbose)
                println("op1:$op1 op2:$op2 parsingFlags:${ctx.decFlags}")
            ctx.decFlags.clearAll()
            val observed = op1.binaryBooleanMethodOp(op2)
            val expected = tc.resBoolean
            assertEquals(expected, observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            val expectedFlags = tc.decFlags()
            val observedFlags = ctx.decFlags
            assertEquals(expectedFlags.toString(), observedFlags.toString())
        }
    }


}