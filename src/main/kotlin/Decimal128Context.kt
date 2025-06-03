package com.decimal128

import java.math.MathContext
import java.math.RoundingMode

class Decimal128Context(val roundingDirection:RoundingDirection) {
    constructor() : this(RoundingDirection.ROUND_TIES_TO_EVEN)
    companion object {
        val MATH_CONTEXT_MAP = arrayOf(
            MathContext.DECIMAL128, // HALF_EVEN
            MathContext(PRECISION_34, RoundingMode.HALF_UP),
            MathContext(PRECISION_34, RoundingMode.DOWN),
            MathContext(PRECISION_34, RoundingMode.CEILING),
            MathContext(PRECISION_34, RoundingMode.FLOOR),
        )
    }

    fun getMathContext() = MATH_CONTEXT_MAP[roundingDirection.value]

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
    fun setInexact(inexact:Boolean) { this.inexact = this.inexact or inexact}

    fun operandIsSignalingNaN(dec34: Dec34) {
        //FIXME - throw exception here unless trap is masked off
    }
}