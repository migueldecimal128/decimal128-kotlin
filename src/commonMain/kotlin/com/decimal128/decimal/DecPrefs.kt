package com.decimal128.decimal

data class DecPrefs(
    val printValuePlusSign: Boolean = false,
    val printNegativeZero: Boolean = true,
    val printExponentPlusSign: Boolean = true,
    val printExponentUppercaseE: Boolean = true,
    val printEngineeringString: Boolean = false,

    val printInfinityAllCaps: Boolean = false,
    val printInfinity8Chars: Boolean = false,

    val printNaNSign: Boolean = true,
    val printNaNPayload: Boolean = true,
    val printNaNPlusSign: Boolean = false,
    val printNaNAllCaps: Boolean = false,
    val printCollapseSNaN: Boolean = false,

    val parseCollapseSNaN: Boolean = false,
    val parseDiscardNanPayload: Boolean = false,
    val parseThrowOnMalformed: Boolean = true,
    val parseThrowOnDigitOverflow: Boolean = false,
    val parseThrowOnOutOfRange: Boolean = false,

    val printAutoMinNoExponent: Int = -6,
    val printStyle: PrintStyle = PrintStyle.AUTO,

    val decodeBidAllowNonCanonical: Boolean = false,
    val propagatePreferSnan: Boolean = true,
) {
    companion object {
        val DEFAULT = DecPrefs()
    }

    enum class PrintStyle { AUTO, ALWAYS_SCIENTIFIC, INTEGER_COEFFICIENT}
    enum class TextCase { MIXED_CASE, UPPER_CASE, LOWER_CASE }
}
