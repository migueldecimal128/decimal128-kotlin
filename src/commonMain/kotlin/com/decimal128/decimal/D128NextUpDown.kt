package com.decimal128.decimal

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
            return ctx.signalInvalid(Decimal.qNaN(signX, x.dw1, x.dw0))
        }
    }
}

internal fun nextFnzAwayFromZero(x: Decimal, ctx: DecContext): Decimal {
    verify { x.isFiniteNonZero() }
    val xSteal = x.steal
    var qExp = stealQexp(xSteal)
    var dw1 = x.dw1
    var dw0 = x.dw0
    val headroom = min(ctx.precision - stealDigitLen(xSteal), qExp - ctx.qTiny)
    val xSign = stealSignFlag(xSteal)
    if (headroom > 0) {
        val (dw1T, dw0T) = umul128xPow10to128(dw1, dw0, headroom)
        dw1 = dw1T
        dw0 = dw0T
        qExp -= headroom
    }
    ++dw0
    dw1 += if (dw0 == 0L) 1L else 0L
    if (dw0 == ctx.decFormat.dw0MaxxCoeff && dw1 == ctx.decFormat.dw1MaxxCoeff) {
        // roll up a decade
        dw1 = ctx.decFormat.dw1MinFullPrecisionCoeff
        dw0 = ctx.decFormat.dw0MinFullPrecisionCoeff
        ++qExp
        if (qExp > ctx.qMax)
            return Decimal.infinity(xSign)
    }
    return Decimal(xSign, qExp, dw1, dw0)
}

internal fun nextFnzTowardZero(x: Decimal, ctx: DecContext): Decimal {
    verify { x.isFiniteNonZero() }
    val xSteal = x.steal
    var xQ = stealQexp(xSteal)
    var dw1 = x.dw1
    var dw0 = x.dw0
    val headroom = min(ctx.precision - stealDigitLen(xSteal), xQ - ctx.qTiny)
    if (headroom > 0) {
        val (dw1T, dw0T) = umul128xPow10to128(dw1, dw0, headroom)
        dw1 = dw1T
        dw0 = dw0T
        xQ -= headroom
    }
    // Check if we're at a power of 10 boundary (would lose a digit if decremented)
    if (dw0 == ctx.decFormat.dw0MinFullPrecisionCoeff &&
        dw1 == ctx.decFormat.dw1MinFullPrecisionCoeff &&
        xQ > ctx.qTiny) {
        // Set to 10^precision - 1 and decrement exponent
        dw1 = ctx.decFormat.dw1MaxxCoeff
        dw0 = ctx.decFormat.dw0MaxxCoeff
        --xQ
    }
    dw1 -= if (dw0 == 0L) 1L else 0L
    --dw0
    return Decimal(x.sign, xQ, dw1, dw0)
}

fun maxFiniteMagnitude(sign: Boolean, ctx: DecContext): Decimal {
    val qExp = ctx.qMax
    val dwHi = ctx.decFormat.dw1MaxxCoeff
    val dwLo = ctx.decFormat.dw0MaxxCoeff - 1L
    return Decimal(sign, qExp, dwHi, dwLo)
}

fun minFiniteMagnitude(sign: Boolean, ctx: DecContext): Decimal =
    Decimal(sign, ctx.qTiny, 0L, 1L)

