package com.decimal128.decimal

object D128Mul {

    fun mulImpl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        // FIXME - HACK WARNING
        val prod = x.dw0 * y.dw0
        return Decimal.from(prod)
    }

}