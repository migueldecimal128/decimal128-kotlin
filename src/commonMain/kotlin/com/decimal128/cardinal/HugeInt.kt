package com.decimal128.cardinal

import com.decimal128.decimal.NAN_DIV_BY_ZERO
import kotlin.math.absoluteValue

class HugeInt private constructor(val sign: Boolean, val car: IntArray) {


    companion object {
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
    }

    fun isZero() = this === ZERO
    fun isNotZero() = this !== ZERO
    fun isNegative() = sign
    fun signum() = if (sign) -1 else if (isZero()) 0 else 1
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

    override fun toString() = Car.toString(this.sign, this.car)

}