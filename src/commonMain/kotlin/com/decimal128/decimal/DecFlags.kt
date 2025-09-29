package com.decimal128.decimal

class DecFlags {
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
    fun signalInexact(z: MutDec): MutDec { inexact = true; return z }
    fun signalUnderflow(z: MutDec): MutDec { underflow = true; return z }
    fun signalInexactUnderflow(z: MutDec): MutDec { inexact = true; underflow = true; return z }
    fun signalInexactOverflow(z: MutDec): MutDec { inexact = true; overflow = true; return z}
    fun signalDivByZero(z: MutDec): MutDec { divByZero = true; return z }
    fun signalInvalid(z: MutDec): MutDec { invalid = true; return z }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (trapInvalid)
            throw RuntimeException("invalid sNaN seen")
    }

}
