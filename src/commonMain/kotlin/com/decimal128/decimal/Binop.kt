package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun nanOperandFound(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xQ = x.qExp
    val yQ = y.qExp
    val maxQ = max(xQ, yQ)
    verify { maxQ >= NON_FINITE_QNAN }
    val theNaN = if (maxQ == xQ) x else y
    if (maxQ == NON_FINITE_QNAN)
        return theNaN
    val quietedNaN = Decimal.qNaN(theNaN.sign, theNaN.dw1, theNaN.dw0)
    return ctx.signalInvalid(quietedNaN)
}

internal fun scaleToMinExp(x: Decimal, otherExp: Int, env: DecContext): Decimal {
    if (x.qExp <= otherExp)
        return x
    val delta = x.qExp - otherExp
    verify { delta > 0 }
    val headroom = env.precision - x.digitLen
    if (headroom == 0)
        return x
    val shiftLeft = min(headroom, delta)
    return D128Pow10.scaleCoeffUpPow10(x, shiftLeft)
}
