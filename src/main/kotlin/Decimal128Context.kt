package com.decimal128

import java.math.MathContext

class Decimal128Context(val roundingDirection:RoundingDirection) {
    constructor() : this(RoundingDirection.ROUND_TIES_TO_EVEN)

    var invalid = false
        private set
    var divByZero = false
        private set
    var overflow = false
        private set
    var underflow = false
        private set
    var inexact = false
        private set

    fun setInvalid() { invalid = true }
    fun setDivByZero() { divByZero = true }
    fun setOverflow() { overflow = true }
    fun setUnderflow() { underflow = true }
    fun setInexact() { inexact = true }
    fun setInexact(inexact:Boolean) { this.inexact = inexact}

}