package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.pow10BitLen
import com.decimal128.decimal.U256Pow10.pow10Offset

object C128Compare {

    fun c128IsZero(x: Decimal) = x.lengths.toInt() == 0

    fun c128IsNotZero(x: Decimal) = x.lengths.toInt() != 0

    fun c128IsOne(x: Decimal) = x.lengths.toInt() == ((1 shl 9) or 1)

    fun c128UnscaledCompare(x: Decimal, y: Decimal) : Int {
        if (x.lengths != y.lengths)
            return x.lengths.compareTo(y.lengths)
        val cmp0 = unsignedCmp(x.dw0, y.dw0)
        val cmp1 = unsignedCmp(x.dw1, y.dw1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        return cmp10
    }

    fun c128UnscaledEQ(x: Decimal, y: Decimal) : Boolean {
        return ((x.lengths - y.lengths).toLong() or
                (x.dw0 - y.dw0) or (x.dw1 - y.dw1)) == 0L
    }

    fun c128GTOne(x: Decimal) = x.lengths.toInt() > ((1 shl 9) or 1)

    fun c128ScaledCompare(x: Decimal, y: Decimal, pow10Delta: Int) : Int {
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

    private fun _cmp128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Int {
        val p1 = unsignedMulHi(y0, pow10)
        val p0 = y0 * pow10

        val cmp1 = unsignedCmp(x1, p1)
        val cmp0 = unsignedCmp(x0, p0)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        return cmp10
    }

    private fun _cmp128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Int {
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = unsignedCmp(x1, p1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        val p2 = carry1 + pp10Hi
        val cmp210 = if (p2 != 0L) -1 else cmp10
        return cmp210
    }

    fun c128ScaledEQ(x: Decimal, y: Decimal, pow10Delta: Int) : Boolean {
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

    private fun _EQ128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Boolean {
        val p1 = unsignedMulHi(y0, pow10)
        val p0 = y0 * pow10

        return ((x1 - p1) or (x0 - p0)) == 0L
    }

    private fun _EQ128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Boolean {
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo

        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

        val p2 = carry1 + pp10Hi
        return ((x0 - p0) or (x1 - p1) or p2) == 0L
    }


}