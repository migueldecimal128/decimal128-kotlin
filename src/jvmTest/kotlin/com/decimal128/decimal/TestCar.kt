package com.decimal128.decimal

import com.decimal128.decimal.CarTransducer.carFromBi
import com.decimal128.decimal.CarTransducer.carFromString
import com.decimal128.decimal.CarTransducer.carToBi
import com.decimal128.decimal.CarTransducer.carToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*

class TestCar {

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
        val car = carFromBi(bi)
        val bi2 = carToBi(car)
        assertEquals(bi, bi2)
    }

    fun testRoundTripStr(str: String) {

        if (verbose)
            println("testRoundTripStr($str)")
        val car = carFromString(str)
        val str2 = carToString(car)
        assertEquals(str, str2)

        val car3 = Car.newFromString(str)
        assert(Car.EQ(car, car3))
        val str3 = Car.toString(car3)
        assertEquals(str, str3)
    }

    fun testRoundTripShift(bi: BigInteger) {
        val shift = random.nextInt(100)
        val car = carFromBi(bi)

        val biLeft = bi.shiftLeft(shift)
        val carLeft = Car.newShiftLeft(car, shift)
        assert(CarTransducer.EQ(carLeft, biLeft))

        Car.mutateShiftRight(carLeft, shift)
        assert(CarTransducer.EQ(carLeft, bi))

        val biRight = bi.shiftRight(shift)
        Car.mutateShiftRight(car, shift)
        assert(CarTransducer.EQ(car, biRight))
    }

    fun testBitLen(bi: BigInteger) {
        val car = carFromBi(bi)
        val bitLen = Car.bitLen(car)
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
        val carA = carFromBi(biA)
        val carB = carFromBi(biB)
        val carSum = Car.newAdd(carA, carB)

        val biSum = biA.add(biB)

        assert(CarTransducer.EQ(carSum, biSum))
    }

    fun testSub(biA: BigInteger, biB: BigInteger) {
        var biX = biA
        var biY = biB
        if (biA < biB) {
            biX = biB
            biY = biA
        }
        val carX = carFromBi(biX)
        val carY = carFromBi(biY)
        Car.mutateSub(carX, carY) // MUTATE

        val biDiff = biX.subtract(biY)

        assert(CarTransducer.EQ(carX, biDiff))
    }

    fun testMul(biA: BigInteger, biB: BigInteger) {
        val carA = carFromBi(biA)
        val carB = carFromBi(biB)
        val carProd = Car.newMul(carA, carB)

        val biProd = biA.multiply(biB)

        assert(CarTransducer.EQ(carProd, biProd))
    }

    fun testDiv(biA: BigInteger, biB: BigInteger) {
        if (biB.signum() == 0)
            return
        val carA = carFromBi(biA)
        val carB = carFromBi(biB)
        val carQuot = Car.newDiv(carA, carB)

        val biQuot = biA.divide(biB)

        assert(CarTransducer.EQ(carQuot, biQuot))
    }

}
