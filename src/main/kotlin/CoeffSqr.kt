package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import com.decimal128.CoeffSet.coeffSetShiftLeft

internal object CoeffSqr {

    fun coeffSqr(z: Coeff, x: Coeff) {
        val xBitLen = x.bitLen
        when {
            (xBitLen <= 64) -> {
                val p1 = unsignedMultiplyHigh(x.dw0, x.dw0)
                val p0 = x.dw0 * x.dw0
                return
            }
            (xBitLen <= 96) -> {
                val (p2, p1, p0) = usqr96to192(x.dw1, x.dw0)
                z.coeffSet192(p2, p1, p0)
            }
            (xBitLen <= 128) -> {
                _sqrCoeff2to4(z, x.dw1, x.dw0)
            }
            else -> throw RuntimeException("coeff mul overflow")
        }
    }

    private fun _sqrCoeff2to4(
        p: Coeff,
        x1: Long, x0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, x0)
        val pp00Lo = x0 * x0
        val p0 = pp00Lo

        val pp01Hi = unsignedMultiplyHigh(x0, x1)
        val pp01Lo = x0 * x1
        val pp10Hi = pp01Hi
        val pp10Lo = pp01Lo
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

        val pp11Hi = unsignedMultiplyHigh(x1, x1)
        val pp11Lo = x1 * x1
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)

        val p3 = carry2 + pp11Hi
        p.coeffSet256(p3, p2, p1, p0)
    }

}
