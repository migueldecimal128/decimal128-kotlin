package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.EQ
import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*

class TestHugeInt {
    val verbose = false

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 1000)
        val bi = BigInteger(bitLength, random)
        return if (random.nextBoolean()) bi.negate() else bi
    }

    @Test
    fun testProblemChild() {
        testSqr(BigInteger("12345678901234567890123456789012345678901234567890"))
        testSqr(BigInteger("99999999999"))
        testAdd(BigInteger.ZERO, BigInteger.ZERO)
        testAdd(BigInteger("-4418548"), BigInteger.ZERO)
        testSub(BigInteger("-4418548"), BigInteger.ZERO)
        testRoundTripShift(BigInteger("-485"))
        testRoundTripShift(BigInteger("20000000000000000000000000"))
        testRoundTripStr("-999999999")
        testRoundTripStr("-1234567890")
        testRoundTripStr("-9999999999")
        testRoundTripStr("-1")
        testRoundTripBi(BigInteger("-1"))
        testDiv(BigInteger("1234567890123456789012"), BigInteger("1234567890123456789013"))
        testMod(ONE, ONE)
        testDiv(ONE.shiftLeft(32), ONE.shiftLeft(32))
        testDiv(ONE, ONE)
        testDiv(TWO, ONE)
        testDiv(TEN, ONE)
        testMod(TEN, ONE)
    }

    @Test
    fun testRoundTrip() {
        for (i in 0..10000) {
            val bi = randBi()
            testBitLen(bi)
            testRoundTripBi(bi)
            testRoundTripStr(bi.toString())
            testRoundTripShift(bi)
            testBigEndianByteArray(bi)
        }
    }


    fun testRoundTripBi(bi: BigInteger) {
        if (verbose)
            println("testRoundTripBi: $bi")
        val hi = bi.toHugeInt()
        val bi2 = hi.toBigInteger()
        assertEquals(bi, bi2)
    }

    fun testRoundTripStr(str: String) {

        if (verbose)
            println("testRoundTripStr($str)")
        val hi = HugeInt.fromString(str)
        val str2 = hi.toString()
        assertEquals(str, str2)
    }

    fun testRoundTripShift(bi: BigInteger) {
        val shift = 1 // random.nextInt(200)
        if (verbose)
            println("testRoundTripShift($bi) shift:$shift")
        val hi = bi.toHugeInt()

        val biLeft = bi shl shift
        val hiLeft = hi shl shift
        if (verbose)
            println(" biLeft:$biLeft hiLeft:$hiLeft")
        assert(hiLeft.EQ(biLeft))

        val hiRight = hiLeft shr shift
        if (verbose)
            println(" hiRight:$hiRight")
        assert(hiRight.EQ(bi))

        val hiRight2 = hiRight shr shift
        val biRight2 = bi shr shift
        if (verbose)
            println(" hiRight2:$hiRight2 biRight2:$biRight2")
        assert(hiRight2.EQ(biRight2))
    }

    fun testBitLen(bi: BigInteger) {
        val hi = bi.toHugeInt()
        val hiBitLen = hi.magnitudeBitLen()
        val biBitLen = bi.bitLength()
        val hiBitLenBI = hi.bitLengthBigIntegerStyle()
        if (verbose)
            println("testBitLen:$bi biBitLen:$biBitLen hiBitLenBI:$hiBitLen")
        assertEquals(biBitLen, hiBitLenBI)
    }

    @Test
    fun testArithmetic() {
        for (i in 0..<10000) {
            val biA = randBi()
            testAdd(biA, biA)
            testSub(biA, biA)
            testMul(biA, biA)
            testDiv(biA, biA)
            testMod(biA, biA)
            testSqr(biA)

            val biB = randBi()
            testAdd(biA, biB)
            testSub(biA, biB)
            testMul(biA, biB)
            testDiv(biA, biB)
            testMod(biA, biB)
            testSqr(biB)

            val biC = biA.add(ONE)
            testAdd(biA, biC)
            testSub(biA, biC)
            testMul(biA, biC)
            testDiv(biA, biC)
            testMod(biA, biC)
            testSqr(biC)

        }
    }

    fun testAdd(biA: BigInteger, biB: BigInteger) {
        val hiA = biA.toHugeInt()
        val hiB = biB.toHugeInt()
        if (verbose)
            println("biA:$biA hiA:$hiA   biB:$biB hiB:$hiB")

        val hiSum = hiA + hiB
        val biSum = biA + biB
        if (! hiSum.EQ(biSum))
            println("biSum:$biSum hiSum:$hiSum")

        assert(hiSum.EQ(biSum))
        if (biSum.signum() == 0)
            assert(hiSum === HugeInt.ZERO)
    }

    fun testSub(biA: BigInteger, biB: BigInteger) {

        val hiA = biA.toHugeInt()
        val hiB = biB.toHugeInt()
        if (verbose)
            println("biA:$biA hiA:$hiA   biB:$biB hiB:$hiB")
        val hiDiff = hiA - hiB
        val biDiff = biA - biB
        if (! hiDiff.EQ(biDiff))
            println("biDiff:$biDiff hiDiff:$hiDiff")
        assert(hiDiff.EQ(biDiff))
        if (biDiff.signum() == 0)
            assert(hiDiff === HugeInt.ZERO)
    }

    fun testMul(biA: BigInteger, biB: BigInteger) {
        val hiA = biA.toHugeInt()
        val hiB = biB.toHugeInt()
        val hiProd = hiA * hiB
        val biProd = biA.multiply(biB)
        assert(hiProd.EQ(biProd))
        if (biProd.signum() == 0)
            assert(hiProd === HugeInt.ZERO)
    }

    fun testDiv(biA: BigInteger, biB: BigInteger) {
        if (verbose)
            println("testDiv: $biA / $biB")
        if (biB.signum() == 0)
            return
        val hiA = biA.toHugeInt()
        val hiB = biB.toHugeInt()
        val hiQuot = hiA / hiB

        val biQuot = biA.divide(biB)
        assert(hiQuot.EQ(biQuot))
        if (biQuot.signum() == 0)
            assert(hiQuot === HugeInt.ZERO)
    }

    fun testMod(biA: BigInteger, biB: BigInteger) {
        if (biB.signum() == 0)
            return
        val hiA = biA.toHugeInt()
        val hiB = biB.toHugeInt()
        val hiRem = hiA % hiB

        val biRem = biA.remainder(biB)
        assert(hiRem.EQ(biRem))
        if (biRem.signum() == 0)
            assert(hiRem === HugeInt.ZERO)
    }

    fun testSqr(bi: BigInteger) {
        val hi = bi.toHugeInt()
        val biSqr = bi * bi
        val hiSqr = hi.sqr()
        val hiSqrMul = hi * hi
        if (! hiSqr.EQ(biSqr))
            println("hi:$hi hiSqr:$hiSqr biSqr:$biSqr")
        assert(hiSqr.EQ(biSqr))
        if (! hiSqr.EQ(hiSqrMul))
            println("hi:$hi hiSqr:$hiSqr hiSqr2:$hiSqrMul")
        assert(hiSqr.EQ(hiSqrMul))
    }

    fun testBigEndianByteArray(bi: BigInteger) {
        val hi = bi.toHugeInt()
        val biBytes = bi.toByteArray()
        val hiBytes = hi.toBigEndianByteArray()

        val biHex = java.util.HexFormat.ofDelimiter(" ").withUpperCase().formatHex(biBytes)
        val hiHex = java.util.HexFormat.ofDelimiter(" ").withUpperCase().formatHex(hiBytes)

        if (verbose)
            println("biHex:$biHex hiHex:$hiHex")
        assertArrayEquals(biBytes, hiBytes)

        val hi2 = HugeInt.fromBigEndianBytes(hiBytes)
        if (verbose)
            println("hi:$hi hi2:$hi2")
        assertEquals(hi, hi2)
    }

}
