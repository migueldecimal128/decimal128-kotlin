package com.decimal128.decimal

import com.decimal128.decimal.DecException.*

class DecFlags {
    var flags = 0

    fun set(decException: DecException) {
        flags = flags or (1 shl decException.ordinal)
    }

    fun reset(decException: DecException) {
        flags = flags and (1 shl decException.ordinal).inv()
    }

    fun isSet(decException: DecException): Boolean {
        return (flags and (1 shl decException.ordinal)) != 0
    }

    fun trapInvalid(t: Boolean) {
        if (t)
            set(INVALID_OPERATION)
        else
            reset(INVALID_OPERATION)
    }

    //
    fun signalInexact(z: MutDec): MutDec { set(INEXACT); return z }
    fun signalUnderflow(z: MutDec): MutDec { set(UNDERFLOW); return z }
    fun signalInexactUnderflow(z: MutDec): MutDec { set(INEXACT); set(UNDERFLOW); return z }
    fun signalInexactOverflow(z: MutDec): MutDec { set(INEXACT); set(OVERFLOW); return z}
    fun signalDivByZero(z: MutDec): MutDec { set(DIV_BY_ZERO); return z }
    fun signalInvalid(z: MutDec): MutDec { set(INVALID_OPERATION); return z }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (isSet(INVALID_OPERATION))
            throw RuntimeException("invalid sNaN seen")
    }

}
