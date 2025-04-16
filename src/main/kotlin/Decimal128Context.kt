package com.decimal128

class Decimal128Context(val roundingDirection: RoundingDirection) {
    constructor() : this(RoundingDirection.ROUND_TIES_TO_EVEN)

    var invalid = false
        private set
    private var divByZero = false
        private set
    private var overflow = false
        private set
    private var underflow = false
        private set
    private var inexact = false
        private set

    fun setInvalid() { invalid = true }
    fun setDivByZero() { divByZero = true }
    fun setOverflow() { overflow = true }
    fun setUnderflow() { underflow = true }
    fun setInexact() { inexact = true }

}