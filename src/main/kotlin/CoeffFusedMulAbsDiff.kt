package com.decimal128

import com.decimal128.CoeffPow10.pow10BitLen
import com.decimal128.CoeffPow10.pow10Offset
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.EXACT_NEGATED
import java.lang.Math.unsignedMultiplyHigh
import kotlin.math.max

object CoeffFusedMulAbsDiff {

    fun coeffFusedMulAbsDiff(z: Coeff, x: Coeff, y: Coeff, a: Coeff): Residue {
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
        val aBitLen = a.bitLen
        val m0 = m.dw0
        val m1 = m.dw1
        val n0 = n.dw0
        val a0 = a.dw0
        val a1 = a.dw1

        val maxProdBitLen = mBitLen + nBitLen
        if (nBitLen <= 64) {
            val residue = when {
                (mBitLen <= 64 && aBitLen <= 128) ->
                    _fmad1x1x2(
                        z, maxProdBitLen,
                        m0,
                        n0,
                        aBitLen, a1, a0
                    )

                (mBitLen <= 128 && aBitLen <= 192) ->
                    _fmad2x1x3(
                        z, maxProdBitLen,
                        m1, m0,
                        n0,
                        aBitLen, a.dw2, a1, a0
                    )

                (mBitLen <= 192) ->
                    _fmad3x1x4(
                        z, maxProdBitLen,
                        m.dw2, m1, m0,
                        n0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )

                else ->
                    _fmad4x1x4(
                        z, maxProdBitLen,
                        m.dw3, m.dw2, m1, m0,
                        n0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
            }
            return residue
        }
        val n1 = n.dw1
        if (nBitLen <= 128) {
            val residue = when {
                (mBitLen <= 128) ->
                    _fmad2x2x4(
                        z, maxProdBitLen,
                        m1, m0,
                        n1, n0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                (mBitLen <= 192) ->
                    _fmad3x2x4(
                        z, maxProdBitLen,
                        m.dw2, m1, m0,
                        n1, n0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return residue
        }
        throw RuntimeException("coeff overflow")
    }

    fun coeffFmadPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff): Residue {
        assert(pow10 >= 0)
        assert(z.hasValidLengths())
        assert(x.hasValidLengths())
        assert(a.hasValidLengths())
        val xBitLen = x.bitLen
        val aBitLen = a.bitLen
        val p10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val x0 = x.dw0
        val a0 = a.dw0
        val a1 = a.dw1
        val maxProdBitLen = xBitLen + p10BitLen
        val maxDiffBitlen = max(maxProdBitLen, aBitLen)
        val p0 = POW10[pow10Offset + 0]
        if (p10BitLen <= 64) {
            val residue = when {
                (xBitLen <= 64 && aBitLen <= 128) ->
                    _fmad1x1x2(z, maxProdBitLen,
                        x0,
                        p0,
                        aBitLen, a1, a0
                    )
                (xBitLen <= 128 && aBitLen <= 192) ->
                    _fmad2x1x3(z, maxProdBitLen,
                        x.dw1, x0,
                        p0,
                        aBitLen, a.dw2, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fmad3x1x4(z, maxProdBitLen,
                        x.dw2, x.dw1, x0,
                        p0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                else ->
                    _fmad4x1x4(z, maxProdBitLen,
                        x.dw3, x.dw2, x.dw1, x0,
                        p0,
                        aBitLen, a.dw3, a.dw2, a1, a0)
            }
            return residue
        }
        val p1 = POW10[pow10Offset + 1]
        if (p10BitLen <= 128) {
            val residue = when {
                (xBitLen <= 64 && aBitLen <= 128) ->
                    _fmad2x1x3(
                        z, maxProdBitLen,
                        p1, p0,
                        x0,
                        aBitLen, a.dw2, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fmad2x2x4(
                        z, maxProdBitLen,
                        x.dw1, x0,
                        p1, p0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                (xBitLen <= 192) ->
                    _fmad3x2x4(
                        z, maxProdBitLen,
                        x.dw2, x.dw1, x0,
                        p1, p0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                else ->  throw RuntimeException("coeff overflow")
            }
            return residue
        }
        val p2 = POW10[pow10Offset + 2]
        if (p10BitLen <= 192) {
            val residue = when {
                (xBitLen <= 64) ->
                    _fmad3x1x4(
                        z, maxProdBitLen,
                        p2, p1, p0,
                        x0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                (xBitLen <= 128) ->
                    _fmad3x2x4(
                        z, maxProdBitLen,
                        p2, p1, p0,
                        x.dw1, x0,
                        aBitLen, a.dw3, a.dw2, a1, a0
                    )
                else -> throw RuntimeException("coeff overflow")
            }
            return residue
        }
        val p3 = POW10[pow10Offset + 3]
        if (xBitLen <= 64) {
            val residue = _fmad4x1x4(
                z, maxProdBitLen,
                p3, p2, p1, p0,
                x0,
                aBitLen, a.dw3, a.dw2, a1, a0
            )
            return residue
        } else {
            throw RuntimeException("coeff overflow")
        }
    }

    fun coeffFusedMulAbsDiff(
        z: Coeff,
        x: Coeff,
        yDigitCount: Int, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ): Residue {
        return (
                //if ((x.dw3 or x.dw2) == 0L) {
                //    if (x.dw1 == 0L)
                //     coeffFusedMulAbsDiff(z, x.digitLen, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
                //    else
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw1, x.dw0, yDigitCount, y1, y0, aDigitCount, a1, a0)
                //} else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, 0L, y1, y0, aDigitCount, 0L, 0L, a1, a0
                    )
                //}
                )
    }

    fun coeffFusedMulAbsDiff(
        z: Coeff, x: Coeff,
        yDigitCount: Int, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ): Residue {
        return (
                //if ((x.dw3 or x.dw2) == 0L) {
                //    if (x.dw1 == 0L)
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
                //    else
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw1, x.dw0, yDigitCount, y2, y1, y0, aDigitCount, a1, a0)
                //} else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
                    )
                //}
                )
    }

    fun coeffFusedMulAbsDiff(
        z: Coeff, x: Coeff,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ): Residue {
        return (
                //if ((x.dw3 or x.dw2) == 0L) {
                //    if (x.dw1 == 0L)
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw0, yDigitCount, y3, y2, y1, y0, aDigitCount, a1, a0)
                //    else
                //        coeffFusedMulAbsDiff(
                //            z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                //            yDigitCount, y3, y2, y1, y0,
                //            aDigitCount, 0L, 0L, a1, a0
                //        )
                //} else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, y3, y2, y1, y0, aDigitCount, 0L, 0L, a1, a0
                    )
                //}
                )
    }

    private fun coeffFusedMulAbsDiff(
        z: Coeff,
        xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        aDigitCount: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val hiMulDigitCount = xDigitCount + yDigitCount
        val hiDiffDigitCount = max(hiMulDigitCount, aDigitCount)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (hiDiffDigitCount < MIN_POW10_DIGIT_LEN_128) {
            val negBorrow0 = -borrow0
            val z0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            z.coeffSet64(z0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (hiDiffDigitCount < MIN_POW10_DIGIT_LEN_192) {
            val negBorrow1 = -borrow1
            var z1 = d1 xor negBorrow1
            val z0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && z0 == 0L) {
                ++z1
            }
            z.coeffSet128(z1, z0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
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
        if (hiDiffDigitCount < MIN_POW10_DIGIT_LEN_256) {
            val negBorrow2 = -borrow2
            var z2 = d2 xor negBorrow2
            var z1 = d1 xor negBorrow2
            val z0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && z0 == 0L) {
                ++z1
                if (z1 == 0L) {
                    ++z2
                }
            }
            z.coeffSet192(z2, z1, z0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
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

        val p4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
        if (p4 == 0L || p4 == 1L && borrow3 == 1L) {
            if (p4 == 0L) {
                val negBorrow3 = -borrow3
                var z3 = d3 xor negBorrow3
                var z2 = d2 xor negBorrow3
                var z1 = d1 xor negBorrow3
                val z0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
                if (negBorrow3 < 0L && z0 == 0L) {
                    ++z1
                    if (z1 == 0L) {
                        ++z2
                        if (z2 == 0L)
                            ++z3
                    }
                }
                z.coeffSet256(z3, z2, z1, z0)
            } else {
                // dw4 == 1L && borrow3 == 1L
                // the multiply carry and the borrow out the top cancel each other out
                z.coeffSet256(d3, d2, d1, d0)
            }
            return if (borrow3 > 0) EXACT_NEGATED else EXACT
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    /////////

    @Suppress("UNUSED")
    private fun _fmad4x4x4(
        f: Coeff,
        maxProdBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long,
        aBitLen: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp02Lo = x0 * y2
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
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
        if (maxDiffBitLen <= 192) {
            val negBorrow2 = -borrow2
            var f2 = d2 xor negBorrow2
            var f1 = d1 xor negBorrow2
            val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                }
            }
            f.coeffSet192(f2, f1, f0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
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

        val p4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
        if (p4 == 0L || p4 == 1L && borrow3 == 1L) {
            if (p4 == 0L) {
                val negBorrow3 = -borrow3
                var f3 = d3 xor negBorrow3
                var f2 = d2 xor negBorrow3
                var f1 = d1 xor negBorrow3
                val f0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
                if (negBorrow3 < 0L && f0 == 0L) {
                    ++f1
                    if (f1 == 0L) {
                        ++f2
                        if (f2 == 0L)
                            ++f3
                    }
                }
                f.coeffSet256(f3, f2, f1, f0)
            } else {
                // dw4 == 1L && borrow3 == 1L
                // the multiply carry and the borrow out the top cancel each other out
                f.coeffSet256(d3, d2, d1, d0)
            }
            return if (borrow3 > 0) EXACT_NEGATED else EXACT
        }
        throw RuntimeException("coeff overflow")
    }

    private fun _fmad4x1x4(
        f: Coeff,
        maxProdBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long,
        aBitLen: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
        }

        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp30Lo = x3 * y0
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        if (maxDiffBitLen <= 192) {
            val negBorrow2 = -borrow2
            var f2 = d2 xor negBorrow2
            var f1 = d1 xor negBorrow2
            val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                }
            }
            f.coeffSet192(f2, f1, f0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
        }

        val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)
        val pp30Hi = unsignedMultiplyHigh(x3, y0)
        val (carry3s, s3) = sumU64(borrow2, a3)
        val (borrow3d, d3) = diffU64(p3, s3)
        val borrow3 = carry3s + borrow3d
        assert(borrow3 in 0..1)

        val p4 = carry3 + pp30Hi
        if (p4 == 0L || p4 == 1L && borrow3 == 1L) {
            if (p4 == 0L) {
                val negBorrow3 = -borrow3
                var f3 = d3 xor negBorrow3
                var f2 = d2 xor negBorrow3
                var f1 = d1 xor negBorrow3
                val f0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
                if (negBorrow3 < 0L && f0 == 0L) {
                    ++f1
                    if (f1 == 0L) {
                        ++f2
                        if (f2 == 0L)
                            ++f3
                    }
                }
                f.coeffSet256(f3, f2, f1, f0)
            } else {
                // dw4 == 1L && borrow3 == 1L
                // the multiply carry and the borrow out the top cancel each other out
                f.coeffSet256(d3, d2, d1, d0)
            }
            return if (borrow3 > 0) EXACT_NEGATED else EXACT
        }
        throw RuntimeException("coeff overflow")
    }

    private fun _fmad3x2x4(
        f: Coeff,
        maxProdBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long,
        aBitLen: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
        }

        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val pp21Lo = x2 * y1
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        if (maxDiffBitLen <= 192) {
            val negBorrow2 = -borrow2
            var f2 = d2 xor negBorrow2
            var f1 = d1 xor negBorrow2
            val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                }
            }
            f.coeffSet192(f2, f1, f0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
        }

        val (carry3, p3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo)
        val pp21Hi = unsignedMultiplyHigh(x2, y1)
        val (carry3s, s3) = sumU64(borrow2, a3)
        val (borrow3d, d3) = diffU64(p3, s3)
        val borrow3 = carry3s + borrow3d
        assert(borrow3 in 0..1)

        val p4 = carry3 + pp21Hi
        if (p4 == 0L || p4 == 1L && borrow3 == 1L) {
            if (p4 == 0L) {
                val negBorrow3 = -borrow3
                var f3 = d3 xor negBorrow3
                var f2 = d2 xor negBorrow3
                var f1 = d1 xor negBorrow3
                val f0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
                if (negBorrow3 < 0L && f0 == 0L) {
                    ++f1
                    if (f1 == 0L) {
                        ++f2
                        if (f2 == 0L)
                            ++f3
                    }
                }
                f.coeffSet256(f3, f2, f1, f0)
            } else {
                // dw4 == 1L && borrow3 == 1L
                // the multiply carry and the borrow out the top cancel each other out
                f.coeffSet256(d3, d2, d1, d0)
            }
            return if (borrow3 > 0) EXACT_NEGATED else EXACT
        }
        throw RuntimeException("coeff overflow")
    }

    private fun _fmad3x1x4(
        f: Coeff,
        maxProdBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long,
        aBitLen: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp20Lo = x2 * y0
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
        }

        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
        val pp20Hi = unsignedMultiplyHigh(x2, y0)
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        if (maxDiffBitLen <= 192) {
            val negBorrow2 = -borrow2
            var f2 = d2 xor negBorrow2
            var f1 = d1 xor negBorrow2
            val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                }
            }
            f.coeffSet192(f2, f1, f0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
        }

        val (carry3, p3) = sumU64(carry2, pp20Hi)
        val (carry3s, s3) = sumU64(borrow2, a3)
        val (borrow3d, d3) = diffU64(p3, s3)
        val borrow3 = carry3s + borrow3d
        assert(borrow3 in 0..1)

        val p4 = carry3
        assert(p4 == 0L || p4 == 1L && borrow3 == 1L)
        if (p4 == 0L) {
            val negBorrow3 = -borrow3
            var f3 = d3 xor negBorrow3
            var f2 = d2 xor negBorrow3
            var f1 = d1 xor negBorrow3
            val f0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
            if (negBorrow3 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                        ++f2
                    if (f2 == 0L)
                        ++f3
                }
            }
            f.coeffSet256(f3, f2, f1, f0)
        } else {
            // dw4 == 1L && borrow3 == 1L
            // the multiply carry and the borrow out the top cancel each other out
            f.coeffSet256(d3, d2, d1, d0)
        }
        return if (borrow3 > 0) EXACT_NEGATED else EXACT
    }

    private fun _fmad2x2x4(
        f: Coeff,
        maxProdBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long,
        aBitLen: Int, a3: Long, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp01Lo = x0 * y1
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp01Hi = unsignedMultiplyHigh(x0, y1)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val pp11Lo = x1 * y1
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
        }

        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)
        val pp11Hi = unsignedMultiplyHigh(x1, y1)
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        if (maxDiffBitLen <= 192) {
            val negBorrow2 = -borrow2
            var f2 = d2 xor negBorrow2
            var f1 = d1 xor negBorrow2
            val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
            if (negBorrow2 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                }
            }
            f.coeffSet192(f2, f1, f0)
            return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
        }

        val (carry3, p3) = sumU64(carry2, pp11Hi)
        val (carry3s, s3) = sumU64(borrow2, a3)
        val (borrow3d, d3) = diffU64(p3, s3)
        val borrow3 = carry3s + borrow3d
        assert(borrow3 in 0..1)

        val p4 = carry3
        assert(p4 == 0L || p4 == 1L && borrow3 == 1L)
        if (p4 == 0L) {
            val negBorrow3 = -borrow3
            var f3 = d3 xor negBorrow3
            var f2 = d2 xor negBorrow3
            var f1 = d1 xor negBorrow3
            val f0 = (d0 xor negBorrow3) - negBorrow3 // complement and increment
            if (negBorrow3 < 0L && f0 == 0L) {
                ++f1
                if (f1 == 0L) {
                    ++f2
                    if (f2 == 0L)
                        ++f3
                }
            }
            f.coeffSet256(f3, f2, f1, f0)
        } else {
            // dw4 == 1L && borrow3 == 1L
            // the multiply carry and the borrow out the top cancel each other out
            f.coeffSet256(d3, d2, d1, d0)
        }
        return if (borrow3 > 0) EXACT_NEGATED else EXACT
    }

    private fun _fmad2x1x3(
        f: Coeff,
        maxProdBitLen: Int,
        x1: Long, x0: Long,
        y0: Long,
        aBitLen: Int, a2: Long, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val pp10Lo = x1 * y0
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp10Hi = unsignedMultiplyHigh(x1, y0)
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        if (maxDiffBitLen <= 128) {
            val negBorrow1 = -borrow1
            var f1 = d1 xor negBorrow1
            val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
            if (negBorrow1 < 0L && f0 == 0L) {
                ++f1
            }
            f.coeffSet128(f1, f0)
            return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
        }

        val (carry2, p2) = sumU64(carry1, pp10Hi)
        val (carry2s, s2) = sumU64(borrow1, a2)
        val (borrow2d, d2) = diffU64(p2, s2)
        val borrow2 = carry2s + borrow2d
        assert(borrow2 in 0..1)
        val negBorrow2 = -borrow2
        var f2 = d2 xor negBorrow2
        var f1 = d1 xor negBorrow2
        val f0 = (d0 xor negBorrow2) - negBorrow2 // complement and increment
        if (negBorrow2 < 0L && f0 == 0L) {
            ++f1
            if (f1 == 0L) {
                ++f2
            }
        }
        f.coeffSet192(f2, f1, f0)
        return if (negBorrow2 < 0) EXACT_NEGATED else EXACT
    }

    private fun _fmad1x1x2(
        f: Coeff,
        maxProdBitLen: Int,
        x0: Long,
        y0: Long,
        aBitLen: Int, a1: Long, a0: Long
    ): Residue {
        val maxDiffBitLen = max(maxProdBitLen, aBitLen)

        val pp00Lo = x0 * y0
        val pp00Hi = unsignedMultiplyHigh(x0, y0)
        val (borrow0, d0) = diffU64(pp00Lo, a0)
        assert(borrow0 in 0..1)
        if (maxDiffBitLen <= 64) {
            val negBorrow0 = -borrow0
            val f0 = (d0 xor negBorrow0) - negBorrow0 // complement and increment
            f.coeffSet64(f0)
            return if (negBorrow0 < 0) EXACT_NEGATED else EXACT
        }

        val p1 = pp00Hi
        val (carry1s, s1) = sumU64(borrow0, a1)
        val (borrow1d, d1) = diffU64(p1, s1)
        val borrow1 = carry1s + borrow1d
        assert(borrow1 in 0..1)
        val negBorrow1 = -borrow1
        var f1 = d1 xor negBorrow1
        val f0 = (d0 xor negBorrow1) - negBorrow1 // complement and increment
        if (negBorrow1 < 0L && f0 == 0L) {
            ++f1
        }
        f.coeffSet128(f1, f0)
        return if (negBorrow1 < 0) EXACT_NEGATED else EXACT
    }


}