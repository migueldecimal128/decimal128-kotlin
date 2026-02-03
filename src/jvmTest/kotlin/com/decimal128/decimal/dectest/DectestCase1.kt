package com.decimal128.decimal.dectest

import com.decimal128.decimal.DecContext
import com.decimal128.decimal.DecException
import com.decimal128.decimal.DecFlags
import com.decimal128.decimal.Decimal

data class DectestCase1(
    val text: String,
    val id: String,
    val operation: String,
    val operand1Str: String,
    val operand2Str: String?,
    val operand3Str: String?,
    val resultStr: String,
    val conditions: List<String>,
    val dectestEnv: DectestEnv,
    val expectedDecFlags: DecFlags,
    val decContext: DecContext,
) {


    val operand1: Decimal
        get() = parseDpd128(operand1Str)

    val operand2: Decimal
        get() {
            if (operand2Str == null)
                throw IllegalArgumentException()
            return parseDpd128(operand2Str)
        }

    val operand3: Decimal
        get() {
            if (operand3Str == null)
                throw IllegalArgumentException()
            return parseDpd128(operand3Str)
        }


    val result: Decimal
        get() = parseDpd128(resultStr)

    val resultInt: Int
        get() = resultStr.toInt()

    val resultBoolean: Boolean
        get() = when (resultStr) {
            "0" -> false
            "1" -> true
            else -> throw IllegalArgumentException("expected 0/1 boolean:$resultStr")
        }

    companion object {

        // --- Precompiled regexes for efficiency ---
        private val ARROW = Regex("""\s*->\s*""")
        private val LHS_PATTERN = Regex("""^(\S+)\s+(\S+)\s+(.*)$""")

        private val CONDITION_SET = setOf(
            "clamped",
            "conversion_syntax",
            "division_by_zero",
            "division_impossible",
            "division_undefined",
            "inexact",
            "insufficient_storage",
            "invalid_context",
            "invalid_operation",
            "lost_digits",
            "overflow",
            "rounded",
            "subnormal",
            "underflow"
        )

        private val mapConditionDecException: Map<String, DecException> = mapOf(
            "invalid_operation" to DecException.INVALID_OPERATION,
            "division_by_zero" to DecException.DIV_BY_ZERO,
            "overflow" to DecException.OVERFLOW,
            "underflow" to DecException.UNDERFLOW,
            "inexact" to DecException.INEXACT,

            "division_undefined" to DecException.INVALID_OPERATION,
            "division_impossible" to DecException.INVALID_OPERATION,
        )

        private fun allocDecContext(dectestEnv: DectestEnv): DecContext {
            require(dectestEnv.precision == 34)
            require(dectestEnv.maxExp == 6144)
            require(dectestEnv.minExp == -6143)
            require(dectestEnv.rounding != null)
            val decContext = DecContext(decRounding = dectestEnv.rounding)
            return decContext
        }

        /**
         * Parse a single decTest test case line into a DectestCase object.
         */
        fun parseDectestCase(text: String, env: DectestEnv): DectestCase1 {
            // Split at ->
            val (lhs, rhs) = ARROW.split(text, 2)

            // Parse ID, operation, and raw operand text
            val m = LHS_PATTERN.find(lhs)
                ?: error("bad test case: $text")

            val id = m.groupValues[1]
            val operation = m.groupValues[2]
            val operandText = m.groupValues[3]

            // Extract at most 3 operands
            val operands = extractOperands(operandText)

            // Operand1 is required by decTest spec
            if (operands.isEmpty())
                error("missing required operand1 in: $text")

            val operand1 = operands[0]
            val operand2 = operands.getOrNull(1)
            val operand3 = operands.getOrNull(2)

            // Extract result from RHS
            val result =
                extractOperands(rhs.trim()).firstOrNull()
                    ?: error("missing result token in: $text")

            val conditions =
                rhs.split(Regex("""\s+"""))
                    .filter { it.lowercase() in CONDITION_SET }

            return DectestCase1(
                text = text,
                id = id,
                operation = operation,
                operand1Str = operand1,
                operand2Str = operand2,
                operand3Str = operand3,
                resultStr = result,
                conditions = conditions,
                dectestEnv = env,
                expectedDecFlags = conditionsToDecFlags(conditions),
                decContext = allocDecContext(env)
            )
        }

        /**
         * Parses a decTest token starting at index `start`.
         *
         * Handles:
         *  - single-quoted tokens
         *  - double-quoted tokens
         *  - doubled inner quotes
         *  - unquoted tokens (terminated by whitespace)
         *
         * Returns: (tokenValue, nextIndex)
         */
        private fun parseQuotedToken(s: String, start: Int): Pair<String, Int> {
            if (start >= s.length)
                return "" to start

            val first = s[start]

            // Unquoted token
            if (first != '\'' && first != '"') {
                var i = start
                while (i < s.length && !s[i].isWhitespace()) i++
                return s.substring(start, i) to i
            }

            // Quoted token
            val quote = first
            val sb = StringBuilder()
            var i = start + 1
            val n = s.length

            while (i < n) {
                val c = s[i]

                if (c == quote) {
                    // Doubled quote → literal quote
                    if (i + 1 < n && s[i + 1] == quote) {
                        sb.append(quote)
                        i += 2
                        continue
                    }
                    // Closing quote
                    return sb.toString() to (i + 1)
                }

                sb.append(c)
                i++
            }

            error("Unterminated quoted token: ${s.substring(start)}")
        }

        /**
         * Extracts up to 3 operands from a decTest LHS operand string.
         * Handles quoted and unquoted tokens.
         */
        private fun extractOperands(text: String): List<String> {
            val result = ArrayList<String>(3)
            var i = 0
            val n = text.length

            while (i < n && result.size < 3) {
                // Skip whitespace
                while (i < n && text[i].isWhitespace()) i++
                if (i >= n) break

                // Parse next token
                val (token, next) = parseQuotedToken(text, i)
                result.add(token)
                i = next
            }

            return result
        }

        fun parseDpd128(str: String): Decimal {
            return Decimal.from(str)
        }

        fun conditionsToDecFlags(conditions: List<String>): DecFlags {
            val decFlags = DecFlags()
            for (condition in conditions) {
                val decException = mapConditionDecException[condition.lowercase()]
                if (decException != null)
                    decFlags.set(decException)
            }
            return decFlags
        }
    }

}