@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.hugeint

import kotlin.math.min

object MutMagia {

    private inline fun dw32(n: Int) = n.toUInt().toULong()

    inline fun newWithMinLen(minLimbLen: Int) : IntArray {
        val t = minLimbLen + 1 - (-minLimbLen)
        return IntArray((t + 3) ushr 2)
    }

    fun newLargerCopyWithMinLen(x: IntArray, newMinLimbLen: Int) : IntArray {
        check (newMinLimbLen > x.size)
        val z = newWithMinLen(newMinLimbLen)
        System.arraycopy(x, 0, z, 0, x.size)
        return z
    }


    fun mutateAdd(x: IntArray, xLen: Int, w: UInt): UInt {
        var carry = w.toULong()
        var i = 0
        while (carry != 0uL && i < xLen) {
            val s = dw32(x[i]) + carry
            x[i] = s.toInt()
            carry = s shr 32
            ++i
        }
        return carry.toUInt()
    }

    fun mutateAdd(x: IntArray, xLen: Int, dw: ULong): ULong {
        var carry = dw
        var i = 0
        while (carry != 0uL && i < xLen) {
            val s = dw32(x[i]) + (carry and 0xFFFF_FFFFuL)
            x[i] = s.toInt()
            carry = (carry shr 32) + (s shr 32)
            ++i
        }
        return carry
    }

    fun mutateAdd(x: IntArray, xLen: Int, y: IntArray, yLen: Int): UInt {
        check (xLen == 0 || x[xLen-1] != 0)
        check (yLen == 0 || y[yLen-1] != 0)
        val minLen = min(xLen, yLen)
        var carry = 0uL
        for (i in 0..<minLen) {
            val s = dw32(x[i]) + dw32(y[i]) + carry
            x[i] = carry.toInt()
            carry = carry shr 32
        }
        if (xLen >= yLen) {
            var i = 0
            while (carry != 0uL && i < xLen) {
                val s = dw32(x[i]) + carry
                x[i] = s.toInt()
                carry = s shr 32
                ++i
            }
        } else {
            for (i in minLen..<yLen) {
                val s = dw32(y[i]) + carry
                x[i] = s.toInt()
                carry = s shr 32
            }
        }
        return carry.toUInt()
    }

    fun mul(p: IntArray, x: IntArray, xLen: Int, y: IntArray, yLen: Int) {
        check (xLen == 0 || x[xLen-1] != 0)
        check (yLen == 0 || y[yLen-1] != 0)
        val m = if (xLen < yLen) x else y // m is the short one
        val n = if (xLen < yLen) y else x
        val mLen = if (xLen < yLen) xLen else yLen
        val nLen = if (xLen < yLen) yLen else xLen
        for (i in 0..<mLen) {
            val mLimb = dw32(m[i])
            var carry = 0uL
            for (j in 0..<nLen) {
                val pp = dw32(n[j]) * mLimb + carry + dw32(p[i + j])
                p[i + j] = pp.toInt()
                carry = pp shr 32
            }
            p[i + nLen] = carry.toInt()
        }
    }

    /**
     * Squares the big integer x[0..<xLen] and stores the result into p[0..<2*xLen].
     *
     * Preconditions:
     *  - p has length >= 2 * xLen
     *  - p is zero-initialized before call
     *
     * Algorithm:
     *  - Phase 1: computes cross terms (i<j), doubled.
     *  - Phase 2: adds diagonal terms (x[i]^2).
     *  - Uses 64-bit unsigned math to avoid overflow.
     */

    fun sqr(p: IntArray, x: IntArray, xLen: Int) {
        check (xLen == 0 || x[xLen-1] != 0)
        // 1) Cross terms: for i<j, add (a[i]*a[j]) twice into p[i+j]
        // these terms are doubled
        for (i in 0..<xLen) {
            val xi = dw32(x[i])
            var carry = 0uL
            for (j in (i + 1)..<xLen) {
                val prod = xi * dw32(x[j])        // 32x32 -> 64
                // add once
                val t1 = prod + dw32(p[i + j]) + carry
                val p1 = t1 and 0xFFFF_FFFFuL
                carry = t1 shr 32
                // add second time (doubling) — avoids (prod << 1) overflow
                val t2 = prod + p1
                p[i + j] = t2.toInt()
                carry += t2 shr 32
            }
            p[i + xLen] = carry.toInt()
        }

        // 2) Diagonals: add a[i]^2 into columns 2*i and 2*i+1
        // terms on the diagonal are not doubled
        var carry = 0uL
        for (i in 0..<xLen) {
            val xi = dw32(x[i])
            carry += xi * xi + dw32(p[2*i])
            p[2*i] = carry.toInt()
            carry = carry shr 32
        }
        p[2*xLen - 1] = carry.toInt()
    }

    fun mutateSub(x: IntArray, len1: Int, w: UInt) {
        var borrow = w.toULong()
        var i = 0
        while (borrow != 0uL && i < len1) {
            borrow = dw32(x[i]) - borrow
            x[i] = borrow.toInt()
            borrow = borrow shr 63
            ++i
        }
    }

    fun mutateSub(x: IntArray, len1: Int, dw: ULong) {
        check (len1 >= 2)
        val t0 = dw32(x[0]) - (dw and 0xFFFF_FFFFuL)
        x[0] = t0.toInt()
        val t1 = dw32(x[1]) - (dw shr 32) - (t0 shr 63)
        x[1] = t1.toInt()
        var borrow = t1 shr 63
        var i = 2
        while (borrow != 0uL && i < len1) {
            borrow = dw32(x[i]) - borrow
            x[i] = borrow.toInt()
            borrow = borrow shr 63
            ++i
        }
    }

    fun mutateSub(x: IntArray, xLen: Int, y: IntArray, yLen: Int) {
        check (xLen == 0 || x[xLen-1] != 0)
        check (yLen == 0 || y[yLen-1] != 0)
        check (xLen >= yLen)
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
        check (borrow == 0uL)
    }

    fun mutateReverseSub(x: IntArray, xLen: Int, y: IntArray, yLen: Int) {
        check (xLen == 0 || x[xLen-1] != 0)
        check (yLen == 0 || y[yLen-1] != 0)
        check (xLen == yLen)
        var borrow = 0uL // 0 or 1
        var i = 0
        while (i < yLen) {
            borrow = dw32(y[i]) - dw32(x[i]) - borrow
            x[i] = borrow.toInt()
            borrow = borrow shr 63
            ++i
        }
        check (borrow == 0uL)
    }

    fun cmp(x: IntArray, xLen: Int, y: IntArray, yLen: Int): Int {
        check (xLen == 0 || x[xLen-1] != 0)
        check (yLen == 0 || y[yLen-1] != 0)
        if (xLen != yLen)
            return if (xLen > yLen) 1 else -1
        for (i in xLen - 1 downTo 0) {
            val cmp = x[i].toUInt().compareTo(y[i].toUInt())
            if (cmp != 0)
                return cmp
        }
        return 0
    }

}