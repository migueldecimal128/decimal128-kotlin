package com.decimal128

import com.decimal128.CoeffCompare.coeffCompare
import com.decimal128.CoeffCompare.coeffGT
import com.decimal128.CoeffCompare.coeffGTOne
import com.decimal128.CoeffSet.coeffSet
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
            val cmp = coeffCompare(x, y)
            if (cmp < 0) {
                val residue = Residue.residueFromRemainderDivisor(x, y)
                z.coeffSetZero()
                return residue
            }
            if (cmp == 0) {
                z.setOne()
                return EXACT
            }
        }
        assert(bitLenDelta >= 0)
        //TODO at this point I know that x.bitLen >= y.bitLen and x > y
        // if (bitLenDelta < some-small-number) then I should use repeated subtraction
        if (y.bitLen <= 32) {
            return divx32(z, x, y.dw0)
        }
        return DivKnuth.knuthDivideWrapper(z, x, y, false)
    }

    private fun divx32(z: Coeff, x: Coeff, y0: Long): Residue {
        assert((y0 ushr 32) == 0L && y0 > 1L)
        if ((x.dw3 or x.dw2) == 0L) {
            if (x.dw1 == 0L)
                return div64x32(z, x.dw0, y0)
            else
                return div128x32(z, x.dw1, x.dw0, y0)
        }
        if (x.dw3 == 0L) {
            return div192x32(z, x.dw2, x.dw1, x.dw0, y0)
        }
        return div256x32(z, x.dw3, x.dw2, x.dw1, x.dw0, y0)
    }

    fun div256x32(z: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Residue {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x3
        val dividend7 = (rem shl 32) or (x3 ushr 32)
        val w7 = divideUnsigned(dividend7, y0)
        rem = remainderUnsigned(dividend7, y0)
        // Process low 32 bits of x3
        val dividend6 = (rem shl 32) or (x3 and MASK32)
        val w6 = divideUnsigned(dividend6, y0)
        rem = remainderUnsigned(dividend6, y0)
        // Process top 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        val w5 = divideUnsigned(dividend5, y0)
        rem = remainderUnsigned(dividend5, y0)
        // Process low 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32)
        val w4 = divideUnsigned(dividend4, y0)
        rem = remainderUnsigned(dividend4, y0)
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        val w3 = divideUnsigned(dividend3, y0)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        val w2 = divideUnsigned(dividend2, y0)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = divideUnsigned(dividend1, y0)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        val w0 = divideUnsigned(dividend0, y0)
        rem = remainderUnsigned(dividend0, y0)

        val q3 = (w7 shl 32) or w6
        val q2 = (w5 shl 32) or w4
        val q1 = (w3 shl 32) or w2
        val q0 = (w1 shl 32) or w0

        val y0Doubled = y0 shl 1
        val residue = when {
            rem == 0L -> EXACT
            rem < y0Doubled -> LT_HALF
            rem == y0Doubled -> HALF
            else -> GT_HALF
        }
        z.coeffSet256(q3, q2, q1, q0)
        return residue
    }

    fun div192x32(z: Coeff, x2: Long, x1: Long, x0: Long, y0: Long): Residue {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        val w5 = divideUnsigned(dividend5, y0)
        rem = remainderUnsigned(dividend5, y0)
        // Process low 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32)
        val w4 = divideUnsigned(dividend4, y0)
        rem = remainderUnsigned(dividend4, y0)
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        val w3 = divideUnsigned(dividend3, y0)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        val w2 = divideUnsigned(dividend2, y0)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = divideUnsigned(dividend1, y0)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        val w0 = divideUnsigned(dividend0, y0)
        rem = remainderUnsigned(dividend0, y0)

        val q2 = (w5 shl 32) or w4
        val q1 = (w3 shl 32) or w2
        val q0 = (w1 shl 32) or w0

        val y0Doubled = y0 shl 1
        val residue = when {
            rem == 0L -> EXACT
            rem < y0Doubled -> LT_HALF
            rem == y0Doubled -> HALF
            else -> GT_HALF
        }
        z.coeffSet192(q2, q1, q0)
        return residue
    }

    fun div128x32(z: Coeff, x1: Long, x0: Long, y0: Long): Residue {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        val w3 = divideUnsigned(dividend3, y0)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        val w2 = divideUnsigned(dividend2, y0)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = divideUnsigned(dividend1, y0)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        val w0 = divideUnsigned(dividend0, y0)
        rem = remainderUnsigned(dividend0, y0)

        val q1 = (w3 shl 32) or w2
        val q0 = (w1 shl 32) or w0

        val y0Doubled = y0 shl 1
        val residue = when {
            rem == 0L -> EXACT
            rem < y0Doubled -> LT_HALF
            rem == y0Doubled -> HALF
            else -> GT_HALF
        }
        z.coeffSet128(q1, q0)
        return residue
    }

    fun div64x32(z: Coeff, x0: Long, y0: Long): Residue {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = divideUnsigned(dividend1, y0)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        val w0 = divideUnsigned(dividend0, y0)
        rem = remainderUnsigned(dividend0, y0)

        val q0 = (w1 shl 32) or w0

        val y0Doubled = y0 shl 1
        val residue = when {
            rem == 0L -> EXACT
            rem < y0Doubled -> LT_HALF
            rem == y0Doubled -> HALF
            else -> GT_HALF
        }
        z.coeffSet64(q0)
        return residue
    }

    fun coeffMod(z: Coeff, x: Coeff, y: Coeff) {
        assert(y.isGTOne())
        if (x.digitLen < y.digitLen) {
            z.coeffSet(x)
            return
        }
        if (x.digitLen == y.digitLen) {
            val cmp = coeffCompare(x, y)
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
            return modx32(z, x, y.dw0)
        }
        DivKnuth.knuthDivideWrapper(z, x, y, true)
    }

    private fun modx32(z: Coeff, x: Coeff, y0: Long) {
        assert((y0 ushr 32) == 0L && y0 > 1L)
        val rem =
            if ((x.dw3 or x.dw2) == 0L) {
                if (x.dw1 == 0L)
                    mod64x32(z, x.dw0, y0)
                else
                    mod128x32(z, x.dw1, x.dw0, y0)
            } else if (x.dw3 == 0L) {
                mod192x32(z, x.dw2, x.dw1, x.dw0, y0)
            } else {
                mod256x32(z, x.dw3, x.dw2, x.dw1, x.dw0, y0)
            }
        z.coeffSet64(rem)
    }

    fun mod256x32(z: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x3
        val dividend7 = (rem shl 32) or (x3 ushr 32)
        rem = remainderUnsigned(dividend7, y0)
        // Process low 32 bits of x3
        val dividend6 = (rem shl 32) or (x3 and MASK32)
        rem = remainderUnsigned(dividend6, y0)
        // Process top 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        rem = remainderUnsigned(dividend5, y0)
        // Process low 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32)
        rem = remainderUnsigned(dividend4, y0)
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        rem = remainderUnsigned(dividend0, y0)

        return rem
    }

    fun mod192x32(z: Coeff, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        rem = remainderUnsigned(dividend5, y0)
        // Process low 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32)
        rem = remainderUnsigned(dividend4, y0)
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        rem = remainderUnsigned(dividend0, y0)

        return rem
    }

    fun mod128x32(z: Coeff, x1: Long, x0: Long, y0: Long): Long {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = remainderUnsigned(dividend3, y0)
        // Process low 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32)
        rem = remainderUnsigned(dividend2, y0)
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        rem = remainderUnsigned(dividend0, y0)

        return rem
    }

    fun mod64x32(z: Coeff, x0: Long, y0: Long): Long {
        assert((y0 ushr 32) == 0L)
        var rem = 0L
        // Process top 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = remainderUnsigned(dividend1, y0)
        // Process low 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32)
        rem = remainderUnsigned(dividend0, y0)

        return rem
    }
}

