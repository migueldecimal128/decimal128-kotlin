package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestHugeIntAccum {

    val verbose = false

    @Test
    fun testHugeIntAccum() {
        for (i in 0..<10000) {
            testAddSub()
            testMul()
            testAddAbsValue()
            testAddSquareOf()
        }
    }

    fun testAddSub() {
        val hia = HugeIntAccumulator()
        var hi = HugeInt.ZERO

        for (i in 0..<rng.nextInt(1000)) {
            val n = randomInt()
            hia += n
            hi += n
            assertTrue(hi EQ hia.toHugeInt())
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val w = randomUInt()
            hia += w
            hi += w
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val l = randomLong()
            hia += l
            hi += l
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val dw = randomULong()
            hia += dw
            hi += dw
        }
        assertTrue(hi EQ hia.toHugeInt())


        for (i in 0..<rng.nextInt(100)) {
            val rand = randomHugeInt(200)
            hia += rand
            hi += rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<5) {
            hia += hia
            hi += hi
        }
        assertTrue(hi EQ hia.toHugeInt())

        // now start subtracting

        for (i in 0..<rng.nextInt(1000)) {
            val n = randomInt()
            hia -= n
            hi -= n
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val w = randomUInt()
            hia -= w
            hi -= w
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val l = randomLong()
            hia -= l
            hi -= l
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val dw = randomULong()
            hia -= dw
            hi -= dw
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(100)) {
            val rand = randomHugeInt(200)
            hia -= rand
            hi -= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        hia -= hia
        hi -= hi
        assertTrue(hi EQ hia.toHugeInt())
    }

    fun testMul() {
        val hia = HugeIntAccumulator().set(1)
        var hi = HugeInt.ONE

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomInt()
            if (rand == 0)
                continue
            hia *= rand
            hi *= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomUInt()
            if (rand == 0u)
                continue
            hia *= rand
            hi *= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomLong()
            if (rand == 0L)
                continue
            hia *= rand
            hi *= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomULong()
            if (rand == 0uL)
                continue
            hia *= rand
            hi *= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomHugeInt(200)
            if (rand.isZero())
                continue
            hia *= rand
            hi *= rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<3) {
            hia *= hia
            hi *= hi
        }
        assertTrue(hi EQ hia.toHugeInt())
    }

    fun testAddAbsValue() {
        val hia = HugeIntAccumulator()
        var hi = HugeInt.ZERO

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomInt()
            hia.addAbsValueOf(rand)
            hi += rand.absoluteValue
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomLong()
            hia.addAbsValueOf(rand)
            hi += rand.absoluteValue
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomHugeInt(200)
            hia.addAbsValueOf(rand)
            hi += rand.abs()
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<3) {
            hia.addAbsValueOf(hia)
            hi += hi.absoluteValue
        }
        assertTrue(hi EQ hia.toHugeInt())
    }

    fun testAddSquareOf() {
        val hia = HugeIntAccumulator()
        var hi = HugeInt.ZERO

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomInt()
            hia.addSquareOf(rand)
            hi += rand.absoluteValue.toLong() * rand.absoluteValue.toLong()
        }
        assertTrue(hi EQ hia.toHugeInt())

        hia.setZero()
        hi = HugeInt.ZERO
        for (i in 0..<rng.nextInt(10)) {
            val rand = randomUInt()
            hia.addSquareOf(rand)
            hi += rand.toULong() * rand.toULong()
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomLong()
            hia.addSquareOf(rand)
            hi += HugeInt.from(rand).sqr()
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomULong()
            hia.addSquareOf(rand)
            hi += HugeInt.from(rand).sqr()
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<rng.nextInt(10)) {
            val rand = randomHugeInt(200)
            if (rand.isZero())
                continue
            hia.addSquareOf(rand)
            hi += if (rng.nextBoolean()) rand.sqr() else rand * rand
        }
        assertTrue(hi EQ hia.toHugeInt())

        for (i in 0..<3) {
            hia.addSquareOf(hia)
            hi += if (rng.nextBoolean()) hi.sqr() else hi * hi
        }
        assertTrue(hi EQ hia.toHugeInt())
    }

    fun testUnsigned() {
        val mhi = HugeIntAccumulator()
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val w = rng.nextUInt()
        if (verbose)
            println("mhi:$mhi w:$w")
        var sumBi = bi + BigInteger.valueOf(w.toLong())
        val sum1 = mhi.copy()
        sum1 += HugeInt.from(w)
        val sum2 = mhi.copy()
        sum2 += w
        assertEquals("$sum1", "$sumBi")
        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        for (i in 0..<25) {
            val w2 = rng.nextUInt()
            sum1 += HugeInt.from(w2)
            sum2 += w2
            sumBi += BigInteger.valueOf(w2.toLong())
        }

        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertEquals("$sum1", "$sum2")
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        val sum1Save = HugeIntAccumulator().set(sum1)
        sum1 += sum2
        sum2 += sum2

        val sumBi1 = sumBi + sumBi
        assertTrue(sum1.toHugeInt() EQ sumBi1.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

    }

    fun testSigned() {
        val mhi = HugeIntAccumulator()
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val n = rng.nextInt()
        if (verbose)
            println("mhi:$mhi n:$n")
        var sumBi = bi + BigInteger.valueOf(n.toLong())
        val sum1 = mhi.copy()
        sum1 += HugeInt.from(n)
        val sum2 = mhi.copy()
        sum2 += n
        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val n2 = rng.nextUInt()
            sum1 += HugeInt.from(n2)
            sum2 += n2
            sumBi += BigInteger.valueOf(n2.toLong())
        }

        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())
    }

    val rng = Random.Default

    fun randomHugeInt(hiBitLen: Int): HugeInt {
        val n = rng.nextInt(hiBitLen)
        val v = HugeInt.fromRandom(n, rng)
        return if (rng.nextBoolean()) v.negate() else v
    }

    fun randomInt(): Int {
        val n = rng.nextInt(31)
        val v = rng.nextInt(1 shl n)
        return if (rng.nextBoolean()) -v else v
    }

    fun randomUInt(): UInt =
        rng.nextLong(1L shl rng.nextInt(33)).toUInt()

    fun randomLong(): Long {
        val n = rng.nextInt(63)
        val v = rng.nextLong(1L shl n)
        return if (rng.nextBoolean()) -v else v
    }

    fun randomULong(): ULong {
        val n = rng.nextInt(64)
        val v = if (n < 63)
            (rng.nextLong(1L shl n) shl 1) + rng.nextInt(2)
        else
            rng.nextLong()
        return v.toULong()
    }
}