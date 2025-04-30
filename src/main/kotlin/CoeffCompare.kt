package com.decimal128

import java.lang.Long.compareUnsigned

private const val MASK32 = 0xFFFF_FFFFL

object CoeffCompare {

    fun coeffCompare(x:Coeff, y:Coeff) : Int {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        if (x.digitCount != y.digitCount)
            return x.digitCount.compareTo(y.digitCount)
        if (x.dw3 != y.dw3)
            return compareUnsigned(x.dw3, y.dw3)
        if (x.dw2 != y.dw2)
            return compareUnsigned(x.dw2, y.dw2)
        if (x.dw1 != y.dw1)
            return compareUnsigned(x.dw1, y.dw1)
        return compareUnsigned(x.dw0, y.dw0)
    }

    fun coeffEQ(x:Coeff, y:Coeff) : Boolean {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        return (x.digitCount == y.digitCount) &&
                (x.dw0 == y.dw0) && (x.dw1 == y.dw1) &&
                (x.dw2 == y.dw2) && (x.dw3 == y.dw3)
    }

    fun coeffGT(x:Coeff, y:Coeff) : Boolean {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        return when {
            (x.digitCount != y.digitCount) -> x.digitCount > y.digitCount
            (x.dw3 !=y.dw3) -> compareUnsigned(x.dw3, y.dw3) > 0
            (x.dw2 !=y.dw2) -> compareUnsigned(x.dw2, y.dw2) > 0
            (x.dw1 !=y.dw1) -> compareUnsigned(x.dw1, y.dw1) > 0
            else -> compareUnsigned(x.dw0, y.dw0) > 0
        }
    }

    fun coeffLT(x:Coeff, y:Coeff) : Boolean {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        return when {
            (x.digitCount != y.digitCount) -> x.digitCount < y.digitCount
            (x.dw3 !=y.dw3) -> compareUnsigned(x.dw3, y.dw3) < 0
            (x.dw2 !=y.dw2) -> compareUnsigned(x.dw2, y.dw2) < 0
            (x.dw1 !=y.dw1) -> compareUnsigned(x.dw1, y.dw1) < 0
            else -> compareUnsigned(x.dw0, y.dw0) < 0
        }
    }

    fun coeffGTOne(x: Coeff) = x.digitCount > 1 || x.digitCount == 1 && x.dw0 != 1L

    fun coeffCompare(x: Coeff, y: IntArray): Int {
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
}