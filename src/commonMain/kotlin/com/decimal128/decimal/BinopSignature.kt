@file:Suppress("NOTHING_TO_INLINE")

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

        private val signatures16 = Array<BinopSignature>(16) {
            i ->
            val xSig = i shr 2
            val ySig = i and 0x03
            if (xSig == 3 || ySig == 3)
                NAN_FOUND
            else
                entries[xSig * 3 + ySig]
        }

        fun of(x: DecOld, y: DecOld): BinopSignature =
            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]

        fun of(x: Decimal, y: Decimal): BinopSignature =
            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]

        fun of(x: MutDec, y: MutDec): BinopSignature =
            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]

        private fun indexOf(qX: Int, bitLenX: Int, qY: Int, bitLenY: Int): Int {
            // these flags are 0/1 Int
            // each operand is identified by 2 bits
            // bit 1 says whether or not the value isSpecial
            // bit 0 says either zer/fnz or inf/nan

            val xIsSpecial01 = (qX - MIN_SPECIAL_VALUE).inv() ushr 31
            val yIsSpecial01 = (qY - MIN_SPECIAL_VALUE).inv() ushr 31
            val xIsFinite01 = 1 - xIsSpecial01
            val yIsFinite01 = 1 - yIsSpecial01
            val xNonZero01 = -bitLenX ushr 31
            val yNonZero01 = -bitLenY ushr 31
            val xIsNan01 = (NON_FINITE_INF - qX) ushr 31
            val yIsNan01 = (NON_FINITE_INF - qY) ushr 31

            val xSignature =
                (xIsSpecial01 shl 1) or (xIsFinite01 and xNonZero01) or (xIsSpecial01 and xIsNan01)
            val ySignature =
                (yIsSpecial01 shl 1) or (yIsFinite01 and yNonZero01) or (yIsSpecial01 and yIsNan01)

            return (xSignature shl 2) + ySignature
        }

    }
}
