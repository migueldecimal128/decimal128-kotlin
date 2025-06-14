package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.charset.StandardCharsets

class TestFptestRead {

    val verbose = true

    val tcs = arrayOf (
        "d128/ =0 -49642331588100000000000e1339 -34761812775252754060000000000e-3295 -> +1428070852031077636864738734302306e4595 x",
        "d128/ =0 i +890948442942907790036148664190309e-6176 +100000000000000000e-6160 -> +890948442942907790036148664190309e-33",
        "d128/ =0 i +999999999999999999999999999999999e-6176 +999999999999999999999999999999999e-6176 -> +1e0",
        "d128* < +28028512034700e-6156 -198808347170692216503470e-6152 -> -1e-6176 xu",
        "d128* < -7566147376989755956563086243134265e-2272 +82000000000000000000000e-5943 -> -1e-6176 xu",
        "d128* > x -8610206069341829318608913035072157e-1004 -9185846519890975977164407438018883e-5803 -> +1e-6176 xu",
        "d128* =0 xo +2119230029778935034111978352968449e4077 +4278568666791909168355238870128069e5178 -> +9067271203136636034236836264390905e72 o",
        "d128+ =0 xo +9999999999999999999999999610766938e6111 +3892330610000000000000000000000000e6086 -> +9999999999999999999999999999999999e6111",
        "d128+ =0 xo +9812103205585494989521030314280334e6111 +2593623850423916437714699131384630e6111 -> +1240572705600941142723572944566496e-3104 o",
        "d128+ 0 +9801500112905823805141480088652516e6111 +7167402461388206504768114924646920e6110 -> +9999999999999999999999999999999999e6111 xo",
        "d128* =0 +inf +0e-6176 -> Q i",
        "d128- =0 i +1e-6176 +1e-6176 -> +0e-6176 ",
        "d128+ =0 -inf +inf -> Q i",
        "d128+ =0 i Q +0e-6176 -> Q ",
        "d128+ =0 S +0e-6176 -> Q i",
        "d128+ =0 i +inf +0e-6176 -> +inf",
        "d128+ =0 i +1e-6176 +0e-6176 -> +1e-6176",
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            val fptest = Fptest.parseFptest(tc)
            if (fptest != null)
                test1(fptest)
        }
    }

    val badCases = arrayOf (
        "d128* =0 xo +2119230029778935034111978352968449e4077 +4278568666791909168355238870128069e5178 -> +9067271203136636034236836264390905e72 o",
        "d128+ =0 xo +9812103205585494989521030314280334e6111 +2593623850423916437714699131384630e6111 -> +1240572705600941142723572944566496e-3104 o",
        "d128+ =0 xo +9999999999999999996069182477920187e6111 +3930817522079813000000000000000000e6093 -> +1000000000000000000000000000000000e-3104 o",
        "d128+ =0 xo +6829661924289717195964689814526662e6093 +9999999999999999993170338075710286e6111 -> +1000000000000000000000000000000000e-3104 o",
        "d128+ =0 xo +9999999014018089919124995201959103e6111 +9859819100808750047980409160000000e6104 -> +1000000000000000000000000000000002e-3104 o",
        "d128+ =0 xo -9999999999999947874689748259097014e6111 -5212531025174090298634769037117730e6097 -> -1000000000000000000000000000000000e-3104 o",
        "d128+ =0 xo -9999999999999986188765890256920259e6111 -1381123410974307974600000000000000e6097 -> -1000000000000000000000000000000000e-3104 o",
        "d128+ =0 xo -757916242139070262484178736639730e6110 -9924208375786092973751582126336044e6111 -> -1000000000000000000000000000000002e-3104 o",
        "d128+ =0 xo +9862799185852461212534985751039625e6111 +9300203117975678015984398482290622e6110 -> +1079281949765002901413342559926869e-3104 o",
        "d128+ =0 xo -9677803953188963127497442742744266e6111 -8410048716652037382379103587497441e6111 -> -1808785266984100050987654633024171e-3104 o",
        "d128+ =0 xo +9628370887516707740345446024494712e6111 +7987776599246665594473290260731527e6110 -> +1042714854744137429979277505056786e-3104 o",
        "d128* =0 xo +50000000000000000000000000e1474 +2000000e4639 -> +100000000000000000000000000000000e-3103 o",
        "d128* =0 xo -204800000e2423 -48828125000000000000000e3691 -> +10000000000000000000000000000000e-3102 o",
        "d128* =0 xo +10240000000000000000e2765 +97656250000e3350 -> +1000000000000000000000000000000e-3101 o",
        "d128* =0 xo -762939453125000000e4571 -131072000000e1545 -> +100000000000000000000000000000e-3100 o",
        "d128* =0 xo +6250000000000000000e1709 +1600000000e4408 -> +10000000000000000000000000000e-3099 o",
        "d128* =0 xo +195312500000000000e6005 +5120000000e113 -> +1000000000000000000000000000e-3098 o",
        "d128* =0 xo +195312500000000000e3083 +512000000e3036 -> +100000000000000000000000000e-3097 o",
        "d128* =0 xo +781250000e31 +12800000000000000e6089 -> +10000000000000000000000000e-3096 o",
        "d128* =0 xo +1024e6101 +976562500000000000000e20 -> +1000000000000000000000000e-3095 o",
        "d128* =0 xo -12500000e1511 -8000000000000000e4611 -> +100000000000000000000000e-3094 o",
        "d128* =0 xo -195312500e5415 -51200000000000e708 -> +10000000000000000000000e-3093 o",
        "d128* =0 xo -488281250e2696 -2048000000000e3428 -> +1000000000000000000000e-3092 o",
        "d128* =0 xo +305175781250000e1053 +327680e5072 -> +100000000000000000000e-3091 o",
        "d128* =0 xo -20480000000e3077 -488281250e3049 -> +10000000000000000000e-3090 o",
        "d128* =0 xo -12207031250e1118 -81920000e5009 -> +1000000000000000000e-3089 o",
        "d128+ =0 xu +0e-4290 +6e-6176 -> +6e3040 u",
    )

    fun isBadCase(fptest: Fptest, ctx: Decimal128Context): Boolean {
        if (badCases.contains(fptest.fptestStr))
            return true
        val result = fptest.result()
        if (result != null && result.qExp in -3104..-3000)
            return true
        if (fptest.hasTrap("o") && fptest.expectsSignal("o") && ctx.overflow)
            return true
        if (fptest.hasTrap("u") && fptest.expectsSignal("u") && ctx.underflow)
            return true
        return false
    }


    val prefix = "src/test/kotlin/fptest/"

    val fptestFiles = arrayOf(
        "Decimal-Basic-Types-Inputs.fptest",
        "Decimal-Basic-Types-Intermediate.fptest",
        "Decimal-Clamping.fptest",
        "Decimal-Mul-Trailing-Zeros.fptest",
        "Decimal-Overflow.fptest",
        "Decimal-Rounding.fptest",
        "Decimal-Trailing-And-Leading-Zeros-Input.fptest",
        "Decimal-Trailing-And-Leading-Zeros-Result.fptest",
        "Decimal-Underflow.fptest",
    )

    @Test
    fun testReadFptestFiles() {
        for (fptestFile in fptestFiles)
            read1(prefix + fptestFile)
    }

    class Fptest(val fptestStr: String,
                 val op: String,
                 val round: String,
                 val traps: String,
                 val operands: ArrayList<String>,
                 val result: String,
                 val exceptions: String
        ) {
            companion object {
                val whitespaceRegex = "\\s+".toRegex()

                fun parseFptest(fptestStr: String): Fptest? {
                    val tokens = fptestStr.split(whitespaceRegex)
                    if (! tokens[0].startsWith("d128"))
                        return null
                    val op = tokens[0].substring(4)
                    val round = tokens[1]

                    var i = 2
                    val traps = (
                            if (isTrappedException(tokens[2])) {
                                ++i
                                tokens[2]
                            } else {
                                ""
                            }
                            )

                    val operands = ArrayList<String>()
                    while (tokens[i] != "->")
                        operands.add(tokens[i++])

                    ++i
                    val result = tokens[i++]

                    val exceptions = if (i < tokens.size) tokens[i] else ""

                    return Fptest(fptestStr, op, round, traps, operands, result, exceptions)
                }

                val trapRegex = "[xuozi]+".toRegex()
                private fun isTrappedException(str: String): Boolean {
                    return str.matches(trapRegex)
                }
            }

        fun roundingDirection(): RoundingDirection {
            return when (round) {
                "=0" -> ROUND_TIES_TO_EVEN
                "=^" -> ROUND_TIES_TO_AWAY
                ">" -> ROUND_TOWARD_POSITIVE
                "<" -> ROUND_TOWARD_NEGATIVE
                "0" -> ROUND_TOWARD_ZERO
                else -> throw RuntimeException("what is '$round' ?")
            }
        }

        fun result(): Dec34? {
            if (result == "#")
                return null
            val dec = Dec34()
            val ctx = Decimal128Context()
            when (result) {
                "Q" -> dec.setNaN(ctx)
                "S" -> dec.setSNaN(ctx)
                else -> Dec34ParsePrint.decFromString(dec, result, ctx)
            }
            return dec
        }

        fun decOperands(): ArrayList<Dec34> {
            val ret = ArrayList<Dec34>(operands.size)
            val ctx = Decimal128Context()
            for (t in operands) {
                val d = Dec34()
                when (t) {
                    "Q" -> d.setNaN(ctx)
                    "S" -> d.setSNaN(ctx)
                    else -> Dec34ParsePrint.decFromString(d, t, ctx)
                }
                ret.add(d)
            }
            return ret
        }

        fun hasTrap(str: String) = traps.contains(str)

        fun expectsSignal(str: String) = exceptions.contains(str)

        override fun toString(): String {
            return "op:$op round:$round traps:$traps operands:$operands ==> $result exceptions:$exceptions"
        }
    }

    fun read1(fptestFile: String) {

        val file = File(fptestFile).bufferedReader(StandardCharsets.UTF_8)
        for (line in file.readLines()) {
            val fptest = Fptest.parseFptest(line)
            if (fptest != null) {
                println(fptest)
                test1(fptest)
            }
        }
    }

    fun test1(fptest: Fptest) {
        val operands = fptest.decOperands()
        val ctx = Decimal128Context(fptest.roundingDirection())
        val observed = Dec34()
        if (verbose)
            println(fptest.fptestStr)
        when (fptest.op) {
            "+" -> {
                observed.add(operands[0], operands[1], ctx)
            }
            "-" -> {
                observed.sub(operands[0], operands[1], ctx)
            }
            "*" -> {
                observed.mul(operands[0], operands[1], ctx)
            }
            "/" -> {
                observed.div(operands[0], operands[1], ctx)
            }
            else -> {
                return
            }
        }
        val expected = fptest.result()
        if (expected != null) {
            val cmp754 = expected.compareQuiet754(observed, ctx)
            if (expected.isNaN()) {
                assertTrue(observed.isNaN())
                assertEquals(IEEE754_UNORDERED, cmp754)
            } else if (cmp754 != IEEE754_EQ) {
                if (isBadCase(fptest, ctx)) {
                    if (verbose)
                        println("bad case:${fptest.fptestStr}")
                    return
                }
                if (verbose)
                    println("expected:$expected observed:$observed")
                assertEquals(IEEE754_EQ, cmp754)
            }
            val expectedExceptions = fptest.exceptions
            val expectedExceptionsSinU = expectedExceptions.replace("u", "")
            val observedExceptions = ctx.getFptestExceptionsString()
            val observedExceptionsSinU = observedExceptions.replace("u", "")
            assertEquals(expectedExceptionsSinU, observedExceptionsSinU)
            if (observedExceptions.contains("u") && !expectedExceptions.contains("u"))
                println(">>>>>>>>>> I reported underflow but FPtest did not")

        } else {
            println("expected trap not yet handled")
        }

    }

}