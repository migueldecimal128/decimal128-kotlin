package com.decimal128.decimal

import kotlin.math.min

abstract class Binop {
    companion object {
        fun nanFound(x: DecOld, y: DecOld, env: DecEnv): DecOld {
            TODO()
        }

        internal fun scaleToMinExp(x: DecOld, otherExp: Int, env: DecEnv): DecOld {
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