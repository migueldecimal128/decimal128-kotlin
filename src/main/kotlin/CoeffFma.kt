package com.decimal128

import java.lang.Math.unsignedMultiplyHigh

class CoeffFma {
    companion object {
        fun fmaCoeff(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, aDigitCount: Int, a0: Long) {
            _fma(p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, 0L,0L, 0L, y0, aDigitCount, 0L, 0L, 0L, a0)
        }

        fun fmaCoeff(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, aDigitCount: Int, a0: Long) {
            _fma(p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, 0L,0L, y1, y0, aDigitCount, 0L, 0L, 0L, a0)
        }

        fun fmaCoeff(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
            _fma(p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, 0L,0L, 0L, y0, aDigitCount, 0L, 0L, a1, a0)
        }

        fun fmaCoeff(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
            _fma(p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, 0L,0L, y1, y0, aDigitCount, 0L, 0L, a1, a0)
        }

        private fun _fma(p: Coeff,
                         xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
                         yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
                         aDigitCount: Int, a3: Long, a2: Long, a1: Long, a0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val hiMulDigitCount = xDigitCount + yDigitCount
            val loMulDigitCount = hiMulDigitCount - 1
            val loSumDigitCount = Math.min(loMulDigitCount, aDigitCount)
            val hiSumDigitCount = loSumDigitCount + 1
            val pp00Lo = x0 * y0
            if (hiSumDigitCount < POW10_128_OFFSET) {
                p.dw0 = pp00Lo + a0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = loSumDigitCount
                tweakDigitCountOnly64(p)
                assert(p.isValidDigitCount())
                return
            }
            val (carry0, p0) = sumU64(pp00Lo, a0)
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp01Lo = x0 * y1
            val pp10Lo = x1 * y0
            if (hiSumDigitCount < POW10_192_OFFSET) {
                p.dw0 = p0
                p.dw1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = loSumDigitCount
                if (p.dw1 == 0L)
                    tweakDigitCountOnly64(p)
                else
                    tweakDigitCountOnly128(p)
                assert(p.isValidDigitCount())
                return
            }
            val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo)
            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp11Lo = x1 * y1
            val pp02Lo = x0 * y2
            val pp20Lo = x2 * y0
            if (hiSumDigitCount < POW10_256_OFFSET) {
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo + a2
                p.dw3 = 0L;
                p.digitCount = loSumDigitCount
                if (p.dw2 == 0L)
                    tweakDigitCountOnly128(p)
                else
                    tweakDigitCountOnly192(p)
                assert(p.isValidDigitCount())
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

            if (hiSumDigitCount < POW10_MAX_OFFSET) {
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo + a3
                p.digitCount = loSumDigitCount
                if (p.dw3 == 0L)
                    tweakDigitCountOnly192(p)
                else
                    tweakDigitCountOnly256(p)
                assert(p.isValidDigitCount())
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
                assert (loSumDigitCount == 77 || loSumDigitCount == 78)
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = loSumDigitCount
                tweakDigitCountOnly256(p)
                assert(p.isValidDigitCount())
                return
            }
            throw RuntimeException("coefficient multiply overflow")
        }


    }
}