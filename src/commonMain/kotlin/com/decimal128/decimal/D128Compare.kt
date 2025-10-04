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

}