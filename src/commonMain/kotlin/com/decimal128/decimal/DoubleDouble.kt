// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.abs

expect fun mathFma(a: Double, b: Double, c: Double): Double

private fun mathUlp(x: Double): Double {
    val exp = ((x.toBits() ushr 52) and 0x7FFL).toInt()
    return when (exp) {
        0x7FF -> kotlin.math.abs(x)          // NaN or Infinity — matches Java
        0     -> Double.MIN_VALUE             // zero or subnormal
        else  -> {
            val e = exp - 52                  // SIGNIFICAND_WIDTH - 1 = 52
            if (e >= -1022) {                 // Double.MIN_EXPONENT = -1022
                Double.fromBits(e.toLong() shl 52)
            } else {
                Double.fromBits(1L shl (e + 1074))  // 1074 = 1022 + 52
            }
        }
    }
}

class DoubleDouble(a: Double, b: Double) {
    constructor() : this(0.0, 0.0)
    var hi = a
    var lo = b

    fun set(other: DoubleDouble) {
        hi = other.hi
        lo = other.lo
    }

    companion object {
        fun newQuickTwoSum(a: Double, b:Double): DoubleDouble {
            val hi = a + b
            val lo = b - (hi - a)
            return DoubleDouble(hi, lo)
        }

        fun newTwoSum(a: Double, b:Double): DoubleDouble {
            val hi = a + b
            val z = hi - a
            val lo = (a - (hi - z)) + (b - z)
            return DoubleDouble(hi, lo)
        }

        fun newAdd(x: DoubleDouble, y:DoubleDouble): DoubleDouble {
            val s = newTwoSum(x.hi, y.hi)
            val t = newTwoSum(x.lo, y.lo)
            val sum = newTwoSum(s.hi, s.lo + t.hi)
            val lo0 = t.lo + sum.lo
            return newQuickTwoSum(sum.hi, lo0)
        }

        fun newSub(x: DoubleDouble, y:DoubleDouble): DoubleDouble {
            val s = newTwoSum(x.hi, -y.hi)
            val t = newTwoSum(x.lo, -y.lo)
            val sum = newTwoSum(s.hi, s.lo + t.hi)
            val lo0 = t.lo + sum.lo
            return newQuickTwoSum(sum.hi, lo0)
        }

        fun newTwoProd(a: Double, b: Double): DoubleDouble {
            val p = a * b
            val err = mathFma(a, b, -p)
            return DoubleDouble(p, err)
        }

        fun newMulApprox(x: DoubleDouble, y: DoubleDouble): DoubleDouble {
            //val (p, e) = twoProd(aH, bH)
            val p = x.hi * y.hi
            val e = mathFma(x.hi, y.hi, -p)
            // 2) cross terms
            val cross = (x.hi * y.lo) + (x.lo * y.hi)
            // 3) combine error and cross into low part and renormalize
            return newQuickTwoSum(p, e + cross)
        }

        fun newMulBetter(x: DoubleDouble, y:DoubleDouble): DoubleDouble {
            val dd = DoubleDouble()
            dd.setMulBetter(x, y)
            return dd
        }

        fun newMulDoubleDoubleByDouble(x: DoubleDouble, y:Double): DoubleDouble {
            val hi  = x.hi * y
            val err1 = mathFma(x.hi, y, -hi)  // exactly recovers the rounding error of hi*d
            val err2 = x.lo * y                 // low limb scaled
            val lo  = err1 + err2              // combined low-order bits
            return DoubleDouble(hi, lo)
        }

        fun newSquare(x: Double): DoubleDouble {
            val hi = x * x
            val lo = mathFma(x, x, -hi)
            return DoubleDouble(hi, lo)
        }

        fun newDiv(x: DoubleDouble, y: DoubleDouble): DoubleDouble {
            val q1 = x.hi / y.hi
            val p = newMulDoubleDoubleByDouble(y, q1)
            val r = newSub(x, p)
            val q2 = (r.hi + r.lo) / y.hi
            return newQuickTwoSum(q1, q2)
        }

        fun newDivApprox(x: DoubleDouble, y: DoubleDouble): DoubleDouble {
            val q1 = x.hi / y.hi
            val p = newMulDoubleDoubleByDouble(y, q1)
            val r = newSub(x, p)
            val q2 = (r.hi + r.lo) / y.hi
            return newQuickTwoSum(q1, q2)
        }


    }

    inline fun setQuickTwoSum(a: Double, b: Double) {
        hi = a + b
        lo = b - (hi - a)
    }

    inline fun setTwoSum(a: Double, b:Double) {
        hi = a + b
        val z = hi - a
        lo = (a - (hi - z)) + (b - z)
    }

    inline fun setAdd(x: DoubleDouble, y: DoubleDouble) {
        // 1) twoSum on hi
        val s  = x.hi + y.hi
        val z1 = s - x.hi
        val e1 = (x.hi - (s - z1)) + (y.hi - z1)

        // 2) twoSum on lo
        val t  = x.lo + y.lo
        val z2 = t - x.lo
        val e2 = (x.lo - (t - z2)) + (y.lo - z2)

        // 3) blend high-error with low-sum
        val e1t = e1 + t
        val u    = s + e1t
        val z3   = u - s
        val e3   = (s - (u - z3)) + (e1t - z3)

        setQuickTwoSum(u, e2 + e3)
    }

    inline fun setSub(x: DoubleDouble, y: DoubleDouble) {
        // 1) subtract hi-words
        val p = x.hi - y.hi
        // 2) compute the exact rounding error of that subtraction
        //    (a.hi - b.hi) – p  plus the lo-words
        val err = mathFma(1.0, x.hi, -y.hi - p) + x.lo - y.lo
        // 3) renormalize (quick two-sum)
        val sum = p + err
        this.hi = sum
        this.lo = err - (sum - p)
    }

    inline fun setMulDoubleDoubleByDouble(x: DoubleDouble, y: Double) {
        val hi  = x.hi * y
        val err1 = mathFma(x.hi, y, -hi)  // exactly recovers the rounding error of hi*d
        val err2 = x.lo * y                 // low limb scaled
        val lo  = err1 + err2              // combined low-order bits
        this.hi = hi
        this.lo = lo
    }

    inline fun setMulApprox(x: DoubleDouble, y: DoubleDouble) {
        //val (p, e) = twoProd(aH, bH)
        val p = x.hi * y.hi
        val e = mathFma(x.hi, y.hi, -p)
        // 2) cross terms
        val cross = (x.hi * y.lo) + (x.lo * y.hi)
        // 3) combine error and cross into low part and renormalize
        setQuickTwoSum(p, e + cross)
    }

    inline fun setMulBetter_X(x: DoubleDouble, y: DoubleDouble) {
        // 1) high‐word product + error
        val pH = x.hi * y.hi
        val pL = mathFma(x.hi, y.hi, -pH)

        // 2) exact cross‐terms c1 = aH*bL, c2 = aL*bH
        val c1 = x.hi * y.lo
        val c2 = x.lo * y.hi
        // twoSum(c1, c2) → (s1,e1)
        val s1 = c1 + c2
        val z1 = s1 - c1
        val e1 = (c1 - (s1 - z1)) + (c2 - z1)

        // 3) merge pL + s1 → (s2,e2)
        val s2 = pL + s1
        val z2 = s2 - pL
        val e2 = (pL - (s2 - z2)) + (s1 - z2)

        // 4) low×low split
        val llH = x.lo * y.lo
        val llL = mathFma(x.lo, y.lo, -llH)
        // and merge s2 + llH → (s3,e3)
        val s3 = s2 + llH
        val z3 = s3 - s2
        val e3 = (s2 - (s3 - z3)) + (llH - z3)

        // 5) merge pH + s3 → (hi0,e0)
        val hi0 = pH + s3
        val z4  = hi0 - pH
        val e0  = (pH - (hi0 - z4)) + (s3 - z4)

        // 6) gather all tiny residues
        val lo0 = e0 + e1 + e2 + e3 + llL

        // 7) final QuickTwoSum(hi0, lo0)
        val h = hi0 + lo0
        lo = lo0 - (h - hi0)
        hi = h
    }

    /** this = a * b  in full double-double precision */
    inline fun setMulBetter_Y(a: DoubleDouble, b: DoubleDouble) {
        // 1) hi-word product
        val p     = a.hi * b.hi
        // 2) error of hi×hi
        val e1    = mathFma(a.hi, b.hi, -p)
        // 3) cross-terms
        val e2    = a.hi * b.lo + a.lo * b.hi
        // 4) lo×lo (very tiny, but for completeness)
        val e3    = a.lo * b.lo

        // 5) accumulate e1 + e2
        val s     = e1 + e2
        val z     = s - e1
        val e12   = (e1 - (s - z)) + (e2 - z)

        // 6) add to p → (hi0, e23)
        val hi0   = p + s
        val z2    = hi0 - p
        val e23   = (p - (hi0 - z2)) + (s - z2)

        // 7) total low part = e12 + e23 + e3
        val lo0   = e12 + e23 + e3

        // 8) final renormalize (quick-two-sum)
        val h     = hi0 + lo0
        val l     = lo0 - (h - hi0)

        this.hi = h
        this.lo = l
    }

    inline fun setMulBetter(a: DoubleDouble, b: DoubleDouble) {
        // hi×hi
        val p  = a.hi * b.hi
        // error of hi×hi
        val e1 = mathFma(a.hi, b.hi, -p)
        // cross-terms
        val c1 = a.hi * b.lo
        val c2 = a.lo * b.hi
        val e2 = c1 + c2
        // lo×lo
        val e3 = a.lo * b.lo

        // renormalize e1+e2
        val s  = e1 + e2
        val z  = s - e1
        val e12 = (e1 - (s - z)) + (e2 - z)

        // merge into p
        val hi0 = p + s
        val z2  = hi0 - p
        val e23 = (p - (hi0 - z2)) + (s - z2)

        // total low part
        val lo0 = e12 + e23 + e3

        // final quick-two-sum
        val h   = hi0 + lo0
        val l   = lo0 - (h - hi0)
        /*
        // debug print
        println("""
      setMulBetter debug:
        a = {hi=${a.hi}, lo=${a.lo}}
        b = {hi=${b.hi}, lo=${b.lo}}
        p  = $p
        e1 = $e1
        c1 = $c1, c2 = $c2
        e2 = $e2
        e3 = $e3
        e12= $e12
        e23= $e23
        hi0= $hi0
        lo0= $lo0
        result = {hi=$h, lo=$l}
    """.trimIndent())
        */
        this.hi = h
        this.lo = l
    }

    inline fun setMul(x: DoubleDouble, y: Double) {
        val pH = x.hi * y
        val pL = mathFma(x.hi, y, -pH)
        // combine low word
        setQuickTwoSum(pH, pL + x.lo * y)
    }

    inline fun setDivApprox(x: DoubleDouble, y: DoubleDouble) {
        // 1) initial quotient
        val q1 = x.hi / y.hi

        // 2) form p = y * q1 in double-double form
        //    high-word
        val pH = y.hi * q1
        //    error of y.hi*q1
        val pL = mathFma(y.hi, q1, -pH)
        //    add cross term y.lo * q1
        val pL2 = pL + y.lo * q1

        // 3) form r = x - p
        //    high-word subtraction
        val s1  = x.hi - pH
        val z1  = s1 - x.hi
        val e1  = (x.hi - (s1 - z1)) + (-pH - z1)

        //    low-word subtraction
        val s2  = x.lo - pL2
        val z2  = s2 - x.lo
        val e2  = (x.lo - (s2 - z2)) + (-pL2 - z2)

        //    combine the two errors
        val e1s2 = e1 + s2
        val rH    = s1 + e1s2
        val z3    = rH - s1
        val e3    = (s1 - (rH - z3)) + (e1s2 - z3)
        val rL    = e2 + e3

        // 4) correction term
        val q2 = (rH + rL) / y.hi

        // 5) renormalize into hi/lo
        //    QuickTwoSum(q1, q2)
        setQuickTwoSum(q1, q2)
    }

    inline fun setInvFast(x: DoubleDouble) {
        // 1) first guess q1 = 1/x.hi
        val q1 = 1.0 / x.hi

        // 2) form p = x * q1  (double-word product)
        //    high-word
        val pH = x.hi * q1
        //    error of that product
        val pL = mathFma(x.hi, q1, -pH)
        //    include x.lo*q1 in the remainder
        val remH = 1.0 - pH
        val remL = -(pL + x.lo * q1)

        // 3) correction term q2 = (remH + remL) * q1
        val q2 = (remH + remL) * q1

        // 4) final QuickTwoSum(q1, q2) → in-place hi/lo
        val hi2 = q1 + q2
        lo = q2 - (hi2 - q1)
        hi = hi2
    }

    inline fun mutateInvFast() {
        // 1) first guess q1 = 1/x.hi
        val q1 = 1.0 / hi

        // 2) form p = x * q1  (double-word product)
        //    high-word
        val pH = hi * q1
        //    error of that product
        val pL = mathFma(hi, q1, -pH)
        //    include x.lo*q1 in the remainder
        val remH = 1.0 - pH
        val remL = -(pL + lo * q1)

        // 3) correction term q2 = (remH + remL) * q1
        val q2 = (remH + remL) * q1

        // 4) final QuickTwoSum(q1, q2) → in-place hi/lo
        val hi2 = q1 + q2
        lo = q2 - (hi2 - q1)
        hi = hi2
    }

    fun mutate2x() {
        hi *= 2
        lo *= 2
    }

    fun newReciprocal(a: DoubleDouble): DoubleDouble {
        // 1. Initial approximation of the reciprocal.
        val q1 = 1.0 / a.hi

        // 2. Calculate the residual: r = 1.0 - a * q1
        // We need to compute this with high precision.
        // First, calculate a * q1 using double-double multiplication.
        val p = newTwoProd(a.hi, q1)
        val mult_hi = p.hi
        val mult_lo = p.lo + a.lo * q1

        // Now, subtract this from 1.0 with high precision.
        val r = newTwoSum(1.0, -mult_hi)
        val r_lo = -mult_lo

        // 3. Calculate the second-order term: q2 = r * q1
        val q2 = r.hi * q1

        // 4. Sum the terms to get the final result.
        // The final result is q1 + r*q1
        return newTwoSum(q1, q2)
    }

    /*
    fun setCoeff(c: Coeff) {
        hi = 0.0
        lo = 0.0
        val d = c.coeffToFloorDouble()
        val L     = c.bitLen
        if (L <= 53) {
            if (L == 0)
                return
            hi = c.coeffToFloorDouble()
            return
        }
        val shift = L - 53
        val top53 = abs.shiftRight(shift)
        val m     = top53.toLong() and ((1L shl 53) - 1)
        val exp   = L - 1
        val mant  = m and ((1L shl 52) - 1)
        val bits  = ((if (sign<0)1L else 0L) shl 63) or
                ((exp+1023).toLong() shl 52) or
                mant
        val hi    = Double.fromBits(bits)
        val rem   = abs.subtract(top53.shiftLeft(shift))
        val lo0   = sign * rem.toDouble()
        // final normalization!
        setQuickTwoSum(hi, lo0)
    }
*/
    inline fun compareTo(other: DoubleDouble): Int {
        val cmpHi = hi.compareTo(other.hi)
        if (cmpHi != 0)
            return cmpHi
        val cmpLo = lo.compareTo(other.lo)
        return cmpLo
    }

    inline fun EQ(other: DoubleDouble) = hi == other.hi && lo == other.lo

    fun EQwithinUlpSlop(other: DoubleDouble, ulpSlop: Int): Boolean {
        if (hi != other.hi)
            return false
        val ulpLo = mathUlp(lo)
        return abs(lo - other.lo) <= ulpLo * ulpSlop
    }

    override fun equals(other: Any?) = (other is DoubleDouble) && (hi == other.hi) && (lo == other.lo)

    override fun toString(): String = "{$hi + $lo}"
}

