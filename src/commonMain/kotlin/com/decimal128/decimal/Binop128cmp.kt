package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.NEG_ONE
import com.decimal128.decimal.Decimal.Companion.NaN
import com.decimal128.decimal.Decimal.Companion.POS_ONE
import com.decimal128.decimal.Decimal.Companion.ZERO
import com.decimal128.decimal.Decimal.Companion.bothFnz
import com.decimal128.decimal.Decimal.Companion.hasNaN

class Binop128cmp : Binop() {


    companion object {

        private val mapToDecimal: Array<Decimal> =
            arrayOf(NEG_ONE, ZERO, POS_ONE, NaN)

        /*
        fun cmpImpl(x: Decimal, y: Decimal, env: DecEnv): Int {
            return if (bothFnz(x, y)) {
                cmpFnzFnz(x, y, env)
            } else when (BinopSignature.enumOf(x, y)) {
                ZER_ZER -> cmpZerZer(x, y, env)
                ZER_FNZ -> cmpZerFnz(x, y, env)
                ZER_INF -> cmpZerInf(x, y, env)

                FNZ_ZER -> cmpFnzZer(x, y, env)
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> cmpFnzInf(x, y, env)

                INF_ZER -> cmpInfZer(x, y, env)
                INF_FNZ -> cmpInfFnz(x, y, env)
                INF_INF -> cmpInfInf(x, y, env)

                NAN_FOUND -> cmpNanFound(x, y, env)
            }
        }

         */

        fun cmpImpl(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            if (hasNaN(x, y))
                return cmpNanFound(x, y, env)
            val binopSig = BinopSignature.enumOf(x, y)
            if (binopSig == ZER_ZER)
                return ZERO
            if (x.sign != y.sign)
                return if (x.sign) NEG_ONE else POS_ONE
            val cmpMag =
                if (bothFnz(x, y)) {
                    cmpFnzFnz(x, y)
                } else when (binopSig) {
                    ZER_ZER -> throw IllegalStateException()
                    ZER_FNZ -> -1
                    ZER_INF -> -1

                    FNZ_ZER -> 1
                    FNZ_FNZ -> throw IllegalStateException()
                    FNZ_INF -> -1

                    INF_ZER -> 1
                    INF_FNZ -> 1
                    INF_INF -> 0

                    NAN_FOUND -> throw IllegalStateException()
                }
            val negateMask = x.sign0Neg1
            val t = (cmpMag xor negateMask) - negateMask
            return mapToDecimal[t + 1]
        }

        fun cmpMagnitudeImpl(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            val cmp =  if (bothFnz(x, y)) {
                cmpMagnitudeFnzFnz(x, y)
            } else when (BinopSignature.enumOf(x, y)) {
                ZER_ZER -> 0
                ZER_FNZ -> -1
                ZER_INF -> -1

                FNZ_ZER -> 1
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> -1

                INF_ZER -> 1
                INF_FNZ -> 1
                INF_INF -> 0

                NAN_FOUND -> return cmpMagnitudeNanFound(x, y, env)
            }
            return mapToDecimal[cmp + 1]
        }

        private fun cmpFnzFnz(x: Decimal, y: Decimal): Int {
            if (x.sign != y.sign)
                return if (x.sign) -1 else 1
            val negateMask = x.sign0Neg1 // 0 or -1
            return (cmpMagnitudeFnzFnz(x, y) xor negateMask) - negateMask
        }

        private fun cmpMagnitudeFnzFnz(x: Decimal, y: Decimal) : Int {
            if (x.qExp == y.qExp)
                return ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
            val cmpSci = x.sciExp.compareTo(y.sciExp)
            if (cmpSci != 0)
                return cmpSci
            val qDelta = x.qExp - y.qExp
            val qDeltaAbs = kotlin.math.abs(qDelta)
            val pow10BitLen = U256Pow10.pow10BitLen(qDeltaAbs)
            val pow10Offset = U256Pow10.pow10Offset(qDeltaAbs)
            val dw0Pow10 = POW10[pow10Offset]
            val dw1Pow10 = POW10[pow10Offset + 1]
            if (qDelta > 0) {
                // x.qExp is larger
                // scale up x.coefficient
                if (pow10BitLen <= 64)
                    return -ucmp128_128x64(y.dw1, y.dw0, x.dw1, x.dw0, dw0Pow10)
                return -ucmp128_128x64(y.dw1, y.dw0, dw1Pow10, dw0Pow10, x.dw0)
            } else {
                // scale up y
                if (pow10BitLen <= 64)
                    return ucmp128_128x64(x.dw1, x.dw0, y.dw1, y.dw0, dw0Pow10)
                return ucmp128_128x64(x.dw1, x.dw0, dw1Pow10, dw0Pow10, y.dw0)
            }
        }

        fun cmpMagnitudeNanFound(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            TODO()
        }

        fun cmpNanFound(x: Decimal, y: Decimal, env: DecEnv): Decimal {
            TODO()
        }

        fun cmpTotalOrderImpl(x: Decimal, y: Decimal, env: DecEnv): Int {
            if (x.sign != y.sign)
                return if (x.sign) -1 else 1
            val negateMask = -x.sign01 // 0 or -1
            return (cmpTotalOrderMagnitudeImpl(x, y, env) xor negateMask) - negateMask
        }

        fun cmpTotalOrderMagnitudeImpl(x: Decimal, y: Decimal, env: DecEnv): Int {
            return if (bothFnz(x, y)) {
                cmpTotalOrderMagnitudeFnzFnz(x, y)
            } else when (BinopSignature.enumOf(x, y)) {
                ZER_ZER -> x.qExp.compareTo(y.qExp)
                ZER_FNZ -> -1
                ZER_INF -> -1

                FNZ_ZER -> 1
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> -1

                INF_ZER -> 1
                INF_FNZ -> 1
                INF_INF -> 0
                NAN_FOUND -> cmpTotalOrderMagnitudeNanFound(x, y)
            }
        }

        private fun cmpTotalOrderMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
            val cmp = cmpMagnitudeFnzFnz(x, y)
            if (cmp != 0)
                return cmp
            // If x and y represent the same floating-point datum:
            //  i) If x and y have negative sign,
            //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
            //  ii) otherwise,
            //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.
            return x.qExp.compareTo(y.qExp)
        }

        private fun cmpTotalOrderMagnitudeNanFound(x: Decimal, y: Decimal): Int {
            return when {
                x.qExp < NON_FINITE_QNAN -> -1
                y.qExp < NON_FINITE_QNAN -> 1
                // if both are the same NaN, then compare payloads
                x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
                // strange ... sNaN sorts less than qNaN
                // TODO ... should I consider swapping qNaN and sNaN qExp values?
                x.qExp == NON_FINITE_QNAN -> -1
                else -> 1
            }
        }

    }
}