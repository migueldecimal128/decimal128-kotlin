package com.decimal128.decimal

data class DecPrefs(
    val printNegativeZero: Boolean = true,
    val printExponentPlusSign: Boolean = true, // Double default is false
    val printExponentUppercaseE: Boolean = true,
    val printEngineeringString: Boolean = false,

    val printSpecialValueAllCaps: Boolean = false,
    val printInfinityShort3Char: Boolean = false,

    val printNaNMinusSign: Boolean = true,
    val printNaNPayload: Boolean = true,
    val printCollapseSNaN: Boolean = false,

    val parseCollapseSNaN: Boolean = false,
    val parseDiscardNanPayload: Boolean = false,
    val parseMalformedSignalsInvalidOperation: Boolean = false,
    val parseThrowOnDigitOverflow: Boolean = false,
    val parseThrowOnOutOfRange: Boolean = false,

    val printMinPlainExponent: Int = -6,
    val printStyle: PrintStyle = PrintStyle.AUTO,

    val propagatePreferSnan: Boolean = true,
) {
    companion object {
        val KOTLIN_DEFAULT = DecPrefs()
    }

    enum class PrintStyle { AUTO, ALWAYS_SCIENTIFIC, INTEGER_COEFFICIENT}
}
