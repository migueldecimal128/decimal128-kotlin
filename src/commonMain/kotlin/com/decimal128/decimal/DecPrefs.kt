package com.decimal128.decimal

data class DecPrefs(
    val printCoefficientPlus: Boolean = false,
    val printNegativeZero: Boolean = true,
    val printExponentPlus: Boolean = false,
    val printUppercaseE: Boolean = true,

    val printNanSign: Boolean = true,
    val printNaNPayload: Boolean = true,
    val printNaNPlusSign: Boolean = false,
    val printNaNTextCase: TextCase = TextCase.MIXED_CASE,
    val printCollapseSNaN: Boolean = false,
    val parseCollapseSNaN: Boolean = false,
    val parsePreserveNaNPayload: Boolean = true,

    val printInfinityPlusSign: Boolean = false,
    val printInfinityTextCase: TextCase = TextCase.MIXED_CASE,
    val print3LetterInf: Boolean = true,

    val printAutoMinNoExponent: Int = -6,
    val printStyle: PrintStyle = PrintStyle.AUTO
) {
    companion object {
        val DEFAULT = DecPrefs()
    }

    enum class PrintStyle { AUTO, ALWAYS_SCIENTIFIC, INTEGER_COEFFICIENT}
    enum class TextCase { MIXED_CASE, UPPER_CASE, LOWER_CASE }
}
