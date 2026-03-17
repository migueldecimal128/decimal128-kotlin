package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE

class DecContext internal constructor(
    internal val decFormat: DecFormat = DecFormat.DECIMAL_128,
    internal val decRounding: DecRounding = DecRounding.ROUND_TIES_TO_EVEN,
    internal val decPrefs: DecPrefs = DecPrefs.KOTLIN_DEFAULT,
    internal val decTrapHandlers: DecTrapHandlers?,
    internal val decFlags: DecFlags,
    internal val tmps: DecTmps

) {
    val precision: Int = decFormat.precision

    companion object {

        fun decimal128Kotlin(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            tmps = DecTmps()
        )

        fun decimal128IEEE(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            tmps = DecTmps()
        )

        fun decimal128Extended(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128_EXTENDED,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            tmps = DecTmps()
        )

        val threadLocal = ThreadLocal.withInitial { decimal128Kotlin() }

        fun current(): DecContext = threadLocal.get()
        fun setCurrent(newDecContext: DecContext) = threadLocal.set(newDecContext)


    }

    fun with(newDecFormat: DecFormat) =
        DecContext(newDecFormat, decRounding, decPrefs, decTrapHandlers, decFlags, tmps)

    fun with(newDecRounding: DecRounding) =
        DecContext(decFormat, newDecRounding, decPrefs, decTrapHandlers, decFlags, tmps)

    fun with(newDecPrefs: DecPrefs) =
        DecContext(decFormat, decRounding, newDecPrefs, decTrapHandlers, decFlags, tmps)

    fun withRoundingAndNewFlags(decRounding: DecRounding) =
        DecContext(decFormat, decRounding, decPrefs, decTrapHandlers, DecFlags(), tmps)

    fun withTrapHandler(decTrapHandler: DecTrapHandler?, vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withTrapHandler(decTrapHandler, exceptions)
        return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags, tmps)
    }

    fun withThrownException(vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withThrownException(exceptions)
        return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags, tmps)
    }

    inline fun <T> compute(block: () -> T ): T = block()

    fun isRoundTowardNegative() = decRounding == ROUND_TOWARD_NEGATIVE

    fun isOverflow(): Boolean = decFlags.isSet(OVERFLOW)

    fun hasTrapHandler(decException: DecException) =
        decTrapHandlers?.hasTrapHandler(decException) ?: false

    fun signal(decException: DecException, d: Decimal, exceptionReason: InvalidOperationReason? = null): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return d
        }
        val trapContext = DecExceptionContext(this, d, decException, exceptionReason)
        return decTrapHandlers.signal(trapContext)
    }

    fun signal(decException: DecException, l: Long, exceptionReason: InvalidOperationReason? = null): Long {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return l
        }
        val trapContext = DecExceptionContext(this, Decimal.from(l), decException, exceptionReason)
        decTrapHandlers.signal(trapContext)
        return l
    }

    fun signalMutDec(decException: DecException, mutDec: MutDec, invalidOpReason: InvalidOperationReason? = null): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException))
            return mutDec
        val trapContext = DecExceptionContext(this, Decimal.from(mutDec), decException, invalidOpReason)
        return mutDec.set(decTrapHandlers.signal(trapContext))
    }

    fun signalInvalid(mutDec: MutDec): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return mutDec
        }
        return signalMutDec(INVALID_OPERATION, mutDec)
    }

    fun signalInvalid(invalidOpReason: InvalidOperationReason): Decimal = signalInvalid(invalidOpReason, Decimal.NaN)

    fun signalInvalid(invalidOpReason: InvalidOperationReason, dec: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return dec
        }
        return signal(INVALID_OPERATION, dec, invalidOpReason)
    }

    fun signalDivByZero(mutDec: MutDec): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
            decFlags.set(DIVIDE_BY_ZERO)
            return mutDec
        }
        return signalMutDec(DIVIDE_BY_ZERO, mutDec)
    }

    fun signalDivByZero(dec: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
            decFlags.set(DIVIDE_BY_ZERO)
            return dec
        }
        return signal(DIVIDE_BY_ZERO, dec)
    }

    fun signalDivByZero(sign: Boolean): Decimal =
        signalDivByZero(if (sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY)

    fun signalInexactOverflow(mutDec: MutDec): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signalMutDec(trap, mutDec)
    }

    fun signalInexactOverflow(decInfinity: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return decInfinity
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, decInfinity)
    }

    fun signalRoundedInexact(dec: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, dec)
    }

    fun signalInexactUnderflow(mutDec: MutDec): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signalMutDec(trap, mutDec)
    }

    fun signalInexactUnderflow(dec: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signal(trap, dec)
    }

    fun signalInexact(mutDec: MutDec): MutDec {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return mutDec
        }
        return signalMutDec(INEXACT, mutDec)
    }

    fun signalInexact(dec: Decimal): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        return signal(INEXACT, dec)
    }

    fun signalInexact(l: Long): Long {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return l
        }
        return signal(INEXACT, l)
    }

    fun signalInvalid(l: Long): Long {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return l
        }
        return signal(
            INVALID_OPERATION,
            l, InvalidOperationReason.CONVERT_NON_FINITE_TO_INTEGER
        )
    }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (hasTrapHandler(INVALID_OPERATION))
            throw RuntimeException("invalid sNaN seen")
    }

    fun getFptestExceptionsString() = decFlags.getFptestExceptionsString()

    fun <T> context(block: DecContext.() -> T): T {
        val result = this.block()
        return result
    }

    fun parseDiscardNanPayload() = decPrefs.parseDiscardNanPayload

    override fun toString(): String =
        "DecContext(decFormat=$decFormat, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"

}
