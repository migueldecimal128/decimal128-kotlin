// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE

fun DecContext.with(newDecFormat: DecFormat) =
    DecContext(newDecFormat, decRounding, decPrefs, decTrapHandlers, decFlags, tmps)

fun DecContext.with(newDecRounding: DecRounding) =
    DecContext(decFormat, newDecRounding, decPrefs, decTrapHandlers, decFlags, tmps)

fun DecContext.with(newDecPrefs: DecPrefs) =
    DecContext(decFormat, decRounding, newDecPrefs, decTrapHandlers, decFlags, tmps)

fun DecContext.withRoundingAndNewFlags(decRounding: DecRounding) =
    DecContext(decFormat, decRounding, decPrefs, decTrapHandlers, DecFlags(), tmps)

fun DecContext.withTrapHandler(decTrapHandler: DecTrapHandler?, vararg exceptions: DecException): DecContext {
    val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withTrapHandler(decTrapHandler, exceptions)
    return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags, tmps)
}

fun DecContext.withThrownException(vararg exceptions: DecException): DecContext {
    val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withThrownException(exceptions)
    return DecContext(decFormat, decRounding, decPrefs, newTrapHandlers, decFlags, tmps)
}

inline fun <T> compute(block: () -> T ): T = block()

fun DecContext.isRoundTowardNegative() = decRounding == ROUND_TOWARD_NEGATIVE

fun DecContext.isOverflow(): Boolean = decFlags.isSet(OVERFLOW)

fun DecContext.hasTrapHandler(decException: DecException) =
    decTrapHandlers?.hasTrapHandler(decException) ?: false

fun DecContext.signal(decException: DecException, d: Decimal, exceptionReason: InvalidOperationReason? = null): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
        decFlags.set(decException)
        return d
    }
    val trapContext = DecExceptionContext(this, d, decException, exceptionReason)
    return decTrapHandlers.signal(trapContext)
}

fun DecContext.signal(decException: DecException, l: Long, exceptionReason: InvalidOperationReason? = null): Long {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
        decFlags.set(decException)
        return l
    }
    val trapContext = DecExceptionContext(this, Decimal.from(l), decException, exceptionReason)
    decTrapHandlers.signal(trapContext)
    return l
}

fun DecContext.signalMutDec(decException: DecException, mutDec: MutDec, invalidOpReason: InvalidOperationReason? = null): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException))
        return mutDec
    val trapContext = DecExceptionContext(this, Decimal.from(mutDec), decException, invalidOpReason)
    return mutDec.set(decTrapHandlers.signal(trapContext))
}

fun DecContext.signalInvalid(mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
        decFlags.set(INVALID_OPERATION)
        return mutDec
    }
    return signalMutDec(INVALID_OPERATION, mutDec)
}

fun DecContext.signalInvalid(invalidOpReason: InvalidOperationReason, mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
        decFlags.set(INVALID_OPERATION)
        return mutDec
    }
    return signalMutDec(INVALID_OPERATION, mutDec, invalidOpReason)
}

fun DecContext.setNanSignalInvalid(z: MutDec, invalidOpReason: InvalidOperationReason): MutDec {
    z.setNaN()
    return signalInvalid(invalidOpReason, z)
}

fun DecContext.signalInvalid(invalidOpReason: InvalidOperationReason): Decimal = signalInvalid(invalidOpReason, Decimal.NaN)

fun DecContext.signalInvalid(invalidOpReason: InvalidOperationReason, dec: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
        decFlags.set(INVALID_OPERATION)
        return dec
    }
    return signal(INVALID_OPERATION, dec, invalidOpReason)
}

fun DecContext.signalDivByZero(mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
        decFlags.set(DIVIDE_BY_ZERO)
        return mutDec
    }
    return signalMutDec(DIVIDE_BY_ZERO, mutDec)
}

fun DecContext.signalDivByZero(dec: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
        decFlags.set(DIVIDE_BY_ZERO)
        return dec
    }
    return signal(DIVIDE_BY_ZERO, dec)
}

fun DecContext.signalDivByZero(sign: Boolean): Decimal =
    signalDivByZero(if (sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY)

fun DecContext.signalInexactOverflow(mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(OVERFLOW)
        decFlags.set(DecException.INEXACT)
        return mutDec
    }
    val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
    return signalMutDec(trap, mutDec)
}

fun DecContext.signalInexactOverflow(decInfinity: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(OVERFLOW)
        decFlags.set(INEXACT)
        return decInfinity
    }
    val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
    return signal(trap, decInfinity)
}

fun DecContext.signalRoundedInexact(dec: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(INEXACT)
        return dec
    }
    val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
    return signal(trap, dec)
}

fun DecContext.signalInexactUnderflow(mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(UNDERFLOW)
        decFlags.set(INEXACT)
        return mutDec
    }
    val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
    return signalMutDec(trap, mutDec)
}

fun DecContext.signalInexactUnderflow(dec: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(UNDERFLOW)
        decFlags.set(INEXACT)
        return dec
    }
    val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
    return signal(trap, dec)
}

fun DecContext.signalInexact(mutDec: MutDec): MutDec {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(INEXACT)
        return mutDec
    }
    return signalMutDec(INEXACT, mutDec)
}

fun DecContext.signalInexact(dec: Decimal): Decimal {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(INEXACT)
        return dec
    }
    return signal(INEXACT, dec)
}

fun DecContext.signalInexact(l: Long): Long {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
        decFlags.set(INEXACT)
        return l
    }
    return signal(INEXACT, l)
}

fun DecContext.signalInvalid(l: Long): Long {
    val decTrapHandlers = decTrapHandlers
    if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
        decFlags.set(INVALID_OPERATION)
        return l
    }
    return signal(
        INVALID_OPERATION,
        l, InvalidOperationReason.CONVERT_NON_FINITE_TO_INTEGER
    )
}

fun DecContext.operandIsSignalingNaN(mutDec: MutDec) {
    if (hasTrapHandler(DecException.INVALID_OPERATION))
        throw RuntimeException("invalid sNaN seen")
}

fun DecContext.getFptestExceptionsString() = decFlags.getFptestExceptionsString()

fun DecContext.parseDiscardNanPayload() = decPrefs.parseDiscardNanPayload

fun <T> DecContext.context(block: context(DecContext) () -> T): T = block(this)

internal fun decContextDecimal128Kotlin(): DecContext = DecContext(
    decFormat = DecFormat.DECIMAL_128,
    decRounding = DecRounding.ROUND_TIES_TO_EVEN,
    decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
    decTrapHandlers = null,
    decFlags = DecFlags(),
    decTmps = DecTmps()
)

internal fun decContextDecimal128IEEE(): DecContext = DecContext(
    decFormat = DecFormat.DECIMAL_128,
    decRounding = DecRounding.ROUND_TIES_TO_EVEN,
    decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
    decTrapHandlers = null,
    decFlags = DecFlags(),
    decTmps = DecTmps()
)

internal fun decContextDecimal128Extended38(): DecContext = DecContext(
    decFormat = DecFormat.DECIMAL_128_EXTENDED,
    decRounding = DecRounding.ROUND_TIES_TO_EVEN,
    decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
    decTrapHandlers = null,
    decFlags = DecFlags(),
    decTmps = DecTmps()
)

inline fun <T> DecContext.eval(block: () -> T): T {
    val previous = DecContext.current()
    DecContext.setCurrent(this)
    return try {
        block()
    } finally {
        DecContext.setCurrent(previous)
    }
}