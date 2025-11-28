package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.IntelTest.Companion.parseIntelTest
import kotlin.test.Test

// *** THIS DOC IN THE SOURCE FILE IS OUT-OF-DATE ... WHAT A SHOCK! ***
// readtest.c - read tests from stdin
//
// This programs reads test of the form:
//
// <TESTID> <FUNCTION> <OP1> <OP2> <OP3> <RESULT> <STATUS>
//
// The testID is simply a string used to help identify which tail may be failing.
// The function name is generally one of the BID library function names.  Up to 3
// operands follow the function name, and then the expected result and expected
// status value.
//
// Each test is read, the appropriate function is called, and the results are
// compared with the expected results.  The operands, and results can appear as
// decimal numbers (e.g. 6.25), or as hexadecimal representations surrounded by
// square brackets (e.g. [31c0000000012345]).  The status value is a hexadecimal
// value (without the leading 0x).

// bid128_abs 0 -0 [30400000000000000000000000000000] 00
// bid128_add 1 -Infinity QNaN [7c000000000000000000000000000000] 00
// bid128_add 4 -68488695427246.927E6129 -999899889999998899988988988888.9E6115 [f8000000000000000000000000000000] 28
// bid64qqq_fma 0 [0884002304040000,3028110aa2440001] [5634b8feaf576e8d,a099718a248757c8] [aedb4145be096c9c,7af1e04e95d2560f] [9db7266e28f1af13] 20

/*
my guess is that "FE" => "Float Exception"

#define DEC_FE_INVALID      0x01
#define DEC_FE_UNNORMAL     0x02
#define DEC_FE_DIVBYZERO    0x04
#define DEC_FE_OVERFLOW     0x08
#define DEC_FE_UNDERFLOW    0x10
#define DEC_FE_INEXACT      0x20

////////////////////////////////////////////////////////

#define BID_INEXACT_EXCEPTION       DEC_FE_INEXACT
#define BID_UNDERFLOW_EXCEPTION     DEC_FE_UNDERFLOW
#define BID_OVERFLOW_EXCEPTION      DEC_FE_OVERFLOW
#define BID_ZERO_DIVIDE_EXCEPTION   DEC_FE_DIVBYZERO
#define BID_DENORMAL_EXCEPTION      DEC_FE_UNNORMAL
#define BID_INVALID_EXCEPTION       DEC_FE_INVALID
#define BID_UNDERFLOW_INEXACT_EXCEPTION (DEC_FE_UNDERFLOW|DEC_FE_INEXACT)
#define BID_OVERFLOW_INEXACT_EXCEPTION (DEC_FE_OVERFLOW|DEC_FE_INEXACT)
*/
class IntelTest private constructor (
    val testLine: String,
    val funcStr: String,
    val rnd: Int,
    val op1Str: String,
    val op2Str: String?,
    val op3Str: String?,
    val resStr: String,
    val status: Int,
    val attrs: Map<String, String>
) {

    val op1Bid128: Decimal
        get() = parseBid128(op1Str)

    val op2Bid128: Decimal
        get() {
            if (op2Str == null)
                throw IllegalStateException("op2 is null:$testLine")
            return parseBid128(op2Str)
        }

    val op3Bid128: Decimal
        get() {
            if (op3Str == null)
                throw IllegalStateException("op3 is null:$testLine")
            return parseBid128(op3Str)
        }

    val resBid128: Decimal
        get() = parseBid128(resStr)

    companion object {
        val decRoundingMap = arrayOf(
            ROUND_TIES_TO_EVEN, DecRounding.ROUND_TOWARD_NEGATIVE,
            DecRounding.ROUND_TOWARD_POSITIVE, DecRounding.ROUND_TOWARD_ZERO, DecRounding.ROUND_TIES_TO_AWAY
        )

        val allowedTailKeys = setOf(
            "ulp",
            "longintsize",
            "undefrlow_before_only", // misspelled version in Intel file
            "underflow_before_only",
            "str_prefix",
        )

        val statusRegex = Regex("""\s((?:0x)?[0-9A-Fa-f]{1,2})(?=\s|$)""")
        val whitespaceRegex = Regex("\\s+")
        val strPrefixRegex = Regex("""str_prefix=\|(.*?)\|""")

        fun parseIntelTest(line: String): IntelTest {
            // strip trailing comments
            val noComments = line.substringBefore("--").trim()

            val m = statusRegex.findAll(noComments).last()
            val end = m.range.first + m.value.length

            val head        = noComments.substring(0, end)
            val baseTokens = head.split(whitespaceRegex)
            require (baseTokens.size in 5..7)

            val tail = noComments.substring(end).trim()

            // check the tail for str_prefix before tokenizing
            val strPrefixMatch = strPrefixRegex.find(tail)
            val strPrefixValue = strPrefixMatch?.groupValues?.get(1)
            // Remove any str_prefix from the tail of the line

            val tailClean = if (strPrefixMatch != null)
                tail.removeRange(strPrefixMatch.range).trim()
            else
                tail
            val tailTokens = tailClean.split(whitespaceRegex)

            val tailAttrs = mutableMapOf<String, String>()
            if (strPrefixValue != null)
                tailAttrs["str_prefix"] = strPrefixValue

            tailTokens
                .filter { it.isNotEmpty() }
                .forEach { tok ->
                    if ("=" in tok) {
                        val (k, v) = tok.split("=", limit = 2)
                        tailAttrs[k] = v
                    } else {
                        tailAttrs[tok] = "true"
                    }
                }

            val unknownKeys = tailAttrs.keys - allowedTailKeys
            require(unknownKeys.isEmpty()) {
                "Unknown tail attribute(s): ${unknownKeys.joinToString(", ")}"
            }

            val res = baseTokens[baseTokens.size - 2]
            val status = baseTokens[baseTokens.size - 1].removePrefix("0x").toInt(16)
            val chopped = baseTokens.dropLast(2)

            val funcStr = chopped[0]
            val rnd = chopped[1].toInt()
            val op1 = chopped[2]
            val op2 = chopped.getOrNull(3)
            val op3 = chopped.getOrNull(4)

            val intelTest = IntelTest(line, funcStr, rnd, op1, op2, op3, res, status, tailAttrs)
            return intelTest
        }

        fun parseAllTests(fileText: String): List<IntelTest> {
            val allTests = ArrayList<IntelTest>()
            for (line in fileText.lineSequence()) {
                // trim comments
                if (line.substringBefore("--").trim().isEmpty())
                    continue
                val intelTest = parseIntelTest(line)
                allTests.add(intelTest)
            }
            return allTests
        }

        val regexHex64 = Regex("""\[[0-9A-Fa-f]{16}\]""")
        val regexHex32 = Regex("""\[[0-9A-Fa-f]{8}\]""")

        fun parseBid128(str: String): Decimal {
            if (str.startsWith('[')) {
                if (regexHex64.matches(str) || regexHex32.matches(str))
                    throw IllegalArgumentException("not bid128:$str")
                val (isValid, dw1, dw0) = DecSerdeBid128.parseIntelBidHex(str)
                if (! isValid)
                    throw IllegalArgumentException("something invalid with bid128:$str")
                val decimal = DecSerdeBid128.decodeBid128(dw1, dw0)
                return decimal
            }
            return DecParsePrint.parseDecimal(str)
        }

    }

    val decRounding: DecRounding
        get() = decRoundingMap[rnd]
}

class IntelTestSmokeTest {

    val verbose = false

    @Test
    fun testBasicReadFile() {
        val fileText = IntelTest::class.java.getResource("/intel/readtest.in")!!.readText()
        val allTests = IntelTest.parseAllTests(fileText)
        for (test in allTests) {
            testDecimalParse(test.op1Str)
            test.op2Str?.let { testDecimalParse(test.op2Str) }

            testDecimalParse(test.resStr)
        }
    }

    val regexHex128 = Regex("""\[[0-9A-Fa-f]{16},?[0-9A-Fa-f]{16}\]""")
    val regexHex64 = Regex("""\[[0-9A-Fa-f]{16}\]""")
    val regexHex32 = Regex("""\[[0-9A-Fa-f]{8}\]""")

    fun testDecimalParse(str: String?) {
        if (str == null)
            return
        if (verbose)
            println("str:$str")
        if (str.startsWith('[')) {
            if (regexHex64.matches(str) || regexHex32.matches(str)) {
                if (verbose)
                    println(" => skipped")
                return
            }
        }
        val (isValid, dw1, dw0) = DecSerdeBid128.parseIntelBidHex(str)
        if (isValid) {
            val decimal = DecSerdeBid128.decodeBid128(dw1, dw0)
            if (verbose)
                println(" => $decimal")
            return
        }
        val d = DecParsePrint.parseDecimalOrErrorString(str)
        if (d is Decimal) {
            if (verbose)
                println(" => $d")
            return
        }
        if (verbose)
            println("problem: $str => $d")
    }

    val tcs = arrayOf(
        "bid128_add 0 +101001100000101.000000E6138 -7695957767658598867966685688.99E6120 [7c000000000000000000000000000000] 01",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1Line(tc)
    }

    fun test1Line(line: String) {
        val intelTest = parseIntelTest(line)
        testDecimalParse(intelTest.op1Str)
        testDecimalParse(intelTest.op2Str)
        testDecimalParse(intelTest.op3Str)
        testDecimalParse(intelTest.resStr)
    }
}