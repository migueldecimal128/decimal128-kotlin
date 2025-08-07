package com.decimal128.decimal

val IEEE754_LT = Compare754Result(0)
val IEEE754_EQ = Compare754Result(1)
val IEEE754_GT = Compare754Result(2)
val IEEE754_UNORDERED = Compare754Result(3)

private val TO_STRING_MAP =
    arrayOf("LESS_THAN", "EQUAL", "GREATER_THAN", "UNORDERED")

@JvmInline
value class Compare754Result constructor(val value: Int) {

    override fun toString(): String {
        return TO_STRING_MAP[value]
    }

}