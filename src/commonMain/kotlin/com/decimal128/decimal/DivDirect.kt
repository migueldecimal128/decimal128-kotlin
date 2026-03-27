package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF

internal object DivDirect {

    fun divx32(z: C256, x: C256, y0: Long): Residue {
        val xBitLen = x.bitLen
        val rem = when {
            ((y0 > 1L) && (y0 ushr 32) == 0L) -> when {
                xBitLen <=  64 ->  divMod64x32(z, x.dw0, y0)
                xBitLen <= 128 -> divMod128x32(z, x.dw1, x.dw0, y0)
                xBitLen <= 192 -> divMod192x32(z, x.dw2, x.dw1, x.dw0, y0)
                else ->           divMod256x32(z, x.dw3, x.dw2, x.dw1, x.dw0, y0)
            }
            (y0 == 1L) -> {
                z.c256Set(x)
                0
            }
            else -> throw IllegalArgumentException()
        }
        val y0Doubled = y0 shl 1
        val residue = when {
            rem == 0L -> EXACT
            rem < y0Doubled -> LT_HALF
            rem == y0Doubled -> HALF
            else -> GT_HALF
        }
        return residue
    }

    fun modx32(z: C256, x: C256, y0: Long) {
        verify { (y0 ushr 32) == 0L && y0 > 1L }
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
        z.c256Set64(rem)
    }

    fun mod256x32(z: C256, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }
        var rem = 0L
        // Process hi 32 bits of x3
        val dividend7 = (rem shl 32) or (x3 ushr 32)
        rem = unsignedRem(dividend7, y0)
        // Process lo 32 bits of x3
        val dividend6 = (rem shl 32) or (x3 and MASK32L)
        rem = unsignedRem(dividend6, y0)
        // Process hi 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        rem = unsignedRem(dividend5, y0)
        // Process lo 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32L)
        rem = unsignedRem(dividend4, y0)
        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = unsignedRem(dividend3, y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        rem = unsignedRem(dividend2, y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = unsignedRem(dividend1, y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        rem = unsignedRem(dividend0, y0)

        return rem
    }

    fun mod192x32(z: C256, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }
        var rem = 0L
        // Process hi 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        rem = unsignedRem(dividend5, y0)
        // Process lo 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32L)
        rem = unsignedRem(dividend4, y0)
        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = unsignedRem(dividend3, y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        rem = unsignedRem(dividend2, y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = unsignedRem(dividend1, y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        rem = unsignedRem(dividend0, y0)

        return rem
    }

    fun mod128x32(z: C256, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }
        var rem = 0L
        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = unsignedRem(dividend3, y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        rem = unsignedRem(dividend2, y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = unsignedRem(dividend1, y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        rem = unsignedRem(dividend0, y0)

        return rem
    }

    fun mod64x32(z: C256, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }
        var rem = 0L
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = unsignedRem(dividend1, y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        rem = unsignedRem(dividend0, y0)

        return rem
    }

    fun divModX32(z: C256?, x: C256, y0: Long): Long {
        if ((y0 ushr 16) == 0L)
            return divModX16(z, x, y0)
        val x0 = x.dw0; val x1 = x.dw1
        val x2 = x.dw2; val x3 = x.dw3
        val xBitLen = x.bitLen
        if ((y0 ushr 32) == 0L) {
            val rem = when {
                xBitLen <= 64 -> divMod64x32(z, x0, y0)
                xBitLen <= 128 -> divMod128x32(z, x1, x0, y0)
                xBitLen <= 192 -> divMod192x32(z, x2, x1, x0, y0)
                else -> divMod256x32(z, x3, x2, x1, x0, y0)
            }
            return rem
        }
        throw IllegalArgumentException()
    }

    fun divModX16(z: C256?, x: C256, y0: Long): Long {
        val x0 = x.dw0; val x1 = x.dw1
        val x2 = x.dw2; val x3 = x.dw3
        val xBitLen = x.bitLen
        val rem = when {
            ((y0 > 1L) && (y0 ushr 16) == 0L) -> when {
                xBitLen <=  64 ->  divMod64x32(z, x0, y0) // yes, this is x32
                xBitLen <= 128 -> divMod128x16(z, x1, x0, y0)
                xBitLen <= 192 -> divMod192x16(z, x2, x1, x0, y0)
                else ->           divMod256x16(z, x3, x2, x1, x0, y0)
            }
            (y0 == 1L) -> {
                z?.c256Set(x)
                0L
            }
            else -> throw IllegalArgumentException()
        }
        return rem
    }

    fun divMod256x32(z: C256?, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }

        // Process 64 bits of x3
        val q3 = unsignedDiv(x3, y0)
        var rem = x3 - (q3 * y0)
        // Process hi 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        val w5 = unsignedDiv(dividend5, y0)
        rem = dividend5 - (w5 * y0)
        // Process lo 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32L)
        val w4 = unsignedDiv(dividend4, y0)
        rem = dividend4 - (w4 * y0)
        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        val w3 = unsignedDiv(dividend3, y0)
        rem = dividend3 - (w3 * y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        val w2 = unsignedDiv(dividend2, y0)
        rem = dividend2 - (w2 * y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q2 = (w5 shl 32) or w4
        val q1 = (w3 shl 32) or w2
        val q0 = (w1 shl 32) or w0

        z?.c256Set256(q3, q2, q1, q0)
        return rem
    }

    fun divMod192x32(z: C256?, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }

        // Process hi 64 bits of x2
        val q2 = unsignedDiv(x2, y0)
        var rem = x2 - (q2 * y0)

        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        val w3 = unsignedDiv(dividend3, y0)
        rem = dividend3 - (w3 * y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        val w2 = unsignedDiv(dividend2, y0)
        rem = dividend2 - (w2 * y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q1 = (w3 shl 32) or w2
        val q0 = (w1 shl 32) or w0

        z?.c256Set192(q2, q1, q0)
        return rem
    }

    fun divMod128x32(z: C256?, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }

        // Process 64 bits of x1
        val q1 = unsignedDiv(x1, y0)
        var rem = x1 - (q1 * y0)

        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q0 = (w1 shl 32) or w0

        z?.c256Set128(q1, q0)
        return rem
    }

    fun divMod64x32(z: C256?, x0: Long, y0: Long): Long {
        verify { (y0 ushr 32) == 0L }

        val q0 = unsignedDiv(x0, y0)
        val rem = x0 - (q0 * y0)

        z?.c256Set64(q0)
        return rem
    }

    fun mod256x16(z: C256, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 16) == 0L }
        var rem = 0L
        // Process hi 32 bits of x3
        val dividend7 = (rem shl 32) or (x3 ushr 32)
        rem = unsignedRem(dividend7, y0)
        // Process lo 32 bits of x3
        val dividend6 = (rem shl 32) or (x3 and MASK32L)
        rem = unsignedRem(dividend6, y0)
        // Process hi 32 bits of x2
        val dividend5 = (rem shl 32) or (x2 ushr 32)
        rem = unsignedRem(dividend5, y0)
        // Process lo 32 bits of x2
        val dividend4 = (rem shl 32) or (x2 and MASK32L)
        rem = unsignedRem(dividend4, y0)
        // Process hi 32 bits of x1
        val dividend3 = (rem shl 32) or (x1 ushr 32)
        rem = unsignedRem(dividend3, y0)
        // Process lo 32 bits of x1
        val dividend2 = (rem shl 32) or (x1 and MASK32L)
        rem = unsignedRem(dividend2, y0)
        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        rem = unsignedRem(dividend1, y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        rem = unsignedRem(dividend0, y0)

        return rem
    }

    fun divMod256x16(z: C256?, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 16) == 0L }

        // Process 64 bits of x3
        val q3 = unsignedDiv(x3, y0)
        var rem = x3 - (q3 * y0)

        // Process hi 48 bits of x2
        val dividend3 = (rem shl 48) or (x2 ushr 16)
        val w3 = unsignedDiv(dividend3, y0)
        rem = dividend3 - (w3 * y0)
        // Process lo 16 bits of x2 + hi 32 bits of x1
        val dividend2 = (rem shl 48) or ((x2 and MASK16L) shl 32) or (x1 ushr 32)
        val w2 = unsignedDiv(dividend2, y0)
        rem = dividend2 - (w2 * y0)
        // Process lo 32 bits of x1 + hi 16 bits of x0
        val dividend1 = (rem shl 48) or ((x1 and MASK32L) shl 16) or (x0 ushr 48)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 48 bits of x0
        val dividend0 = (rem shl 48) or (x0 and MASK48L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q2 = (w3 shl 16) or (w2 ushr 32)
        val q1 = (w2 shl 32) or (w1 ushr 16)
        val q0 = (w1 shl 48) or w0

        z?.c256Set256(q3, q2, q1, q0)
        return rem
    }

    fun divMod192x16(z: C256?, x2: Long, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 16) == 0L }

        // Process 64 bits of x2
        val q2 = unsignedDiv(x2, y0)
        var rem = x2 - (q2 * y0)

        // Process hi 48 bits of x1
        val dividend2 = (rem shl 48) or (x1 ushr 16)
        val w2 = unsignedDiv(dividend2, y0)
        rem = dividend2 - (w2 * y0)
        // Process lo 16 bits of x1 + hi 32 bits of x0
        val dividend1 = (rem shl 48) or ((x1 and MASK16L) shl 32) or (x0 ushr 32)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q1 = (w2 shl 16) or (w1 ushr 32)
        val q0 = (w1 shl 32) or w0

        z?.c256Set192(q2, q1, q0)
        return rem
    }

    fun divMod128x16(z: C256?, x1: Long, x0: Long, y0: Long): Long {
        verify { (y0 ushr 16) == 0L }

        // Process 64 bits of x1
        val q1 = unsignedDiv(x1, y0)
        var rem = x1 - (q1 * y0)

        // Process hi 32 bits of x0
        val dividend1 = (rem shl 32) or (x0 ushr 32)
        val w1 = unsignedDiv(dividend1, y0)
        rem = dividend1 - (w1 * y0)
        // Process lo 32 bits of x0
        val dividend0 = (rem shl 32) or (x0 and MASK32L)
        val w0 = unsignedDiv(dividend0, y0)
        rem = dividend0 - (w0 * y0)

        val q0 = (w1 shl 32) or w0

        z?.c256Set128(q1, q0)
        return rem
    }

}
