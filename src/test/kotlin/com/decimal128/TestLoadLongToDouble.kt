package com.decimal128

import org.junit.jupiter.api.Test
import java.lang.Double.doubleToRawLongBits
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.math.BigInteger.TWO
import java.lang.Math.sqrt

class TestLoadLongToDouble {

    val longCases = longArrayOf(
        1L shl 8,
        2L * 1L,
        2L * 10000L,
        2L * 1000000000000L,
    )

    @Test
    fun testLongCases() {
        for (l in longCases) {
            test1(l)
        }
    }

    fun test1(l: Long) {
        val r = l.toDouble()
        val rBits = doubleToRawLongBits(r)
        val rSign = rBits ushr 63
        val rBiasedExp = (rBits ushr 52) and 0x7FF
        val rUnbiasedExp = rBiasedExp - 1023
        val rSignificand = rBits and 0xF_FFFF_FFFF_FFFFL

        println("radicand %s %5d 0x%03X 0x%013X %s".format(rSign, rUnbiasedExp, rBiasedExp, rSignificand, r))

        val s = sqrt(r)
        val sBits = doubleToRawLongBits(s)
        val sSign = sBits ushr 63
        val sBiasedExp = (sBits ushr 52) and 0x7FF
        val sUnbiasedExp = sBiasedExp - 1023
        val sSignificand = sBits and 0xF_FFFF_FFFF_FFFFL

        println(" -> sqrt %s %5d 0x%03X 0x%013X %s".format(sSign, sUnbiasedExp, sBiasedExp, sSignificand, s))
    }

    val biCases = arrayOf(
        TWO.multiply(TEN.pow(20))
    )

    @Test
    fun testBiCases() {
        for (bi in biCases) {
            val (biEst, kEvenUpPow10) = biSqrtEstimate(bi)
            println("$bi -> $biEst kEvenUpPow10:$kEvenUpPow10")
        }
    }

    fun biSqrtEstimate(bi: BigInteger): Pair<BigInteger, Int> {
        var bi53 = bi
        var kEvenUpPow10 = 0
        val bitLen = bi.bitLength()
        val excess = bitLen - 53
        if (excess > 0) {
            val k = ((excess * 1292913987L) ushr 32).toInt()
            kEvenUpPow10 = k + (k and 1)
            bi53 = bi.divide(TEN.pow(kEvenUpPow10))
        }
        val l = bi53.toLong();
        val d = l.toDouble()
        val s = sqrt(d)
        val sl = s.toLong()
        return BigInteger.valueOf(sl) to kEvenUpPow10/2
    }
}