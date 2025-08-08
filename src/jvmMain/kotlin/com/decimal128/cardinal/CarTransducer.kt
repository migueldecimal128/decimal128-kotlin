package com.decimal128.cardinal

import com.decimal128.decimal.unsignedCmp
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max


// CAR == Cardinal ARray
// cardinal has fallen out of use, but is an unsigned integer
// the "cardinality" of a set
object CarTransducer {

    @Suppress("NOTHING_TO_INLINE")
    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    fun carToBi(x: IntArray): BigInteger {
        var bi = BigInteger.valueOf(U32(x[0]))
        for (i in x.size-1 downTo 1) {
            val t = BigInteger.valueOf(U32(x[i])).shiftLeft(i * 32)
            bi = bi.or(t)
        }
        return bi
    }

    fun carFromBi(bi: BigInteger): IntArray {
        val bitLen = bi.bitLength()
        val wordLen = (bitLen + 0x1F) ushr 5
        val car = IntArray(max(1, wordLen))
        for (i in 0..<wordLen)
            car[i] = bi.shiftRight(i * 32).toInt()
        return car
    }

    fun carToString(car: IntArray): String {
        return carToBi(car).toString()
    }

    fun carFromString(str: String): IntArray {
        return carFromBi(BigInteger(str))
    }

    fun compare(car: IntArray, bi: BigInteger): Int {
        val carBitLen = Car.bitLen(car)
        val cmpBitLen = carBitLen.compareTo(bi.bitLength())
        if (cmpBitLen != 0)
            return cmpBitLen
        for (i in car.size - 1 downTo 0) {
            val cmp = unsignedCmp(car[i], bi.shiftRight(i * 32).toInt())
            if (cmp != 0)
                return cmp
        }
        return 0
    }

    fun EQ(car: IntArray, bi:BigInteger) = compare(car, bi) == 0

    fun calcDigitCount(car: IntArray): Int {
        val bi = carToBi(car)
        val bd = BigDecimal(bi)
        val precision = bd.precision()
        return precision
    }
}
