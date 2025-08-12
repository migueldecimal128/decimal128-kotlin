package com.decimal128.hugeint

import kotlin.math.absoluteValue

class HugeInt private constructor(val sign: Boolean, val magia: IntArray) {


    companion object {
        // all zero values *must* point to this instance of ZERO
        val ZERO = HugeInt(false, Magia.ZERO)
        val ONE = HugeInt(false, Magia.ONE)
        val NEG_ONE = HugeInt(true, Magia.ONE) // share magia .. but no mutation allowed

        @Suppress("NOTHING_TO_INLINE")
        private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

        fun fromInt(n: Int): HugeInt = when {
            n > 0 -> HugeInt(false, intArrayOf(n))
            n < 0 -> HugeInt(true, intArrayOf(-n))
            else -> ZERO
        }
        fun fromUInt(un: UInt) = if (un != 0u) HugeInt(false, intArrayOf(un.toInt())) else ZERO
        fun fromLong(l: Long) = when {
            l > 0L -> HugeInt(false, intArrayOf(l.toInt(), (l ushr 32).toInt()))
            l < 0L -> HugeInt(true, intArrayOf(-l.toInt(), (-l ushr 32).toInt()))
            else -> ZERO
        }
        fun fromULong(ul: ULong) = if (ul != 0uL) HugeInt(true, intArrayOf(ul.toInt(), (ul shr 32).toInt())) else ZERO
        fun fromString(str: String): HugeInt {
            val sign = str.isNotEmpty() && str[0] == '-'
            val magia = Magia.newFromString(sign, str)
            return if (magia.isNotEmpty()) HugeInt(sign, Magia.newFromString(sign, str)) else ZERO
        }
        fun fromBigEndianBytes(bytes: ByteArray) = fromBigEndianBytes(bytes, bytes.size)
        fun fromBigEndianBytes(bytes: ByteArray, len: Int): HugeInt {
            if (len == 0 || len > bytes.size)
                return ZERO
            val signMask = bytes[0].toInt() shr 7 // 0 or 0xFFFF_FFFF
            var ib = 0
            while (ib < len && bytes[ib].toInt() == signMask)
                ++ib
            if (ib == len)
                return if (signMask == 0) ZERO else NEG_ONE
            val magia: IntArray = Magia.newFromBigEndianBytes(signMask, bytes, ib, len-ib)
            if (signMask != 0) {
                var carry = 1L
                for (i in magia.indices) {
                    val t = (magia[i].inv().toLong() and 0xFFFF_FFFFL) + carry
                    magia[i] = t.toInt()
                    carry = t ushr 32
                }
            }
            return HugeInt(signMask != 0, magia)
        }

        fun canonicalizeZero(sign: Boolean, magia: IntArray ): HugeInt {
            if (magia === Magia.ZERO)
                return ZERO
            check(Magia.nonZeroLimbLen(magia) > 0)
            return HugeInt(sign, magia)
        }

        fun withIndexedBitMask(bitIndex: Int, bitWidth: Int): HugeInt {
            if (bitIndex < 0 || bitWidth < 0)
                throw IllegalArgumentException("illegal negative arg bitIndex:$bitIndex bitCount:$bitWidth")
            if (bitWidth <= 1)
                return if (bitWidth == 0) ZERO else withSetBit(bitIndex)
            if (bitIndex == 0)
                return withBitMask(bitWidth)
            val bitLen = bitIndex + bitWidth
            val magia = Magia.newWithBitLen(bitLen)
            var wordIndex = bitIndex ushr 5
            val initialInnerIndex = bitIndex and 0x1F
            val initialBitCount = Math.min(bitWidth, 32 - initialInnerIndex)
            val initialMask = (((1L shl initialBitCount) - 1) shl initialInnerIndex).toInt()
            magia[wordIndex++] = initialMask
            var remainingBitCount = bitWidth - initialBitCount
            if (remainingBitCount > 0) {
                while (remainingBitCount > 32) {
                    magia[wordIndex++] = -1
                    remainingBitCount -= 32
                }
                if (remainingBitCount > 0)
                    magia[wordIndex] = (1 shl remainingBitCount) - 1
            }
            return HugeInt(false, magia)
        }

        fun withSetBit(bitIndex: Int): HugeInt {
            if (bitIndex < 0)
                throw IllegalArgumentException("negative bitIndex:$bitIndex")
            if (bitIndex == 0)
                return ONE
            val magia = Magia.newWithBitLen(bitIndex + 1)
            magia[magia.lastIndex] = 1 shl (bitIndex and 0x1F)
            return HugeInt(false, magia)
        }

        fun withBitMask(bitWidth: Int): HugeInt {
            if (bitWidth < 0)
                throw IllegalArgumentException("negative bitWidth:$bitWidth")
            if (bitWidth == 0)
                return ZERO
            val magia = Magia.newWithBitLen(bitWidth)
            magia.fill(-1)
            val leadingZeroCount = (magia.size * 32) - bitWidth
            magia[magia.lastIndex] = -1 ushr leadingZeroCount
            return HugeInt(false, magia)
        }
    }

    fun isZero() = this === ZERO
    fun isNotZero() = this !== ZERO
    fun isNegative() = sign
    fun signum() = if (sign) -1 else if (isZero()) 0 else 1
    fun magnitudeBitLen() = Magia.bitLen(magia)
    fun magnitudeIntArrayLen() = (Magia.bitLen(magia) + 31) ushr 5
    fun magnitudeLongArrayLen() = (Magia.bitLen(magia) + 63) ushr 6
    fun isMagnitudePowerOfTwo(): Boolean {
        var bitSeen = false
        for (w in this.magia) {
            if (w == 0)
                continue
            if ((w and (w - 1)) != 0)
                return false
            if (bitSeen)
                return false
            bitSeen = true
        }
        return bitSeen
    }

    fun testBit(bitIndex: Int): Boolean = Magia.testBit(this.magia, bitIndex)

    fun bitLengthBigIntegerStyle(): Int {
        val magBitLen = magnitudeBitLen()
        val isNegPowerOfTwo = sign && isMagnitudePowerOfTwo()
        val bitLengthBigIntegerStyle = magBitLen - if (isNegPowerOfTwo) 1 else 0
        return bitLengthBigIntegerStyle
    }

    fun calc2sComplementByteLength(): Int {
        if (isZero())
            return 1
        val bitLen2sComplement = bitLengthBigIntegerStyle() + 1
        val byteLength = (bitLen2sComplement + 7) ushr 3
        return byteLength
    }

    fun fitsInt(): Boolean {
        val bitLen = Magia.bitLen(magia)
        return bitLen <= 31 || sign && bitLen == 32 && magia[0] == Int.MIN_VALUE
    }
    fun fitsUInt() = !sign && Magia.bitLen(magia) <= 32

    fun fitsLong(): Boolean {
        val bitLen = Magia.bitLen(magia)
        return bitLen <= 63 || sign && bitLen == 64 && magia[0] == 0 && magia[1] == Int.MIN_VALUE
    }
    fun fitsULong() = !sign && Magia.bitLen(magia) <= 64

    fun toIntRaw() = if (magia.size != 0) magia[0] else 0
    fun toUIntRaw() = toIntRaw().toUInt()
    fun toLongRaw() = when {
        magia.size == 0 -> 0L
        magia.size == 1 -> U32(magia[0])
        else -> (U32(magia[1]) shl 32) or (U32(magia[0]))
    }
    fun toULongRaw() = toLongRaw().toULong()


    fun toInt(): Int {
        val bitLen = Magia.bitLen(magia)
        val magnitude = magia[0]
        return if (!sign) {
            if (bitLen <= 31) magnitude else Int.MAX_VALUE
        } else {
            if (bitLen <= 31) -magnitude else Int.MIN_VALUE
        }
    }

    fun toUInt(): UInt {
        val bitLen = Magia.bitLen(magia)
        val magnitude = magia[0]
        return if (!sign) {
            if (bitLen <= 32) magnitude.toUInt() else UInt.MAX_VALUE
        } else {
            0u
        }
    }

    fun toLong(): Long {
        val bitLen = Magia.bitLen(magia)
        val magnitude = when (magia.size) {
            0 -> 0L
            1 -> magia[0].toLong() and 0xFFFF_FFFFL
            else -> (magia[1].toLong() shl 32) or (magia[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 63) magnitude else Long.MAX_VALUE
        } else {
            if (bitLen <= 63) -magnitude else Long.MIN_VALUE
        }
    }

    fun toULong(): ULong {
        val bitLen = Magia.bitLen(magia)
        val magnitude = when (magia.size) {
            0 -> 0L
            1 -> magia[0].toLong() and 0xFFFF_FFFFL
            else -> (magia[1].toLong() shl 32) or (magia[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 64) magnitude.toULong() else ULong.MAX_VALUE
        } else {
            0uL
        }
    }

    // note that I am sharing the magia with negate and abs
    // but *no* mutation of magia is allowed
    fun negate() = if (isNotZero()) HugeInt(!sign, magia) else ZERO
    fun abs() = if (sign) HugeInt(false, magia) else this

    private fun addSubImpl(isSub: Boolean, other: HugeInt): HugeInt {
        if (other === ZERO)
            return this
        if (this === ZERO)
            return if (isSub) other.negate() else other
        val otherSign = isSub xor other.sign
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, other.magia))
        val cmp = this.magnitudeCompareTo(other)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, other.magia))
            cmp < 0 -> HugeInt(otherSign, Magia.newSub(other.magia, this.magia))
            else -> ZERO
        }
        return ret
    }

    private fun addSubImpl(isSub: Boolean, n: Int): HugeInt {
        val otherSign = n < 0
        val otherMag = n.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toUInt())
    }

    private fun addSubImpl(isSub: Boolean, un: UInt): HugeInt {
        if (un == 0u)
            return this
        if (isZero()) {
            val magia = intArrayOf(un.toInt())
            return HugeInt(isSub, magia)
        }
        val otherSign = isSub
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, un.toInt()))
        val cmp = this.magnitudeCompareTo(un)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, un.toInt()))
            cmp < 0 -> HugeInt(otherSign, intArrayOf(un.toInt() - this.magia[0]))
            else -> ZERO
        }
        return ret
    }

    private fun addSubImpl(isSub: Boolean, l: Long): HugeInt {
        val otherSign = l < 0L
        val otherMag = l.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toULong())
    }

    private fun addSubImpl(isSub: Boolean, ul: ULong): HugeInt {
        val hi = ul shr 32
        if (hi == 0uL)
            return addSubImpl(isSub, hi.toUInt())
        val otherSign = isSub
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, ul.toLong()))
        val cmp = this.magnitudeCompareTo(ul)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, ul.toLong()))
            cmp < 0 -> {
                val thisMag = this.toULong()
                val diff = ul - thisMag
                HugeInt(otherSign, Magia.newFromLong(diff.toLong()))
            }
            else -> ZERO
        }
        return ret
    }

    operator fun plus(other: HugeInt): HugeInt = this.addSubImpl(false, other)
    operator fun plus(n: Int): HugeInt = this.addSubImpl(false, n)
    operator fun plus(un: UInt): HugeInt = this.addSubImpl(false, un)
    operator fun plus(l: Long): HugeInt = this.addSubImpl(false, l)
    operator fun plus(ul: ULong): HugeInt = this.addSubImpl(false, ul)

    operator fun minus(other: HugeInt): HugeInt = this.addSubImpl(true, other)
    operator fun minus(n: Int): HugeInt = this.addSubImpl(true, n)
    operator fun minus(un: UInt): HugeInt = this.addSubImpl(true, un)
    operator fun minus(l: Long): HugeInt = this.addSubImpl(true, l)
    operator fun minus(ul: ULong): HugeInt = this.addSubImpl(true, ul)

    operator fun times(other: HugeInt): HugeInt {
        return if (isNotZero() && other.isNotZero())
            HugeInt(this.sign xor other.sign, Magia.newMul(this.magia, other.magia))
        else
            ZERO
    }
    operator fun times(n: Int): HugeInt {
        return if (isNotZero() && n != 0)
            HugeInt(this.sign xor (n < 0), Magia.newMul(this.magia, n.absoluteValue))
        else
            ZERO
    }
    operator fun times(un: UInt): HugeInt {
        return if (isNotZero() && un != 0u)
            ZERO
        else
            HugeInt(this.sign, Magia.newMul(this.magia, un.toInt()))
    }
    operator fun times(l: Long): HugeInt {
        return if (isNotZero() && l != 0L)
            HugeInt(this.sign xor (l < 0), Magia.newMul(this.magia, l.absoluteValue))
        else
            ZERO
    }
    operator fun times(ul: ULong): HugeInt {
        return if (isNotZero() && ul != 0uL)
            HugeInt(this.sign, Magia.newMul(this.magia, ul.toLong()))
        else
            ZERO
    }

    operator fun div(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, other.magia)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor other.sign, quot)
        }
        return ZERO
    }
    operator fun div(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, n.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (n < 0), quot)
        }
        return ZERO
    }
    operator fun div(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, un.toInt())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }
    operator fun div(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, l.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (l < 0), quot)
        }
        return ZERO
    }
    operator fun div(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isZero()) {
            val quot = Magia.newDiv(this.magia, ul.toLong())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }

    operator fun rem(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, other.magia)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, n.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, un.toInt())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, l.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, ul.toLong())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    fun divMod(other: HugeInt): Pair<HugeInt, HugeInt> {
        return when {
            other.isZero() -> throw ArithmeticException("div by zero")
            this.isNotZero() -> divModHelper(other.sign, other.magia)
            else -> ZERO to other
        }
    }

    private fun divModHelper(otherSign: Boolean, otherMagia: IntArray): Pair<HugeInt, HugeInt> {
        val (magiaQuot, magiaRem) = Magia.newDivMod(this.magia, otherMagia)
        val hiQuot = if (magiaQuot.isNotEmpty()) HugeInt(this.sign xor otherSign, magiaQuot) else ZERO
        val hiRem = if (magiaRem.isNotEmpty()) HugeInt(this.sign, magiaRem) else ZERO
        return hiQuot to hiRem
    }

    fun divMod(n: Int): Pair<HugeInt, HugeInt> = divModIntHelper(n < 0, n.absoluteValue)
    fun divMod(un: UInt): Pair<HugeInt, HugeInt> = divModIntHelper(false, un.toInt())

    private fun divModIntHelper(nSign: Boolean, nMag: Int): Pair<HugeInt, HugeInt> {
        return when {
            nMag == 0 -> throw ArithmeticException("div by zero")
            this.isNotZero() -> {
                val quot = Magia.newMinimumCopy(this.magia)
                val remN = Magia.mutateDivideRemainder(quot, nMag).toInt()
                val hiQuot = if (Magia.nonZeroLimbLen(quot) > 0) HugeInt(this.sign xor nSign, quot) else ZERO
                val hiRem = if (remN != 0) HugeInt(this.sign, intArrayOf(remN)) else ZERO
                return hiQuot to hiRem
            }
            else -> ZERO to HugeInt(nSign, intArrayOf(nMag))
        }
    }

    fun divMod(l: Long): Pair<HugeInt, HugeInt> = divModLongHelper(l < 0, l.absoluteValue)
    fun divMod(ul: ULong): Pair<HugeInt, HugeInt> = divModLongHelper(false, ul.toLong())

    private fun divModLongHelper(lSign: Boolean, lMag: Long): Pair<HugeInt, HugeInt> {
        val lo = lMag.toInt()
        val hi = (lMag ushr 32).toInt()
        return when {
            lMag == 0L -> throw ArithmeticException("div by zero")
            hi == 0 -> divModIntHelper(lSign, lo)
            else -> divModHelper(lSign, intArrayOf(lo, hi))
        }
    }

    fun sqr(): HugeInt {
        if (this.isNotZero()) {
            check(Magia.nonZeroLimbLen(this.magia) > 0)
            val magiaSqr = Magia.newSqr(this.magia)
            return HugeInt(false, magiaSqr)
        }
        return ZERO
    }

    fun pow(n: Int): HugeInt {
        return when {
            n < 0 -> throw ArithmeticException("cannot raise HugeInt to negative power:$n")
            n == 0 -> ONE
            this.isZero() -> ZERO
            n == 1 -> this
            n == 2 -> sqr()
            else -> {
                val resultSign = this.sign && ((n and 1) != 0)
                var baseMag = this.magia
                var resultMag = Magia.ONE
                var exp = n
                while (true) {
                    if ((exp and 1) != 0)
                        resultMag = Magia.newMul(resultMag, baseMag)
                    exp = exp ushr 1
                    if (exp == 0)
                        break
                    baseMag = Magia.newSqr(baseMag)
                }
                HugeInt(resultSign, resultMag)
            }
        }
    }



    infix fun and(other: HugeInt): HugeInt {
        val magiaAnd = Magia.newAnd(this.magia, other.magia)
        return if (magiaAnd.isNotEmpty()) HugeInt(false, magiaAnd) else ZERO
    }

    infix fun or(other: HugeInt): HugeInt {
        val magiaOr = Magia.newOr(this.magia, other.magia)
        return if (magiaOr.isNotEmpty()) HugeInt(false, magiaOr) else ZERO
    }

    infix fun xor(other: HugeInt): HugeInt {
        val magiaXor = Magia.newXor(this.magia, other.magia)
        return if (magiaXor.isNotEmpty()) HugeInt(false, magiaXor) else ZERO
    }


    infix fun ushr(bitCount: Int): HugeInt {
        if (bitCount == 0)
            return this
        val magia = Magia.newShiftRight(this.magia, bitCount)
        return if (magia.isNotEmpty()) HugeInt(this.sign, magia) else ZERO
    }

    infix fun shr(bitCount: Int): HugeInt {
        if (! sign)
            return ushr(bitCount)
        val roundDown = Magia.testAnyBitInLowerN(this.magia, bitCount)
        var magia = Magia.newShiftRight(this.magia, bitCount)
        if (roundDown)
            magia = Magia.newOrMutateAdd(magia, 1)
        return if (magia.isNotEmpty()) HugeInt(this.sign, magia) else ZERO
    }

    infix fun shl(bitCount: Int): HugeInt {
        return if (isNotZero())
            HugeInt(this.sign, Magia.newShiftLeft(this.magia, bitCount))
        else
            ZERO
    }

    fun magnitudeCompareTo(other: HugeInt): Int = Magia.compare(this.magia, other.magia)
    fun magnitudeCompareTo(n: Int): Int = Magia.compare(this.magia, n)
    fun magnitudeCompareTo(un: UInt): Int = Magia.compare(this.magia, un.toInt())
    fun magnitudeCompareTo(l: Long): Int = Magia.compare(this.magia, l)
    fun magnitudeCompareTo(ul: ULong): Int = Magia.compare(this.magia, ul.toLong())

    operator fun compareTo(other: HugeInt): Int {
        if (this.sign != other.sign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, other.magia)
        return if (this.sign) cmp else -cmp
    }

    operator fun compareTo(n: Int) = compareToHelper(n < 0, n.absoluteValue.toUInt().toULong())
    operator fun compareTo(un: UInt) = compareToHelper(false, un.toULong())
    operator fun compareTo(l: Long) = compareToHelper(l < 0, l.absoluteValue.toULong())
    operator fun compareTo(ul: ULong) = compareToHelper(false, ul)

    private fun compareToHelper(ulSign: Boolean, ulMag: ULong): Int {
        if (this.sign != ulSign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, ulMag.toLong())
        return if (! ulSign) cmp else -cmp
    }

    infix fun EQ(other: HugeInt): Boolean = (this.sign == other.sign) && Magia.EQ(this.magia, other.magia)
    infix fun EQ(n: Int): Boolean = compareTo(n) == 0
    infix fun EQ(un: UInt): Boolean = compareTo(un) == 0
    infix fun EQ(l: Long): Boolean = compareTo(l) == 0
    infix fun EQ(ul: ULong): Boolean = compareTo(ul) == 0

    infix fun NE(other: HugeInt): Boolean = (this.sign != other.sign) || !Magia.EQ(this.magia, other.magia)
    infix fun NE(n: Int): Boolean = compareTo(n) != 0
    infix fun NE(un: UInt): Boolean = compareTo(un) != 0
    infix fun NE(l: Long): Boolean = compareTo(l) != 0
    infix fun NE(ul: ULong): Boolean = compareTo(ul) != 0



    override fun equals(other: Any?): Boolean {
        return ((other is HugeInt) &&
                (this.sign == other.sign) &&
                Magia.EQ(this.magia, other.magia))
    }

    override fun toString() = Magia.toString(this.sign, this.magia)

    fun toBigEndianByteArray(): ByteArray {
        val byteLen = calc2sComplementByteLength()
        val bytes = ByteArray(byteLen)
        val magBitLen = magnitudeBitLen()
        val magByteLen = (magBitLen + 7) ushr 3
        writeBigEndianBytes(bytes, byteLen - magByteLen, magByteLen)
        return bytes
    }

    fun toBigEndianByteArray(bytes: ByteArray): Int {
        val byteLen = calc2sComplementByteLength()
        if (bytes.size < byteLen)
            throw IllegalArgumentException("ByteArray too small")
        val magBitLen = magnitudeBitLen()
        val magByteLen = (magBitLen + 7) ushr 3
        writeBigEndianBytes(bytes, byteLen - magByteLen, magByteLen)
        return byteLen
    }

    private fun writeBigEndianBytes(bytes: ByteArray, offset: Int, magByteLen: Int) {
        val last = offset + magByteLen - 1
        var dest = last
        var remaining = magByteLen
        var i = 0
        val complementMask = if (sign) -1 else 0
        bytes[0] = complementMask.toByte()
        while (remaining > 0 && i < magia.size) {
            var w = magia[i] xor complementMask
            var j = 4
            do {
                bytes[dest--] = w.toByte()
                w = w ushr 8
                --remaining
                --j
            } while (remaining > 0 && j > 0)
            ++i
        }
        if (sign) {
            var carry = 1
            var k = last
            while (carry > 0 && k >= 0) {
                val t = (bytes[k].toInt() and 0xFF) + carry
                bytes[k] = t.toByte()
                carry = t ushr 8
                --k
            }
        }
    }

    fun toLittleEndianIntArray(): IntArray {
        if (sign)
            throw RuntimeException("not implemented for negative value")
        return Magia.newMinimumCopy(magia)
    }

    fun toLittleEndianLongArray(): LongArray {
        if (sign)
            throw RuntimeException("not implemented for negative value")
        val intLen = Magia.nonZeroLimbLen(magia)
        val longLen = (intLen + 1) ushr 1
        val z = LongArray(longLen)
        var iw = 0
        var il = 0
        while (intLen - iw >= 2) {
            val lo = U32(magia[iw])
            val hi = magia[iw + 1].toLong() shl 32
            val l = hi or lo
            z[il] = l
            ++il
            iw += 2
        }
        if (iw < intLen)
            z[il] = U32(magia[iw])
        return z
    }

}