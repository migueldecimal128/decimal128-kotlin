package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.DecOld.Companion.bothFnz
import kotlin.math.min

class BinopMul : Binop() {
    companion object {

        fun mulImpl(x: DecOld, y: DecOld, env: DecEnv): DecOld {
            return if (bothFnz(x, y)) {
                mulFnzFnz(x, y, env)
            } else when (BinopSignature.of(x, y)) {
                ZER_ZER -> mulZero(x, y, env)
                ZER_FNZ -> mulZero(x, y, env)
                ZER_INF -> mulInfZero(x, y, env)

                FNZ_ZER -> mulZero(x, y, env)
                FNZ_FNZ -> mulFnzFnz(x, y, env)
                FNZ_INF -> mulInfNonzero(x, y, env)

                INF_ZER -> mulInfZero(x, y, env)
                INF_FNZ -> mulInfNonzero(x, y, env)
                INF_INF -> mulInfNonzero(x, y, env)

                NAN_FOUND -> nanFound(x, y, env)
            }
        }

        private fun mulZero(x: DecOld, y: DecOld, env: DecEnv): DecOld =
            DecOld.newZero(false, min(x.qExp, y.qExp), env)

        private fun mulInfZero(x: DecOld, y: DecOld, env: DecEnv): DecOld =
            env.signal(DecExceptionReason.MULTIPLICATION_OF_ZERO_BY_INFINITY)

        private fun mulInfNonzero(x: DecOld, y: DecOld, env: DecEnv): DecOld =
            if (x.sign xor y.sign) DecOld.POS_INFINITY else DecOld.NEG_INFINITY

        // fast-path iff ...
        //  product bitLen strictly less than decFormat.maxBitLen
        //  (equal bitLen could overflow coefficient decimal limit)
        //
        //  exponent on the upper end is easy, must be < qMax
        //  exponent on the low end must be >= eMin, not qTiny
        //  anything in the range [qTiny, eMin) is subnormal
        //  and must be scaled, so not on the fast-path
        private fun mulFnzFnz(x: DecOld, y: DecOld, env: DecEnv): DecOld {
            val prodBitLen = x.bitLen + y.bitLen
            val prodExp = x.qExp + y.qExp
            if (prodBitLen < env.maxBitLen && prodExp >= env.eMin && prodExp <= env.qMax) {
                val p0 = x.dw0 * y.dw0
                val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
                val prodSign = x.sign xor y.sign
                val d = DecOld(prodSign, p1, p0, prodExp)
                return d
            }
            return mulFnzFnz256(x, y, env)
        }

        private fun mulFnzFnz256(x: DecOld, y: DecOld, env: DecEnv): DecOld {
            val p = env.decTemps.mdecArg1.set(x)
            val n = env.decTemps.mdecArg2.set(y)
            p.setMul(p, n, env)
            val d = DecOld.from(p)
            return d
        }



    }
}