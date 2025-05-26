package com.decimal128

import com.decimal128.CoeffCompare.coeffUnscaledCompare
import com.decimal128.CoeffSet.coeffSetShiftRight
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.GT_HALF
import com.decimal128.Residue.Companion.HALF
import com.decimal128.Residue.Companion.LT_HALF
import java.lang.Long.*

private inline fun getShiftedLeft(v: IntArray, i: Int, shift: Int): Long {
    return ((v[i] shl shift) or ((v[i - (-i ushr 31)] ushr -shift) and (-shift shr 31))).toLong() and MASK32
    //if (i == 0) {
    //    v[0] shl s
    //} else {
    //    (v[i] shl s) or if (s != 0) (v[i - 1] ushr (32 - s)) else 0
}

object CoeffDivide {

    fun coeffDiv(z: Coeff, x: Coeff, y: Coeff): Residue {
        if (y.bitLen < 64) {
            val y0 = y.dw0
            if (y.bitLen <= 1) {
                if (y.bitLen == 0)
                    throw RuntimeException("div by zero")
                z.coeffSet(x)
                return EXACT
            }
            val x0 = x.dw0
            if (x.bitLen < 64) {
                val quot = x0 / y0
                val rem = x0 % y0
                val residue = when {
                    rem == 0L -> EXACT
                    compareUnsigned(2 * rem, y0) < 0 -> LT_HALF // we are doubling the remainder here
                    2 * rem == y0 -> HALF   // so make sure divisor is small enough
                    else -> GT_HALF
                }
                z.coeffSet64(quot)
                return residue
            }
            if ((y0 and (y0 - 1)) == 0L) {
                // y0 is an exact power of 2 ... just shift right.
                // 0 and 1 cases handled above, so if we are here then ntz >= 1
                val ntz = numberOfTrailingZeros(y0)
                val mask = (1L shl ntz) - 1L
                val rem = x0 and mask
                val residue = when {
                    rem == 0L -> EXACT
                    2 * rem < y0 -> LT_HALF
                    2 * rem == y0 -> HALF
                    else -> GT_HALF
                }
                coeffSetShiftRight(z, x, ntz)
                return residue
            }
        }
        val bitLenDelta = x.bitLen - y.bitLen
        if (bitLenDelta < 0) {
            val residue = when {
                x.bitLen == 0 -> EXACT
                bitLenDelta <= -2 -> LT_HALF
                else -> Residue.residueFromRemainderDivisor(x, y)
            }
            z.coeffSetZero()
            return residue
        }
        if (bitLenDelta == 0) {
            val cmp = coeffUnscaledCompare(x, y)
            if (cmp < 0) {
                val residue = Residue.residueFromRemainderDivisor(x, y)
                z.coeffSetZero()
                return residue
            }
            if (cmp == 0) {
                z.coeffSetOne()
                return EXACT
            }
        }
        assert(bitLenDelta >= 0)
        //TODO at this point I know that x.bitLen >= y.bitLen and x > y
        // if (bitLenDelta < some-small-number) then I should use repeated subtraction
        if (y.bitLen <= 32) {
            return DivDirect.divx32(z, x, y.dw0)
        }
        return DivKnuth.knuthDivideWrapper(z, x, y, false)
    }

    fun coeffMod(z: Coeff, x: Coeff, y: Coeff) {
        assert(y.coeffIsGTOne())
        if (x.digitLen < y.digitLen) {
            z.coeffSet(x)
            return
        }
        if (x.digitLen == y.digitLen) {
            val cmp = coeffUnscaledCompare(x, y)
            if (cmp < 0) {
                val residue = Residue.residueFromRemainderDivisor(x, y)
                z.coeffSet(x)
                return
            }
            if (cmp == 0) {
                z.coeffSetZero()
                return
            }
        }
        if (y.bitLen <= 32) {
            return DivDirect.modx32(z, x, y.dw0)
        }
        DivKnuth.knuthDivideWrapper(z, x, y, true)
    }

}
