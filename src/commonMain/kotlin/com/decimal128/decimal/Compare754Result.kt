package com.decimal128.decimal

enum class Compare754Result {
    IEEE754_LT,
    IEEE754_EQ,
    IEEE754_GT,
    IEEE754_UNORDERED
    ;

    companion object {
        operator fun invoke(value: Int): Compare754Result = when {
            value < 0 -> IEEE754_LT
            value == 0 -> IEEE754_EQ
            else -> IEEE754_GT
        }

    }
}
