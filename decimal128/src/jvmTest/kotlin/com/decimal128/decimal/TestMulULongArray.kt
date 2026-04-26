package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger
import java.util.*
import kotlin.math.min

@OptIn(ExperimentalUnsignedTypes::class)
class TestMulULongArray {

    val verbose = false

    companion object {
        /*inline*/ fun sumU64(dwA:ULong, dwB:ULong) : Pair<ULong, ULong> {
            val sumAB = dwA + dwB
            val carryAB = if (sumAB < dwA) 1UL else 0UL
            return carryAB to sumAB
        }

        /*inline*/ fun sumU64(dwA:ULong, dwB:ULong, dwC: ULong) : Pair<ULong, ULong> {
            val sumAB = dwA + dwB
            val carryAB = if (sumAB < dwA) 1UL else 0UL

            val sumABC = sumAB + dwC
            val carryABC = carryAB + if (sumABC < sumAB) 1UL else 0UL
            return carryABC to sumABC
        }


    }

    fun mulA(x: ULongArray, y: ULongArray): ULongArray {
        if (x.isEmpty() || y.isEmpty())
            return ulongArrayOf()
        val p = ULongArray(x.size + y.size)
        for (i in 0..<x.size) {
            val xi = x[i]
            var carry2 = 0UL
            var carry1 = 0UL
            for (j in 0..<y.size) {
                val yj = y[j]
                val k = i + j

                val lo = xi * yj
                val hi = unsignedMultiplyHigh(xi.toLong(), yj.toLong()).toULong()

                val (carryLo, sLo) = sumU64(p[k], lo, carry1)
                p[k] = sLo

                val (carryHi, sHi) = sumU64(p[k + 1], hi, carryLo)
                p[k + 1] = sHi

                carry1 = carry2
                carry2 = carryHi
            }
            var m = i + y.size
            while (carry2 != 0UL || carry1 != 0UL) {
                p[m] += carry1
                carry1 = carry2 + if (p[m] < carry1) 1UL else 0UL
                carry2 = 0UL
                ++m
            }
        }
        return p
    }

    fun mulB(p: ULongArray, x: ULongArray, xOff: Int, xLen: Int, y: ULongArray) {
        require (xOff >= 0 && xLen >= 0 && xOff + xLen <= x.size)
        require (xLen + y.size <= p.size)
        p.fill(0UL)
        for (i in 0..<xLen) {
            val xi = x[xOff + i]
            var carry2 = 0UL
            var carry1 = 0UL
            for (j in 0..<y.size) {
                val yj = y[j]
                val k = i + j

                val lo = xi * yj
                val hi = unsignedMultiplyHigh(xi.toLong(), yj.toLong()).toULong()

                val (carryLo, sLo) = sumU64(p[k], lo, carry1)
                p[k] = sLo

                val (carryHi, sHi) = sumU64(p[k + 1], hi, carryLo)
                p[k + 1] = sHi

                carry1 = carry2
                carry2 = carryHi
            }
            var m = i + y.size
            while (carry2 != 0UL || carry1 != 0UL) {
                p[m] += carry1
                carry1 = carry2 + if (p[m] < carry1) 1UL else 0UL
                carry2 = 0UL
                ++m
            }
        }
    }

    fun mulC(p: ULongArray, x: ULongArray, xOff: Int, xLen: Int, y: ULongArray): Int {
        require (xOff >= 0 && xLen >= 0 && xOff + xLen <= x.size)
        require (xLen + y.size <= p.size)
        val pLenMax = xLen + y.size
        require (pLenMax <= p.size)
        p.fill(0UL, 0, pLenMax)
        for (i in 0..<xLen) {
            val xi = x[xOff + i]
            for (j in 0..<y.size) {
                val yj = y[j]
                val k = i + j

                val lo = xi * yj
                val hi = unsignedMultiplyHigh(xi.toLong(), yj.toLong()).toULong()

                val (carryLo, sLo) = sumU64(p[k], lo)
                p[k] = sLo

                val (carryHi, sHi) = sumU64(p[k + 1], hi, carryLo)
                p[k + 1] = sHi

                if (carryHi > 0UL) {
                    var kT = k + 2
                    do {
                        val pkT = p[kT] + 1UL
                        p[kT] = pkT
                        ++kT
                    } while (pkT == 0UL)
                }

            }
        }
        val pLen = pLenMax - if (p[pLenMax - 1] == 0UL) 1 else 0
        return pLen
    }

    fun ulongArrayFromBi(bi: BigInteger): ULongArray {
        val bitLen = bi.bitLength()
        val arrayLen = (bitLen + 63) / 64
        val array = ULongArray(arrayLen)
        for (i in 0..<arrayLen)
            array[i] = bi.shiftRight(i * 64).toLong().toULong()
        return array
    }

    private fun biFromULongArray(x: ULongArray) = biFromULongArray(x, x.size)

    private fun biFromULongArray(x: ULongArray, xLen: Int): BigInteger {
        var bi = BigInteger.ZERO
        for (i in 0..<xLen) {
            val t = x[i]
            val biT = BigInteger(t.toString())
            bi = bi or (biT shl (i * 64))
        }
        return bi
    }

    private fun EQ(x: ULongArray, y: ULongArray) = EQ(x, x.size, y, y.size)

    private fun EQ(x: ULongArray, y: ULongArray, yLen: Int) = EQ(x, x.size, y, yLen)

    private fun EQ(x: ULongArray, xLen: Int, y: ULongArray, yLen: Int): Boolean {
        require (xLen >= 0 && yLen >= 0 && xLen <= x.size && yLen <= y.size)
        val minIndex = min(xLen, yLen)
        for (i in 0..<minIndex)
            if (x[i] != y[i])
                return false
        for (i in minIndex..<xLen)
            if (x[i] != 0UL)
                return false
        for (i in minIndex..<yLen)
            if (y[i] != 0UL)
                return false
        return true
    }

    fun test1A(biX: BigInteger, biY: BigInteger) {
        if (verbose)
            println("$biX * $biY")
        val biP = biX * biY

        val x = ulongArrayFromBi(biX)
        val biX2 = biFromULongArray(x)
        assertEquals(biX, biX2)
        val y = ulongArrayFromBi(biY)
        assertEquals(biY, biFromULongArray(y))
        val p = mulA(x, y)
        val biP2 = biFromULongArray(p)
        val biArray = ulongArrayFromBi(biP)

        if (!EQ(biArray, p)) {
            println("FAIL: $biX * $biY")
            println(" biP:$biP")
            println("biP2:$biP2")
            assertTrue(EQ(biArray, p))
        }
    }

    fun test1B(biX: BigInteger, biY: BigInteger) {
        if (verbose)
            println("$biX * $biY")
        val biP = biX * biY

        val xT = ulongArrayFromBi(biX)
        val biX2 = biFromULongArray(xT)
        assertEquals(biX, biX2)

        val xLen = xT.size
        val xOff = random.nextInt(4)
        val xTra = random.nextInt(4)
        val x = ULongArray(xOff + xLen + xTra)
        xT.copyInto(x, xOff)

        val y = ulongArrayFromBi(biY)
        assertEquals(biY, biFromULongArray(y))

        val p = ULongArray(xLen + y.size)
        mulB(p, x, xOff, xLen, y)
        val biP2 = biFromULongArray(p)
        val biArray = ulongArrayFromBi(biP)

        if (!EQ(biArray, p)) {
            println("FAIL: $biX * $biY")
            println(" biP:$biP")
            println("biP2:$biP2")
            assertTrue(EQ(biArray, p))
        }
    }

    val pGlobal = ULongArray(16)
    fun test1C(biX: BigInteger, biY: BigInteger) {
        if (verbose)
            println("$biX * $biY")
        val biP = biX * biY

        val xT = ulongArrayFromBi(biX)
        //val biX2 = biFromULongArray(xT)
        //assertEquals(biX, biX2)

        val xLen = xT.size
        val xOff = random.nextInt(4)
        val xTra = random.nextInt(4)
        val x = ULongArray(xOff + xLen + xTra)
        xT.copyInto(x, xOff)

        val y = ulongArrayFromBi(biY)
        //assertEquals(biY, biFromULongArray(y))

        val pLen = mulC(pGlobal, x, xOff, xLen, y)
        val biP2 = biFromULongArray(pGlobal, pLen)
        val biArray = ulongArrayFromBi(biP)

        if (!EQ(biArray, pGlobal, pLen)) {
            println("FAIL: $biX * $biY")
            println(" biP:$biP")
            println("biP2:$biP2")
            assertTrue(EQ(biArray, pGlobal, pLen))
        }
    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(192, 385)
        val bi = BigInteger(bitLength, random)
        return bi.abs()
    }



    @Test
    fun testRandom() {
        for (i in 0..<1000000) {
            val biX = randBi()
            val biY = randBi()
            test1C(biX, biY)
        }
    }

    @Test
    fun testProblemChild1() {
        test1A(BigInteger("168778141523252370301959339334785769585"),
            BigInteger("322413822440858003804551553095807950514"))
    }

    @Test
    fun testProblemChild2() {
        val biX = BigInteger.ONE.shiftLeft(128) - BigInteger.ONE
        val biY = BigInteger.ONE.shiftLeft(128) - BigInteger.ONE
        test1A(biX, biY)
    }

}