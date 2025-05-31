package com.decimal128

import com.decimal128.CoeffPow10.pow10BitLen
import com.decimal128.CoeffPow10.pow10Offset
import java.lang.Math.max
import java.lang.Math.unsignedMultiplyHigh

object CoeffFma {

    fun coeffFma(z: Coeff, x: Coeff, y: Coeff, a: Coeff) {
        assert(z.coeffHasValidLengths())
        assert(x.coeffHasValidLengths())
        assert(y.coeffHasValidLengths())
        assert(a.coeffHasValidLengths())
        val flipFlop = x.bitLen >= y.bitLen
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val mBitLen = m.bitLen
        val nBitLen = n.bitLen
        assert(mBitLen >= nBitLen)
        val aBitLen = a.bitLen
        val m0 = m.dw0
        val m1 = m.dw1
        val n0 = n.dw0
        val a0 = a.dw0
        val a1 = a.dw1

        val maxFusedBitLen = max(mBitLen + nBitLen, a.bitLen) + 1
        if (nBitLen <= 64) {
            when {
                (mBitLen <= 64 && aBitLen <= 128) ->
                    _fma1x1x2(
                        z, maxFusedBitLen,
                        m0,
                        n0,
                        a1, a0
                    )

                (mBitLen <= 128 && aBitLen <= 192) ->
                    _fma2x1x3(
                        z, maxFusedBitLen,
                        m1, m0,
                        n0,
                        a.dw2, a1, a0
                    )

                (mBitLen <= 192) ->
                    _fma3x1x4(
                        z, maxFusedBitLen,
                        m.dw2, m1, m0,
                        n0,
                        a.dw3, a.dw2, a1, a0
                    )

                else ->
                    _fma4x1x4(
                        z, maxFusedBitLen,
                        m.dw3, m.dw2, m1, m0,
                        n0,
                        a.dw3, a.dw2, a1, a0
                    )
            }
            return
        }
        val n1 = n.dw1
        if (nBitLen <= 128) {
            when {
                (mBitLen <= 128) ->
                    _fma2x2x4(
                        z, maxFusedBitLen,
                        m1, m0,
                        n1, n0,
                        a.dw3, a.dw2, a1, a0
                    )
                (mBitLen <= 192) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        m.dw2, m1, m0,
                        n1, n0,
                        a.dw3, a.dw2, a1, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        throw RuntimeException("coeff overflow")
    }

    fun coeffFmaPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff) {
        assert(pow10 >= 0)
        assert(z.coeffHasValidLengths())
        assert(x.coeffHasValidLengths())
        assert(a.coeffHasValidLengths())
        val xBitLen = x.bitLen
        val aBitLen = a.bitLen
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()
        val x0 = x.dw0
        val x1 = x.dw1
        val a0 = a.dw0
        val a1 = a.dw1
        val maxFusedBitLen = max(xBitLen + p10BitLen, aBitLen) + 1
        if (maxFusedBitLen <= 128) {
            val (f1, f0) = umul128x128to128(x1, x0, p1, p0)
            val (s1, s0) = sumU128(f1, f0, a1, a0)
            z.coeffSet128(s1, s0)
            return
        }
        if (p10BitLen <= 64) {
            when {
                (xBitLen <= 64 && aBitLen <= 128) ->
                    _fma1x1x2(z, maxFusedBitLen,
                        x0,
                        p0,
                        a1, a0
                    )
                (xBitLen <= 128 && aBitLen <= 192) ->
                    _fma2x1x3(z, maxFusedBitLen,
                        x1, x0,
                        p0,
                        a.dw2, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x1x4(z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p0,
                        a.dw3, a.dw2, a1, a0
                    )
                else ->
                    _fma4x1x4(z, maxFusedBitLen,
                        x.dw3, x.dw2, x1, x0,
                        p0,
                        a.dw3, a.dw2, a1, a0)
            }
            return
        }
        if (p10BitLen <= 128) {
            when {
                (xBitLen <= 64 && aBitLen <= 128) ->
                    _fma2x1x3(
                        z, maxFusedBitLen,
                        p1, p0,
                        x0,
                        a.dw2, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fma2x2x4(
                        z, maxFusedBitLen,
                        x1, x0,
                        p1, p0,
                        a.dw3, a.dw2, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p1, p0,
                        a.dw3, a.dw2, a1, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        val p2 = POW10[pow10Offset + 2]
        if (p10BitLen <= 192) {
            when {
                (xBitLen <= 64) ->
                    _fma3x1x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x0,
                        a.dw3, a.dw2, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x1, x0,
                        a.dw3, a.dw2, a1, a0
                    )
                else -> throw RuntimeException("coeff overflow")
            }
            return
        }
        val p3 = POW10[pow10Offset + 3]
        if (xBitLen <= 64) {
            _fma4x1x4(
                z, maxFusedBitLen,
                p3, p2, p1, p0,
                x0,
                a.dw3, a.dw2, a1, a0
            )
            return
        } else {
            throw RuntimeException("coeff overflow")
        }
    }

    fun coeffFmaPow10(z: Coeff, x: Coeff, pow10: Int, a1: Long, a0: Long) {
        assert(pow10 >= 0)
        assert(z.coeffHasValidLengths())
        assert(x.coeffHasValidLengths())
        val xBitLen = x.bitLen
        val aBitLen = (
                if (a1 == 0L)
                    64 - java.lang.Long.numberOfLeadingZeros(a0)
                else
                    128 - java.lang.Long.numberOfLeadingZeros(a1)
                )
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()
        val x0 = x.dw0
        val x1 = x.dw1
        val maxFusedBitLen = max(xBitLen + p10BitLen, aBitLen) + 1
        if (maxFusedBitLen <= 128) {
            val (f1, f0) = umul128x128to128(x1, x0, p1, p0)
            val (s1, s0) = sumU128(f1, f0, a1, a0)
            z.coeffSet128(s1, s0)
            return
        }
        if (p10BitLen <= 64) {
            when {
                (xBitLen <= 64) ->
                    _fma1x1x2(z, maxFusedBitLen,
                        x0,
                        p0,
                        a1, a0
                    )
                (xBitLen <= 128) ->
                    _fma2x1x3(z, maxFusedBitLen,
                        x1, x0,
                        p0,
                        0L, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x1x4(z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p0,
                        0L, 0L, a1, a0
                    )
                else ->
                    _fma4x1x4(z, maxFusedBitLen,
                        x.dw3, x.dw2, x1, x0,
                        p0,
                        0L, 0L, a1, a0)
            }
            return
        }
        if (p10BitLen <= 128) {
            when {
                (xBitLen <= 64) ->
                    _fma2x1x3(
                        z, maxFusedBitLen,
                        p1, p0,
                        x0,
                        0L, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fma2x2x4(
                        z, maxFusedBitLen,
                        x1, x0,
                        p1, p0,
                        0L, 0L, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p1, p0,
                        0L, 0L, a1, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        val p2 = POW10[pow10Offset + 2]
        if (p10BitLen <= 192) {
            when {
                (xBitLen <= 64) ->
                    _fma3x1x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x0,
                        0L, 0L, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x1, x0,
                        0L, 0L, a1, a0
                    )
                else -> throw RuntimeException("coeff overflow")
            }
            return
        }
        val p3 = POW10[pow10Offset + 3]
        if (xBitLen <= 64) {
            _fma4x1x4(
                z, maxFusedBitLen,
                p3, p2, p1, p0,
                x0,
                0L, 0L, a1, a0
            )
            return
        } else {
            throw RuntimeException("coeff overflow")
        }
    }

    fun coeffFmaPow10(z: Coeff, x: Coeff, pow10: Int, a0: Long) {
        assert(pow10 >= 0)
        assert(z.coeffHasValidLengths())
        assert(x.coeffHasValidLengths())
        val xBitLen = x.bitLen
        if (xBitLen == 0) {
            z.coeffSet64(a0)
            return
        }
        val aBitLen = 64 - java.lang.Long.numberOfLeadingZeros(a0)
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()
        val x0 = x.dw0
        val x1 = x.dw1
        val maxFusedBitLen = max(xBitLen + p10BitLen, aBitLen) + 1
        if (maxFusedBitLen <= 128) {
            val (f1, f0) = umul128x128to128(x1, x0, p1, p0)
            val (s1, s0) = sumU128U64(f1, f0, a0)
            z.coeffSet128(s1, s0)
            return
        }
        if (p10BitLen <= 64) {
            when {
                (xBitLen <= 64) ->
                    _fma1x1x2(z, maxFusedBitLen,
                        x0,
                        p0,
                        0L, a0
                    )
                (xBitLen <= 128) ->
                    _fma2x1x3(z, maxFusedBitLen,
                        x1, x0,
                        p0,
                        0L, 0L, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x1x4(z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p0,
                        0L, 0L, 0L, a0
                    )
                else ->
                    _fma4x1x4(z, maxFusedBitLen,
                        x.dw3, x.dw2, x1, x0,
                        p0,
                        0L, 0L, 0L, a0)
            }
            return
        }
        if (p10BitLen <= 128) {
            when {
                (xBitLen <= 64) ->
                    _fma2x1x3(
                        z, maxFusedBitLen,
                        p1, p0,
                        x0,
                        0L, 0L, a0
                    )
                (xBitLen <= 128) ->
                    _fma2x2x4(
                        z, maxFusedBitLen,
                        x1, x0,
                        p1, p0,
                        0L, 0L, 0L, a0
                    )
                (xBitLen <= 192) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p1, p0,
                        0L, 0L, 0L, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        val p2 = POW10[pow10Offset + 2]
        if (p10BitLen <= 192) {
            when {
                (xBitLen <= 64) ->
                    _fma3x1x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x0,
                        0L, 0L, 0L, a0
                    )
                (xBitLen <= 128) ->
                    _fma3x2x4(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x1, x0,
                        0L, 0L, 0L, a0
                    )
                else -> throw RuntimeException("coeff overflow")
            }
            return
        }
        val p3 = POW10[pow10Offset + 3]
        if (xBitLen <= 64) {
            _fma4x1x4(
                z, maxFusedBitLen,
                p3, p2, p1, p0,
                x0,
                0L, 0L, 0L, a0
            )
            return
        } else {
            throw RuntimeException("coeff overflow")
        }
    }

    @Suppress("UNUSED")
    private fun _fma4x4x4(
        f: Coeff,
        maxFusedBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo, a2)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo + a3
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo, a3)
        val pp22Lo = x2 * y2
        val (carry4, f4) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        if ((carry4 or f4) == 0L) {
            assert(maxFusedBitLen in 257..258)
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fma4x1x4(
        f: Coeff,
        maxFusedBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp10Lo, a1)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp10Hi + pp20Lo + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp10Hi, pp20Lo, a2)
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp20Hi + pp30Lo + a3
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3) = sumU64(carry2, pp20Hi, pp30Lo, a3)
        val (carry4, f4) = sumU64(carry3, pp30Hi)
        if ((carry4 or f4) == 0L) {
            assert(maxFusedBitLen in 257..258)
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fma3x2x4(
        f: Coeff,
        maxFusedBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo, a2)
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp11Hi + pp20Hi + pp21Lo + a3
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo, a3)
        val (carry4, f4) = sumU64(carry3, pp21Hi)
        if ((carry4 or f4) == 0L) {
            assert(maxFusedBitLen in 257..258)
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fma3x1x4(
        f: Coeff,
        maxFusedBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp10Lo, a1)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp10Hi + pp20Lo + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp10Hi, pp20Lo, a2)

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp20Hi + a3
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3) = sumU64(carry2, pp20Hi, a3)
        if (carry3 == 0L) {
            assert(maxFusedBitLen in 257..258)
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fma2x2x4(
        f: Coeff,
        maxFusedBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, a2)

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp11Hi + a3
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3) = sumU64(carry2, pp11Hi, a3)
        if (carry3 == 0L) {
            assert(maxFusedBitLen in 257..258)
            f.coeffSet256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fma2x1x3(
        f: Coeff,
        maxFusedBitLen: Int,
        x1: Long, x0: Long,
        y0: Long,
        a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + pp10Lo + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, pp10Lo, a1)
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp10Hi + a2
            f.coeffSet192(f2, f1, f0)
            return
        }
        val (carry2, f2) = sumU64(carry1, pp10Hi, a2)
        val f3 = carry2
        f.coeffSet256(f3, f2, f1, f0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun _fma1x1x2(
        f: Coeff,
        maxFusedBitLen: Int,
        x0: Long,
        y0: Long,
        a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo + a0
            f.coeffSet64(f0)
            return
        }
        val (carry0, f0) = sumU64(pp00Lo, a0)
        if (maxFusedBitLen <= 128) {
            val f1 = carry0 + pp00Hi + a1
            f.coeffSet128(f1, f0)
            return
        }
        val (carry1, f1) = sumU64(carry0, pp00Hi, a1)
        val f2 = carry1
        f.coeffSet192(f2, f1, f0)
        return
    }

}