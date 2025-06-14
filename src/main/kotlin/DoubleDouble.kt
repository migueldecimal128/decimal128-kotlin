package com.decimal128

import java.math.BigInteger

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

        fun newFromBigInteger(n: BigInteger): DoubleDouble {
            val dd = DoubleDouble()
            dd.setBigInteger(n)
            return dd
        }
/*
        fun newFromCoeff(c: Coeff): DoubleDouble {
            val dd = DoubleDouble()
            dd.setCoeff(c)
            return dd;
        }

 */

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
            val err = Math.fma(a, b, -p)
            return DoubleDouble(p, err)
        }

        fun newMulApprox(x: DoubleDouble, y: DoubleDouble): DoubleDouble {
            //val (p, e) = twoProd(aH, bH)
            val p = x.hi * y.hi
            val e = Math.fma(x.hi, y.hi, -p)
            // 2) cross terms
            val cross = (x.hi * y.lo) + (x.lo * y.hi)
            // 3) combine error and cross into low part and renormalize
            return newQuickTwoSum(p, e + cross)
        }

        fun newMulExact(x: DoubleDouble, y:DoubleDouble): DoubleDouble {
            val dd = DoubleDouble()
            dd.setMulBetter(x, y)
            return dd
        }

        fun newMulDoubleDoubleByDouble(x: DoubleDouble, y:Double): DoubleDouble {
            val p = newTwoProd(x.hi, y)
            return newQuickTwoSum(p.hi, p.lo + x.lo * y)
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun setQuickTwoSum(a: Double, b: Double) {
        hi = a + b
        lo = b - (hi - a)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setTwoSum(a: Double, b:Double) {
        hi = a + b
        val z = hi - a
        lo = (a - (hi - z)) + (b - z)
    }

    @Suppress("NOTHING_TO_INLINE")
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun setSub(x: DoubleDouble, y: DoubleDouble) {
        // 1) twoSum on hi
        val s  = x.hi - y.hi
        val z1 = s - x.hi
        val e1 = (x.hi - (s - z1)) + (-y.hi - z1)

        // 2) twoSum on lo
        val t  = x.lo - y.lo
        val z2 = t - x.lo
        val e2 = (x.lo - (t - z2)) + (-y.lo - z2)

        // 3) blend high-error with low-sum
        val e1t = e1 + t
        val u    = s + e1t
        val z3   = u - s
        val e3   = (s - (u - z3)) + (e1t - z3)

        setQuickTwoSum(u, e2 + e3)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setMulApprox(x: DoubleDouble, y: DoubleDouble) {
        //val (p, e) = twoProd(aH, bH)
        val p = x.hi * y.hi
        val e = Math.fma(x.hi, y.hi, -p)
        // 2) cross terms
        val cross = (x.hi * y.lo) + (x.lo * y.hi)
        // 3) combine error and cross into low part and renormalize
        setQuickTwoSum(p, e + cross)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setMulBetter(x: DoubleDouble, y: DoubleDouble) {
        // 1) high‐word product + error
        val pH = x.hi * y.hi
        val pL = Math.fma(x.hi, y.hi, -pH)

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
        val llL = Math.fma(x.lo, y.lo, -llH)
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun setMul(x: DoubleDouble, y: Double) {
        val pH = x.hi * y
        val pL = Math.fma(x.hi, y, -pH)
        // combine low word
        setQuickTwoSum(pH, pL + x.lo * y)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setDivApprox(x: DoubleDouble, y: DoubleDouble) {
        // 1) initial quotient
        val q1 = x.hi / y.hi

        // 2) form p = y * q1 in double-double form
        //    high-word
        val pH = y.hi * q1
        //    error of y.hi*q1
        val pL = Math.fma(y.hi, q1, -pH)
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun setInvFast(x: DoubleDouble) {
        // 1) first guess q1 = 1/x.hi
        val q1 = 1.0 / x.hi

        // 2) form p = x * q1  (double-word product)
        //    high-word
        val pH = x.hi * q1
        //    error of that product
        val pL = Math.fma(x.hi, q1, -pH)
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun mutateInvFast() {
        // 1) first guess q1 = 1/x.hi
        val q1 = 1.0 / hi

        // 2) form p = x * q1  (double-word product)
        //    high-word
        val pH = hi * q1
        //    error of that product
        val pL = Math.fma(hi, q1, -pH)
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

    fun setBigInteger(n: BigInteger) {
        hi = 0.0
        lo = 0.0
        val sign = n.signum()
        if (sign == 0) {
            return
        }
        val abs   = n.abs()
        val L     = abs.bitLength()
        if (L <= 53) {
            hi = sign * abs.toDouble()
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

    fun mutateDouble() {
        hi *= 2
        lo *= 2
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
    @Suppress("NOTHING_TO_INLINE")
    inline fun compareTo(other: DoubleDouble): Int {
        val cmpHi = hi.compareTo(other.hi)
        if (cmpHi != 0)
            return cmpHi
        val cmpLo = lo.compareTo(other.lo)
        return cmpLo
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun EQ(other: DoubleDouble) = hi == other.hi && lo == other.lo

    @Suppress("NOTHING_TO_INLINE")
    inline fun EQwithinUlpSlop(other: DoubleDouble, ulpSlop: Int): Boolean {
        if (hi != other.hi)
            return false
        val ulpLo = Math.ulp(lo)
        return Math.abs(lo - other.lo) <= ulpLo * ulpSlop
    }

    override fun equals(other: Any?) = (other is DoubleDouble) && (hi == other.hi) && (lo == other.lo)

    override fun toString(): String = "{$hi + $lo}"
}