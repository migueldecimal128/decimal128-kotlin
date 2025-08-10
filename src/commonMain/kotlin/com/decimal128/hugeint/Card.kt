package com.decimal128.hugeint

class Card private constructor(val car: IntArray) {

    companion object {
        fun fromInt(n: Int) = Card(intArrayOf(n))
        fun fromUInt(un: UInt) = Card(intArrayOf(un.toInt()))
        fun fromLong(l: Long) = Card(intArrayOf(l.toInt(), (l shr 32).toInt()))
        fun fromULong(ul: ULong) = Card(intArrayOf(ul.toInt(), (ul shr 32).toInt()))
        fun fromString(str: String) = Card(Car.newFromString(str))
    }

    fun isZero() = Car.isZero(car)

    fun toInt() = Car.toInt(car)
    fun toUInt() = Car.toInt(car).toUInt()
    fun toLong() = Car.toLong(car)
    fun toULong() = Car.toLong(car).toULong()

    operator fun plus(other: Card): Card = Card(Car.newAdd(this.car, other.car))
    operator fun plus(n: Int): Card = Card(Car.newAdd(this.car, n))
    operator fun plus(un: UInt): Card = Card(Car.newAdd(this.car, un.toInt()))
    operator fun plus(l: Long): Card = Card(Car.newAdd(this.car, l))
    operator fun plus(ul: ULong): Card = Card(Car.newAdd(this.car, ul.toLong()))

    operator fun minus(other: Card): Card = Card(Car.newSub(this.car, other.car))
    operator fun minus(n: Int): Card = Card(Car.newSub(this.car, n))
    operator fun minus(un: UInt): Card = Card(Car.newSub(this.car, un.toInt()))
    operator fun minus(l: Long): Card = Card(Car.newSub(this.car, l))
    operator fun minus(ul: ULong): Card = Card(Car.newSub(this.car, ul.toLong()))

    operator fun times(other: Card): Card = Card(Car.newMul(this.car, other.car))
    operator fun times(n: Int): Card = Card(Car.newMul(this.car, n))
    operator fun times(un: UInt): Card = Card(Car.newMul(this.car, un.toInt()))
    operator fun times(l: Long): Card = Card(Car.newMul(this.car, l))
    operator fun times(ul: ULong): Card = Card(Car.newMul(this.car, ul.toLong()))

    operator fun div(other: Card): Card = Card(Car.newDiv(this.car, other.car))
    operator fun div(n: Int): Card = Card(Car.newDiv(this.car, n))
    operator fun div(un: UInt): Card = Card(Car.newDiv(this.car, un.toInt()))
    operator fun div(l: Long): Card = Card(Car.newDiv(this.car, l))
    operator fun div(ul: ULong): Card = Card(Car.newDiv(this.car, ul.toLong()))

    operator fun rem(other: Card): Card = Card(Car.newMod(this.car, other.car))
    operator fun rem(n: Int): Card = Card(Car.newMod(this.car, n))
    operator fun rem(un: UInt): Card = Card(Car.newMod(this.car, un.toInt()))
    operator fun rem(l: Long): Card = Card(Car.newMod(this.car, l))
    operator fun rem(ul: ULong): Card = Card(Car.newMod(this.car, ul.toLong()))

    infix fun ushr(bitCount: Int): Card = Card(Car.newShiftRight(this.car, bitCount))
    infix fun shl(bitCount: Int): Card = Card(Car.newShiftLeft(this.car, bitCount))

    fun compareTo(other: Card): Int = Car.compare(this.car, other.car)
    fun compareTo(n: Int): Int = Car.compare(this.car, n)
    fun compareTo(un: UInt): Int = Car.compare(this.car, un.toInt())
    fun compareTo(l: Long): Int = Car.compare(this.car, l)
    fun compareTo(ul: ULong): Int = Car.compare(this.car, ul.toLong())

    override fun toString() = Car.toString(this.car)

}