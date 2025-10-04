package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.pow10BitLen
import com.decimal128.decimal.U256Pow10.pow10Offset
import kotlin.math.max
import kotlin.math.min

object D128Compare {

    fun isZero(d: Decimal) =
        d.packedLengths.toInt() == 0 && d.qExp != NON_FINITE_INF

    fun isOne(d: Decimal): Boolean = d.packedLengths.toInt() == 0x0201

    fun compare(x: Decimal, y: Decimal) : Int {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < NON_FINITE_INF -> compareFinite(x, y)
            qMax == NON_FINITE_INF -> compareInfinite(x, y)
            else -> compareNaN(x, y)
        }
    }

    private fun compareFinite(x: Decimal, y: Decimal) : Int {
        return when {
            x.isZero() and y.isZero() -> 0
            x.sign != y.sign -> if (x.sign) -1 else 1
            x.packedLengths != y.packedLengths ->
                if ((x.packedLengths > y.packedLengths) xor x.sign) 1 else -1
            ((x.dw1 - y.dw1) or (x.dw0 - y.dw0)) == 0L -> 0
            else -> {
                val cmpMag = ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
                check (cmpMag == 1 || cmpMag == -1)
                return if (x.sign) -cmpMag else cmpMag
            }
        }
    }

    private fun compareInfinite(x: Decimal, y: Decimal) : Int {
        check (max(x.qExp, y.qExp) == NON_FINITE_INF)
        val minExp = min(x.qExp, y.qExp)
        return when {
            x.sign != y.sign -> if (x.sign) -1 else 1
            minExp == NON_FINITE_INF -> 0
            x.qExp == NON_FINITE_INF -> if (x.sign) -1 else 1
            else -> if (x.sign) 1 else -1
        }
    }

    private fun compareNaN(x: Decimal, y: Decimal) : Int {
        val minExp = min(x.qExp, y.qExp)
        return when {
            minExp >= NON_FINITE_QNAN -> 0
            x.qExp >= NON_FINITE_QNAN -> 1
            y.qExp >= NON_FINITE_QNAN -> -1
            else -> throw IllegalStateException()
        }
    }

    fun finiteUnscaledCompare(x:Decimal, y:Decimal) : Int {
        check (min(x.qExp, y.qExp) < NON_FINITE_INF)
        if (x.bitLen != y.bitLen)
            return x.bitLen.compareTo(y.bitLen)
        return ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
    }

    fun u256UnscaledCompare(x: U256, y: IntArray): Int {
        require(y.size >= 8)
        val y3 = (y[7].toLong() shl 32) or (y[6].toLong() and MASK32)
        if (x.dw3 != y3)
            return unsignedCmp(x.dw3, y3)
        val y2 = (y[5].toLong() shl 32) or (y[4].toLong() and MASK32)
        if (x.dw2 != y2)
            return unsignedCmp(x.dw2, y2)
        val y1 = (y[3].toLong() shl 32) or (y[2].toLong() and MASK32)
        if (x.dw1 != y1)
            return unsignedCmp(x.dw1, y1)
        val y0 = (y[1].toLong() shl 32) or (y[0].toLong() and MASK32)
        return unsignedCmp(x.dw0, y0)
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