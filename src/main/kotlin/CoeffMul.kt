package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
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
        val maxProdBitLen = mBitLen + nBitLen

        assert(m.bitLen >= n.bitLen)
        // mBitLen >= nBitLen

        val m0 = m.dw0
        val n0 = n.dw0
        when {
            (nBitLen <= 0) -> { // can't actually go negative
                z.coeffSetZero()
            }
            (maxProdBitLen > 257) -> {
                throw RuntimeException("coeff mul overflow")
            }
            (mBitLen <= 64) -> {
                val pHi = unsignedMultiplyHigh(m0, n0)
                val pLo = m0 * n0
                z.coeffSet128(pHi, pLo)
            }
            nBitLen <= 64 -> {
                when {
                    (n0 == 1L) -> {
                        z.coeffSet(m)
                    }
                    (n0 and (n0 - 1) == 0L) -> {
                        // even power of 2 ... just shift
                        val ntz = java.lang.Long.numberOfTrailingZeros(n0)
                        coeffSetShiftLeft(z, m, ntz)
                    }
                    else ->
                        _mulCoeff(z, m, nBitLen, n0)
                }
            }

            nBitLen <= 128 -> {
                _mulCoeff(z, m, nBitLen, n.dw1, n.dw0)
            }

            else -> throw RuntimeException("why wasn't this overflow caught earlier with 257 bitLen test?")
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y0: Long) {
        val maxBitLen = x.bitLen + yBitLen
        if (maxBitLen > 257)
            throw RuntimeException("coeff mul overflow")
        when {
            (x.bitLen <=  64) -> _mulCoeff1x1(product, x.dw0, y0)
            (x.bitLen <= 128) -> _mulCoeff2x1(product, maxBitLen, x.dw1, x.dw0, y0)
            (x.bitLen <= 192) -> _mulCoeff3x1(product, maxBitLen, x.dw2, x.dw1, x.dw0, y0)
            else -> _mulCoeff4x1(product, maxBitLen, x.dw3, x.dw2, x.dw1, x.dw0, y0)
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y1: Long, y0: Long) {
        assert(yBitLen in 65..128)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        if (maxBitLen > 257)
            throw RuntimeException("coeff mul overflow")

        if (xBitLen >= yBitLen) {
            when {
                (x.bitLen <= 128) -> _mulCoeff2x2(product, maxBitLen, x.dw1, x.dw0, y1, y0)
                (x.bitLen <= 192) -> _mulCoeff3x2(product, maxBitLen, x.dw2, x.dw1, x.dw0, y1, y0)
                else -> throw RuntimeException("?que? overflow")
            }
        } else {
            when {
                (xBitLen <= 64) -> when {
                    (xBitLen == 0) -> product.coeffSetZero()
                    (xBitLen == 1) -> product.coeffSet128(y1, y0)
                    else -> _mulCoeff2x1(product, maxBitLen, y1, y0, x.dw0)
                }
                else -> _mulCoeff2x2(product, maxBitLen, y1, y0, x.dw1, x.dw0)
            }
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y2: Long, y1: Long, y0: Long) {
        assert(yBitLen in 129..192)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        if (maxBitLen > 257)
            throw RuntimeException("coeff mul overflow")
        if (xBitLen >= yBitLen) {
            throw RuntimeException("?que? overflow")
        } else {
            when {
                (xBitLen <= 64) -> when {
                    (xBitLen == 0) -> product.coeffSetZero()
                    (xBitLen == 1) -> product.coeffSet192(y2, y1, y0)
                    else -> _mulCoeff3x1(product, maxBitLen, y2, y1, y0, x.dw0)
                }
                (xBitLen <= 128) -> _mulCoeff3x2(product, maxBitLen, y2, y1, y0, x.dw1, x.dw0)
            }
        }
    }

    fun mulCoeff(product: Coeff, x: Coeff, yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
        assert(yBitLen in 193..256)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        if (maxBitLen > 257)
            throw RuntimeException("coeff mul overflow")
        if (xBitLen >= yBitLen) {
            throw RuntimeException("?que? overflow")
        } else {
            when {
                (xBitLen == 0) -> product.coeffSetZero()
                (xBitLen == 1) -> product.coeffSet256(y3, y2, y1, y0)
                else -> _mulCoeff4x1(product, maxBitLen, y3, y2, y1, y0, x.dw0)
            }
        }
    }

    private fun _mulCoeff(product: Coeff, m: Coeff, nBitLen: Int, n0: Long) {
        assert(nBitLen in 1..64)
        assert(m.bitLen >= nBitLen)
        val maxBitLen = m.bitLen+ nBitLen
        when {
            (m.bitLen <=  64) -> _mulCoeff1x1(product, m.dw0, n0)
            (m.bitLen <= 128) -> _mulCoeff2x1(product, maxBitLen, m.dw1, m.dw0, n0)
            (m.bitLen <= 192) -> _mulCoeff3x1(product, maxBitLen, m.dw2, m.dw1, m.dw0, n0)
            else -> _mulCoeff4x1(product, maxBitLen, m.dw3, m.dw2, m.dw1, m.dw0, n0)
        }
    }

    private fun _mulCoeff(product: Coeff, m: Coeff, nBitLen: Int, n1: Long, n0: Long) {
        assert(nBitLen in 65..128)
        assert(m.bitLen >= nBitLen)
        val maxBitLen = m.bitLen+ nBitLen
        when {
            (m.bitLen <= 192) -> _mulCoeff3x2(product, maxBitLen, m.dw2, m.dw1, m.dw0, n1, n0)
            else -> throw RuntimeException("?que? overflow")
        }
    }

    private fun _mulCoeff4x4(
        p: Coeff,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long
    ) {
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

    private fun _mulCoeff4x1(
        p: Coeff,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
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

    private fun _mulCoeff3x2(
        p: Coeff,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
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

    private fun _mulCoeff3x1(
        p: Coeff,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
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

    private fun _mulCoeff2x2(
        p: Coeff,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
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

    private fun _mulCoeff2x1(
        p: Coeff,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y0: Long
    ) {
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

    private fun _mulCoeff1x1(
        p: Coeff,
        /* maxBitLen: Int, */
        x0: Long,
        y0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        val p1 = pp00Hi
        p.coeffSet128(p1, p0)
    }

}