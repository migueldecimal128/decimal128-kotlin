package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TestDoubleDouble {

    val verbose = true

    inner class TC(val biA: BigInteger, val biB: BigInteger, val biC: BigInteger, val biD: BigInteger) {
        constructor(strA: String, strB: String, strC: String, strD: String) : this(BigInteger(strA), BigInteger(strB), BigInteger(strC), BigInteger(strD))
        val biSum = biA + biB
        val biProd = biSum.multiply(biC)
        val biQuot = biProd.divide(biD)
        val ddA = DoubleDouble.newFromBigInteger(biA)
        val ddB = DoubleDouble.newFromBigInteger(biB)
        val ddC = DoubleDouble.newFromBigInteger(biC)
        val ddD = DoubleDouble.newFromBigInteger(biD)
        val ddSum = DoubleDouble.newFromBigInteger(biSum)
        val ddProd = DoubleDouble.newFromBigInteger(biProd)
        val ddQuot = DoubleDouble.newFromBigInteger(biQuot)
    }

    val tcs = arrayOf(
        TC("123456789012345678901234567890", "987654321098765432109876543210", "88", "22"),
        TC("1", "2", "4", "3"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("${tc.biA} + ${tc.biB} => ${tc.biSum}")
        val ddSum = DoubleDouble.newAdd(tc.ddA, tc.ddB)
        assert(ddSum.EQ(tc.ddSum))

        val ddProd = DoubleDouble.newMulExact(ddSum, tc.ddC)
        assert(ddProd.EQ(tc.ddProd))

        val ddQuot = DoubleDouble.newDiv(ddProd, tc.ddD)
        assert(ddQuot.EQ(tc.ddQuot))
    }

    @Test
    fun testUnrolled() {
        val biA = BigInteger("123456789012345678901234567890")
        val biB = BigInteger("987654321098765432109876543210")
        val biC = BigInteger("888888")
        val biD = BigInteger("222222")
        val biSum = biA + biB
        val biProd = biSum.multiply(biC)
        val biQuot = biProd.divide(biD)
        val ddA = DoubleDouble.newFromBigInteger(biA)
        val ddB = DoubleDouble.newFromBigInteger(biB)
        val ddC = DoubleDouble.newFromBigInteger(biC)
        val ddD = DoubleDouble.newFromBigInteger(biD)
        val ddSumX = DoubleDouble.newFromBigInteger(biSum)
        val ddProdX = DoubleDouble.newFromBigInteger(biProd)
        val ddQuotX = DoubleDouble.newFromBigInteger(biQuot)
        println("ddA:$ddA")
        println("ddB:$ddB")
        println("ddSumX:$ddSumX")
        println("ddProdX:$ddProdX")
        println("ddQuotX:$ddQuotX")

        val ddSumY = DoubleDouble.newAdd(ddA, ddB)
        println("ddSumY:$ddSumY")

        val cmpXY = ddSumX.compareTo(ddSumY)
        println("cmpXY:$cmpXY")

        val ddProdY = DoubleDouble.newMulExact(ddC, ddSumY)
        println("ddProdY:$ddProdY")
        assert(ddProdX.EQwithinUlpSlop(ddProdY, 1))

        val ddQuotY = DoubleDouble.newDiv(ddProdY, ddD)
        println("ddQuotY:$ddQuotY")
        assert(ddQuotX.EQwithinUlpSlop(ddQuotY, 16))
    }
}