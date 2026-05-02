package com.decimal128.decimal.dectest

import com.decimal128.decimal.RoundingDirection

object DectestParser1 {

    fun parse(input: String, opName: String = "", printStyleEngineering: Boolean = false): List<DectestCase1> =
        parseLines(input.lineSequence(), opName, printStyleEngineering)

    fun parse(lines: Array<String>, opName: String = "", printStyleEngineering: Boolean = false): List<DectestCase1> =
        parseLines(lines.asSequence(), opName, printStyleEngineering)

    fun parseLines(lines: Sequence<String>, opName: String = "", printStyleEngineering: Boolean = false): List<DectestCase1> {
        var env = DectestEnv()
        val out = ArrayList<DectestCase1>()

        for (line in lines) {
            val raw = line.substringBefore("--").trim()
            if (raw.isEmpty()) continue

            // Directive
            if (':' in raw && !raw.contains("->")) {
                env = applyDirective(env, raw)
                continue
            }

            if (env.isValid()) {
                // Test case
                if ("->" in raw) {
                    val case = DectestCase1.parseDectestCase(raw, env, printStyleEngineering)
                    if (case.operand1Str != "#" && case.operand2Str != "#" && case.operand3Str != "#") {
                        if (opName == "" || case.operation == opName) {
                            out += case
                            continue
                        }
                    }
                }
            }

            // Ignore anything else
        }

        return out
    }

    private fun applyDirective(env: DectestEnv, line: String): DectestEnv {
        val (keyRaw, valueRaw) = line.split(':', limit = 2)
        val key = keyRaw.trim().lowercase()
        val value = valueRaw.trim().lowercase()

        return when (key) {
            "extended"    -> env.copy(extended = value == "1")
            "clamp"       -> env.copy(clamp = value == "1")
            "precision"   -> env.copy(precision = value.toInt())
            "maxexponent" -> env.copy(maxExp = value.toInt())
            "minexponent" -> env.copy(minExp = value.toInt())
            "rounding"    -> env.copy(rounding = decRoundingFromDectestName(value))
            else          -> env
        }
    }

    fun decRoundingFromDectestName(name: String): RoundingDirection? =
        when (name.lowercase()) {
            "half_even" -> RoundingDirection.TIES_TO_EVEN
            "half_up"   -> RoundingDirection.TIES_TO_AWAY
            "ceiling"   -> RoundingDirection.TOWARD_POSITIVE
            "floor"     -> RoundingDirection.TOWARD_NEGATIVE
            "down"      -> RoundingDirection.TOWARD_ZERO
            "up"        -> null
            "05up"      -> null
            "half_down" -> null

            else -> error("Unsupported decTest rounding mode: '$name'")
        }
}