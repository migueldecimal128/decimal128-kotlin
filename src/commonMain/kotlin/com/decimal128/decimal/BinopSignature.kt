@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

enum class BinopSignature {
    FNZ_FNZ,
    FNZ_ZER,
    FNZ_INF,

    ZER_FNZ,
    ZER_ZER,
    ZER_INF,

    INF_FNZ,
    INF_ZER,
    INF_INF,

    NAN_FOUND;


    companion object {

        internal val signatures16 = Array<BinopSignature>(16) {
            i ->
            val xSig = i shr 2
            val ySig = i and 0x03
            if (xSig == 3 || ySig == 3)
                NAN_FOUND
            else
                entries[xSig * 3 + ySig]
        }

        internal inline fun of(x: Decimal, y: Decimal): BinopSignature =
//            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]
            signatures16[indexOf(x.steal, y.steal) and 0x0F]

        internal inline fun of(x: MutDec, y: MutDec): BinopSignature =
            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]

        private fun indexOf(qX: Int, bitLenX: Int, qY: Int, bitLenY: Int): Int {
            val xCat = when {
                qX < MIN_SPECIAL_VALUE -> if (bitLenX != 0) 0 else 1   // FNZ or ZER
                qX == NON_FINITE_INF   -> 2                            // INF
                else                   -> 3                            // NAN
            }
            val yCat = when {
                qY < MIN_SPECIAL_VALUE -> if (bitLenY != 0) 0 else 1   // FNZ or ZER
                qY == NON_FINITE_INF   -> 2                            // INF
                else                   -> 3                            // NAN
            }
            return (xCat shl 2) or yCat
        }

        internal inline fun indexOf(xSteal: Int, ySteal: Int): Int {
            val xCat = stealType(xSteal)
            val yCat = stealType(ySteal)
            return (xCat shl 2) or yCat
        }

    }
}
