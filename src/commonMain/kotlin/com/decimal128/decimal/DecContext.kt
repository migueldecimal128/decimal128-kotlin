package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import kotlin.math.max
import kotlin.math.min

data class DecContext(
    val decFormat: DecFormat = DecFormat.DECIMAL_128,
    val decRounding: DecRounding = DecRounding.ROUND_TIES_TO_EVEN,
    val decPrefs: DecPrefs = DecPrefs.DEFAULT,
    val decTrapHandlers: DecTrapHandlers? = null,
    val decFlags: DecFlags = DecFlags(),
) {
    val precision: Int
        get() = decFormat.precision
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

    internal val tmps: DecTmps = DecTmps()

    companion object {
        //val DECIMAL64 = DecContext(DecFormat.DECIMAL_64)
        val DECIMAL128 = DecContext(DecFormat.DECIMAL_128)
        val DECIMAL128_EXTENDED = DecContext(DecFormat.DECIMAL_128_EXTENDED)

        val DECIMAL128_ZERO_NAN_PAYLOAD = DECIMAL128.with(DECIMAL128.decPrefs.copy(parseDiscardNanPayload = true))

        val threadLocal = ThreadLocal.withInitial { DecContext(DecFormat.DECIMAL_128) }
        fun current(): DecContext = threadLocal.get()


        internal val TMP_ENV_ROUND_TOWARD_ZERO = DECIMAL128.with(DecRounding.ROUND_TOWARD_ZERO)
    }

    fun with(newDecFormat: DecFormat) =
        DecContext(newDecFormat, decRounding, decPrefs, decTrapHandlers, decFlags)

    fun with(newDecRounding: DecRounding) =
        DecContext(decFormat, newDecRounding, decPrefs, decTrapHandlers, decFlags)

    fun with(newDecPrefs: DecPrefs) =
        DecContext(decFormat, decRounding, newDecPrefs, decTrapHandlers, decFlags)

    fun withNewFlags() =
        DecContext(decFormat, decRounding, decPrefs, decTrapHandlers, DecFlags())

    fun withTrapHandler(decTrapHandler: DecTrapHandler?, vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withTrapHandler(decTrapHandler, exceptions)
        return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags)
    }

    fun withThrownException(vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withThrownException(exceptions)
        return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags)
    }

    fun deepCopy() = DecContext(decFormat, decRounding, decPrefs, decTrapHandlers)

    inline fun <T> compute(block: () -> T ): T = block()

    inline fun <T> computeDelayedTrap(block: () -> T): T {
        val blockEnv = DecContext(decFormat, decRounding, decPrefs, null, DecFlags())
        val blockVal = blockEnv.compute(block)
        decTrapHandlers?.delayedTrap(blockEnv)
        return blockVal
    }

    fun isRoundTowardNegative() = decRounding == ROUND_TOWARD_NEGATIVE

    fun hasTrapHandler(decException: DecException) =
        decTrapHandlers?.hasTrapHandler(decException) ?: false

    fun signal(trapContext: DecExceptionContext): Decimal {
        require(decTrapHandlers != null)
        return decTrapHandlers.signal(trapContext)
    }

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

    fun signal(decException: DecException, decExceptionReason: InvalidOperationReason): Decimal {
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return Decimal.NaN
        }
        val trapContext = DecExceptionContext(this, Decimal.NaN, decException, decExceptionReason)
        return decTrapHandlers.signal(trapContext)
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

}
