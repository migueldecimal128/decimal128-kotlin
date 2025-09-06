package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class TestDecTest {

    private val veryVerbose = true
    private val verbose = true

    private val prefix = "src/jvmTest/resources/dectest/"

    private var precision = 34
    private var rounding = "half_even"
    private var maxExponent = 6144
    private var minExponent = -6143
    private var version = ""
    private var extended = 0
    private var clamp = 0

    private val validRoundingStrings = arrayOf(
        "half_even",
        "half_up",
        "down",
        "ceiling",
        "floor",
        "up",
        "05up",
        "half_down",
    )

    private val roundingDirections = arrayOf(
        RoundingDirection.ROUND_TIES_TO_EVEN,
        RoundingDirection.ROUND_TIES_TO_AWAY,
        RoundingDirection.ROUND_TOWARD_ZERO,
        RoundingDirection.ROUND_TOWARD_POSITIVE,
        RoundingDirection.ROUND_TOWARD_NEGATIVE,
    )

    private val dectestFiles = arrayOf(
        "dqMultiply.decTest",
        "dqAdd.decTest",
        "dqSubtract.decTest",
        "dqDivide.decTest",
        //"dqRemainder.decTest",
    )

    val tcs = arrayOf(
        "dqmul770 multiply 1e+40 1e+6101 -> 1.000000000000000000000000000000E+6141 Clamped",
        "rounding:    floor",
        "dqadd71720 add  0        0E-19  ->  0E-19",
        "rounding: half_even",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "dqadd7882 add -NaN26    NaN28 -> -NaN26",
        "dqadd7841 add  sNaN -Inf   ->  NaN  Invalid_operation",
        "dqadd7861 add  NaN1   -Inf    ->  NaN1",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "dqadd7735 add -0    -0     -> -0     -- IEEE 754 special case",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "rounding:half_up",
        "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            processLine(tc)
    }

    @Test
    fun testReadDectestFiles() {
        for (dectestFile in dectestFiles)
            read1(prefix + dectestFile)
    }

    fun read1(dectestFileName: String) {
        //if (verbose)
            println("dectestFileName: $dectestFileName")
        val file = File(dectestFileName).bufferedReader()
        for (line in file.readLines())
            processLine(line)
    }

    fun processLine(line: String) {
        if (veryVerbose)
            println("line:$line")
        val commentIndex = line.indexOf("--")
        val trimmed = (if (commentIndex >= 0) line.substring(0, commentIndex) else line).trim()
        when {
            trimmed.length == 0 -> {}
            processDirective(trimmed) -> {}
            processTest(trimmed) -> {}
            else -> {
                println("UNRECOGNIZED: $trimmed")
            }
        }
    }

    fun processDirective(line: String): Boolean {
        val colonIndex = line.indexOf(':')
        if (colonIndex < 0)
            return false
        if (veryVerbose)
            println(line)
        val directive = line.substring(0, colonIndex).trim().lowercase()
        val value = line.substring(colonIndex + 1).trim().lowercase()
        when (directive) {
            "precision" -> {
                val p = value.toInt()
                require (p >= 3)
                require (p <= 34)
                precision = p
            }
            "rounding" -> {
                require (value in validRoundingStrings)
                rounding = value
            }
            "maxexponent" -> {
                val e = value.toInt()
                require (e >= 0)
                require (e <= 9999)
                maxExponent = e
            }
            "minexponent" -> {
                val e = value.toInt()
                require (e <= 0)
                require (e >= -9999)
                minExponent = e
            }
            "version" -> version = value
            "extended" -> {
                val v = value.toInt()
                require (v == 0 || v == 1)
                extended = v
            }
            "clamp" -> {
                val v = value.toInt()
                require (v == 0 || v == 1)
                clamp = v
            }
            else -> throw RuntimeException("unrecognized directive: $line")
        }
        return true
    }

    val testLhsRegex = """^(\w+)\s+(\w+)\s+(\S+)(?:\s+(\S+))?(?:\s+(\S+))?$""".toRegex()
    val testRhsRegex = """^(\S+)(?:\s+(\S+(?:\s+\S+)*))?$""".toRegex()

    fun processTest(line: String): Boolean {
        val arrowIndex = line.indexOf("->")
        if (arrowIndex < 0)
            return false
        val lhs = line.substring(0, arrowIndex).trim()
        val rhs = line.substring(arrowIndex + 2).trim()

        val lhsMatch = testLhsRegex.matchEntire(lhs)
        val rhsMatch = testRhsRegex.matchEntire(rhs)

        if (lhsMatch == null || rhsMatch == null) {
            println("regex mismatch")
            return false
        }

        val (id, op, operand1, operand2, operand3) = lhsMatch.destructured
        val (result, conditionsString) = rhsMatch.destructured
        val conditions: Array<String> = conditionsString
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.split(Regex("\\s+"))
            ?.toTypedArray()
            ?: emptyArray()

        val dectest = DecTest(id, op, operand1, operand2, operand3, result, conditions)

        if (verbose)
            println(dectest)
        dectest.eval()
        return true
    }

    private val MY_NAN = Decimal("sNaN")

    inner class DecTest(val id: String, val op: String, val operand1: String, val operand2: String, val operand3: String,
                  val result: String, val conditions: Array<String>) {
        val op1 = parseOperand(operand1)
        val op2 = if (operand2 == "") MY_NAN else parseOperand(operand2)
        val op3 = if (op2 !== MY_NAN || operand3 == "") MY_NAN else parseOperand(operand3)
        val res = parseOperand(result)

        override fun toString(): String {
            val sb = StringBuilder("test id:$id op:$op op1:$op1")
            if (op2 !== MY_NAN) {
                sb.append(" op2:$op2")
                if (op3 !== MY_NAN)
                    sb.append(" op3:$op3")
            }
            sb.append(" ==> res:$res")
            if (conditions.isNotEmpty())
                sb.append(conditions.contentToString())
            return sb.toString()
        }

        fun eval() {
            val ctx = buildContext()
            if (ctx == null)
                return
            if (verbose)
                println("op:$op op1:$op1 op2:$op2 ==> res:$res")
            val observed = when (op) {
                "add" -> Decimal.newAdd(op1, op2, ctx)
                "subtract" -> Decimal.newSub(op1, op2, ctx)
                "multiply" -> Decimal.newMul(op1, op2, ctx)
                "divide" -> Decimal.newDiv(op1, op2, ctx)
                //"remainder" -> Decimal.newMod(op1, op2, ctx)
                else -> return
            }
            if (verbose)
                println("    observed:$observed")
            require (res.exactlyEQ(observed))
        }
    }

    fun parseOperand(str: String): Decimal {
        if (str.length == 0)
            return MY_NAN
        var t = str
        if (t[0] == '\'' && t[t.lastIndex] == '\'')
            t = t.substring(1, t.lastIndex).replace("''", "'")
        if (t[0] == '\"' && t[t.lastIndex] == '\"')
            t = t.substring(1, t.lastIndex).replace("\"\"", "\"")
        if (t.contains('#')) {
            println("octothorpe not fully implemented")
            return MY_NAN
        }
        val d = Decimal(t)
        return d
    }

    fun buildContext(): DecimalContext? {
        require (minExponent == -(maxExponent - 1))
        val roundingIndex = validRoundingStrings.indexOf(rounding)
        if (roundingIndex < 0 || roundingIndex >= roundingDirections.size)
            return null
        val fmt = DecimalFormat(precision, maxExponent, roundingDirections[roundingIndex])
        val ctx = DecimalContext(fmt)
        return ctx
    }

    @Test
    fun test1() {
        println("hello world!")
    }

}