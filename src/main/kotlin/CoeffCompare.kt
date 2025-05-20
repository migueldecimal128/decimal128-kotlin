package com.decimal128

import java.lang.Long.compareUnsigned

object CoeffCompare {

    fun coeffUnscaledCompare(x:Coeff, y:Coeff) : Int {
        if (x.bitLen != y.bitLen)
            return x.bitLen.compareTo(y.bitLen)
        val cmp0 = compareUnsigned(x.dw0, y.dw0)
        val cmp1 = compareUnsigned(x.dw1, y.dw1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        if (x.bitLen <= 128)
            return cmp10
        val cmp2 = compareUnsigned(x.dw2, y.dw2)
        val cmp3 = compareUnsigned(x.dw3, y.dw3)
        val cmp32 = if (cmp3 != 0) cmp3 else cmp2
        val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
        return cmp3210
    }

    fun coeffUnscaledCompare(x: Coeff, y: IntArray): Int {
        require(y.size >= 8)
        val y3 = (y[7].toLong() shl 32) or (y[6].toLong() and MASK32)
        if (x.dw3 != y3)
            return compareUnsigned(x.dw3, y3)
        val y2 = (y[5].toLong() shl 32) or (y[4].toLong() and MASK32)
        if (x.dw2 != y2)
            return compareUnsigned(x.dw2, y2)
        val y1 = (y[3].toLong() shl 32) or (y[2].toLong() and MASK32)
        if (x.dw1 != y1)
            return compareUnsigned(x.dw1, y1)
        val y0 = (y[1].toLong() shl 32) or (y[0].toLong() and MASK32)
        return compareUnsigned(x.dw0, y0)
    }

    fun coeffUnscaledEQ(x:Coeff, y:Coeff) : Boolean {
        return ((x.digitLen - y.digitLen).toLong() or
                (x.dw0 - y.dw0) or (x.dw1 - y.dw1) or
                (x.dw2 - y.dw2) or (x.dw3 - y.dw3)) == 0L
    }

    fun coeffGTOne(x: Coeff) = x.digitLen > 1 || x.digitLen == 1 && x.dw0 != 1L

}