package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.IntelTest.Companion.parseIntelTest
import kotlin.test.Test
import kotlin.test.assertTrue

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

class IntelTest private constructor (
    val line: String,
    val funcStr: String,
    val rnd: Int,
    val op1Str: String,
    val op2Str: String?,
    val op3Str: String?,
    val resStr: String,
    val status: Int,
    val attrs: Map<String, String>
) {
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

        val statusRegex = Regex("""\s([0-9A-Fa-f]{2})(?=\s|$)""")
        val whitespaceRegex = Regex("\\s+")
        val strPrefixRegex = Regex("""str_prefix=\|(.*?)\|""")

        fun parseIntelTest(line: String): IntelTest {
            // strip trailing comments
            val noComments = line.substringBefore("--").trim()

            val m = statusRegex.findAll(noComments).last()

            val start = m.range.first + 1     // skip the whitespace before the hex
            val end   = start + 2             // position immediately after the hex token

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
            val status = baseTokens[baseTokens.size - 1].toInt(16)
            val chopped = baseTokens.dropLast(2)

            val funcStr = chopped[0]
            val rnd = chopped[1].toInt()
            val op1 = chopped[2]
            val op2 = chopped.getOrNull(3)
            val op3 = chopped.getOrNull(4)

            val intelTest = IntelTest(line, funcStr, rnd, op1, op2, op3, res, status, tailAttrs)
            return intelTest
        }
    }

    val decRounding: DecRounding
        get() = decRoundingMap[rnd]
}

class IntelTestSmokeTest {

    val verbose = true
    val veryVerbose = true

    @Test
    fun testBasicReadFile() {
        val stream = IntelTest::class.java.getResourceAsStream("/intel/readtest.in")
            ?: error("Resource not found: intel/readtest.in")

        stream.bufferedReader().useLines { sequence ->
            for (line in sequence) {
                // includes trimming comments
                if (line.substringBefore("--").trim().isEmpty())
                    continue
                if (veryVerbose)
                    println(line)
                test1Line(line)
            }
        }
    }

    fun testDecimalParse(str: String?) {
        if (str == null)
            return
        if (verbose)
            println("str:$str")
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
        print("problem: $str => $d")
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