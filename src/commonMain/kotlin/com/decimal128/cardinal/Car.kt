package com.decimal128.cardinal

import com.decimal128.decimal.unsignedCmp
import com.decimal128.decimal.unsignedDiv
import com.decimal128.decimal.unsignedMod
import kotlin.math.min
import kotlin.math.max


// CAR == Cardinal ARray
// the word 'cardinal' has fallen out of use, but is an unsigned integer
// ... the "cardinality" of a set

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

    fun testBit(x: IntArray, bitIndex: Int): Boolean {
        val wordIndex = bitIndex ushr 5
        if (wordIndex >= x.size)
            return false
        val word = x[wordIndex]
        val bitMask = 1 shl (bitIndex and 0x1F)
        return (word and bitMask) != 0
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

    fun newFromInt(n: Int): IntArray = if (n != 0) intArrayOf(n) else EMPTY_CAR

    fun newFromLong(l: Long): IntArray = if (l != 0L) intArrayOf(l.toInt(), (l ushr 32).toInt()) else EMPTY_CAR

    fun nonZeroLimbLen(x: IntArray): Int {
        for (i in x.size - 1 downTo 0)
            if (x[i] != 0)
                return i + 1
        return 0
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun newWithBitLen(bitLen: Int) = if (bitLen > 0) IntArray((bitLen + 0x1F) ushr 5) else EMPTY_CAR

    fun newAdd(x: IntArray, n: Int): IntArray {
        val newBitLen = max(bitLen(x), (32 - n.countLeadingZeroBits())) + 1
        val z = newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, n)
    }

    fun newOrMutateAdd(x: IntArray, n: Int): IntArray {
        val newBitLen = max(bitLen(x), (32 - n.countLeadingZeroBits())) + 1
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, n)
    }

    fun mutateAdd(x: IntArray, n: Int): IntArray {
        var carry = U32(n)
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

    fun newAdd(x: IntArray, l: Long): IntArray {
        val newBitLen = max(bitLen(x), (64 - l.countLeadingZeroBits())) + 1
        val z = newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, l)
    }

    fun newOrMutateAdd(x: IntArray, l: Long): IntArray {
        val newBitLen = max(bitLen(x), (64 - l.countLeadingZeroBits())) + 1
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, l)
    }

    fun mutateAdd(x: IntArray, l: Long): IntArray {
        if (x.size > 0) {
            val t0 = U32(x[0]) + (l and 0xFFFF_FFFFL)
            x[0] = t0.toInt()
            if (x.size > 1) {
                var carry = t0 ushr 32
                val t1 = U32(x[1]) + (l ushr 32) + carry
                x[1] = t1.toInt()
                carry = t1 ushr 32
                var i = 2
                while (carry > 0L && i < x.size) {
                    val t = U32(x[i]) + carry
                    x[i++] = t.toInt()
                    carry = t ushr 32
                    check(carry <= 1L)
                }
            }
        }
        return x
    }

    fun newAdd(x: IntArray, y: IntArray): IntArray {
        val newBitLen = max(bitLen(x), bitLen(y)) + 1
        val z = newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, y)
    }

    fun newOrMutateAdd(x: IntArray, y: IntArray): IntArray {
        val newBitLen = max(bitLen(x), bitLen(y)) + 1
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, y)
    }

    fun mutateAdd(x: IntArray, y: IntArray): IntArray {
        var carry = 0L
        val min = min(x.size, y.size)
        var i = 0
        while (i < min) {
            val t = U32(x[i]) + U32(y[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
            check(carry in 0L..1L)
            ++i
        }
        while (carry == 1L && i < x.size) {
            val t = U32(x[i]) + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
        return x
    }

    fun newSub(x: IntArray, n: Int) = mutateSub(newCopy(x), n)

    fun mutateSub(x: IntArray, n: Int): IntArray {
        var orAccumulator = 0
        var borrow = U32(n)
        for (i in x.indices) {
            val t = U32(x[i]) - borrow
            val xi = t.toInt()
            x[i] = xi
            orAccumulator = orAccumulator or xi
            borrow = t ushr 63
            if (borrow == 0L)
                break
        }
        return if (orAccumulator != 0 && borrow == 0L)
            x
        else
            EMPTY_CAR
    }

    fun newSub(x: IntArray, l: Long) = mutateSub(newCopy(x), l)

    fun mutateSub(x: IntArray, l: Long): IntArray {
        var orAccumulator = 0
        var borrow = 0L
        if (x.isNotEmpty()) {
            val t0 = U32(x[0]) - (l and 0xFFFF_FFFFL)
            val x0 = t0.toInt()
            x[0] = x0
            orAccumulator = orAccumulator or x0
            if (x.size > 1) {
                borrow = t0 ushr 63
                val t1 = U32(x[1]) - (l ushr 32) - borrow
                val x1 = t1.toInt()
                x[1] = x1
                orAccumulator = orAccumulator or x1
                borrow = t1 ushr 63
                var i = 2
                while (borrow > 0L && i < x.size) {
                    val t = U32(x[i]) - borrow
                    val xi = t.toInt()
                    x[i++] = xi
                    orAccumulator = orAccumulator or xi
                    borrow = t ushr 63
                }
            }
        }
        return if (orAccumulator != 0 && borrow == 0L)
            x
        else
            EMPTY_CAR
    }

    fun newSub(x: IntArray, y: IntArray) = mutateSub(newCopy(x), y)

    fun mutateSub(x: IntArray, y: IntArray): IntArray {
        var orAccumulator = 0
        var borrow = 0L
        val min = min(x.size, y.size)
        var i = 0
        while (i < min) {
            val t = U32(x[i]) - U32(y[i]) - borrow
            val xi = t.toInt()
            x[i] = xi
            orAccumulator = orAccumulator or xi
            borrow = t ushr 63
            check(borrow in 0L..1L)
            ++i
        }
        while (borrow == 1L && i < x.size) {
            val t = U32(x[i]) - borrow
            val xi = t.toInt()
            x[i] = xi
            orAccumulator = orAccumulator or xi
            borrow = t ushr 63
            ++i
        }
        while (borrow == 0L && i < y.size)
            borrow = y[i++].toLong()
        return if (orAccumulator != 0 && borrow == 0L)
            x
        else
            EMPTY_CAR
    }

    fun newMul(x: IntArray, n: Int): IntArray {
        val bitLenX = bitLen(x)
        val bitLenN = 32 - n.countLeadingZeroBits()
        if (bitLenX == 0 || bitLenN == 0)
            return EMPTY_CAR
        val newBitLen = bitLenX + bitLenN
        val prod = newCopyWithBitLen(x, newBitLen)
        mutateMul(prod, n)
        return prod
    }

    fun newOrMutateMul(x: IntArray, n: Int): IntArray {
        val bitLenX = bitLen(x)
        val bitLenN = 32 - n.countLeadingZeroBits()
        if (bitLenX == 0 || bitLenN == 0)
            return EMPTY_CAR
        val newBitLen = bitLenX + bitLenN
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
        return mutateAdd(z, n)
    }

    fun mutateMul(x: IntArray, n: Int): IntArray {
        val n64 = U32(n)
        var carry = 0L
        for (i in x.indices) {
            val t = U32(x[i]) * n64 + carry
            x[i] = t.toInt()
            carry = t ushr 32
        }
        return x
    }

    fun newMul(x: IntArray, l: Long): IntArray {
        val lo = l and 0xFFFF_FFFFL
        val hi = l ushr 32
        if (hi == 0L)
            return newMul(x, lo.toInt())
        val xBitLen = bitLen(x)
        if (xBitLen == 0 || l == 0L)
            return EMPTY_CAR
        val newBitLen = bitLen(x) + (64 - l.countLeadingZeroBits())
        val z = newWithBitLen(newBitLen)

        var t = U32(x[0]) * lo
        z[0] = t.toInt()
        var carry        = t ushr 32

        t = U32(x[0]) * hi
        var prevHighLow   = t and 0xFFFF_FFFFL
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
            prevHighLow   = t and 0xFFFF_FFFFL
            prevHighCarry = t ushr 32
        }

        return z
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

    fun newCopy(src: IntArray) = newCopy(src, nonZeroLimbLen(src))

    fun newCopy(src: IntArray, newWordLen: Int): IntArray {
        if (newWordLen > 0) {
            val dst = IntArray(newWordLen)
            copy(dst, src)
            return dst
        }
        return EMPTY_CAR
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
            return EMPTY_CAR
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

    fun mutateShiftRight(x: IntArray, bitCount: Int) = mutateShiftRight(x, nonZeroLimbLen(x), bitCount)

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

    fun newOrMutateShiftLeft(x: IntArray, bitCount: Int): IntArray {
        val newBitLen = bitLen(x) + bitCount
        val z = if (newBitLen <= x.size shl 5) x else newCopyWithBitLen(x, newBitLen)
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

    fun LT(x: IntArray, y: IntArray): Boolean = compare(x, y) < 0

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

    fun compare(x: IntArray, y: Int): Int {
        val limbLen = nonZeroLimbLen(x)
        return when {
            (limbLen > 1) -> 1
            (limbLen == 0) -> if (y == 0) 0 else -1
            else -> unsignedCmp(x[0], y)
        }
    }

    fun compare(x: IntArray, y: Long): Int {
        val limbLen = nonZeroLimbLen(x)
        return when {
            (limbLen > 2) -> 1
            (limbLen == 2) -> {
                val xT = (x[1].toLong() shl 32) or U32(x[0])
                unsignedCmp(xT, y)
            }
            (limbLen == 1) -> if ((y ushr 32) == 0L) unsignedCmp(x[0], y.toInt()) else -1
            else -> if (y == 0L) 0 else -1
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
        val q = newCopy(x)
        mutateDivideRemainder(q, n)
        return q
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
            return IntArray(1)
        val u = x
        val v = y
        val q = IntArray(m - n + 1)
        val r = null
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        return q
    }

    fun newMod(x: IntArray, n: Int): IntArray {
        val q = newCopy(x)
        val rem = mutateDivideRemainder(q, n)
        return if (rem == 0L) EMPTY_CAR else intArrayOf(rem.toInt())
    }

    fun newMod(x: IntArray, l: Long): IntArray {
        val lo = l.toInt()
        val hi = (l ushr 32).toInt()
        return if (hi == 0) newMod(x, lo) else newMod(x, intArrayOf(lo, hi))
    }

    fun newMod(x: IntArray, y: IntArray): IntArray {
        val divMod = newDivMod(x, y)
        val rem = divMod[1]
        return rem
    }

    fun newDivMod(x: IntArray, y: IntArray): Array<IntArray> {
        val n = nonZeroLimbLen(y)
        if (n < 2) {
            val div = newCopy(x)
            val rem = mutateDivideRemainder(div, y[0])
            return arrayOf(div, intArrayOf(rem.toInt()))
        }
        val m = nonZeroLimbLen(x)
        if (m < n)
            return arrayOf(IntArray(1), newCopy(x))
        val u = x
        val v = y
        val q = IntArray(m - n + 1)
        val r = IntArray(m + 1)
        val status = knuthDivide(q, r, u, v, m, n)
        require(status == 0)
        return arrayOf(q, r)
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

    fun toString(car: IntArray) = toString(false, car)

    fun toString(isNegative: Boolean, car: IntArray): String {
        val bitLen = bitLen(car)
        if (bitLen <= 64) {
            if (bitLen == 0)
                return "0"
            val lo = car[0].toULong() and 0xFFFF_FFFFuL
            val hi = if (bitLen <= 32) 0uL else car[1].toULong() shl 32
            val t = hi or lo
            return t.toString()
        }
        val maxDigitLen = ((bitLen * 1234) shr 12) + 1
        val maxSignedLen = maxDigitLen + if (isNegative) 1 else 0
        val t = newCopy(car)
        val bytes = ByteArray(maxDigitLen)
        bytes[0] = '-'.code.toByte()
        var ib = if (isNegative) 1 else 0
        do {
            val chunk = mutateDivideRemainder(t, 1_000_000_000).toInt()
            ib = renderChunkReversed(chunk, 9, bytes, ib)
        } while (compare(t, 1_000_000_000) >= 0)
        ib = renderChunkReversed(t[0], 1, bytes, ib)
        var j = 0
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

    fun renderChunkReversed(n: Int, minDigitCount: Int, bytes: ByteArray, off: Int): Int {
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

    fun newFromString(str: String) = newFromString(false, str)

    fun newFromString(isNegative: Boolean, str: String): IntArray {
        var i = if (isNegative) 1 else 0
        val strLen = str.length
        when {
            (strLen - i <= 0) ->
                throw IllegalArgumentException("cannot parse empty string")
            //str.startsWith("0x") -> {
            //    return newFromHexString(str)
            //}
        }
        var totalDigitCount = 0
        var accumulator = 0
        var accumulatorDigitCount = 0
        while (i < strLen && str[i] == '0')
            ++i

        val bitLen = ((strLen - i) * 13607 + 4095) ushr 12
        val wordLen = (bitLen + 0x1F) ushr 5
        val z = IntArray(wordLen)

        while (i < strLen) {
            val ch = str[i++]
            if (ch !in '0'..'9') {
                if (ch == '_' && i > 0)
                    continue
                throw IllegalArgumentException("unsigned integer parse error:$str")
            }
            val n = ch - '0'
            ++totalDigitCount
            accumulator = accumulator * 10 + n
            ++accumulatorDigitCount
            if (accumulatorDigitCount < 9)
                continue
            mutateAdd(mutateMul(z, 1000000000), accumulator)
            accumulator = 0
            accumulatorDigitCount = 0
        }
        if (accumulatorDigitCount > 0) {
            var pow10 = 1
            for (j in 0..<accumulatorDigitCount)
                pow10 *= 10
            mutateAdd(mutateMul(z, pow10), accumulator)
        }
        return z
    }

}
