package com.decimal128.decimal.dectest

import com.decimal128.decimal.DecRounding

object DectestParser1 {

    fun parse(input: String): List<DectestCase1> =
        parseLines(input.lineSequence())

    fun parse(lines: Array<String>): List<DectestCase1> =
        parseLines(lines.asSequence())

    fun parseLines(lines: Sequence<String>): List<DectestCase1> {
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

            // Test case
            if ("->" in raw) {
                out += DectestCase1.parseDectestCase(raw, env)
                continue
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

    fun decRoundingFromDectestName(name: String): DecRounding? =
        when (name.lowercase()) {
            "half_even" -> DecRounding.ROUND_TIES_TO_EVEN
            "half_up"   -> DecRounding.ROUND_TIES_TO_AWAY
            "ceiling"   -> DecRounding.ROUND_TOWARD_POSITIVE
            "up"        -> DecRounding.ROUND_TOWARD_POSITIVE
            "floor"     -> DecRounding.ROUND_TOWARD_NEGATIVE
            "down"      -> DecRounding.ROUND_TOWARD_ZERO
            "05up"      -> null

            else -> error("Unsupported decTest rounding mode: '$name'")
        }
}