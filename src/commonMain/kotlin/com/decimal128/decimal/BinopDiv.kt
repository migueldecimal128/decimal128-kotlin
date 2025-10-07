package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.bothFnz
import com.decimal128.decimal.Decimal.Companion.newZero

class BinopDiv : Binop() {
    companion object {

        fun divImpl(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            return if (bothFnz(x, y)) {
                divFnzFnz(x, y, env)
            } else when (BinopSignature.enumOf(x, y)) {
                ZER_ZER -> divZeroZero(x, y, env)
                ZER_FNZ -> newZero(x.sign xor y.sign, x.qExp - y.qExp)
                ZER_INF -> newZero(x.sign xor y.sign, env.eMin)

                FNZ_ZER -> if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> newZero(x.sign xor y.sign, env.eMin)

                INF_ZER -> newZero(x.sign xor y.sign, env.eMax)
                INF_FNZ -> newZero(x.sign xor y.sign, env.eMax)
                INF_INF -> divInfInf(x, y, env)

                NAN_FOUND -> nanFound(x, y, env)
            }
        }

        private fun divZeroZero(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal =
            decEnv.signal(DecExceptionReason.DIVISION_OF_ZERO_BY_ZERO)

        private fun divInfInf(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal =
            decEnv.signal(DecExceptionReason.DIVISION_OF_INFINITY_BY_INFINITY)

        private fun divFnzFnz(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            val dividend = env.decTemps.mdecArg1.set(x)
            val divisor = env.decTemps.mdecArg2.set(y)
            val quotient = env.decTemps.mutDecResult.setDiv(dividend, divisor, env)
            return Decimal.from(quotient)
        }
    }
}