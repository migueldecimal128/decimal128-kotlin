@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

 enum class BinopSignature {
    FNZ_FNZ,
    FNZ_INF,
    FNZ_ZER,

    INF_FNZ,
    INF_INF,
    INF_ZER,

    ZER_FNZ,
    ZER_INF,
    ZER_ZER,

    NAN_FOUND;


    companion object {

        internal inline fun of(x: MutDec, y: MutDec): BinopSignature =
            signatures16[indexOf(x.qExp, x.bitLen, y.qExp, y.bitLen) and 0x0F]

        private fun indexOf(qX: Int, bitLenX: Int, qY: Int, bitLenY: Int): Int {
            val xCat = when {
                qX < MIN_SPECIAL_VALUE -> if (bitLenX != 0) 0 else 2   // FNZ or ZER
                qX == NON_FINITE_INF   -> 1                            // INF
                else                   -> 3                            // NAN
            }
            val yCat = when {
                qY < MIN_SPECIAL_VALUE -> if (bitLenY != 0) 0 else 2   // FNZ or ZER
                qY == NON_FINITE_INF   -> 1                            // INF
                else                   -> 3                            // NAN
            }
            return (xCat shl 2) or yCat
        }

        private inline fun indexOf(xSteal: Int, ySteal: Int): Int {
            val xCat = stealType(xSteal)
            val yCat = stealType(ySteal)
            return (xCat shl 2) or yCat
        }

    }
}

internal val signatures16 = Array<BinopSignature>(16) {
        i ->
    val xSig = i shr 2
    val ySig = i and 0x03
    if (xSig == 3 || ySig == 3)
        BinopSignature.NAN_FOUND
    else
        BinopSignature.entries[xSig * 3 + ySig]
}

internal /*inline*/ fun binopSignatureOf(x: DecimalRep, y: DecimalRep): BinopSignature {
    // this is dependant upon type being the bottom 2 bits of steal
    val stealX = x.steal
    val stealY = y.steal
    val index = ((stealX shl 2) or (stealY and 0x03)) and 0x0F
    val sig = signatures16[index]
    return sig
}

