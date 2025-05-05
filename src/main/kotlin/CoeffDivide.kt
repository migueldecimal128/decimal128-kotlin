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

private const val MASK32 = 0xFFFF_FFFFL

object CoeffDivide {
    fun coeffDiv(z: Coeff, x: Coeff, y: Coeff) =
        coeffDiv_bit(z, x, y)

    fun coeffDiv_bit(z: Coeff, x: Coeff, y: Coeff): Residue {
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
        return knuthDivideWrapper(z, x, y, false)
    }

    fun coeffDiv_digit(z: Coeff, x: Coeff, y: Coeff): Residue {
        if (y.digitLen <= 18) { // use 18 instead of 19 to ensure that hi bit is not set
            val y0 = y.dw0
            if (y0 <= 1L) {
                if (y0 == 0L)
                    throw RuntimeException("div by zero")
                z.coeffSet(x)
                return EXACT
            }
            val x0 = x.dw0
            if (x.digitLen <= 18) {
                val quot = x0 / y0
                val rem = x0 % y0
                val residue = when {
                    rem == 0L -> EXACT
                    2 * rem < y0 -> LT_HALF
                    2 * rem == y0 -> HALF
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
        val xBitLen = x.bitLen
        val yBitLen = y.bitLen
        if (xBitLen < yBitLen) {
            val residue = when {
                xBitLen == 0 -> EXACT
                yBitLen - xBitLen >= 2 -> LT_HALF
                else -> Residue.residueFromRemainderDivisor(x, y)
            }
            z.coeffSetZero()
            return residue
        }
        if (xBitLen == yBitLen) {
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
        val bitLenDelta = xBitLen - yBitLen
        assert(bitLenDelta >= 0)
        //TODO at this point I know that xBitLen >= yBitLen and x > y
        // if (bitLenDelta < some-small-number) then I should use repeated subtraction
        if (y.digitLen < POW10_128_OFFSET && (y.dw0 ushr 32) == 0L) {
            return divx32(z, x, y.dw0)
        }
        return knuthDivideWrapper(z, x, y, false)
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
        if (y.digitLen < POW10_128_OFFSET && (y.dw0 ushr 32) == 0L) {
            return modx32(z, x, y.dw0)
        }
        knuthDivideWrapper(z, x, y, true)
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

    private val q = IntArray(8)
    private val vn = IntArray(9)
    private val un = IntArray(9)

    fun knuthDivideWrapper(z: Coeff, x: Coeff, y: Coeff, wantRemainder: Boolean): Residue {
        assert(coeffGTOne(y))
        assert(coeffGT(x, y))

        un[0] = x.dw0.toInt()
        un[1] = (x.dw0 ushr 32).toInt()
        un[2] = x.dw1.toInt()
        un[3] = (x.dw1 ushr 32).toInt()
        un[4] = x.dw2.toInt()
        un[5] = (x.dw2 ushr 32).toInt()
        un[6] = x.dw3.toInt()
        un[7] = (x.dw3 ushr 32).toInt()
        un[8] = 0

        var unNonZeroLen = 8
        while (unNonZeroLen > 0 && un[unNonZeroLen - 1] == 0) {
            --unNonZeroLen
        }
        val m = unNonZeroLen

        vn[0] = y.dw0.toInt()
        vn[1] = (y.dw0 ushr 32).toInt()
        vn[2] = y.dw1.toInt()
        vn[3] = (y.dw1 ushr 32).toInt()
        vn[4] = y.dw2.toInt()
        vn[5] = (y.dw2 ushr 32).toInt()
        vn[6] = y.dw3.toInt()
        vn[7] = (y.dw3 ushr 32).toInt()

        var vnNonZeroIndex = 8
        var vnNonZeroVal = 0
        while (vnNonZeroVal == 0 && --vnNonZeroIndex >= 0) {
            vnNonZeroVal = vn[vnNonZeroIndex]
        }
        val n = vnNonZeroIndex + 1
        val s = Integer.numberOfLeadingZeros(vnNonZeroVal)

        if (s != 0) {
            for (i in n - 1 downTo 1) {
                vn[i] = (vn[i] shl s) or (vn[i - 1] ushr -s)
            }
            vn[0] = vn[0] shl s

            un[m] = un[m - 1] ushr -s
            for (i in m - 1 downTo 1) {
                un[i] = (un[i] shl s) or (un[i - 1] ushr -s)
            }
            un[0] = un[0] shl s
        }

        q.fill(0)

        knuthDivideCore(m, n)

        if (wantRemainder) {
            coeffSetShiftRight(z, un, n, s)
            return EXACT
        }

        val residue =
            if (s == 0 && un[n - 1] < 0) {
                // msb of remainder is set ... doubling it will make it bigger than the divisor
                GT_HALF
            } else {
                if (s == 0) {
                    for (i in n - 1 downTo 1) {
                        un[i] = (un[i] shl 1) or (un[i - 1] ushr -1)
                    }
                    un[0] = un[0] shl 1
                } else {
                    val s1 = s - 1
                    if (s1 > 0) {
                        for (i in 0 until n - 1) {
                            un[i] = (un[i + 1] shl -s1) or (un[i] ushr s1)
                        }
                        un[n - 1] = un[n - 1] ushr s1
                    }
                }
                val cmp = coeffCompare(y, un)
                if (cmp < 0)
                    Residue.LT_HALF
                else if (cmp == 0)
                    Residue.HALF
                else
                    GT_HALF
            }
        coeffSet(z, q, m)
        return residue
    }


    /**
     * Multi‐word division (Knuth’s Algorithm D) in base 2^32.
     *
     * q: quotient array (length ≥ m – n + 1)
     * r: remainder array (length ≥ n), or null if you don’t need it
     * u: dividend array (length = m), little‐endian (u[0] = low word)
     * v: divisor array (length = n ≥ 2), little‐endian, v[n − 1] ≠ 0
     * m: number of words in u (≥ n)
     * n: number of words in v (≥ 1)
     *
     * Returns 0 on success, 1 if (m < n || n ≤ 0 || v[n − 1] == 0).
     */

    fun knuthDivideCore(
//        q: IntArray,
//        r: IntArray,
//        u: IntArray,
//        v: IntArray,
        m: Int,
        n: Int
    ) {
        // invalid parameters?
        if (m > 8 || m < 2 || m < n || n < 2) {
            throw RuntimeException("invalid args")
        }

        val vn_1 = vn[n - 1].toLong() and MASK32
        val vn_2 = vn[n - 2].toLong() and MASK32

        // -- main loop --
        for (j in m - n downTo 0) {

            // estimate q̂ = (un[j+n]*B + un[j+n-1]) / vn[n-1]
            val hi = un[j + n].toLong() and MASK32
            val lo = un[j + n - 1].toLong() and MASK32
            //if (hi == 0L && lo < vn_1) // this would short-circuit,
            //    continue               // but probability is astronomically small
            val num = (hi shl 32) or lo
            var qhat = divideUnsigned(num, vn_1)
            var rhat = remainderUnsigned(num, vn_1)

            // correct estimate
            while ((qhat ushr 32) != 0L ||
                compareUnsigned(
                    qhat * vn_2, (rhat shl 32) + (un[j + n - 2].toLong() and MASK32)
                ) > 0
            ) {
                qhat--
                rhat += (vn[n - 1].toLong() and MASK32)
                if ((rhat ushr 32) != 0L)
                    break
            }


            // multiply & subtract
            var carry = 0L
            for (i in 0 until n) {
                val prod = qhat * (vn[i].toLong() and MASK32)
                val prodHi = prod ushr 32
                val prodLo = prod and MASK32
                val unIJ = (un[j + i].toLong() and MASK32)
                val t = unIJ - prodLo - carry
                un[j + i] = t.toInt()
                carry = prodHi - (t shr 32) // t is signed, so this should *indeed* be signed shr
            }
            val t = (un[j + n].toLong() and MASK32) - carry
            un[j + n] = t.toInt()
            q[j] = (qhat - (t ushr 63)).toInt()
            if (t < 0) {
                var c2 = 0L
                for (i in 0 until n) {
                    val sum = (un[j + i].toLong() and MASK32) +
                            (vn[i].toLong() and MASK32) + c2
                    un[j + i] = sum.toInt()
                    c2 = sum ushr 32
                }
                un[j + n] += c2.toInt()
            }
        }
    }
}

