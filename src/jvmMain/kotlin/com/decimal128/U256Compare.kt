package com.decimal128

import com.decimal128.U256Pow10.pow10BitLen
import com.decimal128.U256Pow10.pow10Offset
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh

object U256Compare {

    fun u256IsZero(x: U256): Boolean {
        return x.bitLen == 0
    }

    fun u256IsNotZero(x: U256): Boolean {
        return x.bitLen != 0
    }

    fun u256IsOne(x: U256): Boolean {
        return x.bitLen == 1
    }

    fun u256UnscaledCompare(x:U256, y:U256) : Int {
        if (x.bitLen != y.bitLen)
            return x.bitLen.compareTo(y.bitLen)
        val cmp0 = compareUnsigned(x.dw0, y.dw0)
        val cmp1 = compareUnsigned(x.dw1, y.dw1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        if (x.bitLen <= 128)
            return cmp10
        val cmp2 = compareUnsigned(x.dw2, y.dw2)
        val cmp3 = compareUnsigned(x.dw3, y.dw3)
        val cmp32 = if (cmp3 != 0) cmp3 else cmp2
        val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
        return cmp3210
    }

    fun u256UnscaledCompare(x: U256, y: IntArray): Int {
        require(y.size >= 8)
        val y3 = (y[7].toLong() shl 32) or (y[6].toLong() and MASK32)
        if (x.dw3 != y3)
            return compareUnsigned(x.dw3, y3)
        val y2 = (y[5].toLong() shl 32) or (y[4].toLong() and MASK32)
        if (x.dw2 != y2)
            return compareUnsigned(x.dw2, y2)
        val y1 = (y[3].toLong() shl 32) or (y[2].toLong() and MASK32)
        if (x.dw1 != y1)
            return compareUnsigned(x.dw1, y1)
        val y0 = (y[1].toLong() shl 32) or (y[0].toLong() and MASK32)
        return compareUnsigned(x.dw0, y0)
    }

    fun u256UnscaledEQ(x:U256, y:U256) : Boolean {
        return ((x.bitLen - y.bitLen).toLong() or
                (x.dw0 - y.dw0) or (x.dw1 - y.dw1) or
                (x.dw2 - y.dw2) or (x.dw3 - y.dw3)) == 0L
    }

    fun u256GTOne(x: U256) = x.bitLen > 1

    fun u256ScaledCompare(x:U256, y:U256, pow10Delta: Int) : Int {
        val pow10BitLen = pow10BitLen(pow10Delta)
        val minYBitLen = y.bitLen + pow10BitLen - 1
        val maxYBitLen = y.bitLen + pow10BitLen(pow10Delta + 1)
        if (x.bitLen < minYBitLen)
            return -1
        if (x.bitLen > maxYBitLen)
            return 1
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val pow10Offset = pow10Offset(pow10Delta)
        val pow10dw0 = POW10[(pow10Offset + 0) and 0x3F]
        val pow10dw1 = POW10[(pow10Offset + 1) and 0x3F]
        if (x.bitLen <= 128) {
            val ret = when {
                y.bitLen <= 64 && pow10BitLen <= 64 ->
                    _cmp128x64x64(x1, x0, y0, pow10dw0)
                y.bitLen <= 64 && pow10BitLen <= 128 ->
                    _cmp128x128x64(x1, x0, pow10dw1, pow10dw0, y0)
                y.bitLen <= 128 && pow10BitLen <= 64 ->
                    _cmp128x128x64(x1, x0, y1, y0, pow10dw0)
                else -> throw RuntimeException()
            }
            return ret
        }
        throw RuntimeException("?que? compare should be <= 128 bits")
    }

    private fun _cmp128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Int {
        val p1 = unsignedMultiplyHigh(y0, pow10)
        val p0 = y0 * pow10

        val cmp1 = compareUnsigned(x1, p1)
        val cmp0 = compareUnsigned(x0, p0)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        return cmp10
    }

    private fun _cmp128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Int {
        val pp00Hi = unsignedMultiplyHigh(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = compareUnsigned(x0, p0)

        val pp10Hi = unsignedMultiplyHigh(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = compareUnsigned(x1, p1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        val p2 = carry1 + pp10Hi
        val cmp210 = if (p2 != 0L) -1 else cmp10
        return cmp210
    }

    fun u256ScaledEQ(x:U256, y:U256, pow10Delta: Int) : Boolean {
        val pow10BitLen = pow10BitLen(pow10Delta)
        val minYBitLen = y.bitLen + pow10BitLen - 1
        val maxYBitLen = y.bitLen + pow10BitLen(pow10Delta + 1)
        if (x.bitLen < minYBitLen || x.bitLen > maxYBitLen)
            return false
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val pow10Offset = pow10Offset(pow10Delta)
        val pow10dw0 = POW10[(pow10Offset + 0) and 0x3F]
        val pow10dw1 = POW10[(pow10Offset + 1) and 0x3F]
        if (x.bitLen <= 128) {
            val ret = when {
                y.bitLen <= 64 && pow10BitLen <= 64 ->
                    _EQ128x64x64(x1, x0, y0, pow10dw0)
                y.bitLen <= 64 && pow10BitLen <= 128 ->
                    _EQ128x128x64(x1, x0, pow10dw1, pow10dw0, y0)
                y.bitLen <= 128 && pow10BitLen <= 64 ->
                    _EQ128x128x64(x1, x0, y1, y0, pow10dw0)
                else -> throw RuntimeException()
            }
            return ret
        }
        throw RuntimeException("?que? EQ should be <= 128 bits")
    }

    private fun _EQ128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Boolean {
        val p1 = unsignedMultiplyHigh(y0, pow10)
        val p0 = y0 * pow10

        return ((x1 - p1) or (x0 - p0)) == 0L
    }

    private fun _EQ128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Boolean {
        val pp00Hi = unsignedMultiplyHigh(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo

        val pp10Hi = unsignedMultiplyHigh(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

        val p2 = carry1 + pp10Hi
        return ((x0 - p0) or (x1 - p1) or p2) == 0L
    }


}