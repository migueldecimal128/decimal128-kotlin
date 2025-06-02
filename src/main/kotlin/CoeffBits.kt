package com.decimal128

import java.lang.Long.numberOfLeadingZeros
import java.lang.Long.numberOfTrailingZeros

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
}
