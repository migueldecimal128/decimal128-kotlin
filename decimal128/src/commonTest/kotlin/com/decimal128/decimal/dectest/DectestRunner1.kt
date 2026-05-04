package com.decimal128.decimal.dectest

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.Decimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.decimal128.decimal.loadTestResourceAsString

object DectestRunner1 {

    fun runUnaryDecimalOp(fileName: String,
                          opName: String,
                          unaryDecimalOp: Decimal.() -> Decimal,
                          verbose: Boolean = true,
                          skip: Boolean = true,
                          skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
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
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
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
                           opName: String,
                           binaryDecimalOp: Decimal.(Decimal) -> Decimal,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
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
                       opName: String,
                       binaryIntOp: Decimal.(Decimal) -> Int,
                       verbose: Boolean = true,
                       skip: Boolean = true,
                       skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
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
                           opName: String,
                           binaryBooleanOp: Decimal.(Decimal) -> Boolean,
                           verbose: Boolean = true,
                           skip: Boolean = true,
                           skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
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

    fun runUnaryDecimalCtxOp(fileName: String,
                             opName: String,
                             unaryStringCtxOp: Decimal.(DecContext) -> Decimal,
                             verbose: Boolean = true,
                             skip: Boolean = true,
                             skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
        runUnaryDecimalCtxOp(allTests, unaryStringCtxOp, skip, skipCases, verbose)
    }

    fun runUnaryDecimalCtxOp(unaryStringCtxOp: Decimal.(DecContext) -> Decimal,
                             verbose: Boolean = true,
                             cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runUnaryDecimalCtxOp(cases2, unaryStringCtxOp, verbose = verbose)
    }


    fun runUnaryDecimalCtxOp(cases: List<DectestCase1>,
                             unaryDecimalCtxOp: Decimal.(DecContext) -> Decimal,
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
            val decCtx = tc.decContext
            val observed = unaryDecimalCtxOp(operand1, decCtx)
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

    fun dectestMethod_Decimal(fileName: String,
                              opName: String,
                              method_Decimal: Decimal.() -> Decimal,
                              verbose: Boolean = true,
                              skip: Boolean = true,
                              skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
        dectestMethod_Decimal(allTests, method_Decimal, skip, skipCases, verbose)
    }

    fun dectestMethod_Decimal(method_Decimal: Decimal.() -> Decimal,
                              verbose: Boolean = true,
                              cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        dectestMethod_Decimal(cases2, method_Decimal, verbose = verbose)
    }


    fun dectestMethod_Decimal(cases: List<DectestCase1>,
                              method_Decimal: Decimal.() -> Decimal,
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
            val decCtx = tc.decContext
            decCtx.eval {
                val observed = operand1.method_Decimal()
                val expected = tc.result
                assertTrue(
                    expected bitwiseEQ observed,
                    "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
                )
                assertEquals(
                    tc.expectedDecFlags, decCtx.decFlags,
                    "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}"
                )
            }
        }
    }

    fun runUnaryStringCtxOp(fileName: String,
                            opName: String,
                            unaryStringCtxOp: (Decimal, DecContext) -> String,
                            formatStyleEngineering: Boolean = false,
                            verbose: Boolean = true,
                            skip: Boolean = true,
                            skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName, formatStyleEngineering)
        runUnaryStringCtxOp(allTests, unaryStringCtxOp, skip, skipCases, verbose)
    }

    fun runUnaryStringCtxOp(unaryStringCtxOp: (Decimal, DecContext) -> String,
                            formatStyleEngineering: Boolean = false,
                            verbose: Boolean = true,
                            cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases, formatStyleEngineering = formatStyleEngineering)
        runUnaryStringCtxOp(cases2, unaryStringCtxOp, verbose = verbose)
    }


    private fun runUnaryStringCtxOp(cases: List<DectestCase1>,
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
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
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
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
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
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
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

    fun runMethod_IntCtx_Decimal(fileName: String,
                                 opName: String,
                                 method: Decimal.(Int, DecContext) -> Decimal,
                                 verbose: Boolean = true,
                                 skip: Boolean = true,
                                 skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
        runMethod_IntCtx_Decimal(allTests, method, skip, skipCases, verbose)
    }

    fun runMethod_IntCtx_Decimal(method: Decimal.(Int, DecContext) -> Decimal,
                                 verbose: Boolean = true,
                                 cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        runMethod_IntCtx_Decimal(cases2, method, verbose = verbose)
    }


    fun runMethod_IntCtx_Decimal(cases: List<DectestCase1>,
                                 method: Decimal.(Int, DecContext) -> Decimal,
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
            val operand2 = tc.operand2Int
            val decCtx = tc.decContext
            val observed = operand1.method(operand2, decCtx)
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }

    fun dectestMethod_Int_Decimal(fileName: String,
                                  opName: String,
                                  method_Int_Decimal: Decimal.(Int) -> Decimal,
                                  verbose: Boolean = true,
                                  skip: Boolean = true,
                                  skipCases: Array<String> = arrayOf(),
    ) {
        val fileText = loadTestResourceAsString("/dectest/$fileName") ?: run {
            println("SKIPPED: $fileName not available on this platform")
            return
        }
        val allTests = DectestParser1.parse(fileText, opName)
        dectestMethod_Int_Decimal(allTests, method_Int_Decimal, skip, skipCases, verbose)
    }

    fun dectestMethod_Int_Decimal(method_Int_Decimal: Decimal.(Int) -> Decimal,
                                  verbose: Boolean = true,
                                  cases: Array<String> = emptyArray(),
    ) {
        val cases2 = DectestParser1.parse(cases)
        dectestMethod_Int_Decimal(cases2, method_Int_Decimal, verbose = verbose)
    }


    fun dectestMethod_Int_Decimal(cases: List<DectestCase1>,
                                  method_Int_Decimal: Decimal.(Int) -> Decimal,
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
            val operand2 = tc.operand2Int
            val decCtx = tc.decContext
            val observed = decCtx.eval {
                operand1.method_Int_Decimal(operand2)
            }
            val expected = tc.result
            assertTrue(expected bitwiseEQ observed,
                "bitwiseEQ mismatch expected=$expected observed=$observed for\n${tc.text}\n"
            )
            assertEquals(tc.expectedDecFlags, decCtx.decFlags,
                "flags mismatch expected=${tc.expectedDecFlags} observed=${decCtx.decFlags}")
        }
    }


}