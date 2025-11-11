@file:Suppress("NOTHING_TO_INLINE")
package com.decimal128.hugeint

import com.decimal128.decimal.unsignedMulHi
import com.decimal128.hugeint.Magia.knuthDivideNormalizedCore
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private inline fun dw32(n: Int) = n.toULong() and 0xFFFF_FFFFuL

private inline fun limbLenFromBitLen(n: Int) = (n + 31) ushr 5

class MutHugeInt private constructor (
    var sign: Boolean,
    var magia: IntArray,
    var limbLen: Int,
    var tmp1: IntArray,
    var tmp2: IntArray) : Comparable<MutHugeInt> {
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
        if ((dw shr 32) == 0uL) {
            this += dw * dw
            return
        }
        if (tmp2.size < 2)
            tmp2 = Magia.newWithMinLen(2)
        tmp2[0] = dw.toInt()
        tmp2[1] = (dw shr 32).toInt()
        addSquareOfImpl(tmp2, 2)
    }
    fun addSquareOf(hi: HugeInt) = addSquareOfImpl(hi.magia, Magia.nonZeroLimbLen(hi.magia))
    fun addSquareOf(mhi: MutHugeInt) {
        when {
            mhi.limbLen == 0 -> return
            mhi === this -> {
                if (tmp2.size < limbLen)
                    tmp2 = Magia.newWithMinLen(limbLen)
                System.arraycopy(magia, 0, tmp2, 0, limbLen)
                addSquareOfImpl(tmp2, limbLen)
            }
            else -> {
                addSquareOfImpl(mhi.magia, mhi.limbLen)
            }
        }
    }

    private fun mutateAddSubImpl(otherSign: Boolean, w: UInt) {
        validate()
        when {
            w == 0u -> {}
            this.sign == otherSign -> mutateAddMagImpl(w)
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

    private fun mutateAddMagImpl(w: UInt) {
        val carry = Magia.mutateAdd(magia, limbLen, w)
        if (carry == 0u)
            return
        val newLen = limbLen + 1
        if (newLen > magia.size)
            magia = Magia.newLongerCopyWithMinLen(magia, newLen)
        magia[limbLen] = carry.toInt()
        limbLen = newLen
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

    private fun addSquareOfImpl(y: IntArray, yLen: Int) {
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

    fun mutateSquare(): MutHugeInt {
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

    private fun mutateDivImpl(wSign: Boolean, w: UInt) {
        if (w == 0u)
            throw ArithmeticException("div by zero")
        if (limbLen > 0) {
            Magia.mutateDivMod(this.magia, limbLen, w)
            if (magia[limbLen - 1] == 0)
                --limbLen
            sign = (sign xor wSign) && (limbLen > 0)
        } else {
            // 0 / non-zero ... nada
        }
    }

    private fun mutateDivImpl(wSign: Boolean, dw: ULong) {
        if ((dw shr 32) == 0uL) {
            mutateDivImpl(wSign, dw.toUInt())
            return
        }
        if (limbLen == 0)
            return
        // FIXME
        mutateDivImpl(wSign, intArrayOf(dw.toInt(), (dw shr 32).toInt()), 2)
    }

    private fun mutateDivImpl(ySign: Boolean, y: IntArray, yLen: Int) {
        if (yLen >= 0 && yLen <= y.size) {
            if (yLen <= 1) {
                if (yLen == 0)
                    throw ArithmeticException("div by zero")
                mutateDivImpl(ySign, y[0].toUInt())
                return
            }
            check(y[yLen - 1] != 0)
            val m = limbLen
            val n = yLen
            if (m < n) {
                setZero()
                return
            }
            val normalizeShift = y[yLen - 1].countLeadingZeroBits()
            val vn = if (normalizeShift > 0) {
                if (tmp1.size < yLen)
                    tmp1 = Magia.newWithMinLen(yLen)
                System.arraycopy(y, 0, tmp1, 0, yLen)
                Magia.mutateShiftLeft(tmp1, yLen, normalizeShift)
                tmp1
            } else {
                y
            }
            if (tmp2.size < limbLen + 1)
                tmp2 = Magia.newWithMinLen(limbLen + 1)
            System.arraycopy(magia, 0, tmp2, 0, limbLen)
            tmp2[limbLen] = 0
            if (normalizeShift > 0)
                Magia.mutateShiftLeft(tmp2, limbLen + 1, normalizeShift)
            val un = tmp2
            val q = magia
            knuthDivideNormalizedCore(q, un, vn, m, n)
            limbLen = Magia.nonZeroLimbLen(magia, m - n + 1)
            sign = (sign xor ySign) and (limbLen > 0)
        } else {
            throw IllegalArgumentException()
        }

    }

    private fun mutateModImpl(w: UInt) {
        when {
            w == 0u -> throw ArithmeticException("div by zero")
            limbLen == 0 -> setZero()
            limbLen == 1 -> set(false, magia[0].toUInt() % w)
            else -> Magia.mutateDivMod(this.magia, limbLen, w)
        }
    }

    private fun mutateModImpl(dw: ULong) {
        when {
            (dw shr 32) == 0uL -> mutateModImpl(dw.toUInt())
            limbLen == 1 -> {} // nada
            limbLen == 2 -> {
                val dwT = (dw32(magia[1]) shl 32) or dw32(magia[0])
                set(false, dwT % dw)
            }
            else -> {
                // FIXME
                mutateModImpl(intArrayOf(dw.toInt(), (dw shr 32).toInt()), 2)
            }
        }
    }

    private fun mutateModImpl(y: IntArray, yLen: Int) {
        if (yLen >= 0 && yLen <= y.size) {
            if (yLen <= 1) {
                if (yLen == 0)
                    throw ArithmeticException("div by zero")
                mutateModImpl(y[0].toUInt())
                return
            }
            check(y[yLen - 1] != 0)
            val m = limbLen
            val n = yLen
            if (m < n) {
                set(false, y, yLen)
                return
            }
            val normalizeShift = y[yLen - 1].countLeadingZeroBits()
            val vn = if (normalizeShift > 0) {
                if (tmp1.size < yLen)
                    tmp1 = Magia.newWithMinLen(yLen)
                System.arraycopy(y, 0, tmp1, 0, yLen)
                Magia.mutateShiftLeft(tmp1, yLen, normalizeShift)
                tmp1
            } else {
                y
            }
            if (magia.size < limbLen + 1)
                    magia = Magia.newLongerCopyWithMinLen(magia, limbLen + 1)
            magia[limbLen] = 0
            if (normalizeShift > 0)
                Magia.mutateShiftLeft(magia, limbLen + 1, normalizeShift)
            val un = magia
            val q = null
            knuthDivideNormalizedCore(q, un, vn, m, n)
            // remainder is denormalized un .. which is magia1
            // but this is the remainder, so the value < vn
            // therefore, we only need to look at the bottom n==yLen limbs
            if (normalizeShift > 0)
                Magia.mutateShiftRight(magia, yLen, normalizeShift)
            limbLen = Magia.nonZeroLimbLen(magia, yLen)
            sign = sign and (limbLen > 0)
        } else {
            throw IllegalArgumentException()
        }
    }

    fun magnitudeBitLen() = Magia.bitLen(magia, limbLen)

    fun bitLengthBigIntegerStyle() = Magia.bitLengthBigIntegerStyle(sign, magia, limbLen)

    //fun toBinaryByteArray(isTwosComplement: Boolean, isBigEndian: Boolean): ByteArray =
    //    Magia.toBinaryByteArray(isTwosComplement, isBigEndian)

    fun toTwosComplementBigEndianByteArray(): ByteArray =
        toBinaryByteArray(isTwosComplement = true, isBigEndian = true)

    fun toBinaryByteArray(isTwosComplement: Boolean, isBigEndian: Boolean): ByteArray =
        Magia.toBinaryByteArray(sign, magia, limbLen, isTwosComplement, isBigEndian)

    override operator fun compareTo(other: MutHugeInt) =
        compareToHelper(other.sign, other.magia, other.limbLen)

    operator fun compareTo(hi: HugeInt) = compareToHelper(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))

    operator fun compareTo(n: Int) = compareToHelper(n < 0, n.absoluteValue.toUInt().toULong())
    operator fun compareTo(w: UInt) = compareToHelper(false, w.toULong())
    operator fun compareTo(l: Long) = compareToHelper(l < 0, l.absoluteValue.toULong())
    operator fun compareTo(dw: ULong) = compareToHelper(false, dw)

    private fun compareToHelper(otherSign: Boolean, otherMagia: IntArray, otherLimbLen: Int): Int {
        if (this.sign != otherSign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, this.limbLen, otherMagia, otherLimbLen)
        return if (this.sign) -cmp else cmp
    }

    private fun compareToHelper(ulSign: Boolean, ulMag: ULong): Int {
        if (this.sign != ulSign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, ulMag)
        return if (!ulSign) cmp else -cmp
    }

    fun magnitudeCompareTo(other: MutHugeInt) = Magia.compare(this.magia, other.magia)
    fun magnitudeCompareTo(hi: HugeInt) = Magia.compare(this.magia, hi.magia)
    fun magnitudeCompareTo(n: Int) = Magia.compare(this.magia, n.toUInt())
    fun magnitudeCompareTo(w: UInt) = Magia.compare(this.magia, w)
    fun magnitudeCompareTo(l: Long) = Magia.compare(this.magia, l.toULong())
    fun magnitudeCompareTo(dw: ULong) = Magia.compare(this.magia, dw)
    fun magnitudeCompareTo(littleEndianIntArray: IntArray) =
        Magia.compare(this.magia, littleEndianIntArray)

    infix fun EQ(other: MutHugeInt): Boolean = compareTo(other) == 0
    infix fun EQ(hi: HugeInt): Boolean = compareTo(hi) == 0
    infix fun EQ(n: Int): Boolean = compareTo(n) == 0
    infix fun EQ(w: UInt): Boolean = compareTo(w) == 0
    infix fun EQ(l: Long): Boolean = compareTo(l) == 0
    infix fun EQ(dw: ULong): Boolean = compareTo(dw) == 0

    infix fun NE(other: MutHugeInt): Boolean = compareTo(other) != 0
    infix fun NE(hi: HugeInt): Boolean = compareTo(hi) != 0
    infix fun NE(n: Int): Boolean = compareTo(n) != 0
    infix fun NE(w: UInt): Boolean = compareTo(w) != 0
    infix fun NE(l: Long): Boolean = compareTo(l) != 0
    infix fun NE(dw: ULong): Boolean = compareTo(dw) != 0

    override fun toString(): String = Magia.toString(this.sign, this.magia, this.limbLen)

}
