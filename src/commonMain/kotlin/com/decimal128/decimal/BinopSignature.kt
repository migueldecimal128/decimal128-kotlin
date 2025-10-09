package com.decimal128.decimal

enum class BinopSignature {
    ZER_ZER,
    ZER_FNZ,
    ZER_INF,

    FNZ_ZER,
    FNZ_FNZ,
    FNZ_INF,

    INF_ZER,
    INF_FNZ,
    INF_INF,

    NAN_FOUND;


    companion object {

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun indexOf(x: Decimal, y: Decimal) = indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen)

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun indexOf(x: MutDec, y: MutDec) = indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen)

        private val signatures = values()

        fun enumOf(x: Decimal, y: Decimal): BinopSignature =
            signatures[indexOf(x, y)]

        fun enumOf(x: MutDec, y: MutDec): BinopSignature =
            signatures[indexOf(x, y)]

        private fun indexOf(qX: Int, bitLenX: Int, qY: Int, bitLenY: Int): Int {
            // these flags are 0/1 Int
            val xIsSpecial = (qX - MIN_SPECIAL_VALUE).inv() ushr 31
            val yIsSpecial = (qY - MIN_SPECIAL_VALUE).inv() ushr 31
            val xNonZero = -bitLenX ushr 31
            val yNonZero = -bitLenY ushr 31
            val xNegativeIfNaN = NON_FINITE_INF - qX
            val yNegativeIfNaN = NON_FINITE_INF - qY

            if ((xNegativeIfNaN or yNegativeIfNaN) >= 0) {
                val x012 = (xIsSpecial shl 1) + xNonZero
                val y012 = (yIsSpecial shl 1) + yNonZero
                val cat = x012 * 3 + y012
                return cat
            } else {
                return NAN_FOUND.ordinal
            }
        }

    }
}
