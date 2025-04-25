package com.decimal128

import java.lang.Long.compareUnsigned

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


}