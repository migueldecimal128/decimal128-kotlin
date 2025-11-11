@file:Suppress("NOTHING_TO_INLINE")
package com.decimal128.hugeint

import com.decimal128.decimal.unsignedMulHi
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class MutHugeInt private constructor (
    var sign: Boolean,
    var magia: IntArray,
    var limbLen: Int,
    var tmp1: IntArray,
    var tmp2: IntArray) {
    constructor() : this(false, IntArray(4), 0, Magia.ZERO, Magia.ZERO)

    fun isValid(): Boolean {
        if (limbLen < 0 || limbLen > magia.size)
            return false
        if (limbLen != 0 && magia[limbLen - 1] == 0)
            return false
        return true
    }

    fun validate() {
        if (!isValid())
            check (false)
    }

    fun copy(): MutHugeInt {
        validate()
        val newMagia = magia.copyOf()
        val duplicate =
            MutHugeInt(sign, newMagia, limbLen, Magia.ZERO, Magia.ZERO)
        duplicate.validate()
        return duplicate
    }

    fun setZero(): MutHugeInt {
        sign = false
        limbLen = 0
        return this
    }

    private fun set(sign: Boolean, w: UInt): MutHugeInt {
        this.sign = sign
        limbLen = if (w == 0u) 0 else 1
        magia[0] = w.toInt()
        return this
    }

    private fun set(sign: Boolean, dw: ULong): MutHugeInt {
        if (dw == dw.toUInt().toULong())
            return set(sign, dw.toUInt())
        this.sign = sign
        limbLen = 2
        if (magia.size < 2)
            magia = IntArray(4)
        magia[0] = dw.toInt()
        magia[1] = (dw shr 32).toInt()
        return this
    }

    fun set(hi: HugeInt): MutHugeInt = set(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))

    fun set(mhi: MutHugeInt): MutHugeInt = set(mhi.sign, mhi.magia, mhi.limbLen)

    private fun set(ySign: Boolean, y: IntArray, yLen: Int): MutHugeInt {
        if (magia.size < yLen)
            magia = Magia.newWithMinLen(yLen)
        sign = ySign
        limbLen = yLen
        System.arraycopy(y, 0, magia, 0, yLen)
        return this
    }

    fun toHugeInt(): HugeInt =
        HugeInt.fromLittleEndianIntArray(sign, magia, limbLen)

    private inline fun toRawULong(): ULong {
        return when {
            limbLen == 1 -> dw32(magia[0])
            limbLen >= 2 -> (dw32(magia[1]) shl 32) or dw32(magia[0])
            else -> 0uL
        }
    }


    operator fun plusAssign(n: Int) = mutateAddSubImpl(n < 0, n.absoluteValue.toUInt())
    operator fun plusAssign(w: UInt) = mutateAddSubImpl(false, w)
    operator fun plusAssign(l: Long) = mutateAddSubImpl(l < 0, l.absoluteValue.toULong())
    operator fun plusAssign(dw: ULong) = mutateAddSubImpl(false, dw)
    operator fun plusAssign(hi: HugeInt) =
        mutateAddSubImpl(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))
    operator fun plusAssign(mhi: MutHugeInt) = mutateAddSubImpl(mhi.sign, mhi.magia, mhi.limbLen)

    operator fun minusAssign(n: Int) = mutateAddSubImpl(n > 0, n.absoluteValue.toUInt())
    operator fun minusAssign(w: UInt) = mutateAddSubImpl(w > 0u, w)
    operator fun minusAssign(l: Long) = mutateAddSubImpl(l > 0L, l.absoluteValue.toULong())
    operator fun minusAssign(dw: ULong) = mutateAddSubImpl(dw > 0uL, dw)
    operator fun minusAssign(hi: HugeInt) =
        mutateAddSubImpl(!hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))
    operator fun minusAssign(mhi: MutHugeInt) = mutateAddSubImpl(!mhi.sign, mhi.magia, mhi.limbLen)

    operator fun timesAssign(n: Int) = mutateMulImpl(n < 0, n.absoluteValue.toUInt())
    operator fun timesAssign(w: UInt) = mutateMulImpl(false, w)
    operator fun timesAssign(l: Long) = mutateMulImpl(l < 0, l.absoluteValue.toULong())
    operator fun timesAssign(dw: ULong) = mutateMulImpl(false, dw)
    operator fun timesAssign(hi: HugeInt) =
        mutateMulImpl(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))
    operator fun timesAssign(mhi: MutHugeInt) {
        if (this === mhi)
            mutateSquare()  // prevent aliasing problems & improve performance
        else
            mutateMulImpl(mhi.sign, mhi.magia, mhi.limbLen)
    }

    fun addSquareOf(n: Int) = addSquareOf(n.absoluteValue.toUInt())
    fun addSquareOf(w: UInt) = plusAssign(w.toULong() * w.toULong())
    fun addSquareOf(l: Long) = addSquareOf(l.absoluteValue.toULong())
    fun addSquareOf(dw: ULong) {
        val lo64 = dw * dw
        if ((dw shr 32) == 0uL) {
            mutateAddMagImpl(lo64)
            return
        }
        val hi64 = unsignedMulHi(dw, dw)
        tmp1[0] = lo64.toInt()
        tmp1[1] = (lo64 shr 32).toInt()
        tmp1[2] = hi64.toInt()
        tmp1[3] = (hi64 shr 32).toInt()
        mutateAddMagImpl(tmp1, Magia.nonZeroLimbLen(tmp1, 4))
    }
    fun addSquareOf(hi: HugeInt) = addSquareOfImpl(hi.magia, Magia.nonZeroLimbLen(hi.magia))
    fun addSquareOf(other: MutHugeInt) {
        // this works OK when this == other because
        // addSquareOfImpl multiplies into tmp1 before the add operation
        if (other.limbLen > 0)
            addSquareOfImpl(other.magia, other.limbLen)
    }

    fun addAbsValueOf(n: Int) = plusAssign(n.absoluteValue.toUInt())
    fun addAbsValueOf(w: UInt) = plusAssign(w)
    fun addAbsValueOf(l: Long) = plusAssign(l.absoluteValue.toULong())
    fun addAbsValueOf(dw: ULong) = plusAssign(dw)
    fun addAbsValueOf(hi: HugeInt) =
        mutateAddMagImpl(hi.magia, Magia.nonZeroLimbLen(hi.magia))
    fun addAbsValueOf(hia: MutHugeInt) =
        mutateAddMagImpl(hia.magia, hia.limbLen)

    private fun mutateAddSubImpl(otherSign: Boolean, w: UInt) {
        validate()
        when {
            w == 0u -> {}
            this.sign == otherSign -> mutateAddMagImpl(w.toULong())
            limbLen == 0 -> set(!otherSign, w)
            limbLen > 1 || magia[0].toUInt() > w -> {
                Magia.mutateSub(magia, limbLen, w)
                normalizeLen1(limbLen)
                sign = sign and (limbLen > 0)
            }
            magia[0].toUInt() < w -> set(otherSign, w - magia[0].toUInt())
            else -> setZero()
        }
    }

    private fun mutateAddSubImpl(otherSign: Boolean, dw: ULong) {
        val rawULong = toRawULong()
        when {
            dw == 0uL -> {}
            (dw shr 32) == 0uL -> mutateAddSubImpl(otherSign, dw.toUInt())
            this.sign == otherSign -> mutateAddMagImpl(dw)
            limbLen == 0 -> set(!otherSign, dw)
            limbLen > 2 || rawULong > dw -> {
                Magia.mutateSub(magia, limbLen, dw)
                normalizeLen1(limbLen)
                sign = sign and (limbLen > 0)
            }
            rawULong < dw -> set(otherSign, dw - rawULong)
            else -> setZero()
        }
    }

    private fun mutateAddSubImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        validate()
        when {
            yLen <= 2 -> {
                when {
                    yLen == 2 -> mutateAddSubImpl(ySign, (dw32(y[1]) shl 32) or dw32(y[0]))
                    yLen == 1 -> mutateAddSubImpl(ySign, y[0].toUInt())
                }
                // if yLen == 0 do nothing
                return
            }

            limbLen == 0 -> { set(ySign, y, yLen); return }
            this.sign == ySign -> { mutateAddMagImpl(y, yLen); return }
        }
        val cmp = Magia.cmp(magia, limbLen, y, yLen)
        when {
            cmp > 0 -> Magia.mutateSub(magia, limbLen, y, yLen)
            cmp < 0 -> {
                if (magia.size < yLen)
                    magia = Magia.newLongerCopyWithMinLen(magia, yLen)
                if (limbLen < yLen)
                    magia.fill(0, limbLen, yLen)
                Magia.mutateReverseSub(magia, yLen, y, yLen)
                limbLen = Magia.nonZeroLimbLen(magia, yLen)
                sign = ySign
            }
            else -> { setZero(); return }
        }
        normalizeLen1(limbLen)
        sign = sign and (limbLen > 0)
    }

    private fun normalizeLen1(maxLimbCount: Int) {
        var last = maxLimbCount - 1
        while (last >= 0 && magia[last] == 0)
            --last
        limbLen = last + 1
    }

    private fun mutateAddMagImpl(dw: ULong) {
        val carry = Magia.mutateAdd(magia, limbLen, dw)
        if (carry == 0uL)
            return
        val newLen = limbLen + if ((carry shr 32) == 0uL) 1 else 2
        if (newLen > magia.size)
            magia = Magia.newLongerCopyWithMinLen(magia, newLen)
        magia[newLen - 1] = (carry shr 32).toInt() // overwritten when carry hi word == 0
        magia[limbLen] = carry.toInt()
        limbLen = newLen
    }

    private fun mutateAddMagImpl(y: IntArray, yLen: Int) {
        if (yLen > magia.size)
            magia = Magia.newLongerCopyWithMinLen(magia, yLen + 1)
        val carry: UInt = Magia.mutateAdd(magia, limbLen, y, yLen)
        val maxOperandLen = max(limbLen, yLen)
        if (carry == 0u) {
            limbLen = maxOperandLen
            return
        }
        if (maxOperandLen + 1 > magia.size)
            magia = Magia.newLongerCopyWithMinLen(magia, maxOperandLen + 1)
        magia[maxOperandLen] = 1
        limbLen = maxOperandLen + 1
    }

    private inline fun addSquareOfImpl(y: IntArray, yLen: Int) {
        val sqrLenMax = yLen * 2
        if (tmp1.size < sqrLenMax)
            tmp1 = Magia.newWithMinLen(sqrLenMax)
        else
            tmp1.fill(0, 0, sqrLenMax)
        Magia.sqr(tmp1, y, yLen)
        val sqrLen = sqrLenMax - if (tmp1[sqrLenMax - 1] == 0) 1 else 0
        mutateAddMagImpl(tmp1, sqrLen)
    }

    private fun mutateSubMagImpl(w: UInt) {
        Magia.mutateSub(magia, limbLen, w)
        var last = limbLen - 1
        while (last >= 0 && magia[last] == 0)
            --last
        limbLen = last + 1
        sign = sign and (limbLen > 0)
    }

    private fun mutateSubMagImpl(dw: ULong) {
        Magia.mutateSub(magia, limbLen, dw)
        var last = limbLen - 1
        while (last >= 0 && magia[last] == 0)
            --last
        limbLen = last + 1
        sign = sign and (limbLen > 0)
    }

    private fun mutateSubMagImpl(y: IntArray, yLen: Int) {
        Magia.mutateSub(magia, limbLen, y, yLen)
        normalizeLen1(limbLen)
        sign = sign and (limbLen > 0)
    }

    private fun mutateMulImpl(wSign: Boolean, w: UInt) {
        validate()
        if (w == 0u || limbLen == 0) {
            setZero()
            return
        }
        val newLimbLenMax = limbLen + 1
        if (magia.size < newLimbLenMax)
            magia = Magia.newLongerCopyWithMinLen(magia, newLimbLenMax)
        magia[limbLen] = 0
        Magia.mul(magia, magia, limbLen, w)
        sign = sign xor wSign
        limbLen += if (magia[limbLen] == 0) 0 else 1
    }

    private fun mutateMulImpl(wSign: Boolean, dw: ULong) {
        validate()
        if ((dw shr 32) == 0uL) {
            mutateMulImpl(wSign, dw.toUInt())
            return
        }
        if (limbLen == 0)
            return
        if (magia.size < limbLen + 2)
            magia = Magia.newLongerCopyWithMinLen(magia, limbLen + 2)
        magia[limbLen] = 0
        magia[limbLen + 1] = 0
        Magia.mul(magia, limbLen + 2, magia, limbLen, dw)
        limbLen += if (magia[limbLen + 1] == 0) 1 else 2
        sign = sign xor wSign
        validate()
    }

    private fun mutateMulImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        validate()
        if (limbLen == 0 || yLen == 0) {
            setZero()
            return
        }
        val m = if (limbLen >= yLen) magia else y
        val mLen = max(limbLen, yLen)
        val n = if (limbLen >= yLen) y else magia
        val nLen = min(limbLen, yLen)
        val pLen = mLen + nLen
        if (tmp1.size < pLen)
            tmp1 = Magia.newWithMinLen(pLen)
        else
            tmp1.fill(0, 0, pLen)
        Magia.mul(tmp1, magia, limbLen, y, yLen)
        val t = magia
        magia = tmp1
        tmp1 = t
        limbLen = pLen - if (magia[pLen - 1] == 0) 1 else 0
        sign = sign xor ySign
    }

    private fun mutateSquare(): MutHugeInt {
        if (limbLen > 0) {
            val newLimbLenMax = limbLen * 2
            if (tmp1.size < newLimbLenMax)
                tmp1 = Magia.newWithMinLen(newLimbLenMax)
            else
                tmp1.fill(0, 0, newLimbLenMax)
            Magia.sqr(tmp1, magia, limbLen)
            val t = tmp1
            tmp1 = magia
            magia = t
            limbLen = newLimbLenMax - if (magia[newLimbLenMax - 1] == 0) 1 else 0
        }
        return this
    }

    override fun toString(): String = Magia.toString(this.sign, this.magia, this.limbLen)

}

private inline fun dw32(n: Int) = n.toULong() and 0xFFFF_FFFFuL
