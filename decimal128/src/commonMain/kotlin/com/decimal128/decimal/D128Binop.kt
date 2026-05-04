// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.min

internal fun nanOperandFound(x: Decimal, y: Decimal): Decimal =
    nanOperandFound(x, y, DecContext.current(), alwaysSignal = false)

internal fun nanOperandFound(x: Decimal, y: Decimal,
                               ctx: DecContext, alwaysSignal: Boolean = false): Decimal {
    val stealX = x.steal
    val stealY = y.steal
    verify { stealHasNAN(stealX, stealY) }
    val preferSNAN = ctx.arithmeticPrefs.propagatePreferSNAN
    val takeY = !stealIsNAN(stealX) || (preferSNAN && stealIsSNAN(stealY) && !stealIsSNAN(stealX))
    val theNaN: Decimal
    val stealNaN: Int
    if (takeY) {
        theNaN = y
        stealNaN = stealY
    } else {
        theNaN = x
        stealNaN = stealX
    }
    verify { stealIsNAN(stealNaN) }
    val isSignaling = stealIsSNAN(stealX) or stealIsSNAN(stealY)
    if (!alwaysSignal && !isSignaling)
        return theNaN
    if (!isSignaling)
        return ctx.signalInvalidOperation(InvalidCause.NAN_OPERAND, theNaN)
    return ctx.signalSNanOperandFound(theNaN)
}

internal fun nanOperandFound(x: Decimal): Decimal =
    nanOperandFound(x, DecContext.current())

internal fun nanOperandFound(x: Decimal, ctx: DecContext): Decimal {
    val steal = x.steal
    verify { stealIsNAN(steal) }
    if (stealIsQNAN(steal))
        return x
    return ctx.signalSNanOperandFound(x)
}

internal fun scaleToMinQExp(xSteal: Int, x: Decimal, otherExp: Int, ctx: DecContext): Decimal {
    val xQ = stealQExp(xSteal)
    if (xQ <= otherExp)
        return if (x.steal == xSteal) x else x.negate()
    val delta = xQ - otherExp
    verify { delta > 0 }
    val headroom = ctx.precision - stealDigitLen(xSteal)
    if (headroom == 0)
        return if (x.steal == xSteal) x else x.negate()
    val shiftLeft = min(headroom, delta)
    return d128ScaleCoeffUpPow10(stealSignFlag(xSteal), x, shiftLeft)
}
