package com.decimal128

import java.lang.Math.max
import java.lang.Math.unsignedMultiplyHigh

object CoeffFma {

    fun coeffFma(z:Coeff, x:Coeff, y:Coeff, a:Coeff) {
        assert(z.hasValidLengths())
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(a.hasValidLengths())
        val flipFlop = x.bitLen >= y.bitLen
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val mBitLen = m.bitLen
        val nBitLen = n.bitLen
        assert(mBitLen >= nBitLen)

        val maxProdBitLen = max(mBitLen + nBitLen, a.bitLen) + 1
        when {
            (maxProdBitLen > 258) -> throw RuntimeException("coeff overflow")
            (mBitLen <= 1) -> {
                z.coeffSet(a)
                if (nBitLen == 1)
                    z.coeffIncrement()
                return;
            }
            (nBitLen <= 1) -> {
                if (nBitLen == 0)
                    z.coeffSet(a)
                else
                    z.add(m, a)
                return
            }
        }
        _fma4x4x4(z, maxProdBitLen,
            m.dw3, m.dw2, m.dw1, m.dw0,
            n.dw3, n.dw2, n.dw1, n.dw0,
            a.dw3, a.dw2, a.dw1, a.dw0)

    }


    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, a: Coeff) {
        if (a.bitLen <= 128) {
            coeffFma(p, x, yDigitCount, y0, a.digitLen, a.dw1, a.dw0)
        } else {
            _fma(
                p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, 0L, 0L, 0L, y0,
                a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
            )

        }
    }

    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, a: Coeff) {
        if (a.bitLen <= 128) {
            coeffFma(p, x, yDigitCount, y1, y0, a.digitLen, a.dw1, a.dw0)
        } else {
            _fma(
                p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, 0L, 0L, y1, y0,
                a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
            )
        }
    }

    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y2: Long, y1: Long, y0: Long, a: Coeff) {
        if (a.bitLen <= 128) {
            coeffFma(p, x, yDigitCount, y2, y1, y0, a.digitLen, a.dw1, a.dw0)
        } else {
            _fma(
                p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, 0L, y2, y1, y0,
                a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
            )
        }
    }

    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long, a: Coeff) {
        if (a.bitLen <= 128) {
            coeffFma(p, x, yDigitCount, y3, y2, y1, y0, a.digitLen, a.dw1, a.dw0)
        } else {
            _fma(
                p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, y3, y2, y1, y0,
                a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
            )
        }
    }

    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
        when {
            x.bitLen <= 64 ->
                _fma(p, x.digitLen, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
            x.bitLen <= 128 ->
                _fma(p, x.digitLen, x.dw1, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
            else ->
                _fma(
                    p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                    yDigitCount, 0L, 0L, 0L, y0, aDigitCount, 0L, 0L, a1, a0
            )
        }

    }

    fun coeffFma(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
        when {
            x.bitLen <= 64 ->
                _fma(p, x.digitLen, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
            x.bitLen <= 128 ->
                _fma(p, x.digitLen, x.dw1, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
            else ->
                _fma(
                    p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                    yDigitCount, 0L, 0L, y1, y0, aDigitCount, 0L, 0L, a1, a0
                )
        }
    }

    fun coeffFma(
        p: Coeff,
        x: Coeff,
        yDigitCount: Int,
        y2: Long,
        y1: Long,
        y0: Long,
        aDigitCount: Int,
        a1: Long,
        a0: Long
    ) {
        when {
            x.bitLen <= 64 ->
                _fma(p, x.digitLen, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
            x.bitLen <= 128 ->
                _fma(p, x.digitLen, x.dw1, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
            else ->
                _fma(
                    p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                    yDigitCount, 0L, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
                )
        }
    }

    fun coeffFma(
        p: Coeff,
        x: Coeff,
        yDigitCount: Int,
        y3: Long,
        y2: Long,
        y1: Long,
        y0: Long,
        aDigitCount: Int,
        a1: Long,
        a0: Long
    ) {
        when {
            x.bitLen <= 64 ->
                _fma(p, x.digitLen, x.dw0, yDigitCount, y3, y2, y1, y0, aDigitCount, a1, a0)
            //FIXME ... why is there no special FMA for 128 bits
            else ->
                _fma(
                    p, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                    yDigitCount, y3, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
                )
        }
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        val pp20Lo = x2 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo + a2
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo, a2)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp12Lo = x1 * y2
        val pp21Lo = x2 * y1
        val pp03Lo = x0 * y3
        val pp30Lo = x3 * y0

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo + a3
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo, a3)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp22Lo = x2 * y2
        val dw4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x1: Long, x0: Long,
        yDigitCount: Int, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp12Lo = x1 * y2

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp12Lo
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp12Lo)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val dw4 = carry3 + pp12Hi
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x1: Long, x0: Long,
        yDigitCount: Int, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2 + pp11Hi
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi)
        val dw4 = carry3
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x1: Long, x0: Long,
        yDigitCount: Int, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp10Lo, a1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp10Hi
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi)

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val p3 = carry2
        assert(loSumDigitCount == 77 || loSumDigitCount == 78)
        p.coeffSet256(p3, p2, p1, p0)
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x0: Long,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi + pp02Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp02Lo)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp03Lo = x0 * y3

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2 + pp02Hi + pp03Lo
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp02Hi, pp03Lo)
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val dw4 = carry3 + pp03Hi
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x0: Long,
        yDigitCount: Int, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi + pp02Lo
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp02Lo)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)

        if (hiSumDigitCount < MAX_DIGIT_LEN) {
            val p3 = carry2 + pp02Hi
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp02Hi)
        val dw4 = carry3
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x0: Long,
        yDigitCount: Int, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1 // no carry possible because of maxMulDigitCount
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val p2 = carry1 + pp01Hi
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi)

        val p3 = carry2
        p.coeffSet256(p3, p2, p1, p0)
    }

    private fun _fma(
        p: Coeff,
        xDigitCount: Int, x0: Long,
        yDigitCount: Int, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ) {
        /*
        if (xDigitCount == 0 || yDigitCount == 0) {
            p.setZero()
            return
        }
         */
        val hiMulDigitCount = xDigitCount + yDigitCount
        val loMulDigitCount = hiMulDigitCount - 1
        val loSumDigitCount = max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        if (hiSumDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val p1 = carry0 + pp00Hi + a1
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, a1)
        val p2 = carry1
        p.coeffSet192(p2, p1, p0)
    }

    private fun _fma4x4x4(
        p: Coeff,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long,
        a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp00Lo = x0 * y0
        if (maxBitLen <= 64) {
            val p0 = pp00Lo + a0
            p.coeffSet64(p0)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1
            p.coeffSet128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo + a2
            p.coeffSet192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo, a2)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo + a3
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo, a3)
        val pp22Lo = x2 * y2
        val (carry4, p4) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        if ((carry4 or p4) == 0L) {
            assert(maxBitLen in 257..258)
            p.coeffSet256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

}