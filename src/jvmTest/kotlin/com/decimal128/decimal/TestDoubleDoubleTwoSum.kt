package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TestDoubleDoubleTwoSum {

    val verbose = false

    fun doubleDoubleFromBigInteger(n: BigInteger): Pair<Double, Double> {
        if (n.signum() == 0) return 0.0 to 0.0
        val sign = n.signum()
        val abs   = n.abs()
        val L     = abs.bitLength()
        if (L <= 53) {
            return (sign * abs.toDouble()) to 0.0
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
        return quickTwoSum(hi, lo0)
    }
    /**
     * FastTwoSum / QuickTwoSum: assumes |a| ≥ |b|, but in our case
     * `sum` ≫ `lo0` so it’s safe.
     * Returns (hi, lo) such that hi+lo == exact a+b, and |lo| < ½ulp(hi).
     */
    private fun quickTwoSum(a: Double, b: Double): Pair<Double, Double> {
        val s   = a + b
        val err = b - (s - a)
        return s to err
    }

    /** Error-free transform (no precondition on magnitudes). */
    private fun twoSum(a: Double, b: Double): Pair<Double, Double> {
        val s = a + b
        val z = s - a
        val err = (a - (s - z)) + (b - z)
        return s to err
    }

    /**
     * Add two double-doubles (aH+aL) + (bH+bL) → (hi, lo)
     * with full renormalization.
     */
    fun addDoubleDouble(
        aH: Double, aL: Double,
        bH: Double, bL: Double
    ): Pair<Double, Double> {
        // 1) high-word sum + error
        val (s,  e1) = twoSum(aH, bH)
        // 2) low-word sum + error
        val (t,  e2) = twoSum(aL, bL)
        // 3) combine high-error with low sum
        val (sum, e3) = twoSum(s, e1 + t)
        // 4) preliminary lo
        val lo0 = e2 + e3
        // 5) renormalize so no overlap between hi and lo
        return quickTwoSum(sum, lo0)
    }

    fun subDoubleDouble(
        aH: Double, aL: Double,
        bH: Double, bL: Double
    ): Pair<Double, Double> {
        // 1) high-word sum + error
        val (s,  e1) = twoSum(aH, -bH)
        // 2) low-word sum + error
        val (t,  e2) = twoSum(aL, -bL)
        // 3) combine high-error with low sum
        val (sum, e3) = twoSum(s, e1 + t)
        // 4) preliminary lo
        val lo0 = e2 + e3
        // 5) renormalize so no overlap between hi and lo
        return quickTwoSum(sum, lo0)
    }

    /**
     * Multiply two double-doubles (aH+aL) * (bH+bL),
     * returning (hi, lo) such that hi+lo == exact product to ~106 bits.
     */
    fun mulDoubleDouble(
        aH: Double, aL: Double,
        bH: Double, bL: Double
    ): Pair<Double, Double> {
        // 1) high-word product + error
        val (p, e) = twoProd(aH, bH)
        // 2) cross terms
        val cross = aH * bL + aL * bH
        // 3) combine error and cross into low part and renormalize
        return quickTwoSum(p, e + cross)
    }


    /**
     * Error-free product: (p, err) = a * b exactly,
     * using hardware FMA for the rounding error.
     */
    private fun twoProd(a: Double, b: Double): Pair<Double, Double> {
        val p = a * b
        // requires Java 9+ (Kotlin stdlib passes through to Math.fma)
        val err = Math.fma(a, b, -p)
        return p to err
    }

    /**
     * Multiply a double-double (aH+aL) by a scalar y,
     * returning (hi, lo) = exact product.
     */
    private fun mulDDByDouble(
        aH: Double, aL: Double,
        y: Double
    ): Pair<Double, Double> {
        val (pH, pL) = twoProd(aH, y)
        // combine low word
        return quickTwoSum(pH, pL + aL * y)
    }

    /**
     * Divide two double-doubles (aH+aL) / (bH+bL),
     * returning a double-double quotient ≈ exact to ~106 bits.
     */
    fun divDoubleDouble(
        aH: Double, aL: Double,
        bH: Double, bL: Double
    ): Pair<Double, Double> {
        // 1) approximate quotient
        val q1 = aH / bH

        // 2) compute p = b * q1
        val (pH, pL) = mulDDByDouble(bH, bL, q1)

        // 3) remainder r = a - p
        val (rH, rL) = addDoubleDouble(aH, aL, -pH, -pL)

        // 4) correction term
        val q2 = (rH + rL) / bH

        // 5) final normalization
        return quickTwoSum(q1, q2)
    }
    /**
     * Compare two double-doubles (aH+aL) vs (bH+bL).
     * Returns:
     *   - negative if (aH,aL) < (bH,bL)
     *   - zero     if equal
     *   - positive if (aH,aL) > (bH,bL)
     */
    fun compareDoubleDouble(
        aH: Double, aL: Double,
        bH: Double, bL: Double
    ): Int {
        // 1) Compare high words
        if (aH < bH) return -1
        if (aH > bH) return  1

        // 2) High words equal → compare low words
        if (aL < bL) return -1
        if (aL > bL) return  1

        // 3) Exactly equal (to the 106 bits of precision)
        return 0
    }

    inner class TC(val biA: BigInteger, val biB: BigInteger) {
        constructor(strA: String, strB: String) : this(BigInteger(strA), BigInteger(strB))
        val biSum = biA + biB
        val pairA = doubleDoubleFromBigInteger(biA)
        val pairB = doubleDoubleFromBigInteger(biB)
        val pairSum = doubleDoubleFromBigInteger(biSum)
    }

    val tcs = arrayOf(
        TC("1234567890123456789012334567890", "987654321098765432109876543210"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val (sumH, sumL) = addDoubleDouble(tc.pairA.first, tc.pairA.second, tc.pairB.first, tc.pairB.second)
        assert(compareDoubleDouble(sumH, sumL, tc.pairSum.first, tc.pairSum.second) == 0)
    }

    @Test
    fun testUnrolled() {
        val biA = BigInteger("1234567890123456789012334567890")
        val biB = BigInteger("987654321098765432109876543210")
        val biC = BigInteger("888888")
        val biD = BigInteger("222222")
        val biSum = biA + biB
        val biProd = biSum.multiply(biC)
        val biQuot = biProd.divide(biD)
        val ddA = doubleDoubleFromBigInteger(biA)
        val ddB = doubleDoubleFromBigInteger(biB)
        val ddC = doubleDoubleFromBigInteger(biC)
        val ddD = doubleDoubleFromBigInteger(biD)
        val ddSumX = doubleDoubleFromBigInteger(biSum)
        val ddProdX = doubleDoubleFromBigInteger(biProd)
        val ddQuotX = doubleDoubleFromBigInteger(biQuot)
        if (verbose) {
            println("ddA:$ddA")
            println("ddB:$ddB")
            println("ddSumX:$ddSumX")
            println("ddProdX:$ddProdX")
            println("ddQuotX:$ddQuotX")
        }

        val ddSumY = addDoubleDouble(ddA.first, ddA.second, ddB.first, ddB.second)
        if (verbose)
            println("ddSumY:$ddSumY")

        val cmpXY = compareDoubleDouble(ddSumX.first, ddSumX.second, ddSumY.first, ddSumY.second)
        if (verbose)
            println("cmpXY:$cmpXY")

        val ddProdY = mulDoubleDouble(ddC.first, ddC.second, ddSumY.first, ddSumY.second)
        if (verbose)
            println("ddProdY:$ddProdY")
        assertEquals(ddProdX, ddProdY)

        val ddQuotY = divDoubleDouble(ddProdY.first, ddProdY.second, ddD.first, ddD.second)
        if (verbose)
            println("ddQuotY:$ddQuotY")
        assertEquals(ddQuotX, ddQuotY)
    }
}