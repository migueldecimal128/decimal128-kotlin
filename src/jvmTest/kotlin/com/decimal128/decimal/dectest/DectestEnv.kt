package com.decimal128.decimal.dectest

import com.decimal128.decimal.DecRounding

data class DectestEnv(
    val extended: Boolean = true,
    val clamp: Boolean = true,
    val precision: Int = 34,
    val maxExp: Int = 6144,
    val minExp: Int = -6143,
    val rounding: DecRounding? = DecRounding.ROUND_TIES_TO_EVEN
) {

    val qExpMax: Int
        get() = maxExp - (precision - 1)
    val qExpTiny: Int
        get() = minExp - (precision - 1)

    fun isValid(): Boolean {
        if (rounding == null) return false
        // other validity tests go here
        return true
    }
}
