package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger

import java.util.Random

fun multu64u64u128(a: Long, b: Long): Pair<Long, Long> {
    val hiSigned = Math.multiplyHigh(a, b)
    val lo = a * b
    val unsignedCorrection = ((a shr 63) and b) + ((b shr 63) and a)
    val hiUnsigned = hiSigned + unsignedCorrection
    return hiUnsigned to lo
}

fun unsignedFMA64(a: Long, b: Long, carryIn: Long): Pair<Long, Long> {
    val unsignedCorrection = ((a shr 63) and b) + ((b shr 63) and a)
    val loBase = a * b
    val hiSigned = Math.multiplyHigh(a, b)
    val hiBase = hiSigned + unsignedCorrection
    val lo = loBase + carryIn
    val carryWithin = if ((lo xor Long.MIN_VALUE) < (loBase xor Long.MIN_VALUE)) 1L else 0L
    val hi = hiBase + carryWithin
    return hi to lo
}



fun arrayFrom(bi: BigInteger) : LongArray {
    if (bi.equals(BigInteger.ZERO))
        return longArrayOf(0)
    var biT = bi
    val arrayList = ArrayList<Long>()
    do {
        arrayList.add(biT.toLong())
        biT = biT.shiftRight(64)
    } while (! biT.equals(BigInteger.ZERO))
    return arrayList.toLongArray()
}
fun arrayFrom(str: String) = arrayFrom(BigInteger(str))

fun biFromArray(dwords: LongArray) = biFromArray(dwords, 0, dwords.size)

fun biFromArray(dwords: LongArray, off: Int, len: Int) : BigInteger {
    var bi = BigInteger.ZERO
    for (i in 0..<len) {
        val biT = biFrom(dwords[off + i]).shiftLeft(i * 64)
        bi = bi.or(biT)
    }
    return bi
}

fun biFromArray(words: IntArray) = biFromArray(words, 0, words.size)

fun biFromArray(words: IntArray, off: Int, len: Int) : BigInteger {
    var bi = BigInteger.ZERO
    for (i in 0..<len) {
        val biT = biFrom(words[off + i]).shiftLeft(i * 32)
        bi = bi.or(biT)
    }
    return bi
}

fun dw1From(str: String) = BigInteger(str).shiftRight(64).toLong()
fun dw0From(str: String) = BigInteger(str).toLong()

fun biFrom(dw: Long) : BigInteger {
    val sixty3 = dw and Long.MAX_VALUE
    var bi = BigInteger(sixty3.toString())
    if (dw < 0)
        bi = bi or BigInteger.ONE.shiftLeft(63)
    return bi
}

fun biFrom(dw1: Long, dw0: Long) = biFrom(dw1).shiftLeft(64).or(biFrom(dw0))

fun biFrom(dw2: Long, dw1: Long, dw0: Long) = biFrom(dw2).shiftLeft(128).or(biFrom(dw1).shiftLeft(64).or(biFrom(dw0)))

fun biFrom(w: Int) : BigInteger {
    val thirty1 = w and Int.MAX_VALUE
    var bi = BigInteger(thirty1.toString())
    if (w < 0)
        bi = bi or BigInteger.ONE.shiftLeft(31)
    return bi
}

fun biFrom(w1: Int, w0: Int) = biFrom(w1).shiftLeft(32).or(biFrom(w0))

fun biFrom(w2: Int, w1: Int, w0: Int) = biFrom(w2).shiftLeft(64).or(biFrom(w1).shiftLeft(32).or(biFrom(w0)))





class TestUnsignedMult64x64to128 {

    class TC(multiplicandStr: String, multiplierStr: String) {
        val biMultiplicand = BigInteger(multiplicandStr)
        val biMultiplier = BigInteger(multiplierStr)
        val biProduct = biMultiplicand.multiply(biMultiplier)
        val productDw1 = biProduct.shiftRight(64).toLong()
        val productDw0 = biProduct.toLong()
        val multiplicandArray = arrayFrom(biMultiplicand)
        val multiplierArray = arrayFrom(biMultiplier)
        val multiplicandDw1 = biMultiplicand.shiftRight(64).toLong()
        val multiplicandDw0 = biMultiplicand.toLong()
        val multiplicandFitsInLong = biMultiplicand.shiftRight(64).equals(BigInteger.ZERO)
        val multiplierDw1 = biMultiplier.shiftRight(64).toLong()
        val multiplierDw0 = biMultiplier.toLong()
        val multiplierFitsInLong = biMultiplier.shiftRight(64).equals(BigInteger.ZERO)
    }

    val tcs = arrayOf(
        TC( 0xFFFFFFFFFFFFFFFFuL.toString(), "1"),
        TC("1", "1"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            testCase(tc)
    }

    fun testCase(tc: TC) {
        val (observedDw1, observedDw0) = multu64u64u128(tc.multiplicandDw0, tc.multiplierDw0)
        assert(observedDw0 == tc.productDw0)
        assert(observedDw1 == tc.productDw1)
    }

    @Test
    fun testRandoms() {
        for (i in 0..<1000000)
            testRandom1()
    }

    val random = Random()

    fun testRandom1() {
        val multiplicandBitLength = random.nextInt(1, 65)
        val biMultiplicand = BigInteger(multiplicandBitLength, random)

        val multiplierBitLength = random.nextInt(1, 65)
        val biMultiplier = BigInteger(multiplierBitLength, random)

        val tc = TC(biMultiplicand.toString(), biMultiplier.toString())

        testCase(tc)
    }
}