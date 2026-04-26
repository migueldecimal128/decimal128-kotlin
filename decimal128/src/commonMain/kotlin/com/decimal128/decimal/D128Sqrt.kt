package com.decimal128.decimal

internal fun d128SqrtImpl(x: Decimal, ctx: DecContext = DecContext.current()): Decimal {
    val xSteal = x.steal
    val xQ = stealQExp(xSteal)
    val xSign = stealSignFlag(xSteal)
    when (stealTyp(x.steal)) {
        STEAL_TYP_FNZ -> {
            if (!xSign) {
                val tmps = ctx.tmps
                return Decimal.from(
                    mutDecSqrtPosFnz(tmps.mdecBridgeResult,
                        tmps.mdecBridge1.set(x),
                        ctx,
                        reduceToPreferredQExp = true)
                )
            } else {
                return signalInvalidOperation(InvalidOperationReason.SQUARE_ROOT_OF_NEG_FINITE_NON_ZERO)
            }
        }

        STEAL_TYP_ZER -> {
            // IEEE754-2019 6.3 p.50
            // Except that squareRoot(−0) shall be −0,
            // every numeric squareRoot result shall have a positive sign.
            return Decimal.zero(false, xQ shr 1)
        }

        STEAL_TYP_INF -> {
            if (!xSign) {
                return Decimal.POS_INFINITY
            } else {
                return signalInvalidOperation(InvalidOperationReason.SQUARE_ROOT_OF_NEG_INFINITY)
            }
        }

        else -> return nanOperandFound(x, ctx)
    }
}
