package com.decimal128.decimal

import com.decimal128.hugeint.Magia
import com.decimal128.hugeint.MagiaTransducer
import com.decimal128.hugeint.MagiaTransducer.magiaFromBi
import com.decimal128.hugeint.MagiaTransducer.magiaFromString
import com.decimal128.hugeint.MagiaTransducer.magiaToBi
import com.decimal128.hugeint.MagiaTransducer.magiaToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*

class TestMagia {

    val verbose = false

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 1024)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    @Test
    fun testProblemChild() {
        testDiv(ONE.shiftLeft(32), ONE.shiftLeft(32))
        testDiv(ONE, ONE)
        testDiv(TWO, ONE)
        testDiv(TEN, ONE)
    }

    @Test
    fun testRoundTrip() {
        for (i in 0..1000) {
            val bi = randBi()
            testBitLen(bi)
            testRoundTripBi(bi)
            testRoundTripStr(bi.toString())
            testRoundTripShift(bi)
        }
    }


    fun testRoundTripBi(bi: BigInteger) {
        val car = magiaFromBi(bi)
        val bi2 = magiaToBi(car)
        assertEquals(bi, bi2)
    }

    fun testRoundTripStr(str: String) {

        if (verbose)
            println("testRoundTripStr($str)")
        val car = magiaFromString(str)
        val str2 = magiaToString(car)
        assertEquals(str, str2)

        val car3 = Magia.newFromString(str)
        assert(Magia.EQ(car, car3))
        val str3 = Magia.toString(car3)
        assertEquals(str, str3)
    }

    fun testRoundTripShift(bi: BigInteger) {
        val shift = random.nextInt(100)
        val car = magiaFromBi(bi)

        val biLeft = bi.shiftLeft(shift)
        val carLeft = Magia.newShiftLeft(car, shift)
        assert(MagiaTransducer.EQ(carLeft, biLeft))

        Magia.mutateShiftRight(carLeft, shift)
        assert(MagiaTransducer.EQ(carLeft, bi))

        val biRight = bi.shiftRight(shift)
        Magia.mutateShiftRight(car, shift)
        assert(MagiaTransducer.EQ(car, biRight))
    }

    fun testBitLen(bi: BigInteger) {
        val car = magiaFromBi(bi)
        val bitLen = Magia.bitLen(car)
        assertEquals(bi.bitLength(), bitLen)
    }

    @Test
    fun testArithmetic() {
        for (i in 0..<1000) {
            val biA = randBi()
            testAdd(biA, biA)
            testSub(biA, biA)
            testMul(biA, biA)
            testDiv(biA, biA)

            val biB = randBi()
            testAdd(biA, biB)
            testSub(biA, biB)
            testMul(biA, biB)
            testDiv(biA, biB)

            val biC = biA.add(ONE)
            testAdd(biA, biC)
            testSub(biA, biC)
            testMul(biA, biC)
            testDiv(biA, biC)

        }
    }

    fun testAdd(biA: BigInteger, biB: BigInteger) {
        val carA = magiaFromBi(biA)
        val carB = magiaFromBi(biB)
        val carSum = Magia.newAdd(carA, carB)

        val biSum = biA.add(biB)

        assert(MagiaTransducer.EQ(carSum, biSum))
    }

    fun testSub(biA: BigInteger, biB: BigInteger) {
        var biX = biA
        var biY = biB
        if (biA < biB) {
            biX = biB
            biY = biA
        }
        val carX = magiaFromBi(biX)
        val carY = magiaFromBi(biY)
        Magia.mutateSub(carX, carY) // MUTATE

        val biDiff = biX.subtract(biY)

        assert(MagiaTransducer.EQ(carX, biDiff))
    }

    fun testMul(biA: BigInteger, biB: BigInteger) {
        val carA = magiaFromBi(biA)
        val carB = magiaFromBi(biB)
        val carProd = Magia.newMul(carA, carB)

        val biProd = biA.multiply(biB)

        assert(MagiaTransducer.EQ(carProd, biProd))
    }

    fun testDiv(biA: BigInteger, biB: BigInteger) {
        if (biB.signum() == 0)
            return
        val carA = magiaFromBi(biA)
        val carB = magiaFromBi(biB)
        val carQuot = Magia.newDiv(carA, carB)

        val biQuot = biA.divide(biB)

        assert(MagiaTransducer.EQ(carQuot, biQuot))
    }

}
