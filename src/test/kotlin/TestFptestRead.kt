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

    val prefix = "src/test/kotlin/fptest/"

    val fptestFiles = arrayOf(
        "Decimal-Basic-Types-Inputs.fptest",
    )

    @Test
    fun testReadFptestFiles() {
        for (fptestFile in fptestFiles)
            read1(prefix + fptestFile)
    }

    class Fptest(val fptestStr: String,
                 val op: String,
                 val round: String,
                 val traps: ArrayList<String>,
                 val operands: ArrayList<String>,
                 val result: String,
                 val exceptions: ArrayList<String>
        ) {
            companion object {
                val whitespaceRegex = "\\s+".toRegex()

                fun parseFptest(fptestStr: String): Fptest? {
                    val tokens = fptestStr.split(whitespaceRegex)
                    if (! tokens[0].startsWith("d128"))
                        return null
                    val op = tokens[0].substring(4)
                    val round = tokens[1]

                    val traps = ArrayList<String>()
                    var i = 2
                    while (isTrappedException(tokens[i]))
                        traps.add(tokens[i++])

                    val operands = ArrayList<String>()
                    while (tokens[i] != "->")
                        operands.add(tokens[i++])

                    ++i
                    val result = tokens[i++]

                    val exceptions = ArrayList<String>()
                    while (i < tokens.size)
                        exceptions.add(tokens[i++])

                    return Fptest(fptestStr, op, round, traps, operands, result, exceptions)
                }

                val trapRegex = "[xuozi]".toRegex()
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
        val ctx = Decimal128Context()
        val observed = Dec34()
        when (fptest.op) {
            "+" -> {
                observed.add(operands[0], operands[1], ctx)
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
            } else {
                assertEquals(IEEE754_EQ, cmp754)
            }
        } else {
            println("expected trap not yet handled")
        }

    }

}