@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.hugeint

import com.decimal128.decimal.unsignedCmp
import com.decimal128.decimal.unsignedDiv
import com.decimal128.decimal.unsignedMod
import com.decimal128.decimal.unsignedMulHi
import kotlin.math.min
import kotlin.math.max


// magia == MAGnitude IntArray ... it's magic!

private const val HEX_DIGIT_AND_UNDERSCORE_MASK  = 0x007E_8000_007E_03FFL

private const val BARRETT_MU_1E9: Long = 0x44B82FA09L       // floor(2^64 / 1e9)
private const val ONE_E_9: Long = 1_000_000_000L

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
private const val S_U64_DIV_1E4 = 11 // + 64 high

object Magia {

    val ZERO = IntArray(0)
    // be careful if you ever start mutating not to inadvertently modify this
    val ONE = intArrayOf(1)

    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL
    private inline fun dw32(n: Int) = n.toUInt().toULong()

    fun testBit(x: IntArray, bitIndex: Int): Boolean = testBit(x, x.size, bitIndex)

    fun testBit(x: IntArray, xLen: Int, bitIndex: Int): Boolean {
        if (xLen >= 0 && xLen <= x.size) {
            val wordIndex = bitIndex ushr 5
            if (wordIndex >= xLen)
                return false
            val word = x[wordIndex]
            val bitMask = 1 shl (bitIndex and 0x1F)
            return (word and bitMask) != 0
        } else {
            throw IllegalArgumentException()
        }
    }

    fun testAnyBitInLowerN(x: IntArray, bitCount: Int): Boolean =
        testAnyBitInLowerN(x, x.size, bitCount)

    fun testAnyBitInLowerN(x: IntArray, xLen: Int, bitCount: Int): Boolean {
        if (xLen >= 0 && xLen <= x.size) {
            val lastBitIndex = bitCount - 1
            val lastWordIndex = lastBitIndex ushr 5
            for (i in 0..<lastWordIndex) {
                if (i == xLen)
                    return false
                if (x[i] != 0)
                    return true
            }
            if (lastWordIndex == xLen)
                return false
            val bitMask = -1 ushr (0x1F - (lastBitIndex and 0x1F))
            return (x[lastWordIndex] and bitMask) != 0
        } else {
            throw IllegalArgumentException()
        }
    }

    fun toRawULong(x: IntArray, xLen: Int): ULong {
        return when (xLen) {
            0 -> 0uL
            1 -> dw32(x[0])
            else -> (dw32(x[1]) shl 32) or dw32(x[0])
        }
    }

    fun toRawInt(x: IntArray, xLen: Int): UInt {
        return if (xLen == 0) 0u else x[0].toUInt()
    }

    fun newFromULong(dw: ULong): IntArray =
        if (dw != 0uL) intArrayOf(dw.toInt(), (dw shr 32).toInt()) else ZERO

    fun nonZeroLimbLen(x: IntArray): Int = nonZeroLimbLen(x, x.size)

    fun nonZeroLimbLen(x: IntArray, xLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size) {
            for (i in xLen - 1 downTo 0)
                if (x[i] != 0)
                    return i + 1
            return 0
        } else {
            throw IllegalArgumentException()
        }
    }

    inline fun newWithMinLen(minLimbLen: Int) : IntArray {
        val t = if (minLimbLen <= 0) 1 else minLimbLen
        val allocSize = (t + 3) and 3.inv()
        return IntArray(allocSize)
    }

    fun newLongerCopyWithMinLen(x: IntArray, newMinLimbLen: Int) : IntArray {
        if (newMinLimbLen > x.size) {
            val z = newWithMinLen(newMinLimbLen)
            System.arraycopy(x, 0, z, 0, x.size)
            return z
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newCopyWithMinLen(x: IntArray, xLen: Int, newMinLimbLen: Int) : IntArray {
        if (xLen >= 0 && xLen <= x.size && newMinLimbLen >= xLen) {
            val z = newWithMinLen(newMinLimbLen)
            System.arraycopy(x, 0, z, 0, xLen)
            return z
        } else {
            throw IllegalArgumentException()
        }
    }

    inline fun newWithBitLen(bitLen: Int) =
        if (bitLen > 0) IntArray((bitLen + 0x1F) ushr 5) else ZERO

    inline fun newWithSetBit(bitIndex: Int): IntArray {
        if (bitIndex >= 0) {
            val magia = Magia.newWithBitLen(bitIndex + 1)
            magia[magia.lastIndex] = 1 shl (bitIndex and 0x1F)
            return magia
        }
        throw IllegalArgumentException()
    }

    fun newAdd(x: IntArray, w: UInt): IntArray {
        val newBitLen = max(bitLen(x), (32 - w.countLeadingZeroBits())) + 1
        val z = newWithBitLen(newBitLen)
        var carry = w.toULong()
        val indexLimit = min(x.size, z.size)
        for (i in 0..<indexLimit) {
            val t = dw32(x[i]) + carry
            z[i] = t.toInt()
            carry = t shr 32
        }
        if (carry != 0uL)
            z[z.lastIndex] = 1
        return z
    }

    fun newAdd(x: IntArray, dw: Long): IntArray {
        val newBitLen = max(bitLen(x), (64 - dw.countLeadingZeroBits())) + 1
        val z = newWithBitLen(newBitLen)
        var carry = dw
        val indexLimit = min(x.size, z.size)
        for (i in 0..<indexLimit) {
            val t = U32(x[i]) + (carry and 0xFFFF_FFFFL)
            z[i] = t.toInt()
            carry = (t ushr 32) + (carry ushr 32)
        }
        if (carry != 0L)
            z[indexLimit] = carry.toInt()
        if (carry ushr 32 != 0L)
            z[indexLimit + 1] = (carry ushr 32).toInt()
        return z
    }

    fun newAdd(x: IntArray, y: IntArray): IntArray {
        val newBitLen = max(bitLen(x), bitLen(y)) + 1
        val z = newWithBitLen(newBitLen)

        val min = min(z.size, min(x.size, y.size))
        var carry = 0L
        var i = 0
        while (i < min) {
            val t = U32(x[i]) + U32(y[i]) + carry
            z[i] = t.toInt()
            carry = t ushr 32
            check(carry in 0L..1L)
            ++i
        }
        val longer = if (x.size > y.size) x else y
        while (i < longer.size && i < z.size) {
            val t = U32(longer[i]) + carry
            z[i] = t.toInt()
            carry = t ushr 32
            ++i
        }
        if (carry != 0L && i < z.size)
            z[i] = 1
        return z
    }

    fun newOrMutateAdd(x: IntArray, w: UInt): IntArray {
        val carry = mutateAdd(x, x.size, w)
        if (carry == 0u)
            return x
        val z = newCopyWithLimbLen(x, x.size + 1)
        z[x.size] = carry.toInt()
        return z
    }

    fun mutateAdd(x: IntArray, xLen: Int, w: UInt): UInt {
        if (xLen >= 0 && xLen <= x.size) {
            var carry = w.toULong()
            var i = 0
            while (carry != 0uL && i < xLen) {
                val t = dw32(x[i]) + carry
                x[i] = t.toInt()
                carry = t shr 32
                ++i
            }
            return carry.toUInt()
        } else {
            throw IllegalArgumentException()
        }
    }

    fun mutateAdd(x: IntArray, xLen: Int, dw: ULong): ULong {
        if (xLen >= 0 && xLen <= x.size) {
            var carry = dw
            var i = 0
            while (carry != 0uL && i < xLen) {
                val s = dw32(x[i]) + (carry and 0xFFFF_FFFFuL)
                x[i] = s.toInt()
                carry = (carry shr 32) + (s shr 32)
                ++i
            }
            return carry
        } else {
            throw IllegalArgumentException()
        }
    }

    fun mutateAdd(x: IntArray, xLen: Int, y: IntArray, yLen: Int): UInt {
        if (xLen >= 0 && xLen <= x.size && yLen >= 0 && yLen <= y.size) {
            check(xLen == 0 || x[xLen - 1] != 0)
            if (y[yLen - 1] == 0)
                println("foo!")
            check(yLen == 0 || y[yLen - 1] != 0)
            val minLen = min(xLen, yLen)
            var carry = 0uL
            for (i in 0..<minLen) {
                carry = dw32(x[i]) + dw32(y[i]) + carry
                x[i] = carry.toInt()
                carry = carry shr 32
            }
            if (xLen >= yLen) {
                var i = yLen
                while (carry != 0uL && i < xLen) {
                    carry = dw32(x[i]) + carry
                    x[i] = carry.toInt()
                    carry = carry shr 32
                    ++i
                }
            } else {
                for (i in minLen..<yLen) {
                    carry = dw32(y[i]) + carry
                    x[i] = carry.toInt()
                    carry = carry shr 32
                }
            }
            return carry.toUInt()
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newSub(x: IntArray, w: Int): IntArray {
        val z = IntArray(nonZeroLimbLen(x))
        var orAccumulator = 0
        var borrow = U32(w)
        for (i in z.indices) {
            val t = U32(x[i]) - borrow
            val zi = t.toInt()
            z[i] = zi
            orAccumulator = orAccumulator or zi
            borrow = t ushr 63
        }
        return if (orAccumulator == 0 || borrow != 0L) ZERO else z
    }

    fun newSub(x: IntArray, dw: Long): IntArray {
        val z = IntArray(nonZeroLimbLen(x))
        var orAccumulator = 0
        var borrow = 0L
        if (z.isNotEmpty()) {
            val t0 = U32(x[0]) - (dw and 0xFFFF_FFFFL)
            val z0 = t0.toInt()
            z[0] = z0
            orAccumulator = z0
            if (z.size > 1) {
                borrow = t0 ushr 63
                val t1 = U32(x[1]) - (dw ushr 32) - borrow
                val z1 = t1.toInt()
                z[1] = z1
                orAccumulator = orAccumulator or z1
                borrow = t1 ushr 63
                var i = 2
                while (i < z.size) {
                    val t = U32(x[i]) - borrow
                    val zi = t.toInt()
                    z[i] = zi
                    orAccumulator = orAccumulator or zi
                    borrow = t ushr 63
                    ++i
                }
            }
        }
        return if (orAccumulator == 0 || borrow != 0L) ZERO else z
    }

    fun newSub(x: IntArray, y: IntArray): IntArray {
        check (compare(x, y) >= 0)
        val z = IntArray(nonZeroLimbLen(x))
        var orAccumulator = 0
        var borrow = 0L
        val min = min(z.size, min(x.size, y.size))
        var i = 0
        while (i < min) {
            val t = U32(x[i]) - U32(y[i]) - borrow
            val zi = t.toInt()
            z[i] = zi
            orAccumulator = orAccumulator or zi
            borrow = t ushr 63
            check(borrow in 0L..1L)
            ++i
        }
        while (i < x.size && i < z.size) {
            val t = U32(x[i]) - borrow
            val zi = t.toInt()
            z[i] = zi
            orAccumulator = orAccumulator or zi
            borrow = t ushr 63
            ++i
        }
        check (borrow == 0L)
        return if (orAccumulator != 0)
            z
        else
            ZERO
    }

    fun mutateSub(x: IntArray, xLen: Int, w: UInt) {
        if (xLen >= 0 && xLen <= x.size) {
            var borrow = w.toULong()
            var i = 0
            while (borrow != 0uL && i < xLen) {
                borrow = dw32(x[i]) - borrow
                x[i] = borrow.toInt()
                borrow = borrow shr 63
                ++i
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    fun mutateSub(x: IntArray, xLen: Int, dw: ULong) {
        if (xLen >= 2 && xLen <= x.size) {
            check(xLen >= 2)
            val t0 = dw32(x[0]) - (dw and 0xFFFF_FFFFuL)
            x[0] = t0.toInt()
            val t1 = dw32(x[1]) - (dw shr 32) - (t0 shr 63)
            x[1] = t1.toInt()
            var borrow = t1 shr 63
            var i = 2
            while (borrow != 0uL && i < xLen) {
                borrow = dw32(x[i]) - borrow
                x[i] = borrow.toInt()
                borrow = borrow shr 63
                ++i
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    fun mutateSub(x: IntArray, xLen: Int, y: IntArray, yLen: Int) {
        if (xLen >= 0 && xLen <= x.size && yLen >= 0 && yLen <= y.size) {
            check(xLen == 0 || x[xLen - 1] != 0)
            check(yLen == 0 || y[yLen - 1] != 0)
            check(xLen >= yLen)
            var borrow = 0uL // 0 or 1
            var i = 0
            while (i < yLen) {
                borrow = dw32(x[i]) - dw32(y[i]) - borrow
                x[i] = borrow.toInt()
                borrow = borrow shr 63
                ++i
            }
            while (borrow != 0uL && i < xLen) {
                borrow = dw32(x[i]) - borrow
                x[i] = borrow.toInt()
                borrow = borrow shr 63
                ++i
            }
            check(borrow == 0uL)
        } else {
            throw IllegalArgumentException()
        }
    }

    fun mutateReverseSub(x: IntArray, xLen: Int, y: IntArray, yLen: Int) {
        if (xLen >= 0 && xLen <= x.size && yLen >= 0 && yLen <= y.size) {
            // x is usually not normalized .. so don't check for it
            check(yLen == 0 || y[yLen - 1] != 0)
            if (xLen != yLen)
                print("foo!")
            check(xLen == yLen)
            var borrow = 0uL // 0 or 1
            var i = 0
            while (i < yLen) {
                borrow = dw32(y[i]) - dw32(x[i]) - borrow
                x[i] = borrow.toInt()
                borrow = borrow shr 63
                ++i
            }
            check(borrow == 0uL)
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newMul(x: IntArray, w: UInt): IntArray {
        val bitLenX = bitLen(x)
        val bitLenN = 32 - w.countLeadingZeroBits()
        if (bitLenX == 0 || bitLenN == 0)
            return ZERO
        val newBitLen = bitLenX + bitLenN
        val prod = newWithBitLen(newBitLen)
        val w64 = w.toULong()
        var carry = 0uL
        for (i in x.indices) {
            val t = dw32(x[i]) * w64 + carry
            prod[i] = t.toInt()
            carry = t shr 32
        }
        if (carry != 0uL)
            prod[prod.lastIndex] = carry.toInt()
        return prod
    }

    /**
     * Mutates x: IntArray in-place.
     *
     * Used during parsing of base-10 text string.
     */
    private fun mutateFma(x: IntArray, m: Int, a: Int) {
        val m64 = U32(m)
        var carry = U32(a)
        for (i in x.indices) {
            val t = U32(x[i]) * m64 + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
    }

    fun newMul(x: IntArray, dw: ULong): IntArray {
        val lo = dw and 0xFFFF_FFFFuL
        val hi = dw shr 32
        if (hi == 0uL)
            return newMul(x, lo.toUInt())
        val xBitLen = bitLen(x)
        if (xBitLen == 0)
            return ZERO
        val newBitLen = bitLen(x) + (64 - dw.countLeadingZeroBits())
        val z = newWithBitLen(newBitLen)

        var carryLo = 0uL

        var ppPrevHi = 0uL

        // i = 1…n-1: do both halves in one pass
        for (i in 0 until z.size) {
            val xi = if (i < x.size) dw32(x[i]) else 0uL

            val pp = xi * lo + carryLo + (ppPrevHi and 0xFFFF_FFFFuL)
            z[i] = pp.toInt()
            carryLo = pp shr 32

            ppPrevHi = xi * hi + (ppPrevHi shr 32)
        }

        return z
    }

    fun newMul(x: IntArray, y: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        val yLen = nonZeroLimbLen(y)
        if (xLen == 0 || yLen == 0)
            return ZERO
        val p = IntArray(xLen + yLen)
        mul(p, x, xLen, y, yLen)
        return p
    }

    fun mul(p: IntArray, x: IntArray, xLen: Int, y: IntArray, yLen: Int): IntArray {
        if (xLen > 0 && yLen > 0 && xLen <= x.size && yLen <= y.size && (xLen + yLen) <= p.size) {
            for (i in 0..<xLen) {
                val xLimb = U32(x[i])
                var carry = 0L
                for (j in 0..<yLen) {
                    val yLimb = U32(y[j])
                    val t = xLimb * yLimb + U32(p[i + j]) + carry
                    p[i + j] = t.toInt()
                    carry = t ushr 32
                }
                if (i + yLen < p.size)
                    p[i + yLen] = carry.toInt()
            }
            return p
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newSqr(x: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        if (xLen == 0)
            return ZERO
        val p = IntArray(2 * xLen)
        sqr(p, x, xLen)
        return p
    }

    fun sqr(p: IntArray, x: IntArray, xLen: Int) {
        // test to encourage bounds check elimination
        if (xLen > 0 && xLen <= x.size && xLen * 2 <= p.size) {
            // 1) Cross terms: for i<j, add (x[i]*x[j]) twice into p[i+j]
            // these terms are doubled
            for (i in 0..<xLen) {
                val xi = U32(x[i])
                var carry = 0L
                for (j in (i + 1)..<xLen) {
                    val prod = xi * U32(x[j])        // 32x32 -> 64
                    // add once
                    val t1 = prod + U32(p[i + j]) + carry
                    val p1 = t1 and 0xFFFF_FFFFL
                    carry = t1 ushr 32
                    // add second time (doubling) — avoids (prod << 1) overflow
                    val t2 = prod + p1
                    p[i + j] = t2.toInt()
                    carry += t2 ushr 32
                }
                // flush carry to the next limb(s)
                val k = i + xLen
                val t = U32(p[k]) + carry
                p[k] = t.toInt()
                carry = t ushr 32
                if (carry != 0L)
                    ++p[k + 1]
            }

            // 2) Diagonals: add x[i]**2 into columns 2*i and 2*i+1
            // terms on the diagonal are not doubled
            for (i in 0..<xLen) {
                val sq = U32(x[i]) * U32(x[i])      // 64-bit
                // add low 32 to p[2*i]
                var t = U32(p[2 * i]) + (sq and 0xFFFF_FFFFL)
                p[2 * i] = t.toInt()
                var carry = t ushr 32
                // add high 32 (and carry) to p[2*i+1]
                t = U32(p[2 * i + 1]) + (sq ushr 32) + carry
                p[2 * i + 1] = t.toInt()
                carry = t ushr 32
                // propagate any remaining carry
                var k = 2 * i + 2
                while (carry != 0L) {
                    t = U32(p[k]) + carry
                    p[k] = t.toInt()
                    carry = t ushr 32
                    k++
                }
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newCopyMinimum(src: IntArray) = newCopyWithLimbLen(src, nonZeroLimbLen(src))

    fun newCopyMinimum4(src: IntArray, srcLen: Int) = newCopyWithLimbLen(src, max(nonZeroLimbLen(src, srcLen), 4))

    fun newCopyWithLimbLen(src: IntArray, newWordLen: Int): IntArray {
        if (newWordLen > 0) {
            val dst = IntArray(newWordLen)
            copy(dst, src)
            return dst
        }
        return ZERO
    }

    fun newCopyRoundUp(src: IntArray, srcLen: Int) = newCopyWithLimbLen(src, max(nonZeroLimbLen(src, srcLen), 4))

    fun newCopyWithLimbLenRoundUp(src: IntArray, srcLen: Int, newWordLen: Int): IntArray {
        if (newWordLen > 0) {
            val dst = IntArray(newWordLen)
            copy(dst, src)
            return dst
        }
        return ZERO
    }

    private fun newCopyWithBitLen(src: IntArray, newBitLen: Int): IntArray {
        val dst = newWithBitLen(newBitLen)
        copy(dst, src)
        return dst
    }

    private fun copy(dst: IntArray, src: IntArray) {
        var i = 0
        val m = min(dst.size, src.size)
        while (i < m) {
            dst[i] = src[i]
            ++i
        }
        while (i < dst.size)
            dst[i++] = 0
    }

    fun newShiftRight(x: IntArray, bitCount: Int): IntArray {
        require(bitCount >= 0)
        val newBitLen = bitLen(x) - bitCount
        if (newBitLen <= 0)
            return ZERO
        val newWordLen = (newBitLen + 0x1F) ushr 5
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        val z = IntArray(max(newWordLen, 0))
        if (innerShift != 0) {
            val iLast = z.size - 1
            for (i in 0..<iLast)
                z[i] = (x[i + wordShift + 1] shl -innerShift) or (x[i + wordShift] ushr innerShift)
            val srcIndex = iLast + wordShift + 1
            z[iLast] = (
                    if (srcIndex < x.size)
                        (x[iLast + wordShift + 1] shl -innerShift)
                    else
                        0) or (x[iLast + wordShift] ushr innerShift)
        } else {
            for (i in z.indices)
                z[i] = x[i + wordShift]
        }
        return z
    }

    fun mutateShiftRight(x: IntArray, xLen: Int, bitCount: Int): IntArray {
        require(bitCount >= 0)
        check(xLen <= x.size)
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        if (wordShift >= xLen) {
            x.fill(0, 0, xLen)
            return x
        }
        val newLen = xLen - wordShift
        if (wordShift > 0) {
            System.arraycopy(x, wordShift, x, 0, newLen)
            for (i in newLen..<xLen)
                x[i] = 0
        }
        if (innerShift > 0) {
            val last = newLen - 1
            for (i in 0..<last)
                x[i] = (x[i + 1] shl (-innerShift)) or (x[i] ushr innerShift)
            x[last] = x[last] ushr innerShift
        }
        return x
    }

    fun newShiftLeft(x: IntArray, bitCount: Int): IntArray {
        val newBitLen = bitLen(x) + bitCount
        val z = newCopyWithBitLen(x, newBitLen)
        mutateShiftLeft(z, z.size, bitCount)
        return z
    }

    fun mutateShiftLeft(x: IntArray, xLen: Int, bitCount: Int) {
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        if (wordShift >= xLen) {
            x.fill(0, 0, xLen)
            return
        }
        val newLen = xLen - wordShift
        if (wordShift > 0) {
            System.arraycopy(x, 0, x, wordShift, newLen)
            for (i in wordShift - 1 downTo 0)
                x[i] = 0
        }
        if (innerShift > 0) {
            for (i in x.size - 1 downTo 1)
                x[i] = (x[i] shl innerShift) or (x[i - 1] ushr -innerShift)
            x[0] = x[0] shl innerShift
        }
    }

    fun isPowerOfTwo(x: IntArray, xLen: Int): Boolean {
        if (xLen >= 0 && xLen <= x.size) {
            var bitSeen = false
            for (i in 0..<xLen) {
                val w = x[i]
                if (w == 0) continue
                if ((w and (w - 1)) != 0) return false
                if (bitSeen) return false
                bitSeen = true
            }
            return bitSeen
        } else {
            throw IllegalArgumentException()
        }
    }


    fun bitLen(x: IntArray): Int = bitLen(x, x.size)

    fun bitLen(x: IntArray, xLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size) {
            for (i in xLen - 1 downTo 0)
                if (x[i] != 0)
                    return 32 - x[i].countLeadingZeroBits() + (i * 32)
            return 0
        } else {
            throw IllegalArgumentException()
        }
    }

    fun bitLengthBigIntegerStyle(sign: Boolean, x: IntArray): Int = bitLengthBigIntegerStyle(sign, x, x.size)

    fun bitLengthBigIntegerStyle(sign: Boolean, x: IntArray, xLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size) {
            val bitLen = bitLen(x, xLen)
            val isNegPowerOfTwo = sign && isPowerOfTwo(x, xLen)
            val bitLengthBigIntegerStyle = bitLen - if (isNegPowerOfTwo) 1 else 0
            return bitLengthBigIntegerStyle
        } else {
            throw IllegalArgumentException()
        }
    }


    fun newAnd(x: IntArray, y: IntArray): IntArray {
        val minLen = min(nonZeroLimbLen(x), nonZeroLimbLen(y))
        val z = IntArray(minLen)
        for (i in z.indices)
            z[i] = x[i] and y[i]
        return if (nonZeroLimbLen(z) > 0) z else ZERO
    }

    fun newOr(x: IntArray, y: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        val yLen = nonZeroLimbLen(y)
        val maxLen = max(xLen, yLen)
        val minLen = min(xLen, yLen)
        if (maxLen == 0)
            return ZERO
        val z = IntArray(maxLen)
        var i = 0
        while (i < minLen) {
            z[i] = x[i] or y[i]
            ++i
        }
        while (i < xLen) {
            z[i] = x[i]
            ++i
        }
        while (i < yLen) {
            z[i] = y[i]
            ++i
        }
        return z
    }

    fun newXor(x: IntArray, y: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        val yLen = nonZeroLimbLen(y)
        val maxLen = max(xLen, yLen)
        val minLen = min(xLen, yLen)
        if (maxLen == 0)
            return ZERO
        val z = IntArray(maxLen)
        var i = 0
        while (i < minLen) {
            z[i] = x[i] xor y[i]
            ++i
        }
        while (i < xLen) {
            z[i] = x[i]
            ++i
        }
        while (i < yLen) {
            z[i] = y[i]
            ++i
        }
        return if (nonZeroLimbLen(z) > 0) z else ZERO
    }

    fun EQ(x: IntArray, y: Int): Boolean {
        if (x.isNotEmpty()) {
            if (x[0] == y) {
                for (i in 1..x.lastIndex)
                    if (x[i] != 0)
                        return false
                return true
            }
        }
        return false
    }

    fun EQ(x: IntArray, y: IntArray): Boolean = compare(x, y) == 0

    fun compare(x: IntArray, y: IntArray): Int =
        compare(x, nonZeroLimbLen(x), y, nonZeroLimbLen(y))

    fun compare(x: IntArray, xLen: Int, y: IntArray, yLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size && yLen >= 0 && yLen <= y.size) {
            val minSize = min(xLen, yLen)
            for (i in xLen - 1 downTo minSize)
                if (x[i] != 0)
                    return 1
            for (i in yLen - 1 downTo minSize)
                if (y[i] != 0)
                    return -1
            for (i in minSize - 1 downTo 0) {
                val cmp = unsignedCmp(x[i], y[i])
                if (cmp != 0)
                    return cmp
            }
            return 0
        } else {
            throw IllegalArgumentException()
        }
    }

    fun compare(x: IntArray, w: UInt): Int {
        val limbLen = nonZeroLimbLen(x)
        return when {
            (limbLen > 1) -> 1
            (limbLen == 0) -> if (w == 0u) 0 else -1
            else -> x[0].toUInt().compareTo(w)
        }
    }

    fun compare(x: IntArray, dw: ULong): Int = compare(x, nonZeroLimbLen(x), dw)

    fun compare(x: IntArray, xLen: Int, dw: ULong): Int {
        if (xLen >= 0 && xLen <= x.size) {
            check (xLen == 0 || x[xLen - 1] != 0)
            return when {
                (xLen > 2) -> 1
                (xLen == 2) -> {
                    val xT = (dw32(x[1]) shl 32) or dw32(x[0])
                    xT.compareTo(dw)
                }
                (xLen == 1) -> if ((dw shr 32) == 0uL) x[0].toUInt().compareTo(dw.toUInt()) else -1
                else -> if (dw == 0uL) 0 else -1
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    fun cmp(x: IntArray, xLen: Int, y: IntArray, yLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size && yLen >= 0 && yLen <= y.size) {
            check(xLen == 0 || x[xLen - 1] != 0)
            check(yLen == 0 || y[yLen - 1] != 0)
            if (xLen != yLen)
                return if (xLen > yLen) 1 else -1
            for (i in xLen - 1 downTo 0) {
                val cmp = x[i].toUInt().compareTo(y[i].toUInt())
                if (cmp != 0)
                    return cmp
            }
            return 0
        } else {
            throw IllegalArgumentException()
        }
    }


//    // returns a 32 bit value as a Long
//    fun mutateDivideRemainder(x: IntArray, n: Int): Long =
//        mutateDivideRemainder(x, x.size, n.toUInt()).toLong()

    fun mutateDivMod(x: IntArray, w: UInt): UInt =
        mutateDivMod(x, x.size, w)

    fun mutateDivMod(x: IntArray, xLen: Int, w: UInt): UInt {
        if (xLen >= 0 && xLen <= x.size) {
            if (w == 0u)
                throw RuntimeException("DivByZero")
            val dw = w.toULong()
            var carry = 0uL
            for (i in xLen - 1 downTo 0) {
                val t = (carry shl 32) + dw32(x[i])
                val q = t / dw
                val r = t % dw
                x[i] = q.toInt()
                carry = r
            }
            return carry.toUInt()
        } else {
            throw IllegalArgumentException()
        }
    }

    fun newDiv(x: IntArray, w: UInt): IntArray {
        val q = newCopyMinimum(x)
        mutateDivMod(q, w)
        return if (nonZeroLimbLen(q) > 0) q else ZERO
    }

    fun newDiv(x: IntArray, dw: ULong): IntArray {
        val lo = dw.toUInt()
        val hi = (dw shr 32).toUInt()
        return if (hi == 0u) newDiv(x, lo) else newDiv(x, intArrayOf(lo.toInt(), hi.toInt()))
    }

    fun newDiv(x: IntArray, y: IntArray): IntArray {
        val n = nonZeroLimbLen(y)
        if (n < 2)
            return newDiv(x, y[0].toUInt())
        val m = nonZeroLimbLen(x)
        if (m < n)
            return ZERO
        val u = x
        val v = y
        val q = IntArray(m - n + 1)
        val r = null
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        return if (nonZeroLimbLen(q) > 0) q else ZERO
    }

    fun newMod(x: IntArray, w: UInt): IntArray {
        val q = newCopyMinimum(x)
        val rem = mutateDivMod(q, w)
        return if (rem == 0u) ZERO else intArrayOf(rem.toInt())
    }

    fun newMod(x: IntArray, dw: ULong): IntArray {
        val lo = dw.toUInt()
        val hi = (dw shr 32).toUInt()
        return if (hi == 0u) newMod(x, lo) else newMod(x, intArrayOf(lo.toInt(), hi.toInt()))
    }

    fun newMod(x: IntArray, y: IntArray): IntArray {
        val n = nonZeroLimbLen(y)
        if (n <= 1) {
            if (n == 0)
                throw ArithmeticException("div by zero")
            return newMod(x, y[0].toUInt())
        }
        val m = nonZeroLimbLen(x)
        if (m == 0)
            return ZERO
        if (m < n)
            return newCopyMinimum(x)
        val u = x
        val v = y
        val q = null
        val r = IntArray(y.size)
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        check (nonZeroLimbLen(r) <= n)
        return if (nonZeroLimbLen(r) > 0) r else ZERO
    }

    fun newDivMod(x: IntArray, y: IntArray): Array<IntArray> {
        val n = nonZeroLimbLen(y)
        if (n <= 1) {
            if (n == 0)
                throw ArithmeticException("div by zero")
            var div = newCopyMinimum(x)
            val rem = mutateDivMod(div, y[0].toUInt())
            if (nonZeroLimbLen(div) == 0)
                div = ZERO
            return arrayOf(div, if (rem != 0u) intArrayOf(rem.toInt()) else ZERO)
        }
        val m = nonZeroLimbLen(x)
        if (m < n)
            return arrayOf(ZERO, newCopyMinimum(x))
        val u = x
        val v = y
        val q = IntArray(m - n + 1)
        val r = IntArray(n)
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        check (nonZeroLimbLen(r) <= n)
        return arrayOf(
            if (nonZeroLimbLen(q) > 0) q else ZERO, if (nonZeroLimbLen(r) > 0) r else ZERO
        )
    }

    /**
     * Multi‐word division (Knuth’s Algorithm D) in base 2^32.
     *
     * q: quotient array (length ≥ m – n + 1), or null if not needed
     * r: remainder array (length ≥ n), or null if not needed
     * u: dividend array (length = m), little‐endian (u[0] = low word)
     * v: divisor array (length = n ≥ 2), little‐endian, v[n − 1] ≠ 0
     * m: number of words in u (≥ n)
     * n: number of words in v (≥ 1)
     *
     * Returns 0 on success, 1 if (m < n || n < 2 || v[n − 1] == 0).
     */
    fun knuthDivide(
        q: IntArray?,
        r: IntArray?,
        u: IntArray,
        v: IntArray,
        m: Int,
        n: Int
    ): Int {
        if (m < n || n < 2 || v[n - 1] == 0)
            return 1

        // Step D1: Normalize
        val un = newCopyWithLimbLen(u, m + 1)
        val vn = newCopyWithLimbLen(v, n)
        val shift = vn[n - 1].countLeadingZeroBits()
        if (shift > 0) {
            mutateShiftLeft(vn, vn.size, shift)
            mutateShiftLeft(un, un.size, shift)
        }

        knuthDivideNormalizedCore(q, un, vn, m, n)

        if (r != null) {
            mutateShiftRight(un, un.size, shift)
            copy(r, un)
        }
        return 0
    }

    /**
     * Core of Knuth division in base 2^32 that takes un and vn,
     * the normalized copies of u an v.
     *
     * un is side-effected and contains the remainder.
     *
     * q: quotient array (length ≥ m – n + 1)
     * un: normalized dividend array (length = m + 1) with an extra zero limb
     * vn: normalized divisor array (length = n ≥ 2), little‐endian, v[n − 1] ≠ 0
     * m: the original m ... one less than the length of un
     * n: number of words in vn (≥ 1)
     *
     * throws IllegalArgumentException if (m < n || n < 2 || v[n − 1] == 0).
     */
    fun knuthDivideNormalizedCore(
        q: IntArray?,
        un: IntArray,
        vn: IntArray,
        m: Int,
        n: Int
    ) {
        if (m < n || n < 2 || vn[n - 1] == 0)
            throw IllegalArgumentException()

        val vn_1 = U32(vn[n - 1])
        val vn_2 = U32(vn[n - 2])

        // -- main loop --
        for (j in m - n downTo 0) {

            // estimate q̂ = (un[j+n]*B + un[j+n-1]) / vn[n-1]
            val hi = U32(un[j + n])
            val lo = U32(un[j + n - 1])
            //if (hi == 0L && lo < vn_1) // this would short-circuit,
            //    continue               // but probability is astronomically small
            val num = (hi shl 32) or lo
            var qhat = unsignedDiv(num, vn_1)
            var rhat = unsignedMod(num, vn_1)

            // correct estimate
            while ((qhat ushr 32) != 0L ||
                unsignedCmp(
                    qhat * vn_2, (rhat shl 32) + U32(un[j + n - 2])
                ) > 0
            ) {
                qhat--
                rhat += U32(vn[n - 1])
                if ((rhat ushr 32) != 0L)
                    break
            }


            // multiply & subtract
            var carry = 0L
            for (i in 0 until n) {
                val prod = qhat * U32(vn[i])
                val prodHi = prod ushr 32
                val prodLo = prod and 0xFFFF_FFFFL
                val unIJ = U32(un[j + i])
                val t = unIJ - prodLo - carry
                un[j + i] = t.toInt()
                carry = prodHi - (t shr 32) // t is signed, so this should *indeed* be signed shr
            }
            val t = U32(un[j + n]) - carry
            un[j + n] = t.toInt()
            if (q != null)
                q[j] = (qhat - (t ushr 63)).toInt()
            if (t < 0) {
                var c2 = 0L
                for (i in 0 until n) {
                    val sum = U32(un[j + i]) + U32(vn[i]) + c2
                    un[j + i] = sum.toInt()
                    c2 = sum ushr 32
                }
                un[j + n] += c2.toInt()
            }
        }
    }

    /**
     * Converts the given unsigned integer magnitude [magia] to its decimal string form.
     *
     * Equivalent to calling [toString] with `isNegative = false`.
     *
     * @param magia the unsigned integer magnitude, least-significant word first.
     * @return the decimal string representation of [magia].
     */
    fun toString(magia: IntArray) = toString(isNegative = false, magia)

    /**
     * Converts a signed magnitude [magia] value into its decimal string representation.
     *
     * Performs a full base-10 conversion. Division and remainder operations
     * are done in chunks of one billion (1 000 000 000) to minimize costly
     * multi-precision divisions. Temporary heap allocation is used for an intermediate
     * quotient array, a temporary UTF-8 buffer, and the final [String] result.
     *
     * The algorithm:
     *  - Estimates the required digit count from [bitLen].
     *  - Copies [magia] into a temporary mutable array.
     *  - Repeatedly divides the number by 1e9 to extract 9-digit chunks.
     *  - Converts each chunk into ASCII digits using [renderChunk9] and [renderChunkTail].
     *  - Prepends a leading ‘-’ if [isNegative] is true.
     *
     * @param isNegative whether to prefix the result with a minus sign.
     * @param magia the magnitude, least-significant word first.
     * @return the decimal string representation of the signed value.
     */
    fun toString(isNegative: Boolean, magia: IntArray): String =
        toString(isNegative, magia, nonZeroLimbLen(magia))

    fun toString(isNegative: Boolean, x: IntArray, xLen: Int): String {
        if (xLen >= 0 && xLen <= x.size) {
            val bitLen = bitLen(x, xLen)
            if (bitLen < 2) {
                if (bitLen == 0)
                    return "0"
                return if (isNegative) "-1" else "1"
            }
            val maxDigitLen = ((bitLen * 1234) shr 12) + 1
            val maxSignedLen = maxDigitLen + if (isNegative) 1 else 0
            var wordLen = nonZeroLimbLen(x, xLen)
            var t = newCopyWithLimbLen(x, wordLen)
            val utf8 = ByteArray(maxSignedLen)
            var ib = utf8.size
            while (wordLen > 1) {
                val newLenAndRemainder = mutateBarrettDivBy1e9(t, wordLen)
                val chunk = newLenAndRemainder and 0xFFFF_FFFFL
                renderChunk9(chunk, utf8, ib)
                wordLen = (newLenAndRemainder ushr 32).toInt()
                t = newCopyWithLimbLen(t, wordLen)
                ib -= 9
            }
            ib -= renderChunkTail(t[0], utf8, ib)
            if (isNegative)
                utf8[--ib] = '-'.code.toByte()
            val len = utf8.size - ib
            return String(utf8, ib, len)
        } else {
            throw IllegalArgumentException()
        }
    }

    /**
     * Renders a single 32-bit unsigned integer [n] into its decimal digits
     * at the end of [utf8], starting from [offMaxx] and moving backward.
     *
     * Digits are emitted least-significant first and written backward into [utf8].
     * Uses reciprocal multiplication by `0xCCCCCCCD` (fixed-point reciprocal of 10)
     * to perform fast division and extract digits.
     *
     * @param n the integer to render (interpreted as unsigned 32-bit).
     * @param utf8 the UTF-8 byte buffer to write digits into.
     * @param offMaxx the maximum exclusive offset within [utf8];
     *                digits are written backward from `offMaxx - 1`.
     * @return the number of bytes written.
     */
    private fun renderChunkTail(n: Int, utf8: ByteArray, offMaxx: Int): Int {
        var t: Long = n.toLong() and 0xFFFF_FFFFL  // treat as unsigned 32-bit
        var ib = offMaxx
        do {
            val divTen = (t * 0xCCCCCCCDL) ushr 35
            val digit = (t - (divTen * 10L)).toInt()
            utf8[--ib] = ('0'.code + digit).toByte()
            t = divTen
        } while (t != 0L)

        return offMaxx - ib
    }

    /**
     * Renders a 9-digit chunk [dw] (0 ≤ [dw] < 1e9) into ASCII digits in [utf8],
     * ending just before [offMaxx].
     *
     * Digits are extracted using reciprocal-multiply division by powers
     * of 10 to avoid slow hardware division instructions.
     *
     * The layout written is:
     * ```
     * utf8[offMaxx - 9] .. utf8[offMaxx - 1] = '0'..'9'
     * ```
     *
     * @param dw the 9-digit unsigned value to render.
     * @param utf8 the output byte buffer for ASCII digits.
     * @param offMaxx the maximum exclusive offset within [utf8];
     * digits occupy the range `offMaxx - 9 .. offMaxx - 1`.
     */
    private fun renderChunk9(dw: Long, utf8: ByteArray, offMaxx: Int) {
        val abcde = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val fghi  = dw - (abcde * 10000L)

        val abc = (abcde * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val de = abcde - (abc * 100L)

        val fg = (fghi * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val hi = fghi - (fg * 100L)

        val a = (abc * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val bc = abc - (a * 100L)

        val b = (bc * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = bc - (b * 10L)

        val d = (de * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = de - (d * 10L)

        val f = (fg * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = fg - (f * 10L)

        val h = (hi * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = hi - (h * 10L)

        utf8[offMaxx - 9] = (a.toInt() + '0'.code).toByte()
        utf8[offMaxx - 8] = (b.toInt() + '0'.code).toByte()
        utf8[offMaxx - 7] = (c.toInt() + '0'.code).toByte()
        utf8[offMaxx - 6] = (d.toInt() + '0'.code).toByte()
        utf8[offMaxx - 5] = (e.toInt() + '0'.code).toByte()
        utf8[offMaxx - 4] = (f.toInt() + '0'.code).toByte()
        utf8[offMaxx - 3] = (g.toInt() + '0'.code).toByte()
        utf8[offMaxx - 2] = (h.toInt() + '0'.code).toByte()
        utf8[offMaxx - 1] = (i.toInt() + '0'.code).toByte()
    }

    /**
     * Performs an in-place Barrett division of a multi-limb integer (`magia`) by 1e9.
     *
     * Each limb of [magia] is a 32-bit unsigned value (stored in [Int]),
     * with the most significant limb at index `len - 1`.
     * The function replaces each limb with its quotient and returns both
     * the new effective length and the remainder.
     *
     * This version uses the **qHat + rHat staged Barrett method**:
     * 1. Compute an approximate quotient `qHat` using the precomputed Barrett reciprocal [BARRETT_MU_1E9].
     * 2. Compute the remainder `rHat = combined − qHat × 1e9`.
     * 3. Conditionally increment `qHat` (and subtract 1e9 from `rHat`) if `rHat ≥ 1e9`.
     *    This is a 0-or-1 correction; `qHat` never decreases.
     *
     * The remainder from each limb is propagated to the next iteration.
     *
     * After all limbs are processed, the function computes the new effective length
     * of [magia] (trimming the most significant zero limb, if present) without looping.
     *
     * @param magia the multi-limb integer to divide. Must have `magia[len - 1] != 0`.
     *              Each element represents 32 bits of the number.
     * @param len the number of limbs in [magia] to process.
     * @return a packed [Long]:
     *   - upper 32 bits: new effective limb count after trimming
     *   - lower 32 bits: remainder of the division by 1e9
     *
     * **Note:** The correction is a 0-or-1 adjustment; `qHat` never decreases.
     * **Correctness:** Guarantees that after each limb, `0 ≤ rHat < 1e9`.
     */
    internal fun mutateBarrettDivBy1e9(magia: IntArray, len: Int): Long {
        var rem = 0L
        check(magia[len - 1] != 0)
        for (i in len - 1 downTo 0) {
            val limb = magia[i].toLong() and 0xFFFF_FFFFL
            val combined = (rem shl 32) or limb

            // approximate quotient using Barrett reciprocal
            var qHat = unsignedMulHi(combined, BARRETT_MU_1E9)

            // compute remainder
            var rHat = combined - qHat * ONE_E_9

            // 0-or-1 adjustment: increment qHat if remainder >= 1e9
            val adjustMask = ((rHat - ONE_E_9) shr 63).inv()
            qHat -= adjustMask
            rHat -= ONE_E_9 and adjustMask

            magia[i] = qHat.toInt()
            rem = rHat
        }

        val mostSignificantLimbNonZero = (-magia[len - 1]) ushr 31 // 0 or 1
        val newLen = len - 1 + mostSignificantLimbNonZero

        // pack new length and remainder into a single Long
        return (newLen.toLong() shl 32) or (rem and 0xFFFF_FFFFL)
    }

    fun toHexString(magia: IntArray) = toHexString(false, magia)

    fun toHexString(isNegative: Boolean, magia: IntArray): String {
        val bitLen = bitLen(magia)
        var nybbleCount = (bitLen + 3) ushr 2
        val strLen = (if (isNegative) 1 else 0) + 2 + max(nybbleCount, 1)
        val bytes = ByteArray(strLen)
        bytes[0] = '-'.code.toByte()
        val n = if (isNegative) 1 else 0
        bytes[n + 0] = '0'.code.toByte()
        bytes[n + 1] = 'x'.code.toByte()
        bytes[n + 2] = '0'.code.toByte()
        var i = 0
        var j = bytes.size
        while (nybbleCount > 0) {
            var w = magia[i++]
            val stepCount = Math.min(8, nybbleCount)
            repeat(stepCount) {
                val nybble = w and 0x0F
                val ch = nybble + if (nybble < 10) '0'.code else 'A'.code - 10
                bytes[--j] = ch.toByte()
                w = w ushr 4
            }
            nybbleCount -= stepCount
        }
        return String(bytes)
    }

    fun from(str: String) = from(StringLatin1Iterator(str))
    fun from(str: String, off: Int, len: Int) = from(StringLatin1Iterator(str, off, len))
    fun from(csq: CharSequence) = from(CharSequenceLatin1Iterator(csq))
    fun from(csq: CharSequence, off: Int, len: Int) =
        from(CharSequenceLatin1Iterator(csq, off, len))

    fun from(chars: CharArray) = from(CharArrayLatin1Iterator(chars))
    fun from(chars: CharArray, off: Int, len: Int) =
        from(CharArrayLatin1Iterator(chars, off, len))

    fun fromAscii(bytes: ByteArray) = from(ByteArrayLatin1Iterator(bytes))
    fun fromAscii(bytes: ByteArray, off: Int, len: Int) =
        from(ByteArrayLatin1Iterator(bytes, off, len))


    fun newFromHex(str: String) = fromHex(StringLatin1Iterator(str, 0, str.length))
    fun newFromHex(str: String, off: Int, len: Int) = fromHex(StringLatin1Iterator(str, off, len))
    fun newFromHex(csq: CharSequence) = fromHex(CharSequenceLatin1Iterator(csq, 0, csq.length))
    fun newFromHex(csq: CharSequence, off: Int, len: Int) =
        fromHex(CharSequenceLatin1Iterator(csq, off, len))

    fun newFromHex(chars: CharArray) = fromHex(CharArrayLatin1Iterator(chars, 0, chars.size))
    fun newFromHex(chars: CharArray, off: Int, len: Int) =
        fromHex(CharArrayLatin1Iterator(chars, off, len))

    fun newFromAsciiHex(bytes: ByteArray) =
        fromHex(ByteArrayLatin1Iterator(bytes, 0, bytes.size))

    fun newFromAsciiHex(bytes: ByteArray, off: Int, len: Int) =
        fromHex(ByteArrayLatin1Iterator(bytes, off, len))

    @Suppress("NOTHING_TO_INLINE")
    private inline fun isHexAsciiCharOrUnderscore(c: Char): Boolean {
        val idx = c.code - '0'.code
        if (idx < 0 || idx > 'f'.code - '0'.code)
            return false
        return ((HEX_DIGIT_AND_UNDERSCORE_MASK ushr idx) and 1L) != 0L
    }

    /**
     * Constructs a [Magia] magnitude from a sequence of raw binary bytes.
     *
     * The input bytes represent a non-negative magnitude if [isNegative] is `false`,
     * or a two’s-complement negative number if [isNegative] is `true`. In the latter case,
     * the bytes are complemented and incremented during decoding to produce the corresponding
     * positive magnitude. The sign itself is handled by the caller.
     *
     * The bytes may be in either big-endian or little-endian order, as indicated by [isBigEndian].
     *
     * @param isNegative  `true` if bytes encode a negative value in two’s-complement form.
     * @param isBigEndian `true` if bytes are in big-endian order, `false` for little-endian.
     * @param bytes       Source byte array.
     * @param off         Starting offset in [bytes].
     * @param len         Number of bytes to read.
     * @return A [Magia] magnitude as an [IntArray].
     * @throws IllegalArgumentException if the range `[off, off + len)` is invalid.
     */
    internal fun fromBinaryBytes(isNegative: Boolean, isBigEndian: Boolean,
                                 bytes: ByteArray, off: Int, len: Int): IntArray {
        if (off < 0 || len < 0 || len > bytes.size - off)
            throw IllegalArgumentException()
        if (len == 0)
            return ZERO

        // calculate offsets and stepping direction for BE BigEndian vs LE LittleEndian
        val offB1 = if (isBigEndian) -1 else 1 // BE == -1, LE ==  1
        val offB2 = offB1 shl 1                // BE == -2, LE ==  2
        val offB3 = offB1 + offB2              // BE == -3, LE ==  3
        val step1HiToLo = - offB1              // BE ==  1, LE == -1
        val step4LoToHi = offB1 shl 2          // BE == -4, LE ==  4

        val ibLast = off + len - 1
        val ibLsb = if (isBigEndian) ibLast else off // index Least significant byte
        var ibMsb = if (isBigEndian) off else ibLast // index Most significant byte

        val negativeMask = if (isNegative) -1 else 0

        // Leading sign-extension bytes (0x00 for non-negative, 0xFF for negative) are flushed
        // If all bytes are flush bytes, the result is [ZERO] or [ONE], depending on [isNegative].
        val leadingFlushByte = negativeMask
        var remaining = len
        while (bytes[ibMsb].toInt() == leadingFlushByte) {
            ibMsb += step1HiToLo
            --remaining
            if (remaining == 0)
                return if (isNegative) ONE else ZERO
        }

        val magia = IntArray((remaining + 3) shr 2)

        var ib = ibLsb
        var iw = 0

        var carry = -negativeMask.toLong() // if (isNegative) then carry = 1 else 0

        while (remaining >= 4) {
            val b3 = bytes[ib + offB3].toInt() and 0xFF
            val b2 = bytes[ib + offB2].toInt() and 0xFF
            val b1 = bytes[ib + offB1].toInt() and 0xFF
            val b0 = bytes[ib        ].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            carry += (w xor negativeMask).toLong() and 0xFFFF_FFFFL
            magia[iw++] = carry.toInt()
            carry = carry shr 32
            check ((carry shr 1) == 0L)
            ib += step4LoToHi
            remaining -= 4
        }
        if (remaining > 0) {
            val b3 = negativeMask and 0xFF
            val b2 = (if (remaining == 3) bytes[ib + offB2].toInt() else negativeMask) and 0xFF
            val b1 = (if (remaining >= 2) bytes[ib + offB1].toInt() else negativeMask) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = (w xor negativeMask) + carry.toInt()
        }
        check(iw == magia.size)
        return magia
    }

    fun toBinaryByteArray(sign: Boolean, x: IntArray, xLen: Int, isTwosComplement: Boolean, isBigEndian: Boolean): ByteArray {
        if (xLen >= 0 && xLen <= x.size) {
            val bitLen =
                if (isTwosComplement) bitLengthBigIntegerStyle(sign, x, xLen) + 1 else max(bitLen(x, xLen), 1)
            val byteLen = (bitLen + 7) ushr 3
            val bytes = ByteArray(byteLen)
            toBinaryBytes(x, xLen, sign and isTwosComplement, isBigEndian, bytes, 0, byteLen)
            return bytes
        } else {
            throw IllegalArgumentException()
        }
    }

    internal fun toBinaryBytes(x: IntArray, isNegative: Boolean, isBigEndian: Boolean,
                               bytes: ByteArray, off: Int, requestedLen: Int): Int =
        toBinaryBytes(x, nonZeroLimbLen(x), isNegative, isBigEndian, bytes, off, requestedLen)

    internal fun toBinaryBytes(x: IntArray, xLen: Int, isNegative: Boolean, isBigEndian: Boolean,
                               bytes: ByteArray, off: Int, requestedLen: Int): Int {
        if (xLen >= 0 && xLen <= x.size &&
            off >= 0 && (requestedLen <= 0 || requestedLen <= bytes.size - off)) {

            val actualLen = if (requestedLen > 0) requestedLen else {
                val bitLen = if (isNegative)
                    bitLengthBigIntegerStyle(isNegative, x, xLen) + 1
                else
                    max(bitLen(x, xLen), 1)
                (bitLen + 7) ushr 3
            }

            // calculate offsets and stepping direction for BE BigEndian vs LE LittleEndian
            val offB1 = if (isBigEndian) -1 else 1 // BE == -1, LE ==  1
            val offB2 = offB1 shl 1                // BE == -2, LE ==  2
            val offB3 = offB1 + offB2              // BE == -3, LE ==  3
            val step1LoToHi = offB1                // BE == -1, LE ==  1
            val step4LoToHi = offB1 shl 2          // BE == -4, LE ==  4

            val ibLast = off + actualLen - 1
            val ibLsb = if (isBigEndian) ibLast else off // index Least significant byte
            val ibMsb = if (isBigEndian) off else ibLast // index Most significant byte

            val negativeMask = if (isNegative) -1 else 0

            var remaining = actualLen

            var ib = ibLsb
            var iw = 0

            var carry = -negativeMask.toLong() // if (isNegative) then carry = 1 else 0

            while (remaining >= 4 && iw < xLen) {
                val v = x[iw++]
                carry += (v xor negativeMask).toLong() and 0xFFFF_FFFFL
                val w = carry.toInt()
                carry = carry shr 32

                val b3 = (w shr 24).toByte()
                val b2 = (w shr 16).toByte()
                val b1 = (w shr 8).toByte()
                val b0 = (w).toByte()

                bytes[ib + offB3] = b3
                bytes[ib + offB2] = b2
                bytes[ib + offB1] = b1
                bytes[ib] = b0

                ib += step4LoToHi
                remaining -= 4
            }
            if (remaining > 0) {
                val v = if (iw < xLen) x[iw++] else 0
                var w = (v xor negativeMask).toLong() + carry.toInt()
                do {
                    bytes[ib] = w.toByte()
                    ib += step1LoToHi
                    w = w shr 8
                } while (--remaining > 0)
            }
            check(iw == xLen || x[iw] == 0)
            check(ib - step1LoToHi == ibMsb)
            return actualLen
        } else {
            throw IllegalArgumentException()
        }
    }


    /**
     * This layer works with magnitudes, so any optional leading sign char is ignored.
     */
    fun from(src: Latin1Iterator): IntArray {
        invalid_syntax@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            if (ch == '-' || ch == '+') // discard leading sign
                ch = src.nextChar()
            if (ch == '0') { // discard leading zero
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    return fromHex(src.reset())
                leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@invalid_syntax
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar() // discard all leading zeros
            }
            var accumulator = 0
            var accumulatorDigitCount = 0
            val remainingLen = src.remainingLen() + if (ch == '\u0000') 0 else 1
            val bitLen = (remainingLen * 13607 + 4095) ushr 12
            if (bitLen == 0) {
                if (leadingZeroSeen)
                    return ZERO
                break@invalid_syntax
            }
            val z = newWithBitLen(bitLen)

            src.prevChar() // back up one
            var chLast = '\u0000'
            while (true) {
                chLast = ch
                ch = src.nextChar()
                if (ch == '_')
                    continue
                if (ch !in '0'..'9')
                    break
                val n = ch - '0'
                accumulator = accumulator * 10 + n
                ++accumulatorDigitCount
                if (accumulatorDigitCount < 9)
                    continue
                mutateFma(z, 1000000000, accumulator)
                accumulator = 0
                accumulatorDigitCount = 0
            }
            if (ch == '\u0000' && chLast != '_') {
                if (accumulatorDigitCount > 0) {
                    var pow10 = 1
                    for (j in 0..<accumulatorDigitCount)
                        pow10 *= 10
                    mutateFma(z, pow10, accumulator)
                }
                return z
            }
        } while (false)
        throw IllegalArgumentException("integer parse error:$src")
    }

    internal fun fromHex(src: Latin1Iterator): IntArray {
        invalid_syntax@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            if (ch == '+' || ch == '-')
                ch = src.nextChar()
            if (ch == '0') {
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    ch = src.nextChar()
                else
                    leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@invalid_syntax
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar()
            }
            if (ch != '\u0000')
                src.prevChar() // back up one
            var nybbleCount = 0
            while (src.hasNext()) {
                ch = src.nextChar()
                if (!isHexAsciiCharOrUnderscore(ch))
                    break@invalid_syntax
                nybbleCount += if (ch == '_') 0 else 1
            }
            if (ch == '_') // last char seen was '_'
                break@invalid_syntax
            if (nybbleCount == 0) {
                if (leadingZeroSeen)
                    return ZERO
                break@invalid_syntax
            }
            val z = newWithBitLen(nybbleCount shl 2)
            var k = 0
            var nybblesLeft = nybbleCount
            do {
                var w = 0
                val stepCount = min(nybblesLeft, 8)
                var n = 0
                do {
                    ch = src.prevChar()
                    val nybble = ch.code - when {
                        ch <= '9' -> '0'.code
                        ch <= 'F' -> 'A'.code - 10
                        ch >= 'a' -> 'a'.code - 10
                        ch == '_' -> continue
                        else -> throw IllegalStateException()
                    }
                    w = w or (nybble shl (n shl 2))
                    ++n
                } while (n < stepCount)
                z[k++] = w
                nybblesLeft -= stepCount
            } while (nybblesLeft > 0)
            return z
        } while (false)
        throw IllegalArgumentException("integer parse error:$src")
    }

    /**
     * number of trailing zeros ... or -1
     */
    internal fun ntz(magia: IntArray): Int {
        for (i in magia.indices) {
            if (magia[i] != 0)
                return (i shl 6) + magia[i].countTrailingZeroBits()
        }
        return -1
    }

    /**
     * number of bits that are set in the provided magia
     */
    fun bitPopulationCount(magia: IntArray): Int {
        var popCount = 0
        for (limb in magia)
            popCount += limb.countOneBits()
        return popCount
    }

}
