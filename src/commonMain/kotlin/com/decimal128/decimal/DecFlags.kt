package com.decimal128.decimal

import com.decimal128.decimal.DecException.*

class DecFlags {
    private var flags = 0

    fun set(decException: DecException) {
        flags = flags or (1 shl decException.ordinal)
    }

    fun clear(decException: DecException) {
        flags = flags and (1 shl decException.ordinal).inv()
    }

    fun clearAll() {
        flags = 0
    }

    fun isSet(decException: DecException): Boolean {
        return (flags and (1 shl decException.ordinal)) != 0
    }

    fun trapInvalid(t: Boolean) {
        if (t)
            set(INVALID_OPERATION)
        else
            clear(INVALID_OPERATION)
    }

    //
    fun signalInexact(z: MutDec): MutDec { set(INEXACT); return z }
    fun signalUnderflow(z: MutDec): MutDec { set(UNDERFLOW); return z }
    fun signalInexactUnderflow(z: MutDec): MutDec { set(INEXACT); set(UNDERFLOW); return z }
    fun signalInexactOverflow(z: MutDec): MutDec { set(INEXACT); set(OVERFLOW); return z}
    fun signalDivByZero(z: MutDec): MutDec { set(DIVIDE_BY_ZERO); return z }
    fun signalInvalid(z: MutDec): MutDec { set(INVALID_OPERATION); return z }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (isSet(INVALID_OPERATION))
            throw RuntimeException("invalid sNaN seen")
    }

    // x == inexact
    // u == underflow
    // o == overflow
    // z == divByZero
    // i == invalid
    fun getFptestExceptionsString(): String {
        val bytes = ByteArray(5)
        var ib = 0
        if (isSet(INEXACT))
            bytes[ib++] = 'x'.code.toByte()
        if (isSet(UNDERFLOW))
            bytes[ib++] = 'u'.code.toByte()
        if (isSet(OVERFLOW))
            bytes[ib++] = 'o'.code.toByte()
        if (isSet(DIVIDE_BY_ZERO))
            bytes[ib++] = 'z'.code.toByte()
        if (isSet(INVALID_OPERATION))
            bytes[ib++] = 'i'.code.toByte()
        return String(bytes, 0, ib)
    }

    fun getSetExceptions(): Set<DecException> {
        return DecException.entries.filter { isSet(it) }.toSet()
    }

    override fun equals(other: Any?) =
        other is DecFlags && flags == other.flags

    override fun toString(): String {
        val setExceptions = getSetExceptions()
        return when {
            setExceptions.isEmpty() -> "DecFlags[]"
            else -> setExceptions.joinToString(", ", "DecFlags[", "]") { it.name }
        }
    }
}
