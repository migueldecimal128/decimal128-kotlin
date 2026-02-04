package com.decimal128.decimal

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object D128Compare {

    fun isZero(d: Decimal) =
        d.packedLengths.toInt() == 0 && d.qExp != NON_FINITE_INF

    fun isOne(d: Decimal): Boolean = d.packedLengths.toInt() == 0x0201

    fun compare(x: Decimal, y: Decimal) : Int {
        val qMax = max(x.qExp, y.qExp)
        when {
            qMax >= NON_FINITE_QNAN -> return when {
                x.isNaN() && y.isNaN() -> 0
                x.isNaN() -> 1
                else -> -1
            }
            x.isZero() && y.isZero() -> return 0
            x.sign != y.sign -> return if (x.sign) -1 else 1
        }
        val cmp =
            if (qMax == NON_FINITE_INF)
                magnitudeCompareInfinite(x, y)
            else
                magnitudeCompareFinite(x, y)
        return if (x.sign) -cmp else cmp
    }

    fun magnitudeCompare(x: Decimal, y: Decimal) : Int {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < NON_FINITE_INF -> magnitudeCompareFinite(x, y)
            qMax == NON_FINITE_INF -> magnitudeCompareInfinite(x, y)
            else -> magnitudeCompareNaN(x, y)
        }
    }

    private fun magnitudeCompareFinite(x: Decimal, y: Decimal) : Int {
        if (x.qExp == y.qExp)
            return ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        val cmpSci = x.eExp.compareTo(y.eExp)
        if (cmpSci != 0)
            return cmpSci
        val qDelta = x.qExp - y.qExp
        val qDeltaAbs = abs(qDelta)
        val pow10BitLen = pow10BitLen(qDeltaAbs)
        val (dw1Pow10, dw0Pow10) = pow10_128(qDeltaAbs)
        if (qDelta > 0) {
            // x.qExp is larger
            // scale up x.coefficient
            if (pow10BitLen <= 64)
                return -ucmp128_128x64(y.dw1, y.dw0, x.dw1, x.dw0, dw0Pow10)
            return -ucmp128_128x64(y.dw1, y.dw0, dw1Pow10, dw0Pow10, x.dw0)
        } else {
            // scale up y
            if (pow10BitLen <= 64)
                return ucmp128_128x64(x.dw1, x.dw0, y.dw1, y.dw0, dw0Pow10)
            return ucmp128_128x64(x.dw1, x.dw0, dw1Pow10, dw0Pow10, y.dw0)
        }
    }

    private fun magnitudeCompareInfinite(x: Decimal, y: Decimal) : Int {
        verify { max(x.qExp, y.qExp) == NON_FINITE_INF }
        val minExp = min(x.qExp, y.qExp)
        return when {
            minExp == NON_FINITE_INF -> 0
            x.qExp == NON_FINITE_INF -> 1
            else -> -1
        }
    }

    private fun magnitudeCompareNaN(x: Decimal, y: Decimal) : Int {
        val minExp = min(x.qExp, y.qExp)
        return when {
            minExp >= NON_FINITE_QNAN -> 0
            x.qExp >= NON_FINITE_QNAN -> 1
            y.qExp >= NON_FINITE_QNAN -> -1
            else -> throw IllegalStateException()
        }
    }
}
