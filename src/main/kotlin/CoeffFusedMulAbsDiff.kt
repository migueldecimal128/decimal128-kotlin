package com.decimal128

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
        if (x.digitLen <= 1) {
            // FIXME I think precision is wrong in these cases
            if (x.digitLen == 0) {
                z.coeffSet(a)
                return EXACT
            }
            if (x.dw0 == 1L) {
                return z.absDiff(y, a)
            }
        }
        if (y.digitLen <= 1) {
            if (y.digitLen == 0) {
                z.coeffSet(a)
                return EXACT
            }
            if (y.dw0 == 1L) {
                return z.absDiff(x, a)
            }
        }
        return when {
            ((y.dw3 or y.dw2) == 0L) -> {
                if (y.dw1 == 0L) {
                    coeffFusedMulAbsDiff(z, x, y.digitLen, y.dw0, a)
                } else {
                    coeffFusedMulAbsDiff(z, x, y.digitLen, y.dw1, y.dw0, a)
                }
            }

            (y.dw3 == 0L) -> {
                coeffFusedMulAbsDiff(z, x, y.digitLen, y.dw2, y.dw1, y.dw0, a)
            }

            else -> {
                coeffFusedMulAbsDiff(z, x, y.digitLen, y.dw3, y.dw2, y.dw1, y.dw0, a)
            }
        }
    }


    fun coeffFusedMulAbsDiff(z: Coeff, x: Coeff, yDigitCount: Int, y0: Long, a: Coeff): Residue {
        return (
                if ((a.dw3 or a.dw2) == 0L) {
                    coeffFusedMulAbsDiff(z, x, yDigitCount, y0, a.digitLen, a.dw1, a.dw0)
                } else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, 0L, 0L, y0,
                        a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
                    )
                })
    }

    fun coeffFusedMulAbsDiff(z: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long, a: Coeff): Residue {
        return (
                if ((a.dw3 or a.dw2) == 0L) {
                    coeffFusedMulAbsDiff(z, x, yDigitCount, y1, y0, a.digitLen, a.dw1, a.dw0)
                } else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, 0L, y1, y0,
                        a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
                    )
                })
    }

    fun coeffFusedMulAbsDiff(z: Coeff, x: Coeff, yDigitCount: Int, y2: Long, y1: Long, y0: Long, a: Coeff): Residue {
        return (
                if ((a.dw3 or a.dw2) == 0L) {
                    coeffFusedMulAbsDiff(z, x, yDigitCount, y2, y1, y0, a.digitLen, a.dw1, a.dw0)
                } else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, y2, y1, y0,
                        a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
                    )
                })
    }

    fun coeffFusedMulAbsDiff(
        z: Coeff,
        x: Coeff,
        yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long,
        a: Coeff
    ): Residue {
        return (
                if ((a.dw3 or a.dw2) == 0L) {
                    coeffFusedMulAbsDiff(z, x, yDigitCount, y3, y2, y1, y0, a.digitLen, a.dw1, a.dw0)
                } else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, y3, y2, y1, y0,
                        a.digitLen, a.dw3, a.dw2, a.dw1, a.dw0
                    )
                })
    }

    fun coeffFusedMulAbsDiff(
        z: Coeff,
        x: Coeff,
        yDigitCount: Int, y0: Long,
        aDigitCount: Int, a1: Long, a0: Long
    ): Residue {
        return (
                //if ((x.dw3 or x.dw2) == 0L) {
                //    if (x.dw1 == 0L)
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
                //    else
                //        coeffFusedMulAbsDiff(z, x.digitLen, x.dw1, x.dw0, yDigitCount, y0, aDigitCount, a1, a0)
                //} else {
                    coeffFusedMulAbsDiff(
                        z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0,
                        yDigitCount, 0L, 0L, 0L, y0, aDigitCount, 0L, 0L, a1, a0
                    )
                //}
                )

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
        if (hiDiffDigitCount < POW10_128_OFFSET) {
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
        if (hiDiffDigitCount < POW10_192_OFFSET) {
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
        if (hiDiffDigitCount < POW10_256_OFFSET) {
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


}