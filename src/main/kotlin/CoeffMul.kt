package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import com.decimal128.CoeffDigitLen.POW10
import com.decimal128.CoeffDigitLen.POW10_BIT_LEN
import com.decimal128.CoeffSet.coeffSetShiftLeft

object CoeffMul {

    fun coeffMul(z: Coeff, x: Coeff, y: Coeff) {
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())

        val flipFlop = x.bitLen >= y.bitLen
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val mBitLen = m.bitLen
        val nBitLen = n.bitLen

        // mBitLen >= nBitLen

        val m0 = m.dw0
        val n0 = n.dw0
        when {
            (mBitLen <= 64) -> {
                val pHi = unsignedMultiplyHigh(m0, n0)
                val pLo = m0 * n0
                z.coeffSet128(pHi, pLo)
            }

            nBitLen <= 64 -> {
                when {
                    ((n0 ushr 1) == 0L) -> {
                        if (n0 == 0L)
                            z.coeffSetZero()
                        else
                            z.coeffSet(m)
                    }

                    (n0 and (n0 - 1) == 0L) -> {
                        // even power of 2 ... just shift
                        val ntz = java.lang.Long.numberOfTrailingZeros(n0)
                        coeffSetShiftLeft(z, m, ntz)
                    }

                    else ->
                        mulCoeff(z, m, n.bitLen, n0)
                }
            }

            nBitLen <= 128 -> {
                mulCoeff(z, m, n.bitLen, n.dw1, n.dw0)
            }

            nBitLen <= 192 -> {
                mulCoeff(z, x, y.bitLen, y.dw2, y.dw1, y.dw0)
            }

            else ->
                mulCoeff(z, x, y.bitLen, y.dw3, y.dw2, y.dw1, y.dw0)
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y0: Long) {
        when {
            (x.bitLen <=  64) -> _mulCoeff(product, x.bitLen, x.dw0, yBitLen, y0)
            (x.bitLen <= 128) -> _mulCoeff(product, x.bitLen, x.dw1, x.dw0, yBitLen, y0)
            (x.bitLen <= 192) -> _mulCoeff(product, x.bitLen, x.dw2, x.dw1, x.dw0, yBitLen, y0)
            else -> _mulCoeff(product, x.bitLen, x.dw3, x.dw2, x.dw1, x.dw0, yBitLen, y0)
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y1: Long, y0: Long) {
        when {
            (x.bitLen <=  64) -> _mulCoeff(product, yBitLen, y1, y0, x.bitLen, x.dw0, )
            (x.bitLen <= 128) -> _mulCoeff(product, x.bitLen, x.dw1, x.dw0, yBitLen, y1, y0)
            (x.bitLen <= 192) -> _mulCoeff(product, x.bitLen, x.dw2, x.dw1, x.dw0, yBitLen, y1, y0)
            else -> throw RuntimeException("?que? overflow")
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y2: Long, y1: Long, y0: Long) {
        when {
            (x.bitLen <=  64) -> _mulCoeff(product, yBitLen, y2, y1, y0, x.bitLen, x.dw0)
            (x.bitLen <= 128) -> _mulCoeff(product, yBitLen, y2, y1, y0, x.bitLen, x.dw1, x.dw0)
            else -> throw RuntimeException("?que? overflow")
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
        when {
            (x.bitLen <=  64) -> _mulCoeff(product, yBitLen, y3, y2, y1, y0, x.bitLen, x.dw0)
            else -> throw RuntimeException("?que? overflow")
        }
    }

    fun mulCoeffPow10(product: Coeff, x: Coeff, pow10: Int) {
        val pow10BitLen = POW10_BIT_LEN[pow10].toInt()
        when {
            (pow10BitLen <= 64) -> mulCoeff(product, x, pow10BitLen, POW10[pow10])
            (pow10BitLen <= 128) -> {
                val index = POW10_128_DWORD_INDEX + (pow10 - POW10_128_OFFSET) * 2
                mulCoeff(product, x, pow10BitLen, POW10[index + 1], POW10[index])
            }
            (pow10BitLen <= 192) -> {
                val index = POW10_192_DWORD_INDEX + (pow10 - POW10_192_OFFSET) * 3
                mulCoeff(product, x, pow10BitLen, POW10[index + 2], POW10[index + 1], POW10[index])
            }
            else -> {
                val index = POW10_256_DWORD_INDEX + (pow10 - POW10_256_OFFSET) * 4
                mulCoeff(product, x, pow10BitLen , POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index])
            }
        }
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val pp22Lo = x2 * y2
        val (carry4, p4) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        if ((carry4 or p4) == 0L) {
            assert(maxBitLen == 257)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        yBitLen: Int, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp20Hi + pp30Lo
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)
        if (carry3 == 0L) {
            assert(maxBitLen == 257)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x2: Long, x1: Long, x0: Long,
        yBitLen: Int, y1: Long, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp20Hi + pp21Lo
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo)
        if (carry3 == 0L) {
            assert(maxBitLen == 257)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x2: Long, x1: Long, x0: Long,
        yBitLen: Int, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)

        val p3 = carry2 + pp20Hi
        p.coeffSet256(p3, p2, p1, p0)
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x1: Long, x0: Long,
        yBitLen: Int, y1: Long, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)
        val p3 = carry2 + pp11Hi
        p.coeffSet256(p3, p2, p1, p0)
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x1: Long, x0: Long,
        yBitLen: Int, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val p2 = carry1 + pp10Hi
        p.coeffSet192(p2, p1, p0)
    }

    private fun _mulCoeff(
        p: Coeff,
        xBitLen: Int, x0: Long,
        yBitLen: Int, y0: Long
    ) {
        if (xBitLen == 0 || yBitLen == 0) {
            p.coeffSetZero()
            return
        }
        val maxBitLen = xBitLen + yBitLen
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        /*
        if (maxBitLen <= 64) {
            p.coeffSet64(p0)
            return
        }
        */
        val p1 = pp00Hi
        p.coeffSet128(p1, p0)
    }


}