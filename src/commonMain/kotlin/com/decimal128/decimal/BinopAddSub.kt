package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.C128Compare.c128UnscaledCompare
import com.decimal128.decimal.Decimal.Companion.bothFnz
import kotlin.math.min

class BinopAddSub : Binop() {
    companion object {

        fun addImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
            return if (bothFnz(x, y)) {
                addFnzFnz(x, y.sign, y, env)
            } else when (BinopSignature.of(x, y)) {
                ZER_ZER -> addZeroZero(x, y.sign, y, env)
                ZER_FNZ -> scaleToMinExp(y, x.qExp, env)
                ZER_INF -> y

                FNZ_ZER -> scaleToMinExp(x, y.qExp, env)
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> y

                INF_ZER -> x
                INF_FNZ -> x
                INF_INF -> addInfInf(x, y.sign, y, env)

                NAN_FOUND -> nanFound(x, y, env)
            }
        }

        fun subImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
            return if (bothFnz(x, y)) {
                addFnzFnz(x, !y.sign, y, env)
            } else when (BinopSignature.of(x, y)) {
                ZER_ZER -> addZeroZero(x, !y.sign, y, env)
                ZER_FNZ -> y.negate()
                ZER_INF -> y.negate()

                FNZ_ZER -> x
                FNZ_FNZ -> throw IllegalStateException()
                FNZ_INF -> y.negate()

                INF_ZER -> x
                INF_FNZ -> x
                INF_INF -> addInfInf(x, !y.sign, y, env)

                NAN_FOUND -> nanFound(x, y, env)
            }
        }

        private fun addZeroZero(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
            // Both operands are zero. This is where the special rules apply.
            if (x.sign == ySign) {
                // Rule: x + x = x. Preserves the sign of zero. (-0) + (-0) = -0.
                return if (x.qExp <= y.qExp) x else y // return min qExp
            }
            // Rule: (+0) + (-0). The signs are different.
            // Result is +0 unless rounding is roundTowardNegative.
            val isRoundTowardNegative = env.isRoundTowardNegative()
            var qMin = x.qExp
            if (x.qExp <= y.qExp) {
                if (x.sign == isRoundTowardNegative)
                    return x
            } else {
                if (y.sign == isRoundTowardNegative)
                    return y
                qMin = y.qExp
            }
            return Decimal.newZero(env.isRoundTowardNegative(), qMin, env)
        }

        private fun addInfInf(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal =
            if (x.sign == ySign)
                x
            else
                env.signal(DecExceptionReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)

        private fun addFnzFnz(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext) =
            if (x.qExp == y.qExp)
                addFnzFnzUnscaled(x, ySign, y, env)
            else
                addFnzFnzScaled(x, ySign, y, env)

        private fun addFnzFnzUnscaled(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
            if (x.sign == ySign)
                return addUnscaledMagnitudes(x, y, env)
            val cmp = c128UnscaledCompare(x, y)
            return when {
                (cmp > 0) -> C128AddSub.c128UnscaledSub(x.sign, x, y)
                (cmp < 0) -> C128AddSub.c128UnscaledSub(ySign, y, x)
                else -> Decimal.newZero(env.isRoundTowardNegative(), x.qExp, env)
            }
        }

        private fun addUnscaledMagnitudes(x: Decimal, y: Decimal, env: DecContext): Decimal {
            val sumBitLen = x.bitLen + y.bitLen + 1
            val sum = if (sumBitLen < env.decFormat.maxBitLen) {
                val x0 = x.dw0
                val y0 = y.dw0
                val s0 = x0 + y0
                val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
                val x1 = x.dw1
                val y1 = y.dw1
                val s1 = x1 + y1 + carry0
                Decimal.from(s1, s0, x.signExp)
            } else {
                val arg1 = env.decTemps.mdecArg1.set(x)
                val arg2 = env.decTemps.mdecArg2.set(y)
                val mdecSum = env.decTemps.mutDecResult.setAdd(arg1, arg2, env)
                Decimal.from(mdecSum)
            }
            return sum
        }

        private fun addFnzFnzScaled(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
            if (x.sign == y.sign)
                return addScaledMagnitudes(x, y, env)
            // signs differ ... subtract scaled magnitudes
            val cmpMag = x.magnitudeCompareTo(y)
            return when {
                cmpMag > 0 -> subScaledMagnitudes(x.sign, x, y, env)
                cmpMag < 0 -> subScaledMagnitudes(ySign, y, x, env)
                else -> Decimal.newZero(x.sign && ySign, min(x.qExp, y.qExp), env)
            }
        }

        private fun addScaledMagnitudes(x: Decimal, y: Decimal, env: DecContext): Decimal {
            val flip = x.qExp > y.qExp
            val m = if (flip) x else y
            val n = if (flip) y else x
            val qDelta = m.qExp - n.qExp
            verify { qDelta >= 0 }
            val headroom = env.precision - m.digitLen
            return if (qDelta <= headroom) {
                // we can resolve this in our D128 world
                val shiftLeft = min(qDelta, headroom)
                D128Pow10.fmaCoeffPow10(m, shiftLeft, n)
            } else {
                fullWidthAdd(x.sign, x, y.sign, y, env)
            }
        }

        private fun fullWidthAdd(xSign: Boolean, x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
            val arg1 = env.decTemps.mdecArg1.set(x)
            arg1.sign = xSign
            val arg2 = env.decTemps.mdecArg2.set(y)
            arg2.sign = ySign
            val mdecSum = env.decTemps.mutDecResult.setAdd(arg1, arg2, env)
            val sum = Decimal.from(mdecSum)
            return sum
        }

        private fun subScaledMagnitudes(sign: Boolean, m: Decimal, s: Decimal, env: DecContext): Decimal {
            // non-zero with different signs ... subtract magnitudes
            verify { m.magnitudeCompareTo(s) > 0 }
            verify { s.isNotZero() }
            verify { m.qExp != s.qExp }
            if (m.qExp < s.qExp) {
                // TC("22E1", "-2E2"),
                // signs opposite, |m| > |s|, but m.qExp < s.qExp
                // scale s before subtraction
                val qDelta = s.qExp - m.qExp
                verify { qDelta < PRECISION_34 }
                return D128Pow10.fusedSubtractMulPow10(sign, m, s, qDelta)
            } else {
                // |m| > |s| && m.qExp > s.qExp
                val headroom = env.precision - m.digitLen
                val qDelta = m.qExp - s.qExp
                if (headroom >= qDelta) {
                    // 12E3, -4
                    // m has enough headroom to scale and align with s.qExp
                    return D128Pow10.fusedMulPow10Subtract(sign, m, qDelta, s)
                }
                val qAlign = m.qExp - headroom
                val shiftRight = qAlign - s.qExp
                if (shiftRight >= s.digitLen) {
                    // s is fully swamped
                    // this becomes a rounding/residue problem
                    // FIXME
                    //  This requires residue and rounding in the D128 world
                    //  I'm not ready to tackle that yet
                }

            }
            return fullWidthAdd(sign, m, !sign, s, env)
        }

    }
}