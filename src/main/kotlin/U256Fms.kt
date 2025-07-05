package com.decimal128

import com.decimal128.U256Pow10.pow10BitLen
import com.decimal128.U256Pow10.pow10Offset
import java.lang.Math.max
import java.lang.Math.unsignedMultiplyHigh

object U256Fms {

    fun u256Fms(z: U256, x: U256, y: U256, s: U256) {
        assert(z.u256HasValidLengths())
        assert(x.u256HasValidLengths())
        assert(y.u256HasValidLengths())
        assert(s.u256HasValidLengths())
        val flipFlop = x.bitLen >= y.bitLen
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val mBitLen = m.bitLen
        val nBitLen = n.bitLen
        assert(mBitLen >= nBitLen)
        val m0 = m.dw0
        val m1 = m.dw1
        val n0 = n.dw0
        val sBitLen = s.bitLen
        require(sBitLen <= 128)
        val s0 = s.dw0
        val s1 = s.dw1

        val maxFusedBitLen = max(mBitLen + nBitLen, s.bitLen) + 1
        if (nBitLen <= 64) {
            when {
                (mBitLen <= 64) ->
                    _fms1x1x2(
                        z, maxFusedBitLen,
                        m0,
                        n0,
                        s1, s0
                    )

                (mBitLen <= 128) ->
                    _fms2x1x2(
                        z, maxFusedBitLen,
                        m1, m0,
                        n0,
                        s1, s0
                    )

                (mBitLen <= 192) ->
                    _fms3x1x2(
                        z, maxFusedBitLen,
                        m.dw2, m1, m0,
                        n0,
                        s1, s0
                    )

                else ->
                    _fms4x1x2(
                        z, maxFusedBitLen,
                        m.dw3, m.dw2, m1, m0,
                        n0,
                        s1, s0
                    )
            }
            return
        }
        val n1 = n.dw1
        if (nBitLen <= 128) {
            when {
                (mBitLen <= 128) ->
                    _fms2x2x2(
                        z, maxFusedBitLen,
                        m1, m0,
                        n1, n0,
                        s1, s0
                    )
                (mBitLen <= 192) ->
                    _fms3x2x2(
                        z, maxFusedBitLen,
                        m.dw2, m1, m0,
                        n1, n0,
                        s1, s0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        throw RuntimeException("coeff overflow")
    }



    fun u256FmsPow10(z: U256, x: U256, pow10: Int, y: U256) {
        assert(pow10 > 0)
        assert(z.u256HasValidLengths())
        assert(x.u256HasValidLengths())
        assert(y.u256HasValidLengths())
        assert(y.bitLen <= 128)
        assert(y.u256ScaledCompareTo(x, pow10) <= 0)
        val xBitLen = x.bitLen
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()
        val maxFusedBitLen = xBitLen + p10BitLen
        /*
        if (maxFusedBitLen <= 128) {
            val (f1, f0) = umul128x128to128(x1, x0, p1, p0)
            val (d1, d0) = diffU128(f1, f0, y1, y0)
            z.coeffSet128(d1, d0)
            return
        }

         */
        if (p10BitLen <= 64) {
            when {
                (xBitLen <= 64) ->
                    _fms1x1x2(z, maxFusedBitLen,
                        x0,
                        p0,
                        y1, y0
                    )
                (xBitLen <= 128) ->
                    _fms2x1x2(z, maxFusedBitLen,
                        x1, x0,
                        p0,
                        y1, y0
                    )
                (xBitLen <= 192) ->
                    _fms3x1x2(z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p0,
                        y1, y0
                    )
                else ->
                    _fms4x1x2(z, maxFusedBitLen,
                        x.dw3, x.dw2, x1, x0,
                        p0,
                        y1, y0
                    )
            }
            return
        }
        if (p10BitLen <= 128) {
            when {
                (xBitLen <= 64) ->
                    _fms2x1x2(
                        z, maxFusedBitLen,
                        p1, p0,
                        x0,
                        y1, y0
                    )
                (xBitLen <= 128) ->
                    _fms2x2x2(
                        z, maxFusedBitLen,
                        x1, x0,
                        p1, p0,
                        y1, y0
                    )
                (xBitLen <= 192) ->
                    _fms3x2x2(
                        z, maxFusedBitLen,
                        x.dw2, x1, x0,
                        p1, p0,
                        y1, y0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return
        }
        val p2 = POW10[pow10Offset + 2]
        if (p10BitLen <= 192) {
            when {
                (xBitLen <= 64) ->
                    _fms3x1x2(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x0,
                        y1, y0
                    )
                (xBitLen <= 128) ->
                    _fms3x2x2(
                        z, maxFusedBitLen,
                        p2, p1, p0,
                        x1, x0,
                        y1, y0
                    )
                else -> throw RuntimeException("coeff overflow")
            }
            return
        }
        val p3 = POW10[pow10Offset + 3]
        if (xBitLen <= 64) {
            _fms4x1x2(
                z, maxFusedBitLen,
                p3, p2, p1, p0,
                x0,
                y1, y0
            )
            return
        } else {
            throw RuntimeException("coeff overflow")
        }
    }

    fun u256FmsPow10(z: U256, x: U256, y: U256, pow10: Int) {
        assert(pow10 > 0)
        assert(z.u256HasValidLengths())
        assert(x.u256HasValidLengths())
        assert(y.u256HasValidLengths())
        assert(x.bitLen <= 128)
        assert(x.u256ScaledCompareTo(y, pow10) >= 0)
        val xBitLen = x.bitLen
        val yBitLen = y.bitLen
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()

        val pp00Hi = unsignedMultiplyHigh(y0, p0)
        val pp00Lo = y0 * p0
        val pp10Lo = y1 * p0
        val pp01Lo = y0 * p1

        val f0 = pp00Lo
        val f1 = pp00Hi + pp10Lo + pp01Lo
        val (borrow0, d0) = diffU64(x0, f0)
        val d1 = x1 - f1 - borrow0
        z.u256Set128(d1, d0)
    }

    @Suppress("UNUSED")
    private fun _fms4x4x4(
        f: U256,
        maxFusedBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long,
        s3: Long, s2: Long, s1: Long, s0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxFusedBitLen <= 64) {
            val f0 = pp00Lo - s0
            f.u256Set64(f0)
            return
        }
        val (borrow0, f0) = diffU64(pp00Lo, s0)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxFusedBitLen <= 128) {
            val f1 = pp00Hi + pp01Lo + pp10Lo - s1 - borrow0
            f.u256Set128(f1, f0)
            return
        }
        val (carry1, f1p) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val (borrow1, f1) = diffU64withBorrow(f1p, s1, borrow0)

        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxFusedBitLen <= 192) {
            val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo - s2 - borrow1
            f.u256Set192(f2, f1, f0)
            return
        }
        val (carry2, f2p) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
        val (borrow2, f2) = diffU64withBorrow(f2p, s2, borrow1)

        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxFusedBitLen <= 256) {
            val f3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo - s3 - borrow2
            f.u256Set256(f3, f2, f1, f0)
            return
        }
        val (carry3, f3p) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val (borrow3, f3) = diffU64withBorrow(f3p, s3, borrow2)
        val pp22Lo = x2 * y2
        val (carry4, f4t) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        val (borrow4, f4) = diffU64(f4t, borrow3)
        if (carry4 == 0L && borrow4 == 0L && f4 == 0L) {
            f.u256Set256(f3, f2, f1, f0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _fms4x1x2(
        f: U256,
        maxFusedBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            x3, x2, x1, x0,
            0L, 0L, 0L, y0,
            0L, 0L,s1, s0)
    }

    private fun _fms3x2x2(
        f: U256,
        maxFusedBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            0L, x2, x1, x0,
            0L, 0L, y1, y0,
            0L, 0L,s1, s0)
    }

    private fun _fms3x1x2(
        f: U256,
        maxFusedBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            0L, x2, x1, x0,
            0L, 0L, 0L, y0,
            0L, 0L,s1, s0)
    }

    private fun _fms2x2x2(
        f: U256,
        maxFusedBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            0L, 0L, x1, x0,
            0L, 0L, y1, y0,
            0L, 0L,s1, s0)
    }

    private fun _fms2x1x2(
        f: U256,
        maxFusedBitLen: Int,
        x1: Long, x0: Long,
        y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            0L, 0L, x1, x0,
            0L, 0L, 0L, y0,
            0L, 0L,s1, s0)
    }

    private fun _fms1x1x2(
        f: U256,
        maxFusedBitLen: Int,
        x0: Long,
        y0: Long,
        s1: Long, s0: Long
    ) {
        _fms4x4x4(f, maxFusedBitLen,
            0L, 0L, 0L, x0,
            0L, 0L, 0L, y0,
            0L, 0L,s1, s0)
    }

}