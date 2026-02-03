package com.decimal128.decimal.dectest

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object DectestRunner1 {

    fun runUnaryDecimalOp(fileName: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = true,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText)
        runUnaryDecimalOp(allTests, unaryDecimalOp, skip, skipCases, verbose)
    }

    fun runUnaryDecimalOp(unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = true,
                          cases: Array<String> = emptyArray(),
                          ) {
        val cases2 = DectestParser1.parse(cases)
        runUnaryDecimalOp(cases2, unaryDecimalOp, verbose = verbose)
    }

    fun runUnaryDecimalOp(cases: List<DectestCase1>,
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

    fun runUnaryIntOp(fileName: String,
                      unaryIntOp: Decimal.() -> Int,
                      verbose: Boolean = true,
                      skip: Boolean = true,
                      skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText)
        runUnaryIntOp(allTests, unaryIntOp, skip, skipCases, verbose)
    }

    fun runUnaryIntOp(unaryIntOp: Decimal.() -> Int,
                      verbose: Boolean = true,
                      cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runUnaryIntOp(cases2, unaryIntOp, verbose = verbose)
    }

    fun runUnaryIntOp(cases: List<DectestCase1>,
                      unaryIntOp: Decimal.() -> Int,
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
                           binaryDecimalOp: Decimal.(Decimal) -> Decimal,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText)
        runBinaryDecimalOp(allTests, binaryDecimalOp, skip, skipCases, verbose)
    }

    fun runBinaryDecimalOp(binaryDecimalOp: Decimal.(Decimal) -> Decimal,
                           verbose: Boolean = true,
                           cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runBinaryDecimalOp(cases2, binaryDecimalOp, verbose = verbose)
    }

    fun runBinaryDecimalOp(cases: List<DectestCase1>,
                           binaryDecimalOp: Decimal.(Decimal) -> Decimal,
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
                       binaryIntOp: Decimal.(Decimal) -> Int,
                       verbose: Boolean = true,
                       skip: Boolean = true,
                       skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText)
        runBinaryIntOp(allTests, binaryIntOp, skip, skipCases, verbose)
    }

    fun runBinaryIntOp(binaryIntOp: Decimal.(Decimal) -> Int,
                       verbose: Boolean = true,
                       cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runBinaryIntOp(cases2, binaryIntOp, verbose = verbose)
    }

    fun runBinaryIntOp(cases: List<DectestCase1>,
                       binaryIntOp: Decimal.(Decimal) -> Int,
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
                           binaryBooleanOp: Decimal.(Decimal) -> Boolean,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText)
        runBinaryBooleanOp(allTests, binaryBooleanOp, skip, skipCases, verbose)
    }

    fun runBinaryBooleanOp(binaryBooleanOp: Decimal.(Decimal) -> Boolean,
                           verbose: Boolean = true,
                           cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runBinaryBooleanOp(cases2, binaryBooleanOp, verbose = verbose)
    }

    fun runBinaryBooleanOp(cases: List<DectestCase1>,
                           binaryBooleanOp: Decimal.(Decimal) -> Boolean,
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

    fun runUnaryStringCtxOp(fileName: String,
                            opName: String,
                            unaryStringCtxOp: (Decimal, DecContext) -> String,
                            verbose: Boolean = true,
                            skip: Boolean = true,
                            skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText, opName)
        runUnaryStringCtxOp(allTests, unaryStringCtxOp, skip, skipCases, verbose)
    }

    fun runUnaryStringCtxOp(unaryStringCtxOp: (Decimal, DecContext) -> String,
                            verbose: Boolean = true,
                            cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runUnaryStringCtxOp(cases2, unaryStringCtxOp, verbose = verbose)
    }


    fun runUnaryStringCtxOp(cases: List<DectestCase1>,
                            unaryStringCtxOp: (Decimal, DecContext) -> String,
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
            val decCtx = tc.decContext
            val observed = unaryStringCtxOp(operand1, decCtx)
            val expected = tc.resultStr
            assertEquals(expected, observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

    fun runBinaryDecimalCtxOp(fileName: String,
                              opName: String,
                              binaryDecimalCtxOp: (Decimal, Decimal, DecContext) -> Decimal,
                              verbose: Boolean = true,
                              skip: Boolean = true,
                              skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText, opName)
        runBinaryDecimalCtxOp(allTests, binaryDecimalCtxOp, skip, skipCases, verbose)
    }

    fun runBinaryDecimalCtxOp(binaryDecimalCtxOp: (Decimal, Decimal, DecContext) -> Decimal,
                              verbose: Boolean = true,
                              cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runBinaryDecimalCtxOp(cases2, binaryDecimalCtxOp, verbose = verbose)
    }

    fun runBinaryDecimalCtxOp(cases: List<DectestCase1>,
                              binaryDecimalCtxOp: (Decimal, Decimal, DecContext) -> Decimal,
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
            val decCtx = tc.decContext
            val observed = binaryDecimalCtxOp(operand1, operand2, decCtx)
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

    fun runBinaryIntCtxOp(fileName: String,
                          opName: String,
                          binaryIntCtxOp: (Decimal, Decimal, DecContext) -> Int,
                          verbose: Boolean = true,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText, opName)
        runBinaryIntCtxOp(allTests, binaryIntCtxOp, skip, skipCases, verbose)
    }

    fun runBinaryIntCtxOp(binaryDecimalCtxOp: (Decimal, Decimal, DecContext) -> Int,
                          verbose: Boolean = true,
                          cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runBinaryIntCtxOp(cases2, binaryDecimalCtxOp, verbose = verbose)
    }


    fun runBinaryIntCtxOp(cases: List<DectestCase1>,
                          binaryIntCtxOp: (Decimal, Decimal, DecContext) -> Int,
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
            val decCtx = tc.decContext
            val observed = binaryIntCtxOp(operand1, operand2, decCtx)
            val expected = tc.resultInt
            assertEquals(expected, observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

    fun runTernaryDecimalCtxOp(fileName: String,
                               opName: String,
                               ternaryDecimalCtxOp: (Decimal, Decimal, Decimal, DecContext) -> Decimal,
                               verbose: Boolean = true,
                               skip: Boolean = true,
                               skipCases: Array<String> = arrayOf(),
    ) {
        val fileText: String = DectestParser1::class.java.getResource("/dectest/$fileName")!!.readText()
        val allTests = DectestParser1.parse(fileText, opName)
        runTernaryDecimalCtxOp(allTests, ternaryDecimalCtxOp, skip, skipCases, verbose)
    }

    fun runTernaryDecimalCtxOp(ternaryDecimalCtxOp: (Decimal, Decimal, Decimal, DecContext) -> Decimal,
                               verbose: Boolean = true,
                               cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runTernaryDecimalCtxOp(cases2, ternaryDecimalCtxOp, verbose = verbose)
    }

    fun runTernaryDecimalCtxOp(cases: List<DectestCase1>,
                               ternaryDecimalCtxOp: (Decimal, Decimal, Decimal, DecContext) -> Decimal,
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
            val operand3 = tc.operand3
            val decCtx = tc.decContext
            val observed = ternaryDecimalCtxOp(operand1, operand2, operand3, decCtx)
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

}