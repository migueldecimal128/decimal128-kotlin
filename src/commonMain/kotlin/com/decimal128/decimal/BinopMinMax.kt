package com.decimal128.decimal

internal fun maxImpl(x: Decimal, y: Decimal): Decimal =
    maxImpl(x, y, DecContext.current())

internal fun maxImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (Decimal.neitherIsNaN(x, y))
        return if (cmpTotalOrderImpl(x, y, ctx) >= 0) x else y
    return nanOperandFound(x, y, ctx)
}

internal fun maxNumImpl(x: Decimal, y: Decimal): Decimal =
    maxNumImpl(x, y, DecContext.current())

internal fun maxNumImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (Decimal.neitherIsNaN(x, y))
        return if (cmpTotalOrderImpl(x, y, ctx) >= 0) x else y
    if (!x.isSignaling() && !y.isSignaling()) {
        if (!x.isNaN())
            return x
        if (!y.isNaN())
            return y
    }
    return nanOperandFound(x, y, ctx)
}

internal fun minImpl(x: Decimal, y: Decimal): Decimal =
    minImpl(x, y, DecContext.current())

internal fun minImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (Decimal.neitherIsNaN(x, y))
        return if (cmpTotalOrderImpl(x, y, ctx) <= 0) x else y
    return nanOperandFound(x, y, ctx)
}

internal fun minNumImpl(x: Decimal, y: Decimal): Decimal =
    minNumImpl(x, y, DecContext.current())

internal fun minNumImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (Decimal.neitherIsNaN(x, y))
        return if (cmpTotalOrderImpl(x, y, ctx) <= 0) x else y
    if (!x.isSignaling() && !y.isSignaling()) {
        if (!x.isNaN())
            return x
        if (!y.isNaN())
            return y
    }
    return nanOperandFound(x, y, ctx)
}



