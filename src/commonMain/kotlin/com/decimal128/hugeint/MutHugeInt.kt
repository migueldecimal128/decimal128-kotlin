@file:Suppress("NOTHING_TO_INLINE")
package com.decimal128.hugeint

import com.decimal128.decimal.unsignedMulHi
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

private inline fun dw32(n: Int) = n.toULong() and 0xFFFF_FFFFuL

class MutHugeInt(var sign: Boolean, var magia1: IntArray, var len1: Int, var magia2: IntArray, var magia3: IntArray) {
    constructor() : this(false, IntArray(4), 0, IntArray(4), IntArray(4))

    fun setZero(): MutHugeInt {
        sign = false
        len1 = 0
        return this
    }

    fun set(n: Int): MutHugeInt = set(n < 0, n.absoluteValue.toUInt())

    fun set(w: UInt): MutHugeInt = set(false, w)

    fun set(sign: Boolean, w: UInt): MutHugeInt {
        this.sign = sign
        len1 = if (w == 0u) 0 else 1
        magia1[0] = w.toInt()
        return this
    }

    fun set(l: Long): MutHugeInt = set(l < 0, l.absoluteValue.toULong())

    fun set(dw: ULong): MutHugeInt = set(false, dw)

    fun set(sign: Boolean, dw: ULong): MutHugeInt {
        if (dw == dw.toUInt().toULong())
            return set(sign, dw.toUInt())
        this.sign = sign
        len1 = 2
        magia1[0] = dw.toInt()
        magia1[1] = (dw shr 32).toInt()
        return this
    }

    fun set(hi: HugeInt): MutHugeInt = set(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))

    fun set(mhi: MutHugeInt): MutHugeInt = set(mhi.sign, mhi.magia1, mhi.len1)

    private fun set(ySign: Boolean, y: IntArray, yLen: Int): MutHugeInt {
        if (magia1.size < yLen)
            magia1 = Magia.newWithMinLen(yLen)
        sign = ySign
        len1 = yLen
        System.arraycopy(y, 0, magia1, 0, yLen)
        return this
    }

    fun toRawULong(): ULong {
        return when {
            len1 == 1 -> dw32(magia1[0])
            len1 >= 2 -> (dw32(magia1[1]) shl 32) or dw32(magia1[0])
            else -> 0uL
        }
    }

    fun setSquare(n: Int): MutHugeInt = setSquare(n.absoluteValue.toUInt())
    fun setSquare(w: UInt): MutHugeInt = set(w.toULong() * w.toULong())
    fun setSquare(l: Long): MutHugeInt = setSquare(l.absoluteValue.toULong())
    fun setSquare(dw: ULong): MutHugeInt {
        val hi = unsignedMulHi(dw, dw)
        val lo = dw * dw
        magia1[0] = lo.toInt()
        magia1[1] = (lo shr 32).toInt()
        magia1[2] = hi.toInt()
        magia1[3] = (hi shr 32).toInt()
        normalizeLen1(4)
        sign = false
        return this
    }
    fun setSquare(hi: HugeInt): MutHugeInt = setSquareImpl(hi.magia, hi.magia.size)
    fun setSquare(mhi: MutHugeInt): MutHugeInt = setSquareImpl(mhi.magia1, mhi.magia1.size)

    private fun setSquareImpl(x: IntArray, xLen: Int): MutHugeInt {
        val pLen = xLen + xLen
        if (pLen > x.size)
            realloc1(pLen)
        else
            magia1.fill(0, 0, pLen)
        Magia.sqr(magia1, x, xLen)
        normalizeLen1(pLen)
        sign = false
        return this
    }

    operator fun plusAssign(n: Int) = mutateAddSubImpl(n < 0, n.absoluteValue.toUInt())
    operator fun plusAssign(w: UInt) = mutateAddSubImpl(false, w)
    operator fun plusAssign(l: Long) = mutateAddSubImpl(l < 0, l.absoluteValue.toULong())
    operator fun plusAssign(dw: ULong) = mutateAddSubImpl(false, dw)
    operator fun plusAssign(hi: HugeInt) = mutateAddSubImpl(hi.sign, hi.magia, hi.magia.size)
    operator fun plusAssign(mhi: MutHugeInt) = mutateAddSubImpl(mhi.sign, mhi.magia1, mhi.len1)

    operator fun minusAssign(n: Int) = mutateAddSubImpl(n > 0, n.absoluteValue.toUInt())
    operator fun minusAssign(w: UInt) = mutateAddSubImpl(w > 0u, w)
    operator fun minusAssign(l: Long) = mutateAddSubImpl(l > 0L, l.absoluteValue.toULong())
    operator fun minusAssign(dw: ULong) = mutateAddSubImpl(dw > 0uL, dw)
    operator fun minusAssign(hi: HugeInt) = mutateAddSubImpl(!hi.sign, hi.magia, hi.magia.size)
    operator fun minusAssign(mhi: MutHugeInt) = mutateAddSubImpl(!mhi.sign, mhi.magia1, mhi.len1)

    operator fun timesAssign(n: Int) = mutateMulImpl(n < 0, n.absoluteValue.toUInt())
    operator fun timesAssign(w: UInt) = mutateMulImpl(false, w)
    operator fun timesAssign(l: Long) = mutateMulImpl(l < 0, l.absoluteValue.toULong())
    operator fun timesAssign(dw: ULong) = mutateMulImpl(false, dw)
    operator fun timesAssign(hi: HugeInt) = mutateMulImpl(hi.sign, hi.magia, hi.magia.size)
    operator fun timesAssign(mhi: MutHugeInt) = mutateMulImpl(mhi.sign, mhi.magia1, mhi.magia1.size)

    operator fun divAssign(n: Int) = mutateDivImpl(n < 0, n.absoluteValue.toUInt())
    operator fun divAssign(w: UInt) = mutateDivImpl(false, w)
    operator fun divAssign(l: Long) = mutateDivImpl(l < 0, l.absoluteValue.toULong())
    operator fun divAssign(dw: ULong) = mutateDivImpl(false, dw)
    operator fun divAssign(hi: HugeInt) = mutateDivImpl(hi.sign, hi.magia, hi.magia.size)
    operator fun divAssign(mhi: MutHugeInt) = mutateDivImpl(mhi.sign, mhi.magia1, mhi.magia1.size)

    operator fun remAssign(n: Int) = mutateRemImpl(n.absoluteValue.toUInt())
    operator fun remAssign(w: UInt) = mutateRemImpl(w)
    operator fun remAssign(l: Long) = mutateRemImpl(l.absoluteValue.toULong())
    operator fun remAssign(dw: ULong) = mutateRemImpl(dw)
    operator fun remAssign(hi: HugeInt) = mutateRemImpl(hi.magia, hi.magia.size)
    operator fun remAssign(mhi: MutHugeInt) = mutateRemImpl(mhi.magia1, mhi.magia1.size)

    private fun mutateAddSubImpl(otherSign: Boolean, w: UInt) {
        when {
            w == 0u -> {}
            this.sign == otherSign -> mutateAddMagImpl(w)
            len1 == 0 -> set(!otherSign, w)
            len1 > 1 || magia1[0].toUInt() > w -> {
                Magia.mutateSub(magia1, len1, w)
                normalizeLen1(len1)
                sign = sign and (len1 > 0)
            }
            magia1[0].toUInt() < w -> set(otherSign, w - magia1[0].toUInt())
            else -> setZero()
        }
    }

    private fun mutateAddSubImpl(otherSign: Boolean, dw: ULong) {
        val rawULong = toRawULong()
        when {
            dw == 0uL -> {}
            (dw shr 32) == 0uL -> mutateAddSubImpl(otherSign, dw.toUInt())
            this.sign == otherSign -> mutateAddMagImpl(dw)
            len1 == 0 -> set(!otherSign, dw)
            len1 > 2 || rawULong > dw -> {
                Magia.mutateSub(magia1, len1, dw)
                normalizeLen1(len1)
                sign = sign and (len1 > 0)
            }
            rawULong < dw -> set(otherSign, dw - rawULong)
            else -> setZero()
        }
    }

    private fun mutateAddSubImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        when {
            yLen <= 2 -> {
                when {
                    yLen == 2 -> mutateAddSubImpl(ySign, (dw32(y[1]) shl 32) or dw32(y[0]))
                    yLen == 1 -> mutateAddSubImpl(ySign, y[0].toUInt())
                }
                return
            }

            len1 == 0 -> { set(ySign, y, yLen); return }
            this.sign == ySign -> { mutateAddMagImpl(y, yLen); return }
        }
        val cmp = Magia.cmp(magia1, len1, y, yLen)
        when {
            cmp > 0 -> Magia.mutateSub(magia1, len1, y, yLen)
            cmp < 0 -> {
                if (magia1.size < yLen)
                    resize1(yLen)
                Magia.mutateReverseSub(magia1, len1, y, yLen)
                len1 = yLen
                sign = ySign
            }
            else -> { setZero(); return }
        }
        normalizeLen1(len1)
        sign = sign and (len1 > 0)
    }

    private fun mutateAddMagImpl(w: UInt) {
        val carry = Magia.mutateAdd(magia1, len1, w)
        if (carry == 0u)
            return
        val newLen = len1 + 1
        if (newLen > magia1.size)
            resize1(newLen)
        magia1[len1] = carry.toInt()
        len1 = newLen
    }

    private fun resize1(newLimbCount: Int) {
        magia1 = Magia.newLargerCopyWithMinLen(magia1, newLimbCount)
    }

    private fun realloc1(newLimbCount: Int) {
        magia1 = Magia.newWithMinLen(newLimbCount)
    }

    private fun realloc2(newLimbCount: Int) {
        magia2 = Magia.newWithMinLen(newLimbCount)
    }

    private fun normalizeLen1(maxLimbCount: Int) {
        var last = maxLimbCount - 1
        while (last >= 0 && magia1[last] == 0)
            --last
        len1 = last + 1
    }

    private fun mutateAddMagImpl(dw: ULong) {
        val carry = Magia.mutateAdd(magia1, len1, dw)
        if (carry == 0uL)
            return
        val newLen = len1 + 1 + (-(carry shr 32).toInt() shr 31)
        if (newLen > magia1.size)
            resize1(newLen)
        magia1[newLen - 1] = (carry shr 32).toInt() // overwritten when carry hi word == 0
        magia1[len1] = carry.toInt()
        len1 = newLen
    }

    private fun mutateAddMagImpl(y: IntArray, yLen: Int) {
        if (yLen > magia1.size)
            resize1(yLen + 1)
        val carry: UInt = Magia.mutateAdd(magia1, len1, y, yLen)
        val maxOperandLen = max(len1, yLen)
        if (carry == 0u) {
            len1 = maxOperandLen
            return
        }
        resize1(maxOperandLen + 1)
        magia1[maxOperandLen] = 1
    }

    private fun mutateSubMagImpl(w: UInt) {
        Magia.mutateSub(magia1, len1, w)
        var last = len1 - 1
        while (last >= 0 && magia1[last] == 0)
            --last
        len1 = last + 1
        sign = sign and (len1 > 0)
    }

    private fun mutateSubMagImpl(dw: ULong) {
        Magia.mutateSub(magia1, len1, dw)
        var last = len1 - 1
        while (last >= 0 && magia1[last] == 0)
            --last
        len1 = last + 1
        sign = sign and (len1 > 0)
    }

    private fun mutateSubMagImpl(y: IntArray, yLen: Int) {
        Magia.mutateSub(magia1, len1, y, yLen)
        normalizeLen1(len1)
        sign = sign and (len1 > 0)
    }

    private fun mutateMulImpl(wSign: Boolean, w: UInt) {
        if (w == 0u) {
            setZero()
            return
        }
        sign = sign xor wSign
        val dw = w.toULong()
        val carry = 0uL
        for (i in 0..<len1) {
            val p = dw32(magia1[i]) * dw + carry
            magia1[i] = p.toInt()
        }
        if (carry == 0uL)
            return
        val newLen = len1 + 1
        if (newLen > magia1.size)
            resize1(newLen)
        magia1[len1] = carry.toInt()
        len1 = newLen
    }

    private fun mutateMulImpl(wSign: Boolean, dw: ULong) {
        if ((dw shr 32) == 0uL) {
            mutateMulImpl(wSign, dw.toUInt())
            return
        }
        // FIXME
        val y = intArrayOf(dw.toInt(), (dw shr 32).toInt())
        mutateMulImpl(wSign, y, 2)
    }

    private fun mutateMulImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        val m = if (len1 >= yLen) magia1 else y
        val mLen = max(len1, yLen)
        val n = if (len1 >= yLen) y else magia1
        val nLen = min(len1, yLen)
        val pLen = mLen + nLen
        if (magia2.size < pLen)
            magia2 = Magia.newWithMinLen(pLen)
        else
            magia2.fill(0, 0, pLen)
        Magia.mul(magia2, magia1, len1, y, yLen)
        val t = magia1
        magia1 = magia2
        magia2 = t
        len1 = pLen - if (magia1[pLen - 1] == 0) 1 else 0
    }

    private fun mutateDivImpl(wSign: Boolean, w: UInt) {
        if (w == 0u)
            throw ArithmeticException("div by zero")
        if (len1 > 0) {
            Magia.mutateDivideRemainder(this.magia1, len1, w)
            if (magia1[len1 - 1] == 0)
                --len1
            sign = (sign xor wSign) && (len1 > 0)
        }
    }

    private fun mutateDivImpl(wSign: Boolean, dw: ULong) {
        if ((dw shr 32) == 0uL) {
            mutateDivImpl(wSign, dw.toUInt())
            return
        }
        if (len1 == 0)
            return
        // FIXME
        mutateDivImpl(wSign, intArrayOf(dw.toInt(), (dw shr 32).toInt()), 2)
    }

    private fun mutateDivImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        TODO()
    }

    private fun mutateRemImpl(w: UInt) {
        if (w == 0u)
            throw ArithmeticException("div by zero")
        val rem =
            if (len1 > 0)
                Magia.mutateDivideRemainder(this.magia1, len1, w)
            else
                w
        set(sign, rem)
    }

    private fun mutateRemImpl(dw: ULong) {
        if ((dw shr 32) == 0uL) {
            mutateRemImpl(dw.toUInt())
            return
        }
        if (len1 == 0)
            return
        TODO()
    }

    private fun mutateRemImpl(y: IntArray, yLen: Int) {
        TODO()
    }
}
