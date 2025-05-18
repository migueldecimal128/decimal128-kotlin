package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.GT_HALF
import com.decimal128.Residue.Companion.HALF
import com.decimal128.Residue.Companion.LT_HALF
import java.lang.Long.divideUnsigned
import java.lang.Long.remainderUnsigned

object DivDirect {
    
    fun divx32(z: Coeff, x: Coeff, y0: Long): Residue {
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

    fun modx32(z: Coeff, x: Coeff, y0: Long) {
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