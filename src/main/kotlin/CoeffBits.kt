package com.decimal128

import java.lang.Long.*

@Suppress("NOTHING_TO_INLINE")
inline fun calcBitLen64(dw0: Long): Int {
    val nlz0 = numberOfLeadingZeros(dw0)
    val bitLen = 64 - nlz0
    return bitLen
}

@Suppress("NOTHING_TO_INLINE")
inline fun calcBitLen128(dw1: Long, dw0: Long): Int {
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz1 = numberOfLeadingZeros(dw1)
    val nlz0 = numberOfLeadingZeros(dw0)
    val bitLen = 128 - nlz1 - (nlz0 and dw1IsZeroMask)
    return bitLen
}

@Suppress("NOTHING_TO_INLINE")
inline fun calcBitLen192(dw2: Long, dw1: Long, dw0: Long): Int {
    val dw2IsZeroMask = ((dw2 or -dw2) shr 63).inv().toInt()
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz2 = numberOfLeadingZeros(dw2)
    val nlz1 = numberOfLeadingZeros(dw1)
    val nlz0 = numberOfLeadingZeros(dw0)
    val nlz10 = nlz1 + (nlz0 and dw1IsZeroMask)
    val bitLen = 192 - nlz2 - (nlz10 and dw2IsZeroMask)
    return bitLen
}

@Suppress("NOTHING_TO_INLINE")
inline fun calcBitLen256(dw3: Long, dw2: Long, dw1: Long, dw0: Long): Int {
    val dw3IsZeroMask = ((dw3 or -dw3) shr 63).inv().toInt()
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val dw23 = dw2 or dw3
    val dw23IsZeroMask = ((dw23 or -dw23) shr 63).inv().toInt()

    val nlz3 = numberOfLeadingZeros(dw3)
    val nlz2 = numberOfLeadingZeros(dw2)
    val nlz1 = numberOfLeadingZeros(dw1)
    val nlz0 = numberOfLeadingZeros(dw0)
    val nlz23 = nlz3 + (nlz2 and dw3IsZeroMask)
    val nlz10 = nlz1 + (nlz0 and dw1IsZeroMask)
    val bitLen = 256 - nlz23 - (nlz10 and dw23IsZeroMask)
    return bitLen
}

private const val MASK_BITS_0_MOD_4 = 0x1111111111111111L
private const val MASK_BITS_1_MOD_4 = MASK_BITS_0_MOD_4 shl 1
private const val MASK_BITS_2_MOD_4 = MASK_BITS_0_MOD_4 shl 2
private const val MASK_BITS_3_MOD_4 = MASK_BITS_0_MOD_4 shl 3

object CoeffBits {

    fun numberOfTrailingZeros(x: Coeff): Int {
        val ntz0 = numberOfTrailingZeros(x.dw0)
        val ntz1 = 64 + numberOfTrailingZeros(x.dw1)
        val ntz2 = 128 + numberOfTrailingZeros(x.dw2)
        val ntz3 = 192 + numberOfTrailingZeros(x.dw3)
        val ntz01 = if (x.dw0 != 0L) ntz0 else ntz1
        val ntz23 = if (x.dw2 != 0L) ntz2 else ntz3
        val ntz0123 = if ((x.dw0 or x.dw1) != 0L) ntz01 else ntz23
        return ntz0123
    }

    fun getDwordAtBitIndex(x: Coeff, bitIndex: Int): Long {
        val dwordShift = bitIndex ushr 6
        val innerShift = bitIndex and 0x3F
        val nonZeroMask = (-innerShift shr 31).toLong()
        return when (dwordShift) {
            0 -> (nonZeroMask and (x.dw1 shl -innerShift)) or (x.dw0 ushr innerShift)
            1 -> (nonZeroMask and (x.dw2 shl -innerShift)) or (x.dw1 ushr innerShift)
            2 -> (nonZeroMask and (x.dw3 shl -innerShift)) or (x.dw2 ushr innerShift)
            3 ->                                                 (x.dw3 ushr innerShift)
            else -> 0L
        }
    }

    fun isMultipleOfFive_64(dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw0 and m0)
        val count1 = bitCount(dw0 and m1)
        val count2 = bitCount(dw0 and m2)
        val count3 = bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_128(dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_192(dw2: Long, dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw2 and m0) + bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw2 and m1) + bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw2 and m2) + bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw2 and m3) + bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun isMultipleOfFive_256(dw3:Long, dw2: Long, dw1:Long, dw0: Long): Boolean {
        val m0 = MASK_BITS_0_MOD_4
        val m1 = MASK_BITS_1_MOD_4
        val m2 = MASK_BITS_2_MOD_4
        val m3 = MASK_BITS_3_MOD_4

        val count0 = bitCount(dw3 and m0) + bitCount(dw2 and m0) +
                bitCount(dw1 and m0) + bitCount(dw0 and m0)
        val count1 = bitCount(dw3 and m1) + bitCount(dw2 and m1) +
                bitCount(dw1 and m1) + bitCount(dw0 and m1)
        val count2 = bitCount(dw3 and m2) + bitCount(dw2 and m2) +
                bitCount(dw1 and m2) + bitCount(dw0 and m2)
        val count3 = bitCount(dw3 and m3) + bitCount(dw2 and m3) +
                bitCount(dw1 and m3) + bitCount(dw0 and m3)

        val weightedSum = count0 * 1 + count1 * 2 + count2 * 4 + count3 * 3
        val ret = ((weightedSum * 838861) ushr 22) * 5 == weightedSum
        return ret
    }

    fun coeffIsMultipleOf5(x: Coeff): Boolean {
        val bitLen = x.bitLen
        return when {
            bitLen <=  64 -> isMultipleOfFive_64(x.dw0)
            bitLen <= 128 -> isMultipleOfFive_128(x.dw1, x.dw0)
            bitLen <= 192 -> isMultipleOfFive_192(x.dw2, x.dw1, x.dw0)
            else -> isMultipleOfFive_256(x.dw3, x.dw2, x.dw1, x.dw0)
        }
    }

}
