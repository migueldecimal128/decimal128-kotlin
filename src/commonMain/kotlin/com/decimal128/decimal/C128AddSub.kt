package com.decimal128.decimal

object C128AddSub {

    fun c128UnscaledAdd(x: Decimal, y: Decimal): Decimal {
        check(x.validate())
        check(y.validate())
        val x0 = x.dw0
        val y0 = y.dw0
        val s0 = x0 + y0
        val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
        val x1 = x.dw1
        val y1 = y.dw1
        val s1 = x1 + y1 + carry0
        val sum = Decimal.from(s1, s0, x.signExp)
        return sum
    }

    fun c128UnscaledSub(x: Decimal, y: Decimal): Decimal {
        check(x.validate())
        check(y.validate())
        check(x.bitLen >= y.bitLen)

        val d0 = x.dw0 - y.dw0
        val carry0 = if (unsignedCmp(d0, x.dw0) > 0) 1L else 0L
        val d1 = x.dw1 - y.dw1 - carry0
        val diff = Decimal.from(d1, d0, x.signExp)
        return diff
    }

}
