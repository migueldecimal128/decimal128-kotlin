package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import com.decimal128.decimal.Decimal.Companion.decimalFinite
import kotlin.math.min

internal fun nextUpOrDown(isUp: Boolean, x: Decimal, ctx: DecContext): Decimal {
    val stealX = x.steal
    val signX = stealSignFlag(stealX)
    when {
        stealIsFNZ(stealX) -> {
            return if (signX == isUp)
                nextFnzTowardZero(x, ctx)
            else
                nextFnzAwayFromZero(x, ctx)
        }
        stealIsZER(stealX) -> return minFiniteMagnitude(sign = !isUp, ctx)
        stealIsINF(stealX) -> {
            return if (signX == isUp)
                maxFiniteMagnitude(signX, ctx)
            else
                x
        }
        stealIsQNAN(stealX) -> return x
        else -> {
            verify { stealIsSNAN(stealX) }
            return ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, Decimal.qNaN(signX, x.dw1, x.dw0))
        }
    }
}

internal fun nextFnzAwayFromZero(x: Decimal, ctx: DecContext): Decimal {
    verify { x.isFiniteNonZero() }
    val xSteal = x.steal
    var qExp = stealQExp(xSteal)
    var dw1 = x.dw1
    var dw0 = x.dw0
    val headroom = min(ctx.precision - stealDigitLen(xSteal), qExp - Q_TINY)
    val xSignBit = stealSignBit(xSteal)
    if (headroom > 0) {
        val pentad = ctx.tmps.pentad1
        umul128xPow10to128(pentad, dw1, dw0, headroom)
        dw1 = pentad.dw1
        dw0 = pentad.dw0
        qExp -= headroom
    }
    ++dw0
    dw1 += if (dw0 == 0L) 1L else 0L
    if (dw0 == ctx.decFormat.dw0MaxxCoeff && dw1 == ctx.decFormat.dw1MaxxCoeff) {
        // roll up a decade
        dw1 = ctx.decFormat.dw1MinFullPrecisionCoeff
        dw0 = ctx.decFormat.dw0MinFullPrecisionCoeff
        ++qExp
        if (qExp > Q_MAX)
            return Decimal.infinity(xSignBit)
    }
    return decimalFNZ(xSignBit, qExp, dw1, dw0)
}

internal fun nextFnzTowardZero(x: Decimal, ctx: DecContext): Decimal {
    verify { x.isFiniteNonZero() }
    val xSteal = x.steal
    var xQ = stealQExp(xSteal)
    var dw1 = x.dw1
    var dw0 = x.dw0
    val headroom = min(ctx.precision - stealDigitLen(xSteal), xQ - Q_TINY)
    if (headroom > 0) {
        val pentad = ctx.tmps.pentad1
        umul128xPow10to128(pentad, dw1, dw0, headroom)
        dw1 = pentad.dw1
        dw0 = pentad.dw0
        xQ -= headroom
    }
    // Check if we're at a power of 10 boundary (would lose a digit if decremented)
    if (dw0 == ctx.decFormat.dw0MinFullPrecisionCoeff &&
        dw1 == ctx.decFormat.dw1MinFullPrecisionCoeff &&
        xQ > Q_TINY) {
        // Set to 10^precision - 1 and decrement exponent
        dw1 = ctx.decFormat.dw1MaxxCoeff
        dw0 = ctx.decFormat.dw0MaxxCoeff
        --xQ
    }
    dw1 -= if (dw0 == 0L) 1L else 0L
    --dw0
    return decimalFinite(x.signFlag(), xQ, dw1, dw0)
}

internal fun maxFiniteMagnitude(sign: Boolean, ctx: DecContext): Decimal {
    val qExp = Q_MAX
    val dwHi = ctx.decFormat.dw1MaxxCoeff
    val dwLo = ctx.decFormat.dw0MaxxCoeff - 1L
    return decimalFNZ(if (sign) 1 else 0, qExp, dwHi, dwLo)
}

internal fun minFiniteMagnitude(sign: Boolean, ctx: DecContext): Decimal =
    decimalFNZ(if (sign) 1 else 0, Q_TINY, 0L, 1L)

