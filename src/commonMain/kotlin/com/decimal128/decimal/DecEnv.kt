package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecExceptionReason.IS_INEXACT
import com.decimal128.decimal.DecExceptionReason.OTHER
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import kotlin.math.max
import kotlin.math.min

data class DecEnv(
    val decFormat: DecFormat = DecFormat.DECIMAL_128,
    val decRounding: DecRounding = DecRounding.ROUND_TIES_TO_EVEN,
    val decPrefs: DecPrefs = DecPrefs.DEFAULT,
    val decTraps: DecTraps? = null,
    val decFlags: DecFlags = DecFlags(),
    val decTemps: DecTemps = DecTemps()
) {
    val precision: Int
        get() = decFormat.precision
    val maxBitLen: Int
        get() = decFormat.maxBitLen
    val qTiny: Int
        get() = decFormat.qTiny
    val qMax: Int
        get() = decFormat.qMax
    val eMax: Int
        get() = decFormat.eMax
    val eMin: Int
        get() = decFormat.eMin
    val overflow: Boolean
        get() = decFlags.isSet(OVERFLOW)

    companion object {
        val DECIMAL64 = DecEnv(DecFormat.DECIMAL_64)
        val DECIMAL128 = DecEnv(DecFormat.DECIMAL_128)
        val DECIMAL128_EXTENDED = DecEnv(DecFormat.DECIMAL_128_EXTENDED)

        val DECIMAL128_ZERO_NAN_PAYLOAD = DECIMAL128.with(DECIMAL128.decPrefs.copy(parseDiscardNanPayload = true))
    }

    fun with(newDecFormat: DecFormat) =
        DecEnv(newDecFormat, decRounding, decPrefs, decTraps, decFlags, decTemps)

    fun with(newDecRounding: DecRounding) =
        DecEnv(decFormat, newDecRounding, decPrefs, decTraps, decFlags, decTemps)

    fun with(newDecPrefs: DecPrefs) =
        DecEnv(decFormat, decRounding, newDecPrefs, decTraps, decFlags, decTemps)

    fun deepCopy() = DecEnv(decFormat, decRounding, decPrefs, decTraps)

    inline fun <T> compute(block: () -> T ): T = block()

    inline fun <T> computeDelayedTrap(block: () -> T): T {
        val blockEnv = DecEnv(decFormat, decRounding, decPrefs, null, DecFlags(), decTemps)
        val blockVal = blockEnv.compute(block)
        decTraps?.delayedTrap(blockEnv)
        return blockVal
    }

    fun isRoundTowardNegative() = decRounding == ROUND_TOWARD_NEGATIVE

    fun hasTrapHandler(decException: DecException) =
        decTraps?.hasTrapHandler(decException) ?: false

    fun signal(trapContext: DecExceptionContext): DecOld {
        require(decTraps != null)
        return decTraps.signal(trapContext)
    }

    fun signal(decException: DecException, exceptionReason: DecExceptionReason, operation: String, d: DecOld): DecOld {
        if (decTraps == null || !decTraps.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return d
        }
        val trapContext = DecExceptionContext(decException, exceptionReason, operation, this)
        return decTraps.signal(trapContext)
    }

    fun signal(decException: DecException, exceptionReason: DecExceptionReason, operation: String, mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(decException))
            return mutDec
        val trapContext = DecExceptionContext(decException, exceptionReason, operation, this)
        return mutDec.set(decTraps.signal(trapContext))
    }

    fun signal(decExceptionReason: DecExceptionReason): DecOld {
        // TODO
        return DecOld.NaN
    }

    // used by partialCompare when there is a sNaN, but no return value
    fun signalInvalid() {
        if (decTraps == null || !decTraps.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return
        }
        // provide a dummy value when none was provided ... e.g. by partialCompare
        signal(INVALID_OPERATION, OTHER, "whatever", MutDec())
    }

    fun signalInvalid(mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return mutDec
        }
        return signal(INVALID_OPERATION, OTHER, "whatever", mutDec)
    }

    fun signalDivByZero(mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(DIV_BY_ZERO)) {
            decFlags.set(DIV_BY_ZERO)
            return mutDec
        }
        return signal(DIV_BY_ZERO, OTHER, "whatever", mutDec)
    }

    fun signalDivByZero(sign: Boolean): DecOld {
        val inf = if (sign) DecOld.NEG_INFINITY else DecOld.POS_INFINITY
        if (decTraps == null || !decTraps.hasTrapHandler(DIV_BY_ZERO)) {
            decFlags.set(DIV_BY_ZERO)
            return inf
        }
        return signal(DIV_BY_ZERO, OTHER, "whatever", inf)
    }

    fun signalInexactOverflow(mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(OVERFLOW) && !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTraps.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, OTHER, "whatever", mutDec)
    }

    fun signalInexactOverflow(decInfinity: DecOld): DecOld {
        if (decTraps == null || !decTraps.hasTrapHandler(OVERFLOW) && !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return decInfinity
        }
        val trap = if (decTraps.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, OTHER, "whatever", decInfinity)
    }

    fun signalRoundedInexact(dec: DecOld): DecOld {
        if (decTraps == null || !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTraps.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, OTHER, "whatever", dec)
    }

    fun signalInexactUnderflow(mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(UNDERFLOW) && !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTraps.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signal(trap, OTHER, "whatever", mutDec)
    }

    fun signalInexactUnderflow(dec: DecOld): DecOld {
        if (decTraps == null || !decTraps.hasTrapHandler(UNDERFLOW) && !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTraps.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signal(trap, OTHER, "whatever", dec)
    }

    fun signalInexact(mutDec: MutDec): MutDec {
        if (decTraps == null || !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return mutDec
        }
        return signal(INEXACT, IS_INEXACT, "whatever", mutDec)
    }

    fun signalInexact(dec: DecOld): DecOld {
        if (decTraps == null || !decTraps.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        return signal(INEXACT, IS_INEXACT, "whatever", dec)
    }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (hasTrapHandler(INVALID_OPERATION))
            throw RuntimeException("invalid sNaN seen")
    }

    fun getFptestExceptionsString() = decFlags.getFptestExceptionsString()

    fun <T> context(block: DecEnv.() -> T): T {
        val result = this.block()
        return result
    }

    // Member-extension operator overloads:
    operator fun DecOld.plus(other: DecOld): DecOld = BinopAddSub.addImpl(this, other, this@DecEnv)
    operator fun DecOld.minus(other: DecOld): DecOld = BinopAddSub.subImpl(this, other, this@DecEnv)
    operator fun DecOld.times(other: DecOld): DecOld = D128Mul.mulImpl(this, other, this@DecEnv)

    fun parseDiscardNanPayload() = decPrefs.parseDiscardNanPayload

    fun capExponentRange(qExp: Int) = max(min(qExp, qMax), qTiny)
}
