package com.decimal128.decimal

internal fun stripTrailingZeros(x: Decimal, ctx: DecContext): Decimal {
    val qX = x.qExp
    return when {
        qX < NON_FINITE_INF -> {
            // FIXME - I have a general aliasing problem
            //  mdecArg1 is being used at a lower level for
            //  MutDec.setStripTrailingZeros()
            //  I guess I need a more general solution ...
            //  like having a set of tmps reserved for each layer?
            val t = ctx.decTemps.mdecArg2.set(x)
            val r = ctx.decTemps.mdecResult.setStripTrailingZeros(t, ctx)
            Decimal.from(r)
        }
        qX == NON_FINITE_INF -> x
        else -> nanOperandFound(x, ctx)
    }
}
