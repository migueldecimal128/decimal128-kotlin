package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount64
import com.decimal128.CoeffDigitCount.setDigitCount128
import com.decimal128.CoeffDigitCount.setDigitCount192
import com.decimal128.CoeffDigitCount.setDigitCount256
import java.lang.Math.unsignedMultiplyHigh
import kotlin.math.max

object CoeffFusedMulAbsDiff {

    fun coeffFusedMulAbsDiff(z:Coeff, x:Coeff, y:Coeff, a:Coeff) {
        assert(z.isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        assert(a.isValidDigitCount())
        if (x.digitCount <= 1) {
            if (x.digitCount == 0) {
                z.set(a)
                return
            }
            if (x.dw0 == 1L) {
                z.absDiff(y, a)
                return
            }
        }
        if (y.digitCount <= 1) {
            if (y.digitCount == 0) {
                z.set(a)
                return
            }
            if (y.dw0 == 1L) {
                z.absDiff(x, a)
                return
            }
        }
        when {
            ((y.dw3 or y.dw2) == 0L) -> {
                if (y.dw1 == 0L) {
                    coeffFusedMulAbsDiff(z, x, y.digitCount, y.dw0, a)
                } else {
                    coeffFusedMulAbsDiff(z, x, y.digitCount, y.dw1, y.dw0, a)
                }
            }
            (y.dw3 == 0L) -> {
                coeffFusedMulAbsDiff(z, x, y.digitCount, y.dw2, y.dw1, y.dw0, a)
            }
            else -> {
                coeffFusedMulAbsDiff(z, x, y.digitCount, y.dw3, y.dw2, y.dw1, y.dw0, a)
            }
        }
    }


    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, a: Coeff) {
        if ((a.dw3 or a.dw2) == 0L) {
            coeffFusedMulAbsDiff(p, x, yDigitCount, y0, a.digitCount, a.dw1, a.dw0)
        } else {
            fusedMulAbsDiff(
                p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, 0L, 0L, 0L, y0,
                a.digitCount, a.dw3, a.dw2, a.dw1, a.dw0
            )

        }
    }

    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, a: Coeff) {
        //if ((a.dw3 or a.dw2) == 0L) {
        //    coeffFusedMulAbsDiff(p, x, yDigitCount, y1, y0, a.digitCount, a.dw1, a.dw0)
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, 0L, 0L, y1, y0,
            a.digitCount, a.dw3, a.dw2, a.dw1, a.dw0
        )
        //}
    }

    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y2: Long, y1: Long, y0: Long, a: Coeff) {
        //if ((a.dw3 or a.dw2) == 0L) {
        //    coeffFusedMulAbsDiff(p, x, yDigitCount, y2, y1, y0, a.digitCount, a.dw1, a.dw0)
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, 0L, y2, y1, y0,
            a.digitCount, a.dw3, a.dw2, a.dw1, a.dw0
        )
        //}
    }

    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long, a: Coeff) {
        //if ((a.dw3 or a.dw2) == 0L) {
        //    coeffFusedMulAbsDiff(p, x, yDigitCount, y3, y2, y1, y0, a.digitCount, a.dw1, a.dw0)
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, y3, y2, y1, y0,
            a.digitCount, a.dw3, a.dw2, a.dw1, a.dw0
        )
        //}
    }

    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
        //if ((x.dw3 or x.dw2) == 0L) {
        //    if (x.dw1 == 0L)
        //        fusedMulAbsDiff(p, x.digitCount, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
        //    else
        //        fusedMulAbsDiff(p, x.digitCount, x.dw1, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
        //} else {
            fusedMulAbsDiff(
                p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
                yDigitCount, 0L, 0L, 0L, y0, aDigitCount, 0L, 0L, a1, a0
            )
        //}

    }

    fun coeffFusedMulAbsDiff(p: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, aDigitCount: Int, a1: Long, a0: Long) {
        //if ((x.dw3 or x.dw2) == 0L) {
        //    if (x.dw1 == 0L)
        //        _fma(p, x.digitCount, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
        //    else
        //        _fma(p, x.digitCount, x.dw1, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, 0L, 0L, y1, y0, aDigitCount, 0L, 0L, a1, a0
        )
        //}
    }

    fun coeffFusedMulAbsDiff(
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
        //if ((x.dw3 or x.dw2) == 0L) {
        //    if (x.dw1 == 0L)
        //        _fma(p, x.digitCount, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
        //    else
        //        _fma(p, x.digitCount, x.dw1, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, 0L, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
        )
        //}
    }

    fun coeffFusedMulAbsDiff(
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
        //if ((x.dw3 or x.dw2) == 0L) {
        //    if (x.dw1 == 0L)
        //        _fma(p, x.digitCount, x.dw0, yDigitCount, y3, y2, y1, y0, aDigitCount, a1, a0)
        //    else
        //        _fmAbsDiff(
        //            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, y3, y2, y1, y0, aDigitCount, 0L, 0L, a1,
        //            a0
        //        )
        //} else {
        fusedMulAbsDiff(
            p, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0,
            yDigitCount, y3, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
        )
        //}
    }

    private fun fusedMulAbsDiff(
        z: Coeff,
        xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ) {
        val hiMulDigitCount = xDigitCount + yDigitCount
        val hiDiffDigitCount = max(hiMulDigitCount, aDigitCount)

        val pp00Lo = x0 * y0
        val (carry0, p0) = 0L to pp00Lo
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (hiDiffDigitCount < POW10_128_OFFSET) {
            val negBorrow0 = -borrow0
            z.dw3 = 0L; z.dw2 = 0L; z.dw1 = 0L
            z.dw0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            setDigitCount64(z)
            return

        }

        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (hiDiffDigitCount < POW10_192_OFFSET) {
            val negBorrow1 = -borrow1
            z.dw3 = 0L; z.dw2 = 0L
            z.dw1 = d1 xor negBorrow1
            z.dw0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && z.dw0 == 0L) {
                ++z.dw1
                if (z.dw1 == 0L)
                    ++z.dw2
            }
            setDigitCount128(z)
            return
        }

        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp12Lo = x1 * y2
        val pp21Lo = x2 * y1
        val pp03Lo = x0 * y3
        val pp30Lo = x3 * y0
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        if (hiDiffDigitCount < POW10_256_OFFSET) {
            val negBorrow2 = -borrow2
            z.dw3 = 0L
            z.dw2 = d2 xor negBorrow2
            z.dw1 = d1 xor negBorrow2
            z.dw0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && z.dw0 == 0L) {
                ++z.dw1
                if (z.dw1 == 0L) {
                    ++z.dw2
                }
            }
            setDigitCount192(z)
            return
        }

        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val pp22Lo = x2 * y2
        val (carry3s, s3) = sumU64(borrow2, a3)
        val (borrow3d, d3) = diffU64(p3, s3)
        val borrow3 = carry3s + borrow3d
        assert(borrow3 in 0..1)

        val dw4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
        if (dw4 == 0L || dw4 == 1L && borrow3 == 1L) {
            if (dw4 == 0L) {
                val negBorrow3 = -borrow3
                z.dw3 = d3 xor negBorrow3
                z.dw2 = d2 xor negBorrow3
                z.dw1 = d1 xor negBorrow3
                z.dw0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
                if (negBorrow3 < 0L && z.dw0 == 0L) {
                    ++z.dw1
                    if (z.dw1 == 0L) {
                        ++z.dw2
                        if (z.dw2 == 0L)
                            ++z.dw3
                    }
                }
            } else {
                // dw4 == 1L && borrow3 == 1L
                // the multiply carry and the borrow out the top cancel each other out
                z.dw3 = d3; z.dw2 = d2; z.dw1 = d1; z.dw0 = d0
            }
            setDigitCount256(z)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp12Lo = x1 * y2

        if (hiSumDigitCount < POW10_MAX_OFFSET) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp12Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp12Lo)
        val pp12Hi = unsignedMultiplyHigh(x1, y2)
        val dw4 = carry3 + pp12Hi
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp01Lo + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, pp10Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)

        if (hiSumDigitCount < POW10_MAX_OFFSET) {
            val p3 = carry2 + pp11Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi)
        val dw4 = carry3
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp10Lo = x1 * y0
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp10Lo + a1 // no carry possible because of maxMulDigitCount
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp10Lo, a1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp10Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi)

        if (hiSumDigitCount < POW10_MAX_OFFSET) {
            val p3 = carry2
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        val p3 = carry2
        assert(loSumDigitCount == 77 || loSumDigitCount == 78)
        p.dw0 = p0
        p.dw1 = p1
        p.dw2 = p2
        p.dw3 = p3
        setDigitCount256(p)
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp01Hi + pp02Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp02Lo)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)
        val pp03Lo = x0 * y3

        if (hiSumDigitCount < POW10_MAX_OFFSET) {
            val p3 = carry2 + pp02Hi + pp03Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp02Hi, pp03Lo)
        val pp03Hi = unsignedMultiplyHigh(x0, y3)
        val dw4 = carry3 + pp03Hi
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp02Lo = x0 * y2
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp01Hi + pp02Lo
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp02Lo)
        val pp02Hi = unsignedMultiplyHigh(x0, y2)

        if (hiSumDigitCount < POW10_MAX_OFFSET) {
            val p3 = carry2 + pp02Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp02Hi)
        val dw4 = carry3
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert(loSumDigitCount == 77 || loSumDigitCount == 78)
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            setDigitCount256(p)
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    private fun fusedMulAbsDiff(
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            p.dw0 = p0
            p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
            setDigitCount64(p)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + pp01Lo + a1 // no carry possible because of maxMulDigitCount
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            setDigitCount128(p)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, pp01Lo, a1)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        if (hiSumDigitCount < POW10_256_OFFSET) {
            val p2 = carry1 + pp01Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            CoeffDigitCount.setDigitCount192(p)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi)

        val p3 = carry2
        p.dw0 = p0
        p.dw1 = p1
        p.dw2 = p2
        p.dw3 = p3
        setDigitCount256(p)
    }

    private fun fusedMulAbsDiff(
        z: Coeff,
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
        val loSumDigitCount = Math.max(loMulDigitCount, aDigitCount)
        val hiSumDigitCount = loSumDigitCount + 1
        val pp00Lo = x0 * y0
        if (hiSumDigitCount < POW10_128_OFFSET) {
            val p0 = pp00Lo + a0
            z.dw0 = p0
            z.dw1 = 0L; z.dw2 = 0L; z.dw3 = 0L
            setDigitCount64(z)
            return
        }
        val (carry0, p0) = sumU64(pp00Lo, a0)
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        if (hiSumDigitCount < POW10_192_OFFSET) {
            val p1 = carry0 + pp00Hi + a1
            z.dw0 = p0
            z.dw1 = p1
            z.dw2 = 0L; z.dw3 = 0L;
            setDigitCount128(z)
            return
        }
        val (carry1, p1) = sumU64(carry0, pp00Hi, a1)
        val p2 = carry1
        z.dw0 = p0
        z.dw1 = p1
        z.dw2 = p2
        z.dw3 = 0L;
        CoeffDigitCount.setDigitCount192(z)
    }

}