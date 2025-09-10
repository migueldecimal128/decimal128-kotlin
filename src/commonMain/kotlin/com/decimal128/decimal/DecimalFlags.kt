package com.decimal128.decimal

class DecimalFlags {
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

    var trapInvalid = false
        private set

    fun trapInvalid(t: Boolean) {
        trapInvalid = t
    }

    //
    fun signalInexact(z: Decimal): Decimal { inexact = true; return z }
    fun signalUnderflow(z: Decimal): Decimal { underflow = true; return z }
    fun signalInexactUnderflow(z: Decimal): Decimal { inexact = true; underflow = true; return z }
    fun signalInexactOverflow(z: Decimal): Decimal { inexact = true; overflow = true; return z}
    fun signalDivByZero(z: Decimal): Decimal { divByZero = true; return z }
    fun signalInvalid(z: Decimal): Decimal { invalid = true; return z }

    fun operandIsSignalingNaN(decimal: Decimal) {
        if (trapInvalid)
            throw RuntimeException("invalid sNaN seen")
    }

}
