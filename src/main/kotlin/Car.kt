package com.decimal128

import java.lang.Integer.compareUnsigned
import java.lang.Integer.numberOfLeadingZeros
import java.lang.Long.*
import kotlin.math.min


// CAR == Cardinal ARray
// cardinal has fallen out of use, but is an unsigned integer
// the "cardinality" of a set

object Car {

    val EMPTY_CAR = IntArray(0)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    @Suppress("NOTHING_TO_INLINE")
    inline fun setZero(x: IntArray) = x.fill(0)

    @Suppress("NOTHING_TO_INLINE")
    inline fun isZero(x: IntArray): Boolean {
        for (w in x)
            if (w != 0)
                return false
        return true
    }

    fun nonZeroLimbLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return i + 1
        return 0
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun newWithBitLen(bitLen: Int) = IntArray((bitLen + 0x1F) ushr 5)

    fun newAdd(x: IntArray, n: Int): IntArray {
        val newBitLen = bitLen(x) + (32 - numberOfLeadingZeros(n)) + 1
        val z = newCopyWithBitLen(x, newBitLen)
        mutateAdd(z, n)
        return z
    }

    fun mutateAdd(x: IntArray, n: Int) {
        var carry = U32(n)
        for (i in x.indices) {
            val t = U32(x[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
            if (carry == 0L)
                return
            assert(carry == 1L)
        }
    }

    fun newAdd(x: IntArray, y: IntArray): IntArray {
        val newBitLen = bitLen(x) + bitLen(y) + 1
        val z = newCopyWithBitLen(x, newBitLen)
        mutateAdd(z, y)
        return z
    }

    fun newOrMutateAdd(x: IntArray, y: IntArray): IntArray {
        val newBitLen = bitLen(x) + bitLen(y) + 1
        val newWordLen = (newBitLen + 0x1F) ushr 5
        val z = if (newWordLen == x.size) x else newCopy(x, newWordLen)
        mutateAdd(z, y)
        return z
    }

    fun mutateAdd(x: IntArray, y: IntArray) {
        var carry = 0L
        val min = min(x.size, y.size)
        var i = 0
        while (i < min) {
            val t = U32(x[i]) + U32(y[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
            assert(carry in 0L..1L)
            ++i
        }
        while (carry == 1L && i < x.size) {
            val t = U32(x[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
    }

    fun newSub(x: IntArray, n: Int): IntArray {
        val z = newCopy(x)
        mutateSub(z, 1)
        return z
    }

    fun mutateSub(x: IntArray, n: Int): IntArray {
        var borrow = U32(n)
        for (i in x.indices) {
            val t = U32(x[i]) - borrow
            x[i] = t.toInt()
            borrow = t ushr 63
            if (borrow == 0L)
                break
        }
        return x
    }

    fun mutateSub(x: IntArray, y: IntArray) {
        var borrow = 0L
        val min = min(x.size, y.size)
        var i = 0
        while (i < min) {
            val t = U32(x[i]) - U32(y[i]) - borrow
            x[i] = t.toInt()
            borrow = t ushr 63
            assert(borrow in 0L..1L)
            ++i
        }
        while (borrow == 1L && i < x.size) {
            val t = U32(x[i]) - borrow
            x[i] = t.toInt()
            borrow = t ushr 63
        }
    }

    fun newMul(x: IntArray, n: Int): IntArray {
        val newBitLen = bitLen(x) + 32 - numberOfLeadingZeros(n)
        val prod = newCopyWithBitLen(x, newBitLen)
        mutateMul(prod, n)
        return prod
    }

    fun mutateMul(x: IntArray, n: Int) {
        val n64 = U32(n)
        var carry = 0L
        for (i in x.indices) {
            val t = U32(x[i]) * n64 + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
    }

    fun newMul(x: IntArray, y: IntArray): IntArray {
        val p = IntArray(x.size + y.size)
        for (i in x.indices) {
            val xLimb = U32(x[i])
            var carry = 0L
            for (j in y.indices) {
                val yLimb = U32(y[j])
                val t = xLimb * yLimb + U32(p[i + j]) + carry
                p[i + j] = t.toInt()
                carry = t ushr 32
            }
            if (i + y.size < p.size)
                p[i + y.size] = carry.toInt()
        }
        return p
    }

    fun newCopy(src: IntArray) = newCopy(src, src.size)

    fun newCopy(src: IntArray, newWordLen: Int): IntArray {
        val dst = IntArray(newWordLen)
        copy(dst, src)
        return dst
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

    fun newShiftRight(x: IntArray, bitCount: Int) : IntArray {
        val newBitLen = bitLen(x) - bitCount
        val newWordLen = (newBitLen + 0x1F) ushr 5
        val z = newCopy(x, newWordLen)
        mutateShiftRight(z, bitCount)
        return z
    }

    fun mutateShiftRight(x: IntArray, bitCount: Int): IntArray {
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        if (wordShift >= x.size) {
            x.fill(0)
            return x
        }
        val newLen = x.size - wordShift
        if (wordShift > 0) {
            System.arraycopy(x, wordShift, x, 0, newLen)
            for (i in newLen..<x.size)
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

    fun newShiftLeft(x: IntArray, bitCount: Int) : IntArray {
        val newBitLen = bitLen(x) + bitCount
        val newWordLen = (newBitLen + 0x1F) ushr 5
        val y = newCopy(x, newWordLen)
        mutateShiftLeft(y, bitCount)
        return y
    }

    fun mutateShiftLeft(x: IntArray, bitCount: Int) {
        val wordShift = bitCount ushr 5
        val innerShift = bitCount and ((1 shl 5) - 1)
        if (wordShift >= x.size) {
            x.fill(0)
            return
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
    }

    fun bitLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return 32 - numberOfLeadingZeros(x[i]) + (i * 32)
        return 0
    }

    fun newAnd(x: IntArray, y: IntArray): IntArray {
        val m = min(x.size, y.size)
        val z = IntArray(m)
        for (i in 0..<m)
            z[i] = x[i] and y[i]
        return z
    }

    fun mutateAnd(x: IntArray, y: IntArray): IntArray {
        val m = min(x.size, y.size)
        for (i in 0..<m)
            x[i] = x[i] and y[i]
        for (i in m..<x.size)
            x[i] = 0
        return x
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
            val cmp = compareUnsigned(x[i], y[i])
            if (cmp != 0)
                return cmp
        }
        return 0
    }

    fun mutateDivideRemainder(x: IntArray, n: Int): Long {
        if (n == 0)
            throw RuntimeException("DivByZero")
        val n64 = U32(n)
        var carry = 0L
        for (i in x.size-1 downTo 0) {
            val t = (carry shl 32) + U32(x[i])
            val q = divideUnsigned(t, n64)
            val r = remainderUnsigned(t, n64)
            x[i] = q.toInt()
            carry = r
        }
        return carry
    }

    fun newDivide(x: IntArray, n: Int): IntArray {
        val q = newCopy(x)
        mutateDivideRemainder(q, n)
        return q
    }

    fun newDivide(x: IntArray, y: IntArray): IntArray {
        val n = nonZeroLimbLen(y)
        if (n < 2)
            return newDivide(x, y[0])
        val m = nonZeroLimbLen(x)
        if (m < n)
            return IntArray(1)
        val u = x
        val v = y
        val q = IntArray(m-n+1)
        val r = null
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        return q
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
        val shift = Integer.numberOfLeadingZeros(vn[n - 1])
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
            var qhat = divideUnsigned(num, vn_1)
            var rhat = remainderUnsigned(num, vn_1)

            // correct estimate
            while ((qhat ushr 32) != 0L ||
                compareUnsigned(
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

}