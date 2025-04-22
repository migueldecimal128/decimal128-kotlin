package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestUlarShiftRight {

    class TC(val biA: BigInteger, val bitShift:Int) {
        val biExpected = biA.shiftRight(bitShift)

        constructor(a: String, b:Int) : this(BigInteger(a), b)
    }

    val cases = arrayOf(
        TC("2", 1),
        TC("129984421268392750770225925334746398472047", 11),
        TC("19084", 91),
        TC("0", 1),
        TC("1", 2),
        TC("3", 5),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandomShift() {
        for (i in 0..<100000) {
            val bi = randBi()
            val case = TC(randBi(), randShift(bi))
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 320)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun randShift(bi:BigInteger) : Int {
        val bitLength = bi.bitLength()
        val max = bitLength + 128
        val bitShift = random.nextInt(0, max)
        return bitShift
    }

    fun test1(case: TC) {
        val biA = case.biA
        val bitShift = case.bitShift
        val expected = case.biExpected
        val ularExpected = Ular.from(expected)

        val ularA = Ular.from(biA)
        Ular.mutateShiftRight(ularA, bitShift)

        val observed = Ular.toBigInteger(ularA)
        if (! observed.equals(expected)) {
            throw RuntimeException("$biA shr $bitShift = expected:$expected observed:$observed")
        }
        val bLen = ularA.size
        val bOff = random.nextInt(0, 4)
        val b = LongArray(bOff + bLen + random.nextInt(0, 4))
        Ular.set(b, bOff, bLen, biA)
        Ular.mutateShiftRight(b, bOff, bLen, bitShift)
        val observedB = Ular.toBigInteger(b, bOff, bLen)
        if (! observedB.equals(expected)) {
            throw RuntimeException("$biA shr $bitShift = expected:$expected observedB:$observedB")
        }
    }

}
