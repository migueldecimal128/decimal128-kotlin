package com.decimal128.decimal

import com.decimal128.cardinal.Car
import com.decimal128.decimal.U256Set.u256Set
import com.decimal128.decimal.U256Set.u256SetShiftLeft
import com.decimal128.decimal.U256Set.u256SetShiftRight
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestInt256ShiftLeftRight {

    val verbose = false

    val tcs = arrayOf(
        BigInteger.ONE.shiftLeft(31),
        //BigInteger("1234567890123"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    val random = Random()

    fun randBi(): BigInteger {
        val bitLen = random.nextInt(0, 256)
        val bi = BigInteger(bitLen, random)
        return bi
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val biA = randBi()
            test1(biA)
        }
    }

    fun test1(tc: BigInteger) {
        val strA = tc.toString()
        val a = Int256(strA)
        assertEquals(tc.bitLength(), a.bitLen)
        if (verbose)
            println("$tc")
        val headRoom = 256 - a.bitLen
        val leftShift = 0 //random.nextInt(headRoom + 1)
        val left = Int256()
        u256SetShiftLeft(left, a, leftShift)
        val biLeft = tc.shiftLeft(leftShift)
        assertEquals(biLeft.toString(), left.toString())

        val rightShift = 20 // random.nextInt(32)
        val right = Int256()
        u256SetShiftRight(right, left, rightShift)
        val biRight = biLeft.shiftRight(rightShift)
        assertEquals(biRight.toString(), right.toString())

        val right2 = Int256()
        val car = biToIntArrayWithExtra(biLeft)
        val carShift = Car.mutateShiftRight(car, rightShift)
        u256Set(right2, carShift)
        assertEquals(biRight.toString(), right2.toString())
    }

    fun biToIntArrayWithExtra(bi: BigInteger): IntArray {
        val len = (bi.bitLength() + 0x1F) ushr 5
        val a = IntArray(len + 1)
        for (i in 0..<len)
            a[i] = bi.shiftRight(i * 32).toInt()
        return a
    }
}