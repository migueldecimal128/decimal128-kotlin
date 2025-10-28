@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.hugeint

import com.decimal128.decimal.unsignedCmp
import com.decimal128.decimal.unsignedDiv
import com.decimal128.decimal.unsignedMod
import kotlin.math.min
import kotlin.math.max


// magia == MAGnitude IntArray

private const val LOWERCASE_DELTA = 'x'.code - 'X'.code

private const val HEX_DIGIT_AND_UNDERSCORE_MASK  = 0x007E_8000_007E_03FFL


object Magia {

    val ZERO = IntArray(0)

    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    fun testBit(x: IntArray, bitIndex: Int): Boolean {
        val wordIndex = bitIndex ushr 5
        if (wordIndex >= x.size)
            return false
        val word = x[wordIndex]
        val bitMask = 1 shl (bitIndex and 0x1F)
        return (word and bitMask) != 0
    }

    fun testAnyBitInLowerN(x: IntArray, bitCount: Int): Boolean {
        val lastBitIndex = bitCount - 1
        val lastWordIndex = lastBitIndex ushr 5
        for (i in 0..<lastWordIndex) {
            if (i == x.size)
                return false
            if (x[i] != 0)
                return true
        }
        if (lastWordIndex == x.size)
            return false
        val bitMask = -1 ushr (0x1F - (lastBitIndex and 0x1F))
        return (x[lastWordIndex] and bitMask) != 0
    }

    fun toLong(x: IntArray): Long {
        return when (x.size) {
            0 -> 0
            1 -> U32(x[0])
            else -> (x[1].toLong() shl 32) or U32(x[0])
        }
    }

    fun toInt(x: IntArray): Int {
        return if (x.isEmpty()) 0 else x[0]
    }

    fun newFromLong(dw: Long): IntArray =
        if (dw != 0L) intArrayOf(dw.toInt(), (dw ushr 32).toInt()) else ZERO

    fun nonZeroLimbLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return i + 1
        return 0
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


    fun newAdd(x: IntArray, w: Int): IntArray {
        val newBitLen = max(bitLen(x), (32 - w.countLeadingZeroBits())) + 1
        val z = newWithBitLen(newBitLen)
        var carry = U32(w)
        val indexLimit = min(x.size, z.size)
        for (i in 0..<indexLimit) {
            val t = U32(x[i]) + carry
            z[i] = t.toInt()
            carry = t ushr 32
        }
        if (carry != 0L)
            z[z.lastIndex] = 1
        return z
    }

    fun newOrMutateAdd(x: IntArray, w: Int): IntArray {
        val newBitLen = max(bitLen(x), (32 - w.countLeadingZeroBits())) + 1
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, w)
    }

    fun mutateAdd(x: IntArray, w: Int): IntArray {
        var carry = U32(w)
        for (i in x.indices) {
            val t = U32(x[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
            if (carry == 0L)
                break
            check(carry == 1L)
        }
        return x
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

    fun newSub(x: IntArray, w: Int): IntArray {
        val z = IntArray(wordLen(x))
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
        val z = IntArray(wordLen(x))
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

    //fun newSub(x: IntArray, y: IntArray) = mutateSub(newMinimumCopy(x), y)

    fun newSub(x: IntArray, y: IntArray): IntArray {
        check (compare(x, y) >= 0)
        val z = IntArray(wordLen(x))
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

    fun newMul(x: IntArray, n: Int): IntArray {
        val bitLenX = bitLen(x)
        val bitLenN = 32 - n.countLeadingZeroBits()
        if (bitLenX == 0 || bitLenN == 0)
            return ZERO
        val newBitLen = bitLenX + bitLenN
        val prod = newWithBitLen(newBitLen)
        val n64 = U32(n)
        var carry = 0L
        for (i in x.indices) {
            val t = U32(x[i]) * n64 + carry
            prod[i] = t.toInt()
            carry = t ushr 32
        }
        if (carry != 0L)
            prod[prod.lastIndex] = carry.toInt()
        return prod
    }

    fun mutateMul(x: IntArray, w: Int): IntArray {
        val n64 = U32(w)
        var carry = 0L
        for (i in x.indices) {
            val t = U32(x[i]) * n64 + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
        return x
    }

    fun newMul(x: IntArray, dw: Long): IntArray {
        val lo = dw and 0xFFFF_FFFFL
        val hi = dw ushr 32
        if (hi == 0L)
            return newMul(x, lo.toInt())
        val xBitLen = bitLen(x)
        if (xBitLen == 0 || dw == 0L)
            return ZERO
        val newBitLen = bitLen(x) + (64 - dw.countLeadingZeroBits())
        val z = newWithBitLen(newBitLen)

        var t = U32(x[0]) * lo
        z[0] = t.toInt()
        var carry = t ushr 32

        t = U32(x[0]) * hi
        var prevHighLow = t and 0xFFFF_FFFFL
        var prevHighCarry = t ushr 32

        // i = 1…n-1: do both halves in one pass
        for (i in 1 until z.size) {
            val xi = if (i < x.size) U32(x[i]) else 0L

            // 1) combine previous high-half, previous low-half carry, and xi * a
            val s = prevHighLow + carry + xi * lo
            z[i] = s.toInt()
            carry = s ushr 32

            // 2) compute this limb’s high-half product for next iteration
            t = xi * hi + prevHighCarry
            prevHighLow = t and 0xFFFF_FFFFL
            prevHighCarry = t ushr 32
        }

        return z
    }

    fun newMul(x: IntArray, y: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        val yLen = nonZeroLimbLen(y)
        if (xLen == 0 || yLen == 0)
            return ZERO
        val p = IntArray(xLen + yLen)
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
    }

    fun newSqr(x: IntArray): IntArray {
        val xLen = nonZeroLimbLen(x)
        if (xLen == 0)
            return ZERO

        val p = IntArray(2 * xLen)

        // 1) Cross terms: for i<j, add (a[i]*a[j]) twice into p[i+j]
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
            var k = i + xLen
            while (carry != 0L) {
                val t = U32(p[k]) + carry
                p[k] = t.toInt()
                carry = t ushr 32
                k++
            }
        }

        // 2) Diagonals: add a[i]^2 into columns 2*i and 2*i+1
        // terms on the diagonal are not doubled
        for (i in 0 until xLen) {
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

        return p
    }

    fun newMinimumCopy(src: IntArray) = newCopy(src, nonZeroLimbLen(src))

    fun newCopy(src: IntArray, newWordLen: Int): IntArray {
        if (newWordLen > 0) {
            val dst = IntArray(newWordLen)
            copy(dst, src)
            return dst
        }
        return ZERO
    }

    fun newCopyWithBitLen(src: IntArray, newBitLen: Int): IntArray {
        val dst = newWithBitLen(newBitLen)
        copy(dst, src)
        return dst
    }

    fun copy(dst: IntArray, src: IntArray) {
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

    fun mutateShiftRight(x: IntArray, bitCount: Int) =
        mutateShiftRight(x, nonZeroLimbLen(x), bitCount)

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
        return mutateShiftLeft(z, bitCount)
    }

    fun mutateShiftLeft(x: IntArray, bitCount: Int): IntArray {
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        if (wordShift >= x.size) {
            x.fill(0)
            return x
        }
        val newLen = x.size - wordShift
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
        return x
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

    fun bitLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return 32 - x[i].countLeadingZeroBits() + (i * 32)
        return 0
    }

    fun wordLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return i + 1
        return 0
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

    fun compare(x: IntArray, y: IntArray): Int {
        val minSize = min(x.size, y.size)
        for (i in x.size - 1 downTo minSize)
            if (x[i] != 0)
                return 1
        for (i in y.size - 1 downTo minSize)
            if (y[i] != 0)
                return -1
        for (i in minSize - 1 downTo 0) {
            val cmp = unsignedCmp(x[i], y[i])
            if (cmp != 0)
                return cmp
        }
        return 0
    }

    fun compare(x: IntArray, w: Int): Int {
        val limbLen = nonZeroLimbLen(x)
        return when {
            (limbLen > 1) -> 1
            (limbLen == 0) -> if (w == 0) 0 else -1
            else -> unsignedCmp(x[0], w)
        }
    }

    fun compare(x: IntArray, dw: Long): Int {
        val limbLen = nonZeroLimbLen(x)
        return when {
            (limbLen > 2) -> 1
            (limbLen == 2) -> {
                val xT = (x[1].toLong() shl 32) or U32(x[0])
                unsignedCmp(xT, dw)
            }

            (limbLen == 1) -> if ((dw ushr 32) == 0L) unsignedCmp(x[0], dw.toInt()) else -1
            else -> if (dw == 0L) 0 else -1
        }
    }

    // returns a 32 bit value as a Long
    fun mutateDivideRemainder(x: IntArray, n: Int): Long {
        if (n == 0)
            throw RuntimeException("DivByZero")
        val n64 = U32(n)
        var carry = 0L
        for (i in x.size - 1 downTo 0) {
            val t = (carry shl 32) + U32(x[i])
            val q = unsignedDiv(t, n64)
            val r = unsignedMod(t, n64)
            x[i] = q.toInt()
            carry = r
        }
        return carry
    }

    fun newDiv(x: IntArray, n: Int): IntArray {
        val q = newMinimumCopy(x)
        mutateDivideRemainder(q, n)
        return if (nonZeroLimbLen(q) > 0) q else ZERO
    }

    fun newDiv(x: IntArray, l: Long): IntArray {
        val lo = l.toInt()
        val hi = (l ushr 32).toInt()
        return if (hi == 0) newDiv(x, lo) else newDiv(x, intArrayOf(lo, hi))
    }

    fun newDiv(x: IntArray, y: IntArray): IntArray {
        val n = nonZeroLimbLen(y)
        if (n < 2)
            return newDiv(x, y[0])
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

    fun newMod(x: IntArray, n: Int): IntArray {
        val q = newMinimumCopy(x)
        val rem = mutateDivideRemainder(q, n)
        return if (rem == 0L) ZERO else intArrayOf(rem.toInt())
    }

    fun newMod(x: IntArray, l: Long): IntArray {
        val lo = l.toInt()
        val hi = (l ushr 32).toInt()
        return if (hi == 0) newMod(x, lo) else newMod(x, intArrayOf(lo, hi))
    }

    fun newMod(x: IntArray, y: IntArray): IntArray {
        val divMod = newDivMod(x, y)
        val rem = divMod[1]
        return if (nonZeroLimbLen(rem) > 0) rem else ZERO
    }

    fun newDivMod(x: IntArray, y: IntArray): Array<IntArray> {
        val n = nonZeroLimbLen(y)
        if (n <= 1) {
            if (n == 0)
                throw ArithmeticException("div by zero")
            var div = newMinimumCopy(x)
            val rem = mutateDivideRemainder(div, y[0])
            if (nonZeroLimbLen(div) == 0)
                div = ZERO
            return arrayOf(div, if (rem != 0L) intArrayOf(rem.toInt()) else ZERO)
        }
        val m = nonZeroLimbLen(x)
        if (m < n)
            return arrayOf(ZERO, newMinimumCopy(x))
        val u = x
        val v = y
        val q = IntArray(m - n + 1)
        val r = IntArray(m + 1)
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        return arrayOf(
            if (nonZeroLimbLen(q) > 0) q else ZERO, if (nonZeroLimbLen(r) > 0) r else ZERO
        )
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
     * Returns 0 on success, 1 if (m < n || n < 2 || v[n − 1] == 0).
     */
    fun knuthDivide(
        q: IntArray,
        r: IntArray?,
        u: IntArray,
        v: IntArray,
        m: Int,
        n: Int
    ): Int {
        if (m < n || n < 2 || v[n - 1] == 0)
            return 1

        // Step D1: Normalize
        val vn = newCopy(v, n)
        val un = newCopy(u, m + 1)
        val shift = vn[n - 1].countLeadingZeroBits()
        if (shift > 0) {
            mutateShiftLeft(vn, shift)
            mutateShiftLeft(un, shift)
        }

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

        if (r != null) {
            mutateShiftRight(un, shift)
            copy(r, un)
        }
        return 0
    }

    fun toString(magia: IntArray) = toString(false, magia)

    fun toString(isNegative: Boolean, magia: IntArray): String {
        val bitLen = bitLen(magia)
        if (bitLen < 2) {
            if (bitLen == 0)
                return "0"
            return if (isNegative) "-1" else "1"
        }
        val maxDigitLen = ((bitLen * 1234) shr 12) + 1
        val maxSignedLen = maxDigitLen + if (isNegative) 1 else 0
        val t = newMinimumCopy(magia)
        val bytes = ByteArray(maxSignedLen)
        bytes[0] = '-'.code.toByte()
        var j = if (isNegative) 1 else 0
        var ib = j
        while (compare(t, 1_000_000_000) >= 0) {
            val chunk = mutateDivideRemainder(t, 1_000_000_000).toInt()
            ib = renderChunkReversed(chunk, 9, bytes, ib)
        }
        ib = renderChunkReversed(t[0], 1, bytes, ib)
        var k = ib - 1
        while (j < k) {
            val t0 = bytes[j]
            bytes[j] = bytes[k]
            bytes[k] = t0
            ++j
            --k
        }
        return String(bytes, 0, ib)
    }

    private fun renderChunkReversed(n: Int, minDigitCount: Int, bytes: ByteArray, off: Int): Int {
        var t = U32(n)
        var ib = off
        val minIb = ib + minDigitCount
        while (t != 0L || ib < minIb) {
            val divTen = (t * 0xCCCCCCCD) ushr 35
            val digit = (t - (divTen * 10)).toInt()
            bytes[ib++] = ('0'.code + digit).toByte()
            t = divTen
        }
        return ib
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

    internal fun fromBigEndianBytes(
        signExtendedPrefix: Int, bytes: ByteArray,
        off: Int, len:
        Int
    ): IntArray =
        fromBigEndianBytesX(signExtendedPrefix, bytes, off, len)

    /**
     * Creates a [Magia] from an array
     */
    internal fun fromBigEndianBytesX(
        signExtendedPrefix: Int, bytes: ByteArray,
        off: Int, len:
        Int
    ): IntArray {
        if (len <= 0)
            return ZERO
        val magia = IntArray((len + 3) ushr 2)
        var ib = off + len - 1
        var iw = 0
        var remaining = len
        while (remaining >= 4) {
            val b3 = bytes[ib - 3].toInt() and 0xFF
            val b2 = bytes[ib - 2].toInt() and 0xFF
            val b1 = bytes[ib - 1].toInt() and 0xFF
            val b0 = bytes[ib - 0].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
            ib -= 4
            remaining -= 4
        }
        if (remaining > 0) {
            val b3 = signExtendedPrefix and 0xFF
            val b2 = (if (remaining == 3) (bytes[ib - 2].toInt()) else signExtendedPrefix) and 0xFF
            val b1 = (if (remaining >= 2) (bytes[ib - 1].toInt()) else signExtendedPrefix) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
            check(iw == magia.size)
        }
        return magia
    }

    /**
     * Creates a [Magia] from an array
     */
    internal fun fromNonNegativeBigEndianBytes(bytes: ByteArray, off: Int, len: Int): IntArray {
        if (off < 0 || len < 0 || len > bytes.size - off)
            throw IllegalArgumentException()
        if (len <= 0)
            return ZERO

        var ibMsb = off

        var remaining = len
        while (bytes[ibMsb].toInt() == 0) {
            ++ibMsb
            --remaining
            if (remaining == 0)
                return ZERO
        }

        val magia = IntArray((remaining + 3) shr 2)

        var ib = ibMsb + remaining - 1
        var iw = 0

        while (remaining >= 4) {
            val b3 = bytes[ib - 3].toInt() and 0xFF
            val b2 = bytes[ib - 2].toInt() and 0xFF
            val b1 = bytes[ib - 1].toInt() and 0xFF
            val b0 = bytes[ib - 0].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
            ib -= 4
            remaining -= 4
        }
        if (remaining > 0) {
            // b3 is always 0
            val b2 = (if (remaining == 3) (bytes[ib - 2].toInt()) else 0) and 0xFF
            val b1 = (if (remaining >= 2) (bytes[ib - 1].toInt()) else 0) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
        }
        check(iw == magia.size)
        return magia
    }

    /**
     * Creates a [Magia] from an array
     */
    internal fun fromNegativeTwosComplementBigEndianBytes(
        bytes: ByteArray,
        off: Int,
        len: Int
    ): IntArray {
        if (off < 0 || len < 0 || len > bytes.size - off)
            throw IllegalArgumentException()
        if (len <= 0)
            return ZERO

        var ibMsb = off

        var remaining = len
        // flush most significant byte that match the sign prefix ... 0 or -1
        while (bytes[ibMsb].toInt() == -1) {
            ++ibMsb
            --remaining
            if (remaining == 0)
                return intArrayOf(1)
        }

        val magia = IntArray((remaining + 3) shr 2)

        var ib = ibMsb + remaining - 1
        var iw = 0

        var carry = 1L
        while (remaining >= 4) {
            val b3 = bytes[ib - 3].toInt() and 0xFF
            val b2 = bytes[ib - 2].toInt() and 0xFF
            val b1 = bytes[ib - 1].toInt() and 0xFF
            val b0 = bytes[ib - 0].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0

            val wT = U32(w.inv()) + carry
            magia[iw++] = wT.toInt()
            carry = wT shr 32

            ib -= 4
            remaining -= 4
        }
        if (remaining > 0) {
            val b3 = 0xFF
            val b2 = (if (remaining == 3) (bytes[ib - 2].toInt()) else 0xFF) and 0xFF
            val b1 = (if (remaining >= 2) (bytes[ib - 1].toInt()) else 0xFF) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0

            val wT = w.inv() + carry
            magia[iw++] = wT.toInt()
        }
        check(iw == magia.size)
        return magia
    }

    /**
     * Creates a [Magia] from an array
     */
    internal fun fromNonNegativeLittleEndianBytes(bytes: ByteArray, off: Int, len: Int): IntArray {
        if (off < 0 || len < 0 || len > bytes.size - off)
            throw IllegalArgumentException()
        if (len <= 0)
            return ZERO

        var ibMsb = off + len - 1

        var remaining = len
        while (bytes[ibMsb].toInt() == 0) {
            --ibMsb
            --remaining
            if (remaining == 0)
                return ZERO
        }

        val magia = IntArray((remaining + 3) shr 2)

        var ib = off
        var iw = 0

        while (remaining >= 4) {
            val b3 = bytes[ib + 3].toInt() and 0xFF
            val b2 = bytes[ib + 2].toInt() and 0xFF
            val b1 = bytes[ib + 1].toInt() and 0xFF
            val b0 = bytes[ib + 0].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
            ib += 4
            remaining -= 4
        }
        if (remaining > 0) {
            // b3 is always 0
            val b2 = (if (remaining == 3) (bytes[ib + 2].toInt()) else 0) and 0xFF
            val b1 = (if (remaining >= 2) (bytes[ib + 1].toInt()) else 0) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b2 shl 16) or (b1 shl 8) or b0
            magia[iw++] = w
        }
        check(iw == magia.size)
        return magia
    }

    /**
     * Creates a [Magia] from an array
     */
    internal fun fromNegativeTwosComplementLittleEndianBytes(
        bytes: ByteArray,
        off: Int,
        len: Int
    ): IntArray {
        if (off < 0 || len < 0 || len > bytes.size - off)
            throw IllegalArgumentException()
        if (len <= 0)
            return ZERO

        var ibMsb = off + len - 1

        var remaining = len
        // flush most significant byte that match the sign prefix ... 0 or -1
        while (bytes[ibMsb].toInt() == -1) {
            --ibMsb
            --remaining
            if (remaining == 0)
                return intArrayOf(1)
        }

        val magia = IntArray((remaining + 3) shr 2)

        var ib = off
        var iw = 0

        var carry = 1L
        while (remaining >= 4) {
            val b3 = bytes[ib + 3].toInt() and 0xFF
            val b2 = bytes[ib + 2].toInt() and 0xFF
            val b1 = bytes[ib + 1].toInt() and 0xFF
            val b0 = bytes[ib + 0].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0

            val wT = U32(w.inv()) + carry
            magia[iw++] = wT.toInt()
            carry = wT shr 32

            ib += 4
            remaining -= 4
        }
        if (remaining > 0) {
            val b3 = 0xFF
            val b2 = (if (remaining == 3) (bytes[ib + 2].toInt()) else 0xFF) and 0xFF
            val b1 = (if (remaining >= 2) (bytes[ib + 1].toInt()) else 0xFF) and 0xFF
            val b0 = bytes[ib].toInt() and 0xFF
            val w = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0

            val wT = w.inv() + carry
            magia[iw++] = wT.toInt()
        }
        check(iw == magia.size)
        return magia
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
                mutateAdd(mutateMul(z, 1000000000), accumulator)
                accumulator = 0
                accumulatorDigitCount = 0
            }
            if (ch == '\u0000' && chLast != '_') {
                if (accumulatorDigitCount > 0) {
                    var pow10 = 1
                    for (j in 0..<accumulatorDigitCount)
                        pow10 *= 10
                    mutateAdd(mutateMul(z, pow10), accumulator)
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
