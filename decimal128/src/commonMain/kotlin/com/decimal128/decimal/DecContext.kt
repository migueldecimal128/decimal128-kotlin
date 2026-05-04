// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_NEGATIVE

expect abstract class DecContextRep(
    roundingDirection: RoundingDirection,
    parsePrefs: ParsePrefs,
    printPrefs: PrintPrefs,
    arithmeticPrefs: ArithmeticPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean = false
) {
    internal val roundingDirection: RoundingDirection
    internal val parsePrefs: ParsePrefs
    internal val printPrefs: PrintPrefs
    internal val arithmeticPrefs: ArithmeticPrefs
    internal val decTrapHandlers: DecTrapHandlers?
    internal val decFlags: DecFlags
    internal val tmps: DecTmps
    internal val isExtendedPrecision38: Boolean

    internal val precision: Int

    internal val dw0MaxxCoeff: Long
    internal val dw1MaxxCoeff: Long
    internal val dw0MinFullPrecisionCoeff: Long
    internal val dw1MinFullPrecisionCoeff: Long

}

expect object DecContextThreadLocal {

    fun current(): DecContext
    fun setCurrent(newDecContext: DecContext)

    fun internal38(): DecContext
    fun setInternal38(newDecContext: DecContext)

}

class DecContext(
    roundingDirection: RoundingDirection,
    parsePrefs: ParsePrefs,
    printPrefs: PrintPrefs,
    arithmeticPrefs: ArithmeticPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean = false
) : DecContextRep(
    roundingDirection, parsePrefs, printPrefs, arithmeticPrefs, decTrapHandlers, decFlags, decTmps, isExtendedPrecision38
) {

    companion object {
        fun decimal128Kotlin(): DecContext = DecContext(
            roundingDirection = RoundingDirection.TIES_TO_EVEN,
            parsePrefs = ParsePrefs.DEFAULT_KOTLIN,
            printPrefs = PrintPrefs.DEFAULT_KOTLIN,
            arithmeticPrefs = ArithmeticPrefs.DEFAULT_KOTLIN,
            decTrapHandlers = null,
            decFlags = DecFlags(),
            decTmps = DecTmps(),
        )

        fun decimal128IEEE(): DecContext = DecContext(
            roundingDirection = RoundingDirection.TIES_TO_EVEN,
            parsePrefs = ParsePrefs.DEFAULT_IEEE,
            printPrefs = PrintPrefs.DEFAULT_IEEE,
            arithmeticPrefs = ArithmeticPrefs.DEFAULT_IEEE,
            decTrapHandlers = null,
            decFlags = DecFlags(),
            decTmps = DecTmps(),
        )

        fun decimal128Extended38(): DecContext = DecContext(
            roundingDirection = RoundingDirection.TIES_TO_EVEN,
            parsePrefs = ParsePrefs.DEFAULT_IEEE,
            printPrefs = PrintPrefs.DEFAULT_IEEE,
            arithmeticPrefs = ArithmeticPrefs.DEFAULT_IEEE,
            decTrapHandlers = null,  // parseMalformedSignalsInvalidOperation = false
            decFlags = DecFlags(),
            decTmps = DecTmps(),
            isExtendedPrecision38 = true
        )

        fun current(): DecContext = DecContextThreadLocal.current()
        fun setCurrent(newDecContext: DecContext) {
            DecContextThreadLocal.setCurrent(newDecContext)
        }

        fun internal38(): DecContext = DecContextThreadLocal.internal38()
        fun setInternal38(newDecContext: DecContext) {
            DecContextThreadLocal.setInternal38(newDecContext)
        }

    }


    fun withExtendedPrecision38() =
        DecContext(roundingDirection, parsePrefs, printPrefs, arithmeticPrefs, decTrapHandlers, decFlags, tmps, isExtendedPrecision38 = true)

    fun with(newRoundingDirection: RoundingDirection) =
        DecContext(newRoundingDirection, parsePrefs, printPrefs, arithmeticPrefs, decTrapHandlers, decFlags, tmps, isExtendedPrecision38)

    fun with(newParsePrefs: ParsePrefs) =
        DecContext(roundingDirection, newParsePrefs, printPrefs, arithmeticPrefs, decTrapHandlers, decFlags, tmps, isExtendedPrecision38)

    fun with(newPrintPrefs: PrintPrefs) =
        DecContext(roundingDirection, parsePrefs, newPrintPrefs, arithmeticPrefs, decTrapHandlers, decFlags, tmps, isExtendedPrecision38)

    fun with(newArithmeticPrefs: ArithmeticPrefs) =
        DecContext(roundingDirection, parsePrefs, printPrefs, newArithmeticPrefs, decTrapHandlers, decFlags, tmps, isExtendedPrecision38)

    fun withRoundingAndNewFlags(roundingDirection: RoundingDirection) =
        DecContext(roundingDirection, parsePrefs, printPrefs, arithmeticPrefs, decTrapHandlers, DecFlags(), tmps, isExtendedPrecision38)

    fun withTrapHandler(decTrapHandler: DecTrapHandler?, vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withTrapHandler(decTrapHandler, exceptions)
        return DecContext(roundingDirection, parsePrefs, printPrefs, arithmeticPrefs, newTrapHandlers, decFlags, tmps, isExtendedPrecision38)
    }

    fun withThrownException(vararg exceptions: DecException): DecContext {
        val newTrapHandlers = (decTrapHandlers ?: DecTrapHandlers.NONE).withThrownException(exceptions)
        return DecContext(roundingDirection, parsePrefs, printPrefs, arithmeticPrefs, newTrapHandlers, decFlags, tmps, isExtendedPrecision38)
    }

    fun isOverflow(): Boolean = decFlags.isSet(OVERFLOW)

    fun isRoundTowardNegative(): Boolean = roundingDirection == RoundingDirection.TOWARD_NEGATIVE

    fun hasTrapHandler(decException: DecException) =

        decTrapHandlers?.hasTrapHandler(decException) ?: false

    fun signal(
        decException: DecException,
        d: Decimal,
        exceptionReason: InvalidCause? = null
    ): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return d
        }
        val trapContext = DecExceptionContext(this, d, decException, exceptionReason)
        return decTrapHandlers.signal(trapContext)
    }

    fun signal(decException: DecException, l: Long, exceptionReason: InvalidCause? = null): Long {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException)) {
            decFlags.set(decException)
            return l
        }
        val trapContext = DecExceptionContext(this, Decimal.from(l), decException, exceptionReason)
        decTrapHandlers.signal(trapContext)
        return l
    }

    fun signalMutDec(
        decException: DecException,
        mutDec: MutDec,
        invalidOpReason: InvalidCause? = null
    ): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(decException))
            return mutDec
        val trapContext = DecExceptionContext(this, Decimal.from(mutDec), decException, invalidOpReason)
        return mutDec.set(decTrapHandlers.signal(trapContext))
    }

    fun signalInvalidOperation(mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return mutDec
        }
        return signalMutDec(INVALID_OPERATION, mutDec)
    }

    fun signalInvalidOperation(invalidOpReason: InvalidCause, mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return mutDec
        }
        return signalMutDec(INVALID_OPERATION, mutDec, invalidOpReason)
    }

    fun setNanSignalInvalidOperation(z: MutDec, invalidOpReason: InvalidCause): MutDec {
        z.setNaN()
        return signalInvalidOperation(invalidOpReason, z)
    }

    fun signalInvalidOperation(invalidOpReason: InvalidCause): Decimal =
        this@DecContext.signalInvalidOperation(invalidOpReason, Decimal.NaN)

    fun signalInvalidOperation(invalidOpReason: InvalidCause, dec: Decimal): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return dec
        }
        return signal(INVALID_OPERATION, dec, invalidOpReason)
    }

    fun signalSNanOperandFound(nan: Decimal): Decimal {
        verify { nan.isNaN() }
        // Intel libbid tests do not prefer sNaN,
        // They return the first left-to-right NaN, but signal if any operant is sNaN
        // therefore, this might be called with a NaN that is actually a qNaN that
        // does not need to be quieted.
        val quietedNaN =
            if (nan.isSignaling())
                Decimal.qNaN(nan.signFlag, nan.dw1, nan.dw0)
            else
                nan
        return signalInvalidOperation(InvalidCause.SNAN_OPERAND, quietedNaN)
    }

    fun signalDivByZero(mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
            decFlags.set(DIVIDE_BY_ZERO)
            return mutDec
        }
        return signalMutDec(DIVIDE_BY_ZERO, mutDec)
    }

    fun signalDivByZero(dec: Decimal): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(DIVIDE_BY_ZERO)) {
            decFlags.set(DIVIDE_BY_ZERO)
            return dec
        }
        return signal(DIVIDE_BY_ZERO, dec)
    }

    fun signalDivByZero(sign: Boolean): Decimal =
        signalDivByZero(if (sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY)

    fun setInfinitySignalInexactOverflow(z: MutDec, isNegative: Boolean): MutDec {
        when (roundingDirection) {
            RoundingDirection.TOWARD_ZERO,
            RoundingDirection.TOWARD_NEGATIVE ->
                if (isNegative) z.setInfinite(true) else z.setMaxFiniteMagnitude(false, this)
            RoundingDirection.TOWARD_POSITIVE ->
                if (isNegative) z.setMaxFiniteMagnitude(true, this) else z.setInfinite(false)
            else -> z.setInfinite(isNegative)
        }
        return signalInexactOverflow(z)
    }


    fun signalInexactOverflow(mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(
                INEXACT
            )
        ) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signalMutDec(trap, mutDec)
    }

    fun signalInexactOverflow(dec: Decimal): Decimal {
        // must be either infinite or max finite magnitude
        // FIXME - consider do not pass an arg, but construct one here since
        //  we came here to the context to decide which to construct
        verify { dec.isInfinite() ||
                dec.qExp == 6111 && dec.dw1 == this.dw1MaxxCoeff && dec.dw0 == this.dw0MaxxCoeff - 1 }
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(OVERFLOW) && !decTrapHandlers.hasTrapHandler(
                INEXACT
            )
        ) {
            decFlags.set(OVERFLOW)
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, dec)
    }

    fun signalRoundedInexact(dec: Decimal): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(OVERFLOW)) OVERFLOW else INEXACT
        return signal(trap, dec)
    }

    fun setZeroSignalInexactUnderflow(z: MutDec, sign: Boolean = false): MutDec =
        signalInexactUnderflow(z.setZero(sign, Q_TINY))

    fun signalInexactUnderflow(mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(
                INEXACT
            )
        ) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return mutDec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signalMutDec(trap, mutDec)
    }

    fun signalInexactUnderflow(dec: Decimal): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(UNDERFLOW) && !decTrapHandlers.hasTrapHandler(
                INEXACT
            )
        ) {
            decFlags.set(UNDERFLOW)
            decFlags.set(INEXACT)
            return dec
        }
        val trap = if (decTrapHandlers.hasTrapHandler(UNDERFLOW)) UNDERFLOW else INEXACT
        return signal(trap, dec)
    }

    fun signalInexact(mutDec: MutDec): MutDec {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return mutDec
        }
        return signalMutDec(INEXACT, mutDec)
    }

    fun signalInexact(dec: Decimal): Decimal {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return dec
        }
        return signal(INEXACT, dec)
    }

    fun signalInexact(l: Long): Long {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INEXACT)) {
            decFlags.set(INEXACT)
            return l
        }
        return signal(INEXACT, l)
    }

    fun signalInvalidOperation(l: Long): Long {
        val decTrapHandlers = decTrapHandlers
        if (decTrapHandlers == null || !decTrapHandlers.hasTrapHandler(INVALID_OPERATION)) {
            decFlags.set(INVALID_OPERATION)
            return l
        }
        return signal(
            INVALID_OPERATION,
            l, InvalidCause.CONVERT_NON_FINITE_TO_INTEGER
        )
    }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (hasTrapHandler(INVALID_OPERATION))
            throw RuntimeException("invalid sNaN seen")
    }

    fun isSet(decException: DecException): Boolean =
        this.decFlags.isSet(decException)

    fun getFptestExceptionsString() = decFlags.getFptestExceptionsString()

    // FIXME - switch to preserve rather than discard
    fun parseDiscardNanPayload() = parsePrefs.preserveNANPayload

    internal fun coeffFits(dw1: Long, dw0: Long): Boolean {
        // if bitLen < 113 then it is guaranteed to fit
        if (dw1.countLeadingZeroBits() > 128 - 113)
            return true
        val pow10Offset = (this.precision shl 1) and POW10_BCE
        val maxxHi = POW10[pow10Offset + 1]
        return unsignedLT(dw1, maxxHi) || dw1 == maxxHi && unsignedLT(dw0, POW10[pow10Offset])
    }

    internal inline fun coeffQexpFit(dw1: Long, dw0: Long, qExp: Int): Boolean {
        if (qExp < Q_TINY || qExp > Q_MAX)
            return false
        if (dw1.countLeadingZeroBits() > 128 - 113)
            return true
        val pow10Offset = (this.precision shl 1) and POW10_BCE
        val maxxHi = POW10[pow10Offset + 1]
        return unsignedLT(dw1, maxxHi) || dw1 == maxxHi && unsignedLT(dw0, POW10[pow10Offset])
    }

    internal inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean {
        val pow10Offset = (this.precision shl 1) and POW10_BCE
        return dw0 == POW10[pow10Offset] && dw1 == POW10[pow10Offset + 1]
    }

    inline fun <T> eval(block: () -> T): T {
        val previous = DecContext.current()
        DecContext.setCurrent(this)
        return try {
            block()
        } finally {
            DecContext.setCurrent(previous)
        }

    }
}

internal fun signalDivByZero(dec: Decimal): Decimal =
    DecContext.current().signalDivByZero(dec)

internal fun signalInexact(dec: Decimal): Decimal =
    DecContext.current().signalInexact(dec)

internal fun signalInexact(l: Long): Long =
    DecContext.current().signalInexact(l)

internal fun signalInvalidOperation(invalidOpReason: InvalidCause, x: Decimal = Decimal.NaN): Decimal =
    DecContext.current().signalInvalidOperation(invalidOpReason, x)

