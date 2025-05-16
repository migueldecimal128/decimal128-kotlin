package com.decimal128

import java.lang.Long.numberOfTrailingZeros


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
