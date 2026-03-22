@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE

expect open class C256Rep() {
    internal var steal: Int
    internal var dw0: Long
    internal var dw1: Long
    internal var dw2: Long
    internal var dw3: Long

}

open class C256() :
C256Rep() {

    internal var bitLen: Int
        get() = stealBitLen(steal)
        set(value) {
            verify { value in 0..253 }
            this.steal = stealWithBitLen(steal, value)
        }

    internal var digitLen: Int
        get() = stealDigitLen(steal)
        set(value) {
            verify { value in 0..76 || value == 111 }
            this.steal = stealWithDigitLen(steal, value)
        }

    internal fun setPackedLengths(packedLengths: Int) {
        this.steal = stealWithPackedLengths(steal, packedLengths)
    }

    internal fun setDigitLenBitLen(digitLen: Int, bitLen: Int) {
        verify { digitLen in 0..76 }
        verify { bitLen in 0..253 }
        this.steal = stealWithDigitLenBitLen(steal, digitLen, bitLen)
    }

    companion object {
        internal operator fun invoke(dw0: Long): C256 = C256().c256Set64(dw0)
        internal operator fun invoke(dw3: Long, dw2: Long, dw1: Long, dw0: Long): C256 =
            C256().c256Set256(dw3, dw2, dw1, dw0)
        internal operator fun invoke(str: String): C256 = C256().c256Set(str)

        internal operator fun invoke(c: C256): C256 = C256().c256Set(c)
    }

    fun c256SetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L;
        steal = steal and STEAL_PACKED_LENGTHS_MASK.inv()
    }

    fun c256SetOne() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 1L;
        steal = stealWithPackedLengths(steal, stealPackLengths(1, 1))
    }

    internal inline fun c256IsZero() = steal and STEAL_PACKED_LENGTHS_MASK == 0

    fun updateDigitLenBitLen() {
        val bitLen = calcBitLen256(dw3, dw2, dw1, dw0)
        val digitLen = calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
        updateDigitLenBitLen(digitLen, bitLen)
    }

    fun updateLengthsAfterIncrement() {
        val oldSteal = steal
        val dw0 = dw0
        val dw1 = dw1
        val dw2 = dw2
        val dw3 = dw3
        verify { (dw0 or dw1 or dw2 or dw3) != 0L }
        val popCount = dw0.countOneBits() + dw1.countOneBits() + dw2.countOneBits() + dw3.countOneBits()
        val bitLenIncrement = 1 - ((1 - popCount) ushr 31)
        val newBitLen = stealBitLen(oldSteal) + bitLenIncrement

        if (newBitLen <= 128) {
            val oldDigitLen = stealDigitLen(oldSteal)
            val offset = (oldDigitLen shl 1) and POW10_BCE
            val matchIfZero = (dw0 - POW10[offset]) or (dw1 - POW10[offset + 1])
            val digitLenIncrement = 1 - ((matchIfZero or -matchIfZero) ushr 63).toInt()
            if ((bitLenIncrement or digitLenIncrement) == 0)
                return
            val newDigitLen = oldDigitLen + digitLenIncrement
            steal = stealWithDigitLenBitLen(oldSteal, newDigitLen, newBitLen)
            verify { bitLen == calcBitLen128(dw1, dw0) && digitLen == calcDigitLen128(bitLen, dw1, dw0) }
        } else {
            calcDigitLen256(this.bitLen, dw3, dw2, dw1, dw0)
        }
    }

    internal inline fun updateDigitLenBitLen(digitLen: Int, bitLen: Int) {
        this.steal = stealWithDigitLenBitLen(steal, digitLen, bitLen)
    }

    internal fun c256HasValidLengths(): Boolean {
        if (bitLen != calcBitLen256(dw3, dw2, dw1, dw0))
            return false
        if (digitLen != calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        return true
    }


    internal inline operator fun get(index: Int): Long {
        return when (index) {
            0 -> dw0
            1 -> dw1
            2 -> dw2
            3 -> dw3
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    internal inline operator fun set(index: Int, value: Long) {
        verify { digitLen == 111 }
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    internal inline fun c256EnableIndexSetAndZeroOut() {
        digitLen = 111
        dw0 = 0L; dw1 = 0L; dw2 = 0L; dw3 = 0L
    }

    internal inline fun c256DisableIndexSetAndUpdateLengths() {
        //check(packedLengths.toInt() == -1)
        verify { digitLen == 111 }
        updateDigitLenBitLen()
    }

    internal /*inline*/ fun c256Set64(d0: Long): C256 {
        dw3 = 0L; dw2 = 0L; dw1 = 0L
        dw0 = d0
        setPackedLengths(calcStealPackedLengths64(d0))
        return this
    }

    internal /*inline*/ fun c256Set128(d1: Long, d0: Long): C256 {
        dw3 = 0L; dw2 = 0L
        dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths128(d1, d0))
        return this
    }

    internal /*inline*/ fun c256Set128(pentad: Pentad): C256 {
        val d1 = pentad.dw1; val d0 = pentad.dw0
        dw3 = 0L; dw2 = 0L
        dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths128(d1, d0))
        return this
    }

    internal /*inline*/ fun c256Set192(d2: Long, d1: Long, d0: Long): C256 {
        dw3 = 0L
        dw2 = d2; dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths256(0L, d2, d1, d0))
        return this
    }

    internal /*inline*/ fun c256Set192(pentad: Pentad): C256 {
        val d2 = pentad.dw2; val d1 = pentad.dw1; val d0 = pentad.dw0
        dw3 = 0L
        dw2 = d2; dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths256(0L, d2, d1, d0))
        return this
    }

    internal /*inline*/ fun c256Set256(d3: Long, d2: Long, d1: Long, d0: Long): C256 {
        dw3 = d3; dw2 = d2; dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths256(d3, d2, d1, d0))
        return this
    }

    fun c256Set(x: C256): C256 {
        val d3 = x.dw3; val d2 = x.dw2; val d1 = x.dw1; val d0 = x.dw0
        dw3 = d3; dw2 = d2; dw1 = d1; dw0 = d0
        setPackedLengths(calcStealPackedLengths256(d3, d2, d1, d0))
        return this
    }


    internal inline fun c256Set(str: String): C256 {
        IntegerParsePrint.u256FromString(this, false, str)
        return this
    }

    internal inline fun c256SetShiftRight(x: C256, bitShift: Int) = c256SetShiftRight(this, x, bitShift)

    internal inline fun c256SetShiftLeft(x: C256, bitShift: Int) = c256SetShiftLeftOr(this, x, bitShift, 0L)

    internal inline fun c256MutateShiftLeft(bitShift: Int) = c256SetShiftLeftOr(this, this, bitShift, 0L)

    internal inline fun c256MutateShiftLeftOr(bitShift: Int, d0: Long) = c256SetShiftLeftOr(this, this, bitShift, d0)

    internal inline fun c256Set(x: LongArray, xOff: Int, xLen: Int) = c256Set(this, x, xOff, xLen)

    internal inline fun c256ToFloorDouble() = c256ToFloorDouble(this)

    internal inline fun c256Set(d: Double) = c256Set(this, d)

    internal inline fun c256ToNewDoubleDouble() = c256ToNewDoubleDouble(this)

    internal inline fun c256Set(dd: DoubleDouble) = c256Set(this, dd, DecContext.current().tmps.pentad1)

    internal inline fun c256MutateIncrement(doRoundUp: Boolean) {
        if (doRoundUp)
            c256MutateIncrement(this)
    }

    internal inline fun c256MutateIncrement() = c256MutateIncrement(this)

    internal inline fun c256MutateDecrement() = c256MutateDecrement(this)

    internal inline fun c256NumberTrailingZeros() = c256Ctz(this)

    internal inline fun c256DwordAtBitIndex(bitIndex: Int) = getDwordAtBitIndex(this, bitIndex)

    open fun toHexString() = IntegerParsePrint.int256ToHexString(false, this)
    //override fun toString() = coeffToBigInteger().toString()
    override fun toString() =
        if (digitLen == 111)
            "MutDec \uD83D\uDEA7 under construction \uD83D\uDEA7"
        else
            IntegerParsePrint.int256ToString(false, this)
    fun u256ToUtf8(bytes: ByteArray, off: Int) = IntegerParsePrint.int256ToUtf8(false, this, bytes, off)

    override fun equals(other: Any?) = other is C256 && c256UnscaledEQ(this, other)

}