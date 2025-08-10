package com.decimal128.cardinal

import com.decimal128.decimal.NAN_DIV_BY_ZERO
import kotlin.math.absoluteValue

class HugeInt private constructor(val sign: Boolean, val car: IntArray) {


    companion object {
        // all zero values *must* point to this instance of ZERO
        val ZERO = HugeInt(false, Car.EMPTY_CAR)

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
            val car = Car.newFromString(sign, str)
            return if (car.isNotEmpty()) HugeInt(sign, Car.newFromString(sign, str)) else ZERO
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
                return if (signMask == 0) ZERO else HugeInt(true, intArrayOf(1))
            val car: IntArray = Car.newFromBigEndianBytes(signMask, bytes, ib, len-ib)
            if (signMask != 0) {
                var carry = 1L
                for (i in car.indices) {
                    val t = (car[i].inv().toLong() and 0xFFFF_FFFFL) + carry
                    car[i] = t.toInt()
                    carry = t ushr 32
                }
            }
            return HugeInt(signMask != 0, car)
        }
    }

    fun isZero() = this === ZERO
    fun isNotZero() = this !== ZERO
    fun isNegative() = sign
    fun signum() = if (sign) -1 else if (isZero()) 0 else 1
    fun magnitudeBitLen() = Car.bitLen(car)
    fun isMagnitudePowerOfTwo(): Boolean {
        var bitSeen = false
        for (w in this.car) {
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
        val bitLen = Car.bitLen(car)
        return bitLen <= 31 || sign && bitLen == 32 && car[0] == Int.MIN_VALUE
    }
    fun fitsUInt() = !sign && Car.bitLen(car) <= 32

    fun fitsLong(): Boolean {
        val bitLen = Car.bitLen(car)
        return bitLen <= 63 || sign && bitLen == 64 && car[0] == 0 && car[1] == Int.MIN_VALUE
    }
    fun fitsULong() = !sign && Car.bitLen(car) <= 64

    fun toInt(): Int {
        val bitLen = Car.bitLen(car)
        val magnitude = car[0]
        return if (!sign) {
            if (bitLen <= 31) magnitude else Int.MAX_VALUE
        } else {
            if (bitLen <= 31) -magnitude else Int.MIN_VALUE
        }
    }

    fun toUInt(): UInt {
        val bitLen = Car.bitLen(car)
        val magnitude = car[0]
        return if (!sign) {
            if (bitLen <= 32) magnitude.toUInt() else UInt.MAX_VALUE
        } else {
            0u
        }
    }

    fun toLong(): Long {
        val bitLen = Car.bitLen(car)
        val magnitude = when (car.size) {
            0 -> 0L
            1 -> car[0].toLong() and 0xFFFF_FFFFL
            else -> (car[1].toLong() shl 32) or (car[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 63) magnitude else Long.MAX_VALUE
        } else {
            if (bitLen <= 63) -magnitude else Long.MIN_VALUE
        }
    }

    fun toULong(): ULong {
        val bitLen = Car.bitLen(car)
        val magnitude = when (car.size) {
            0 -> 0L
            1 -> car[0].toLong() and 0xFFFF_FFFFL
            else -> (car[1].toLong() shl 32) or (car[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 64) magnitude.toULong() else ULong.MAX_VALUE
        } else {
            0uL
        }
    }

    // note that I am sharing the car with negate
    fun negate() = if (isNotZero()) HugeInt(!sign, car) else ZERO
    fun abs() = if (sign) HugeInt(false, car) else this

    private fun addSubImpl(isSub: Boolean, other: HugeInt): HugeInt {
        val otherSign = other.sign xor isSub
        if (this.sign == otherSign)
            return HugeInt(this.sign, Car.newAdd(this.car, other.car))
        val cmp = this.magnitudeCompareTo(other)
        if (cmp == 0)
            return ZERO
        return if (cmp < 0)
            HugeInt(otherSign, Car.newSub(other.car, this.car))
        else
            HugeInt(sign, Car.newSub(this.car, other.car))
    }

    private fun addSubImpl(isSub: Boolean, n: Int): HugeInt {
        val otherSign = (n < 0) xor isSub
        val otherMag = n.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toUInt())
    }

    private fun addSubImpl(isSub: Boolean, un: UInt): HugeInt {
        val otherSign = false
        if (this.sign == otherSign)
            return HugeInt(this.sign, Car.newAdd(this.car, un.toInt()))
        val cmp = this.magnitudeCompareTo(un)
        if (cmp == 0)
            return ZERO
        return if (cmp > 0) {
            HugeInt(sign, Car.newSub(this.car, un.toInt()))
        } else {
            HugeInt(otherSign, intArrayOf(un.toInt() - car[0]))
        }
    }

    private fun addSubImpl(isSub: Boolean, l: Long): HugeInt {
        val otherSign = (l < 0L) xor isSub
        val otherMag = l.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toULong())
    }

    private fun addSubImpl(isSub: Boolean, ul: ULong): HugeInt {
        val otherSign = false
        if (this.sign == otherSign)
            return HugeInt(this.sign, Car.newAdd(this.car, ul.toLong()))
        val cmp = this.magnitudeCompareTo(ul)
        if (cmp == 0)
            return ZERO
        return if (cmp > 0) {
            HugeInt(sign, Car.newSub(this.car, ul.toLong()))
        } else {
            val thisMag = this.toULong()
            val diff = ul - thisMag
            HugeInt(otherSign, Car.newFromLong(diff.toLong()))
        }
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
            HugeInt(this.sign xor other.sign, Car.newMul(this.car, other.car))
        else
            ZERO
    }
    operator fun times(n: Int): HugeInt {
        return if (isNotZero() && n != 0)
            HugeInt(this.sign xor (n < 0), Car.newMul(this.car, n.absoluteValue))
        else
            ZERO
    }
    operator fun times(un: UInt): HugeInt {
        return if (isNotZero() && un != 0u)
            ZERO
        else
            HugeInt(this.sign, Car.newMul(this.car, un.toInt()))
    }
    operator fun times(l: Long): HugeInt {
        return if (isNotZero() && l != 0L)
            HugeInt(this.sign xor (l < 0), Car.newMul(this.car, l.absoluteValue))
        else
            ZERO
    }
    operator fun times(ul: ULong): HugeInt {
        return if (isNotZero() && ul != 0uL)
            HugeInt(this.sign, Car.newMul(this.car, ul.toLong()))
        else
            ZERO
    }

    operator fun div(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Car.newDiv(this.car, other.car)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor other.sign, quot)
        }
        return ZERO
    }
    operator fun div(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Car.newDiv(this.car, n.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (n < 0), quot)
        }
        return ZERO
    }
    operator fun div(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Car.newDiv(this.car, un.toInt())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }
    operator fun div(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Car.newDiv(this.car, l.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (l < 0), quot)
        }
        return ZERO
    }
    operator fun div(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isZero()) {
            val quot = Car.newDiv(this.car, ul.toLong())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }

    operator fun rem(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Car.newMod(this.car, other.car)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Car.newMod(this.car, n.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Car.newMod(this.car, un.toInt())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Car.newMod(this.car, l.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }
    operator fun rem(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Car.newMod(this.car, ul.toLong())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    infix fun ushr(bitCount: Int): HugeInt {
        val car = Car.newShiftRight(this.car, bitCount)
        return if (car.isNotEmpty()) HugeInt(this.sign, car) else ZERO
    }

    infix fun shr(bitCount: Int): HugeInt {
        if (! sign)
            return ushr(bitCount)
        val roundDown = Car.testAnyBitInLowerN(this.car, bitCount)
        var car = Car.newShiftRight(this.car, bitCount)
        if (roundDown)
            car = Car.newOrMutateAdd(car, 1)
        return if (car.isNotEmpty()) HugeInt(this.sign, car) else ZERO
    }

    infix fun shl(bitCount: Int): HugeInt {
        return if (isNotZero())
            HugeInt(this.sign, Car.newShiftLeft(this.car, bitCount))
        else
            ZERO
    }

    fun magnitudeCompareTo(other: HugeInt): Int = Car.compare(this.car, other.car)
    fun magnitudeCompareTo(n: Int): Int = Car.compare(this.car, n)
    fun magnitudeCompareTo(un: UInt): Int = Car.compare(this.car, un.toInt())
    fun magnitudeCompareTo(l: Long): Int = Car.compare(this.car, l)
    fun magnitudeCompareTo(ul: ULong): Int = Car.compare(this.car, ul.toLong())

    fun compareTo(other: HugeInt): Int {
        if (this.sign != other.sign)
            return if (this.sign) -1 else 1
        val cmp = Car.compare(this.car, other.car)
        return if (this.sign) cmp else -cmp
    }

    fun compareTo(n: Int): Int {
        if (this.sign != (n < 0))
            return if (this.sign) -1 else 1
        val mag = if (n < 0) -n else n
        val cmp = Car.compare(this.car, mag)
        return if (this.sign) cmp else -cmp
    }

    fun compareTo(un: UInt): Int {
        if (this.sign)
            return -1
        return Car.compare(this.car, un.toInt())
    }

    fun compareTo(l: Long): Int {
        if (this.sign != (l < 0))
            return if (this.sign) -1 else 1
        val mag = if (l < 0) -l else l
        val cmp = Car.compare(this.car, mag)
        return if (this.sign) cmp else -cmp
    }

    fun compareTo(ul: ULong): Int {
        if (this.sign)
            return -1
        return Car.compare(this.car, ul.toLong())
    }

    override fun equals(other: Any?): Boolean {
        return ((other is HugeInt) &&
                (this.sign == other.sign) &&
                Car.EQ(this.car, other.car))
    }

    override fun toString() = Car.toString(this.sign, this.car)

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
        while (remaining > 0 && i < car.size) {
            var w = car[i] xor complementMask
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

}