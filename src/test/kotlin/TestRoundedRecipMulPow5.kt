package com.decimal128

import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TEN
import java.math.RoundingMode

//─────────────────────────────────────────────────────────────────────────────
// High-precision constant ρ = log₂(10)
private val RHO = BigDecimal(
    "3.321928094887362347870319429489390175864831393024580612054"
)

/**
 * Calculate the minimal bit-width y needed to approximate 5^(–x) (after
 * shifting out 2^x) so that you get both:
 *   1) the exact quotient ⌊C/10^x⌋
 *   2) fully faithful fractional bits for rounding
 *
 * @param qDigitCount  number of decimal digits in your max dividend Cₘₐₓ
 * @param xPow10       exponent x (so divisor=10^x=2^x·5^x)
 * @return minimal y
 */
fun calcMinYFor5Power(qDigitCount: Int, xPow10: Int): Int {
    // 1) ρ·x and its fractional part {ρx}
    val rhoTimesX = RHO.multiply(BigDecimal.valueOf(xPow10.toLong()))
    val fracRx    = rhoTimesX.subtract(rhoTimesX.setScale(0, RoundingMode.FLOOR))

    // 2) add ρ·q, ceil → bits for full 10^x divisor
    val raw = fracRx.add(RHO.multiply(BigDecimal.valueOf(qDigitCount.toLong())))

    // 3) subtract x to account for the 2^x shift already performed
    return raw.setScale(0, RoundingMode.CEILING)
        .intValueExact() - xPow10
}

/**
 * Compute the round-up reciprocal multiplier for 5^x, i.e.
 *   mul = ceil(2^y / 5^x)
 *
 * @param y         bit-width from calcMinYFor5Power
 * @param xPow10    exponent x
 * @return BigInteger multiplier of y bits
 */
fun computeMultiplierFor5Power(y: Int, xPow10: Int): BigInteger {
    val twoPowY = ONE.shiftLeft(y)            // 2^y
    val fivePowX = BigInteger.valueOf(5).pow(xPow10)     // 5^x
    // ceil(2^y / 5^x) = (2^y + 5^x - 1) / 5^x
    return twoPowY.add(fivePowX).subtract(ONE)
        .divide(fivePowX)
}

/**
 * Example usage: divide a 256-bit C by 10^x with correct rounding.
 */
fun main() {
    // 1) Your max dividend Cmax (here 2^256–1) and its digit count q
    val maxDividendPow10 = 2
    val Cmax = TEN.pow(maxDividendPow10).subtract(ONE)
    println("Cmax:$Cmax")
    val q = Cmax.toString().length    // decimal digits in Cmax

    // 2) Divisor = 10^x; pick x
    val x = 1
    println("divisor 10^$x")

    // 3) Pre-shift amount = x - 1 bits
    val preShift = x - 1
    val CmaxShifted = Cmax.shiftRight(preShift)
    val qShifted = CmaxShifted.toString().length


    // 4) Compute y and multiplier for 5^x
    val y   = calcMinYFor5Power(qShifted, x)        // ≃216 for full 5^44 step
    val mul = computeMultiplierFor5Power(y, x)

    println("Pre-shift bits    = $preShift")
    println("Multiplier bits  = $y")
    println("Multiplier (hex) = ${mul.toString(16)}")

    // 5) To perform the division on an arbitrary BigInteger C:
    //    a) exact divide by 2^x
    //    b) multiply by the 5^x-reciprocal
    //    c) shift off y bits for quotient; low y bits give remainder info
    val C = BigInteger("51")  // your 256-bit input
    val Cprime = C.shiftRight(preShift)
    val prod   = Cprime.multiply(mul)
    val Q      = prod.shiftRight(y+1)  // ⌊C / 10^x⌋
    val R      = prod.and(ONE.shiftLeft(y+1).subtract(ONE))
    // R’s MSB is the ½-bit; the rest are the mantissa for EXACT/LT_HALF/HALF/GT_HALF
    println("C = $C")
    println("Quotient = $Q")
    println("R = 0x${R.toString(16)}")

// 5) extract the “round” bit at index y:
    val roundBit = prod.testBit(y)
    println("roundBit = $roundBit")

    // after
//   val prod = Cprime.multiply(mul)
//   val Q    = prod.shiftRight(y)
// and assuming
//   val mul: BigInteger
//   val y:   Int

// 1) Mask off the low y bits of the product
    val maskY    = BigInteger.ONE.shiftLeft(y).subtract(BigInteger.ONE)
    val fracBits = prod.and(maskY)   // bits [0..y-1]

// 2) The first threshold: mul = ceil(2^y / d).
//    If fracBits < mul, then r must be 0 → EXACT remainder.
    val category = when {
        // remainder == 0 exactly
        !roundBit && fracBits < mul -> "EXACT"
        !roundBit -> "LT_HALF"
        fracBits < mul -> "HALF"
        else -> "GT_HALF"
    }

// 3) (Optional) rounded quotient
    val roundedQ = if (category == "GT_HALF" || category == "HALF")
        Q.add(BigInteger.ONE)
    else
        Q

    println("floor Q = $Q, category = $category, rounded Q = $roundedQ")

}
