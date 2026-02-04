package com.decimal128.decimal

object DecPow10 {

    fun umul128Pow10(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
        return when {
            pow10 < POW10_64_COUNT -> umul128x64to128(dw1, dw0, POW10[pow10 and 0x3F])
            dw1 == 0L -> {
                val pow10Offset = pow10Offset(pow10) and 0x7F // bounds check elimination
                val m0 = POW10[pow10Offset]
                val m1 = POW10[pow10Offset + 1]

                umul128x64to128(m1, m0, dw0)
            }
            else -> throw IllegalStateException()
        }
    }
}