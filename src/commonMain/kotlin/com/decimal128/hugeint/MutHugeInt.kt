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
    constructor() : this(false, Magia.ZERO, 0, Magia.ZERO, Magia.ZERO)

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
        val newMagia = Magia.newCopyWithMinLen(magia, limbLen, limbLen)
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

    fun set(n: Int): MutHugeInt = set(n < 0, n.absoluteValue.toUInt())

    fun set(w: UInt): MutHugeInt = set(false, w)

    fun set(sign: Boolean, w: UInt): MutHugeInt {
        this.sign = sign
        limbLen = if (w == 0u) 0 else 1
        if (magia.size == 0)
            magia = IntArray(4)
        magia[0] = w.toInt()
        return this
    }

    fun set(l: Long): MutHugeInt = set(l < 0, l.absoluteValue.toULong())

    fun set(dw: ULong): MutHugeInt = set(false, dw)

    fun set(sign: Boolean, dw: ULong): MutHugeInt {
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

    fun set(str: String): MutHugeInt {
        this.magia = Magia.from(str)
        this.limbLen = Magia.nonZeroLimbLen(magia)
        this.sign = str[0] == '-'
        return this
    }

    private fun set(ySign: Boolean, y: IntArray, yLen: Int): MutHugeInt {
        if (magia.size < yLen)
            magia = Magia.newWithMinLen(yLen)
        sign = ySign
        limbLen = yLen
        System.arraycopy(y, 0, magia, 0, yLen)
        return this
    }

    fun toRawULong(): ULong {
        return when {
            limbLen == 1 -> dw32(magia[0])
            limbLen >= 2 -> (dw32(magia[1]) shl 32) or dw32(magia[0])
            else -> 0uL
        }
    }

    fun setSquare(n: Int): MutHugeInt = setSquare(n.absoluteValue.toUInt())
    fun setSquare(w: UInt): MutHugeInt = set(w.toULong() * w.toULong())
    fun setSquare(l: Long): MutHugeInt = setSquare(l.absoluteValue.toULong())
    fun setSquare(dw: ULong): MutHugeInt {
        val hi = unsignedMulHi(dw, dw)
        val lo = dw * dw
        magia[0] = lo.toInt()
        magia[1] = (lo shr 32).toInt()
        magia[2] = hi.toInt()
        magia[3] = (hi shr 32).toInt()
        normalizeLen1(4)
        sign = false
        return this
    }
    fun setSquare(hi: HugeInt): MutHugeInt = setSquareImpl(hi.magia, hi.magia.size)
    fun setSquare(mhi: MutHugeInt): MutHugeInt {
        return if (mhi == this)
            mutateSquare() // prevent aliasing problems & improve performance
        else
            setSquareImpl(mhi.magia, mhi.magia.size)
    }

    private fun setSquareImpl(x: IntArray, xLen: Int): MutHugeInt {
        val pLen = xLen + xLen
        if (pLen > magia.size)
            magia = Magia.newWithMinLen(pLen)
        else
            magia.fill(0, 0, pLen)
        Magia.sqr(magia, x, xLen)
        normalizeLen1(pLen)
        sign = false
        return this
    }

    fun setRandom(bitLen: Int, random: Random = Random.Default): MutHugeInt {
        if (bitLen >= 0) {
            val bitLimbLen = limbLenFromBitLen(bitLen)
            var zeroTest = 0
            if (magia.size < bitLimbLen)
                magia = Magia.newWithMinLen(bitLimbLen)
            var mask = (if ((bitLen and 0x1F) == 0) 0 else 1 shl (bitLen and 0x1F)) - 1
            for (i in bitLimbLen - 1 downTo 0) {
                val rand = random.nextInt() and mask
                magia[i] = rand
                zeroTest = zeroTest or rand
                mask = -1
            }
            this.limbLen = Magia.nonZeroLimbLen(magia, bitLimbLen)
            validate()
            return this
        } else {
            throw IllegalArgumentException()
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
            mutateMulImpl(mhi.sign, mhi.magia, mhi.magia.size)
    }

    operator fun divAssign(n: Int) = mutateDivImpl(n < 0, n.absoluteValue.toUInt())
    operator fun divAssign(w: UInt) = mutateDivImpl(false, w)
    operator fun divAssign(l: Long) = mutateDivImpl(l < 0, l.absoluteValue.toULong())
    operator fun divAssign(dw: ULong) = mutateDivImpl(false, dw)
    operator fun divAssign(hi: HugeInt) =
        mutateDivImpl(hi.sign, hi.magia, Magia.nonZeroLimbLen(hi.magia))
    operator fun divAssign(mhi: MutHugeInt) = mutateDivImpl(mhi.sign, mhi.magia, mhi.magia.size)

    operator fun remAssign(n: Int) = mutateModImpl(n.absoluteValue.toUInt())
    operator fun remAssign(w: UInt) = mutateModImpl(w)
    operator fun remAssign(l: Long) = mutateModImpl(l.absoluteValue.toULong())
    operator fun remAssign(dw: ULong) = mutateModImpl(dw)
    operator fun remAssign(hi: HugeInt) =
        mutateModImpl(hi.magia, Magia.nonZeroLimbLen(hi.magia))
    operator fun remAssign(mhi: MutHugeInt) = mutateModImpl(mhi.magia, mhi.magia.size)

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

    private fun mutateAddMagImpl(w: UInt) {
        val carry = Magia.mutateAdd(magia, limbLen, w)
        if (carry == 0u)
            return
        val newLen = limbLen + 1
        if (newLen > magia.size)
            resize1(newLen)
        magia[limbLen] = carry.toInt()
        limbLen = newLen
    }

    private fun resize1(newLimbCount: Int) {
        magia = Magia.newLongerCopyWithMinLen(magia, newLimbCount)
    }

    private fun realloc2(newLimbCount: Int) {
        tmp1 = Magia.newWithMinLen(newLimbCount)
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
            // nada
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
        if (w == 0u)
            throw ArithmeticException("div by zero")
        val rem =
            if (limbLen > 0)
                Magia.mutateDivMod(this.magia, limbLen, w)
            else
                w
        set(sign, rem)
    }

    private fun mutateModImpl(dw: ULong) {
        if ((dw shr 32) == 0uL) {
            mutateModImpl(dw.toUInt())
            return
        }
        if (limbLen == 0)
            return
        // FIXME
        mutateModImpl(intArrayOf(dw.toInt(), (dw shr 32).toInt()), 2)
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
