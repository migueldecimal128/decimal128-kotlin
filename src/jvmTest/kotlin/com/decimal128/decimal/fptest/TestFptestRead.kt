package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.io.PrintWriter

private val captureOperandText = false
private val operandCaptureFilename = "src/jvmTest/resources/fptestOperandsRaw.txt"
private var operandPrintWriter: PrintWriter? = null


class TestFptestRead {

    val verbose = false

    val tcs = arrayOf (
        "d64* =0 i +0e-398 +0e-398 -> +0e-398",

        // I created this case from a bad case ... so I don't know if u flag is correct or not
        "d64+ 0 xu +6e-398 +0 -> +6e-398 u",

        // this case is decimal64 subnormal, so there are only 15 digits in coefficient
        "d64* =0 +354384610389529e-97 -74e-303 -> -262244611688251e-398 xu",

        "d128/ =0 -49642331588100000000000e1339 -34761812775252754060000000000e-3295 -> +1428070852031077636864738734302306e4595 x",

        "d64* =0 i -160000e-271 -625000000e-141 -> +1e-398",
        "d64+ =0 i +1e-398 +0e-398 -> +1e-398 ",
        "d128+ =0 xu -5414267123623525953667080660113040e-6176 +5414267123623525953667080660113039e-6176 -> -1e3040 u",
        "d64+ =0 xu -2073006932212847e-398 +1599988532498004e-398 -> -473018399714843e178 u",
        "d64+ =0 xu -2073006932212847e-398 +1599988532498004e-398 -> -473018399714843e178 u",
        "d64+ =0 xu -817274705688422e-398 -182725294311576e-398 -> -999999999999998e178 u",
        "d64+ =0 xu +1e-398 +0e119 -> +1e178 u",
        "d64+ =0 xu +663702891003955e-398 +336297108996043e-398 -> +999999999999998e178 u",
        "d64+ =0 xu -0e-185 -1e-398 -> -1e178 u",
        "d64+ =0 xu -0e88 -6e-398 -> -6e178 u",
        "d64+ =0 xu +0e22 +1e-398 -> +1e178 u",
        "d64+ =0 xu +987285451156649e-397 -9872854511566485e-398 -> +5e178 u",
        "d128* < +28028512034700e-6156 -198808347170692216503470e-6152 -> -1e-6176 xu",
        "d64+ =0 i -1e-398 +0e-398 -> -1e-398",
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
        "d64+ 0 xo +8096804903999112e369 +8128923351043907e369 -> +1622572825504301e-206 o",
        "d64+ 0 xo -4100000000000000e355 -9999999999999977e369 -> -1000000000000001e-206 o",
        "d64+ 0 xo -9619596163790245e369 -3804038362097600e368 -> -1000000000000000e-206 o",
        "d64+ 0 xo -9323523658066863e361 -9999999906764764e369 -> -1000000000000000e-206 o",
        "d64+ 0 xo +9999999999994263e369 +5751970035499092e357 -> +1000000000000001e-206 o",
        "d64+ 0 xo +9993748163439678e369 +6251836560323261e366 -> +1000000000000000e-206 o",
        "d64+ 0 xo +9621719000000000e360 +9999999990378281e369 -> +1000000000000000e-206 o",
        "d64+ =0 xo -9081030182497474e369 -5897948016606959e369 -> -1497897819910443e-206 o",
        "d64+ =0 xo +7247338377022046e369 +8540154290575367e369 -> +1578749266759741e-206 o",
        "d64+ =0 xo -5103308018897212e369 -6620982393283973e369 -> -1172429041218118e-206 o",
        "d64+ =0 xo +9162354442836365e369 +1708030689477041e369 -> +1087038513231341e-206 o",
        "d64+ =0 xo -9999999999999956e369 -6132400880895693e355 -> -1000000000000002e-206 o",
        "d64+ =0 xo -6503623069072396e362 -9999999349637703e369 -> -1000000000000001e-206 o",
        "d64+ =0 xo -8163160715582000e357 -9999999999991837e369 -> -1000000000000000e-206 o",
        "d64+ =0 xo +9999999991564134e369 +8435886432584849e360 -> +1000000000000002e-206 o",
        "d64+ =0 xo +9992869578034892e369 +7130421965112000e366 -> +1000000000000000e-206 o",
        "d64+ =0 xo +8561373866473000e366 +9991438626133527e369 -> +1000000000000000e-206 o",
        "d128+ =0 xu -5414267123623525953667080660113040e-6176 +5414267123623525953667080660113039e-6176 -> -1e3040 u",
        "d128+ =0 xu +0e1620 +1e-6176 -> +1e3040 u",
        "d64+ =0 xu -2073006932212847e-398 +1599988532498004e-398 -> -473018399714843e178 u",
        "d64+ =0 xu -1383509241006312e-398 +1300790449786174e-398 -> -82718791220138e178 u",
        "d64+ =0 xu -3214222826997667e-398 +3208948004124495e-398 -> -5274822873172e178 u",
        "d64+ =0 xu +6084817746929578e-398 -5225527196945912e-398 -> +859290549983666e178 u",
        "d64+ =0 xu +9552525012888e-398 +33560391957388e-398 -> +43112916970276e178 u",
        "d64+ =0 xu +2405070671137e-398 +1607622316620e-398 -> +4012692987757e178 u",
        "d64+ =0 xu -817274705688422e-398 -182725294311576e-398 -> -999999999999998e178 u",
        "d64+ =0 xu +1e-398 +0e119 -> +1e178 u",
        "d64+ =0 xu +663702891003955e-398 +336297108996043e-398 -> +999999999999998e178 u",
        "d64+ =0 xu -0e-185 -1e-398 -> -1e178 u",
        "d64+ =0 xu -0e88 -6e-398 -> -6e178 u",
        "d64+ =0 xu +0e22 +1e-398 -> +1e178 u",
        "d64+ =0 xu +987285451156649e-397 -9872854511566485e-398 -> +5e178 u",
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

    fun isBadCase(fptest: Fptest, ctx: DecContext): Boolean {
        if (badCases.contains(fptest.fptestStr))
            return true
        val result = fptest.result()

        if (result != null && result.qExp in -3104..-3000)
            return true
        if (fptest.hasTrap("o") && fptest.expectsSignal("o") && ctx.overflow)
            return true

        val hasTrapU = fptest.hasTrap("u")
        val expectsSignalU = fptest.expectsSignal("u")
        if (hasTrapU && expectsSignalU && result != null) {
            if (result.qExp in 38..179)
                return true
            if (result.qExp in -201..-37)
                return true
            if (result.qExp in 2282..3041)
                return true
            if (result.qExp in 360..648)
                return true
            if (result.qExp in 902..1049)
                return true
            if (result.qExp in -1063..-207)
                return true
            if (result.qExp in -2972..-2034)
                return true
        }

        return false
    }


    val prefix = "src/jvmTest/resources/fptest/"

    val fptestFiles = arrayOf(
        "Decimal-Basic-Types-Inputs.fptest",
        "Decimal-Basic-Types-Intermediate.fptest",
        "Decimal-Clamping.fptest",
        "Decimal-Mul-Trailing-Zeros.fptest",
        "Decimal-Overflow-correctedByMTH.fptest",
        "Decimal-Rounding.fptest",
        "Decimal-Trailing-And-Leading-Zeros-Input.fptest",
        "Decimal-Trailing-And-Leading-Zeros-Result.fptest",
        "Decimal-Underflow.fptest",
    )

    @Test
    fun testReadFptestFiles() {
        if (captureOperandText)
            operandPrintWriter = File(operandCaptureFilename).printWriter()
        for (fptestFile in fptestFiles)
            read1(prefix + fptestFile)
        operandPrintWriter?.close()
    }

    class Fptest(val fptestStr: String,
                 val format: String,
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
                    val tokens0 = tokens[0]
                    val isDecimal128 = tokens0.startsWith("d128")
                    val isDecimal64 = tokens0.startsWith("d64")
                    if (! isDecimal128 && ! isDecimal64)
                        return null
                    val format = tokens0.substring(0, if (isDecimal64) 3 else 4)
                    val op = tokens0.substring(if (isDecimal64) 3 else 4)
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
                    if (isDecimal64 && result.endsWith("e-206")) {
                        if (traps == "xu" && exceptions.contains('u'))
                            println("underflow correction needed? $fptestStr")
                        else if (traps == "xo" && exceptions.contains('o'))
                            println("overflow correction needed? $fptestStr")
                    }

                    return Fptest(fptestStr, format, op, round, traps, operands, result, exceptions)
                }

                val trapRegex = "[xuozi]+".toRegex()
                private fun isTrappedException(str: String): Boolean {
                    return str.matches(trapRegex)
                }
            }

        fun roundingDirection(): DecRounding {
            return when (round) {
                "=0" -> ROUND_TIES_TO_EVEN
                "=^" -> ROUND_TIES_TO_AWAY
                ">" -> ROUND_TOWARD_POSITIVE
                "<" -> ROUND_TOWARD_NEGATIVE
                "0" -> ROUND_TOWARD_ZERO
                else -> throw RuntimeException("what is '$round' ?")
            }
        }

        fun result(): MutDec? {
            if (result == "#")
                return null
            val dec = MutDec()
            //val ctx = DecContext()
            val env = DecContext.decimal128Kotlin()
            when (result) {
                "Q" -> dec.setNaN(env)
                "S" -> dec.setSNaN(env)
                else -> DecimalParsePrint.decFromString(dec, result, env)
            }
            return dec
        }

        fun decOperands(): ArrayList<MutDec> {
            val ret = ArrayList<MutDec>(operands.size)
            //val ctx = DecContext()
            val env = DecContext.decimal128Kotlin()
            for (t in operands) {
                val d = MutDec()
                when (t) {
                    "Q" -> d.setNaN(env)
                    "S" -> d.setSNaN(env)
                    else -> {
                        operandPrintWriter?.println(t)
                        DecimalParsePrint.decFromString(d, t, env)
                    }
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

        val file = File(fptestFile).bufferedReader()
        for (line in file.readLines()) {
            val fptest = Fptest.parseFptest(line)
            if (fptest != null) {
                if (verbose)
                    println(fptest)
                test1(fptest)
            }
        }
    }

    fun test1(fptest: Fptest) {
        val format = fptest.format
        val operands = fptest.decOperands()
        val ctx = when {
            format == "d128" -> DecContext.decimal128Kotlin().with(fptest.roundingDirection())
            format == "d64" -> return // DecContext(DecFormat.DECIMAL_64, fptest.roundingDirection())
            else -> throw IllegalStateException()
        }
        val observed = MutDec()
        if (verbose)
            println(fptest.fptestStr)
        when (fptest.op) {
            "+" -> {
                observed.setAdd(operands[0], operands[1], ctx)
            }
            "-" -> {
                observed.setSub(operands[0], operands[1], ctx)
            }
            "*" -> {
                observed.setMul(operands[0], operands[1], ctx)
            }
            "/" -> {
                observed.setDiv(operands[0], operands[1], ctx)
            }
            else -> {
                throw RuntimeException("not impl" + fptest.op)
                // return
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
                println(fptest.fptestStr)
                println("expected:$expected observed:$observed")
                assertEquals(IEEE754_EQ, cmp754)
            }
            val expectedExceptions = fptest.exceptions
            val expectedExceptionsSinU = expectedExceptions.replace("u", "")
            val observedExceptions = ctx.getFptestExceptionsString()
            val observedExceptionsSinU = observedExceptions.replace("u", "")
            assertEquals(expectedExceptionsSinU, observedExceptionsSinU)
            if (observedExceptions.contains("u") && !expectedExceptions.contains("u")) {
                println(fptest.fptestStr)
                println(">>>>>>>>>> I reported underflow but FPtest did not")
                throw RuntimeException()
            }

        } else {
            //println("expected trap not yet handled")
        }

    }

}