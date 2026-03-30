package com.decimal128.decimal

data class DecPrefs(
    val printNegativeZero: Boolean = true,
    val printExponentPlusSign: Boolean = false, // Double default is false
    val printExponentLowercaseE: Boolean = false,
    val printEngineeringString: Boolean = false,

    val printSpecialValueAllCaps: Boolean = false,
    val printInfinityShort3Char: Boolean = false,

    val printNaNMinusSign: Boolean = true,
    val printNaNPayload: Boolean = true,
    val printCollapseSNaN: Boolean = false,

    val parseCollapseSNaN: Boolean = false,
    val parseDiscardNanPayload: Boolean = false,
    val parseMalformedThrowsNumberFormatException: Boolean = false,
    val parseThrowOnDigitOverflow: Boolean = false,
    val parseThrowOnOutOfRange: Boolean = false,

    val printMinPlainExponent: Int = -6,
    val printStyle: PrintStyle = PrintStyle.AUTO,

    val propagatePreferSnan: Boolean = true,
) {
    companion object {
        val IEEE_DEFAULT = DecPrefs()
        val KOTLIN_DEFAULT = DecPrefs(
            parseMalformedThrowsNumberFormatException = true,
        )
    }

    enum class PrintStyle { AUTO, ENGINEERING, COEFFICIENT_QEXPONENT}
}
