package com.decimal128.decimal

import kotlin.math.min

abstract class Binop {
    companion object {
        fun nanFound(x: Decimal, y: Decimal, env: env): Decimal {
            TODO()
        }

        internal fun scaleToMinExp(x: Decimal, otherExp: Int, env: env): Decimal {
            if (x.qExp <= otherExp)
                return x
            val delta = x.qExp - otherExp
            check(delta > 0)
            val headroom = env.precision - x.digitLen
            if (headroom == 0)
                return x
            val shiftLeft = min(headroom, delta)
            return D128Pow10.scaleCoeffUpPow10(x, shiftLeft)
        }

    }
}