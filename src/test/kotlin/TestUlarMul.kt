package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestUlarMul {

    class TC(val biA: BigInteger, val biB: BigInteger) {
        val biProduct = biA.multiply(biB)

        constructor(a: String, b:String) : this(BigInteger(a), BigInteger(b))
    }

    val cases = arrayOf(
        TC("340282366920938463463374607431768211456", "340282366920938463463374607431768211456"),
        TC(BigInteger.ONE.shiftLeft(64).add(BigInteger.TWO), 3.toBigInteger().shiftLeft(64).add(4.toBigInteger())),
        TC("23552335528420943652", "34522815225662007740"),
        TC("0", "1"),
        TC("1", "1"),
        TC("3", "5"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testBinaryBoundaries() {
        val quads = longArrayOf(
            -2, -1, -1, -1,
            -1, 0, 0, 0,
            -1, -1, 0, 0,
            -1, -1, -1, 0,
            -2, -1, -1, -1,
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
        )

        for (i in quads.indices step 4) {
            val a0 = quads[i + 0]
            val a1 = quads[i + 1]
            val a2 = quads[i + 2]
            val a3 = quads[i + 3]
            val biA = Ular.toBigInteger(a3, a2, a1, a0)
            for (j in quads.indices step 4) {
                val b0 = quads[j + 0]
                val b1 = quads[j + 1]
                val b2 = quads[j + 2]
                val b3 = quads[j + 3]
                val biB = Ular.toBigInteger(b3, b2, b1, b0)
                for (aDelta in deltas) {
                    for (bDelta in deltas) {
                        val tc = TC(biA.add(aDelta).abs(), biB.add(bDelta.abs()))
                        test1(tc)
                    }
                }
            }
        }
    }



    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi(), randBi())
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 320)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val biA = case.biA
        val biB = case.biB
        val expected = case.biProduct
        val ularExpected = Ular.from(expected)

        val ularA = Ular.from(biA)
        val ularB = Ular.from(biB)
        val ularProd = LongArray(ularA.size + ularB.size)
        Ular.mul(ularProd, ularA, ularB)

        val observed = Ular.toBigInteger(ularProd)
        if (! observed.equals(expected)) {
            throw RuntimeException("$biA * $biB = expected:$expected observed:$observed")
        }

        val aOff = random.nextInt(0, 4)
        val aLen = ularA.size
        val a = LongArray(aOff + aLen + random.nextInt(0, 4))
        Ular.set(a, aOff, aLen, biA)

        val bOff = random.nextInt(0, 4)
        val bLen = ularB.size
        val b = LongArray(bOff + bLen + random.nextInt(0, 4))
        Ular.set(b, bOff, bLen, biB)

        val pOff = random.nextInt(0, 4)
        val pLen = aLen + bLen
        val p = LongArray(pOff + pLen + random.nextInt(0, 4))
        p.fill(0xCAFEBABEDEADBEEFuL.toLong())
        Ular.mul(p, pOff, pLen, a, aOff, aLen, b, bOff, bLen)

        val observed2 = Ular.toBigInteger(p, pOff, pLen)

        if (! observed2.equals(expected)) {
            throw RuntimeException("$biA * $biB = expected:$expected observed2:$observed2")
        }




    }

}
