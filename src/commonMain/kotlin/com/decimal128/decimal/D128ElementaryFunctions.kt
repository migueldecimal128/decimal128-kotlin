package com.decimal128.decimal

import com.decimal128.decimal.InvalidOperationReason.LOG_OF_NEG_NUMBER

internal fun d128LogImpl(x: Decimal, isLog10: Boolean): Decimal {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> when {
            stealSignFlag(xSteal) ->
                return signalInvalidOperation(LOG_OF_NEG_NUMBER)
            else -> return logImplFNZ(x, isLog10)
        }

        STEAL_TYP_ZER -> return signalDivByZero(Decimal.NEG_INFINITY)
        STEAL_TYP_INF -> {
            return if (stealSignFlag(xSteal))
                signalInvalidOperation(LOG_OF_NEG_NUMBER)
            else
                Decimal.POS_INFINITY
        }

        else -> return nanOperandFound(x)
    }
}

private fun logImplFNZ(x: Decimal, isLog10: Boolean): Decimal {
    val ctx = DecContext.current()
    val tmps = ctx.tmps
    val mutDec = tmps.mdecBridge1.set(x)
    val result = logImplFNZ(tmps.mdecBridgeResult, mutDec, isLog10, ctx)
    return Decimal.from(result)
}

internal fun d128ExpImpl(x: Decimal, isExp10: Boolean): Decimal {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return d128ExpFNZ(x, isExp10)
        STEAL_TYP_ZER -> return Decimal.ONE
        STEAL_TYP_INF -> when {
            stealSignFlag(xSteal) -> return Decimal.ZERO
            else -> return Decimal.POS_INFINITY
        }
        else -> return nanOperandFound(x)
    }
}

private fun d128ExpFNZ(x: Decimal, isExp10: Boolean): Decimal {
    val ctx = DecContext.current()
    val tmps = ctx.tmps
    val mutDec = tmps.mdecBridge1.set(x)
    val result = tmps.mdecBridgeResult
    if (isExp10)
        exp10ImplFNZ(result, mutDec, ctx)
    else
        expImplFNZ(result, mutDec, ctx)
    return Decimal.from(result)
}