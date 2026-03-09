package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun nanOperandFound_y(x: Decimal, y: Decimal,
                             ctx: DecContext, alwaysSignal: Boolean = false): Decimal {
    val xQ = x.qExp
    val yQ = y.qExp
    val maxQ = max(xQ, yQ)
    verify { maxQ >= NON_FINITE_QNAN }
    val theNaN =
        if (ctx.decPrefs.propagatePreferSnan) {
            if (maxQ == xQ) x else y
        } else {
            if (xQ >= NON_FINITE_QNAN) x else y
        }
    verify { theNaN.qExp >= NON_FINITE_QNAN }
    if (!alwaysSignal && maxQ == NON_FINITE_QNAN)
        return theNaN
    if (theNaN.qExp == NON_FINITE_QNAN)
        return ctx.signalInvalid(theNaN)
    val quietedNaN = Decimal.qNaN(theNaN.sign, theNaN.dw1, theNaN.dw0)
    return ctx.signalInvalid(quietedNaN)
}

internal fun nanOperandFound(x: Decimal, y: Decimal,
                               ctx: DecContext, alwaysSignal: Boolean = false): Decimal {
    val stealX = x.steal
    val stealY = y.steal
    verify { stealHasNAN(stealX, stealY) }
    val preferSnan = ctx.decPrefs.propagatePreferSnan
    val takeY = !stealIsNAN(stealX) || (preferSnan && stealIsSNAN(stealY) && !stealIsSNAN(stealX))
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
    if (!theNaN.isSignaling())
        return ctx.signalInvalid(theNaN)
    val quietedNaN = Decimal.qNaN(stealSignFlag(stealNaN), theNaN.dw1, theNaN.dw0)
    return ctx.signalInvalid(quietedNaN)
}

internal fun nanOperandFound(x: Decimal, ctx: DecContext): Decimal {
    val steal = x.steal
    verify { stealIsNAN(steal) }
    if (stealIsQNAN(steal))
        return x
    val quietedNaN = Decimal.qNaN(stealSignFlag(steal), x.dw1, x.dw0)
    return ctx.signalInvalid(quietedNaN)
}

internal fun scaleToMinExp(xSign: Boolean, x: Decimal, otherExp: Int, ctx: DecContext): Decimal {
    if (x.qExp <= otherExp)
        return if (x.sign == xSign) x else x.negate()
    val delta = x.qExp - otherExp
    verify { delta > 0 }
    val headroom = ctx.precision - x.digitLen
    if (headroom == 0)
        return if (xSign == x.sign) x else x.negate()
    val shiftLeft = min(headroom, delta)
    return d128ScaleCoeffUpPow10(xSign, x, shiftLeft)
}
