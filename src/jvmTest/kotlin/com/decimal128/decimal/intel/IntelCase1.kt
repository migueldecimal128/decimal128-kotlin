package com.decimal128.decimal.intel

import com.decimal128.decimal.D128ParsePrint
import com.decimal128.decimal.D128SerdeBid
import com.decimal128.decimal.DecContext
import com.decimal128.decimal.DecException
import com.decimal128.decimal.DecFlags
import com.decimal128.decimal.DecRounding
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.Decimal

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
class IntelCase1 private constructor (
    val text: String,
    val funcStr: String,
    val rounding: Int,
    val op1Str: String,
    val op2Str: String?,
    val op3Str: String?,
    val resStr: String,
    val flagRegister: Int,
    val attrs: Map<String, String>
) {

    fun op1Bid128(ctx: DecContext): Decimal =
         parseBid128(op1Str, ctx)

    fun op2Bid128(ctx: DecContext): Decimal {
        if (op2Str == null)
            throw IllegalStateException("op2 is null:$text")
        return parseBid128(op2Str, ctx)
    }

    fun op2Int(): Int {
        if (op2Str == null)
            throw IllegalStateException("op2 is null:$text")
        return op2Str.toInt()
    }

    fun op3Bid128(ctx: DecContext): Decimal {
        if (op3Str == null)
            throw IllegalStateException("op3 is null:$text")
        return parseBid128(op3Str, ctx)
    }

    fun resBid128(ctx: DecContext): Decimal =
        parseBid128(resStr, ctx)

    val resInt: Int
        get() = resStr.toInt()

    val resLong: Long
        get() = resStr.toLong()

    val resBoolean: Boolean
        get() {
            if (resStr == "0")
                return false
            if (resStr == "1")
                return true
            throw IllegalStateException("non-boolean 0/1 Intel test result")
        }

    fun decFlags(): DecFlags {
        val decFlags = DecFlags()
        if ((flagRegister and 0x01) != 0)
            decFlags.set(DecException.INVALID_OPERATION)
        if ((flagRegister and 0x04) != 0)
            decFlags.set(DecException.DIV_BY_ZERO)
        if ((flagRegister and 0x08) != 0)
            decFlags.set(DecException.OVERFLOW)
        if ((flagRegister and 0x10) != 0)
            decFlags.set(DecException.UNDERFLOW)
        if ((flagRegister and 0x20) != 0)
            decFlags.set(DecException.INEXACT)
        return decFlags
    }

    companion object {
        val decRoundingMap = arrayOf(
            ROUND_TIES_TO_EVEN,
            DecRounding.ROUND_TOWARD_NEGATIVE,
            DecRounding.ROUND_TOWARD_POSITIVE,
            DecRounding.ROUND_TOWARD_ZERO,
            DecRounding.ROUND_TIES_TO_AWAY
        )

        val allowedTailKeys = setOf(
            "ulp",
            "longintsize",
            "undefrlow_before_only", // misspelled version in Intel file
            "underflow_before_only",
            "str_prefix",
        )

        val flagRegisterRegex = Regex("""\s((?:0x)?[0-9A-Fa-f]{1,2})(?=\s|$)""")
        val whitespaceRegex = Regex("\\s+")
        val strPrefixRegex = Regex("""str_prefix=\|(.*?)\|""")

        fun parseIntelCase(text: String): IntelCase1 {
            // strip trailing comments
            val noComments = text.substringBefore("--").trim()

            val m = flagRegisterRegex.findAll(noComments).last()
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
            val rounding = chopped[1].toInt()
            val op1 = chopped[2]
            val op2 = chopped.getOrNull(3)
            val op3 = chopped.getOrNull(4)

            val intelCase = IntelCase1(text, funcStr, rounding, op1, op2, op3, res, status, tailAttrs)
            return intelCase
        }

        val regexHex64 = Regex("""\[[0-9A-Fa-f]{16}\]""")
        val regexHex32 = Regex("""\[[0-9A-Fa-f]{8}\]""")

        fun parseBid128(str: String, ctx: DecContext): Decimal {
            if (str.startsWith('[')) {
                if (regexHex64.matches(str) || regexHex32.matches(str))
                    throw IllegalArgumentException("not bid128:$str")
                val (isValid, dw1, dw0) = D128SerdeBid.parseIntelBidHex(str)
                if (! isValid)
                    throw IllegalArgumentException("something invalid with bid128:$str")
                val decimal = D128SerdeBid.decodeBid128(dw1, dw0, ctx)
                return decimal
            }
            return D128ParsePrint.parseDecimal(str, ctx)
        }

    }

    fun decRounding(): DecRounding = decRoundingMap[rounding]
}
