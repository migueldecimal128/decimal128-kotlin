package com.decimal128.decimal

data class DecEnv(
    val decFormat: DecimalFormat = DecimalFormat.DECIMAL_128,
    val decRounding: RoundingDirection = RoundingDirection.ROUND_TIES_TO_EVEN,
    val decFlags: DecimalFlags = DecimalFlags(),
    val decPrefs: DecimalPreferences = DecimalPreferences.DEFAULT,
) {
    fun withRounding(newRoundingDirection: RoundingDirection) =
        DecEnv(decFormat, newRoundingDirection, decFlags, decPrefs)
    fun withFormat(newDecimalFormat: DecimalFormat) =
        DecEnv(newDecimalFormat, decRounding, decFlags, decPrefs)
    fun withPrefs(newDecimalPreferences: DecimalPreferences) =
        DecEnv(decFormat, decRounding, decFlags, newDecimalPreferences)
}