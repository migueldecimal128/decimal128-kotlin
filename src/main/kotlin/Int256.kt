package com.decimal128

import kotlin.random.Random

class Int256 : S256 {
    constructor() : super()
    constructor(x: Int256) : super() {
        s256Set(x)
    }
    constructor(sign: Boolean, dw3: Long, dw2: Long, dw1: Long, dw0: Long):
    super(sign, dw3, dw2, dw1, dw0)
    constructor(str: String): super(str)
    constructor(bitLen: Int, random: Random): super() {
        set(bitLen, random)
    }

    operator fun plus(other: Int256): Int256 {
        val sum = Int256()
        sum.s256Add(this, other)
        return sum
    }

    operator fun minus(other: Int256): Int256 {
        val diff = Int256()
        diff.s256Sub(this, other)
        return diff
    }

    operator fun times(other: Int256): Int256 {
        val prod = Int256()
        prod.s256Mul(this, other)
        return prod
    }

    operator fun div(other: Int256): Int256 {
        val quot = Int256()
        quot.s256Div(this, other)
        return quot
    }

    operator fun plusAssign(other: Int256) {
        this.s256Add(this, other)
    }

    operator fun minusAssign(other: Int256) {
        this.s256Sub(this, other)
    }

    operator fun timesAssign(other: Int256) {
        this.s256Mul(this, other)
    }

    operator fun divAssign(other: Int256) {
        this.s256Div(this, other)
    }

    operator fun remAssign(other: Int256) {
        this.s256Mod(this, other)
    }

    operator fun unaryMinus() = Int256(! sign, dw3, dw2, dw1, dw0)

    operator fun compareTo(other: Int256) = this.s256UnscaledCompareTo(other)

    fun setZero(): Int256 {s256SetZero(); return this}

    fun setSigned(n: Long): Int256 { s256SetSigned(n); return this }

    fun setUnsigned(u: Long): Int256 { s256SetUnsigned(u); return this }

    fun set(sign: Boolean, dw3: Long, dw2: Long, dw1: Long, dw0: Long): Int256 {
        s256Set(sign, dw3, dw2, dw1, dw0)
        return this
    }

    fun set(bitLen: Int, random: Random): Int256 {
        if (bitLen < 0 || bitLen > 256)
            throw IllegalArgumentException()
        val dw3 = if (bitLen <= 192) 0 else random.nextLong() shr (256 - bitLen)
        val dw2 = if (bitLen <= 128) 0 else random.nextLong() shr if ((192 - bitLen) > 0) (192 - bitLen) else 0
        val dw1 = if (bitLen <=  64) 0 else random.nextLong() shr if ((128 - bitLen) > 0) (128 - bitLen) else 0
        val dw0 = if (bitLen ==   0) 0 else random.nextLong() shr if (( 64 - bitLen) > 0) ( 64 - bitLen) else 0
        val sign = random.nextBoolean()
        set(sign, dw3, dw2, dw1, dw0)
        return this
    }

}