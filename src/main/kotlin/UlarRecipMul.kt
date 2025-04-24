package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.HALF
import com.decimal128.Residue.Companion.LT_HALF
import com.decimal128.Residue.Companion.GT_HALF
import java.lang.Math.unsignedMultiplyHigh
import java.lang.Long.compareUnsigned

class UlarRecipMul {
    companion object {

        fun ularRecipMul4(z:LongArray,
                          m:LongArray, mOff:Int, mLen:Int,
                          n3:Long, n2:Long, n1:Long, n0:Long, bitShift:Int, stickyBitsPow2EqZero:Boolean) : Residue  {
           return ularRecipMul4(z, 0, z.size, m, mOff, mLen, n3, n2, n1, n0, bitShift, stickyBitsPow2EqZero)
        }

        fun ularRecipMul4(z:LongArray, zOff:Int, zLen:Int,
                          m:LongArray, mOff:Int, mLen:Int,
                          n3:Long, n2:Long, n1:Long, n0:Long, bitShift:Int, stickyBitsPow2EqZero:Boolean) : Residue {
            var remainingBitShiftInclHalfUlp = bitShift + 1
            val halfUlpShift = bitShift and 0x3F
            val halfUlpMask = 1L shl halfUlpShift
            val remainingBitsMask = halfUlpMask - 1
            var halfUlpIsolated = 0L
            var fracCmp = 0
            assert((bitShift + 63) ushr 6 >= mLen)

            var z_1 = 0L
            var zI = 0

            var pp31_4 = 0L
            var pp31_3 = 0L
            var pp31_2 = 0L
            var pp31_1 = 0L

            var pp30_3 = 0L
            var pp30_2 = 0L
            var pp30_1 = 0L

            var pp21_3 = 0L
            var pp21_2 = 0L
            var pp21_1 = 0L

            var pp20_2 = 0L
            var pp20_1 = 0L

            var pp11_2 = 0L
            var pp11_1 = 0L

            var pp10_1 = 0L

            var pp01_1 = 0L

            var carry_1 = 0L
            var mI = 0
            while (mI < mLen) {
                val mX = m[mOff + mI]

                val pp01_0 = unsignedMultiplyHigh(mX, n0)
                val pp00_0 = mX * n0

                val pp11_0 = unsignedMultiplyHigh(mX, n1)
                val pp10_0 = mX * n1

                val pp21_0 = unsignedMultiplyHigh(mX, n2)
                val pp20_0 = mX * n2

                val pp31_0 = unsignedMultiplyHigh(mX, n3)
                val pp30_0 = mX * n3

                val (carry_0, z_0) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
                z[zOff + zI] = z_0
                if (remainingBitShiftInclHalfUlp > 0) {
                    var mask = -1L
                    if (remainingBitShiftInclHalfUlp <= 64) {
                        halfUlpIsolated = z_0 and halfUlpMask
                        mask = remainingBitsMask
                    }
                    if (mask != 0L) {
                        val cmp = compareUnsigned((z_0 and mask), mX)
                        fracCmp = if (cmp != 0) cmp else fracCmp
                    }
                    remainingBitShiftInclHalfUlp -= 64
                }

                z_1 = z_0

                pp31_4 = pp31_3
                pp31_3 = pp31_2
                pp31_2 = pp31_1
                pp31_1 = pp31_0

                pp30_3 = pp30_2
                pp30_2 = pp30_1
                pp30_1 = pp30_0

                pp21_3 = pp21_2
                pp21_2 = pp21_1
                pp21_1 = pp21_0

                pp20_2 = pp20_1
                pp20_1 = pp20_0

                pp11_2 = pp11_1
                pp11_1 = pp11_0

                pp10_1 = pp10_0

                pp01_1 = pp01_0

                carry_1 = carry_0
                ++mI
                ++zI
            }
            val (carry1, z1) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_1)
            z[zOff + zI] = z1
            if (z1 != 0L && remainingBitShiftInclHalfUlp > 0) {
                var mask = -1L
                if (remainingBitShiftInclHalfUlp <= 64) {
                    halfUlpIsolated = z1 and halfUlpMask
                    mask = remainingBitsMask
                }
                if ((z1 and mask) != 0L)
                    fracCmp = 1
            }
            remainingBitShiftInclHalfUlp -= 64
            ++zI
            val (carry2, z2) = sumU64(pp31_3, pp30_2, pp21_2, pp20_1, pp11_1, carry1)
            z[zOff + zI] = z2
            if (z2 != 0L && remainingBitShiftInclHalfUlp > 0) {
                var mask = -1L
                if (remainingBitShiftInclHalfUlp <= 64) {
                    halfUlpIsolated = z2 and halfUlpMask
                    mask = remainingBitsMask
                }
                if ((z2 and mask) != 0L)
                    fracCmp = 1
            }
            remainingBitShiftInclHalfUlp -= 64
            ++zI
            val (carry3, z3) = sumU64(pp31_2, pp30_1, pp21_1, carry2)
            z[zOff + zI] = z3
            if (z3 != 0L && remainingBitShiftInclHalfUlp > 0) {
                var mask = -1L
                if (remainingBitShiftInclHalfUlp <= 64) {
                    halfUlpIsolated = z3 and halfUlpMask
                    mask = remainingBitsMask
                }
                if ((z3 and mask) != 0L)
                    fracCmp = 1
            }
            remainingBitShiftInclHalfUlp -= 64
            ++zI
            //val (carry4, z4) = sumU64(pp31_1, carry3)
            //require(carry4 == 0L)
            val z4 = pp31_1 + carry3
            if (z4 != 0L) {
                z[zOff + zI] = z4
                if (remainingBitShiftInclHalfUlp > 0) {
                    var mask = -1L
                    if (remainingBitShiftInclHalfUlp <= 64) {
                        halfUlpIsolated = z4 and halfUlpMask
                        mask = remainingBitsMask
                   }
                    if ((z4 and mask) != 0L)
                        fracCmp = 1
                }
                remainingBitShiftInclHalfUlp -= 64
                ++zI
            }
            while (zI < zLen) {
                z[zOff + zI] = 0L
                ++zI
            }
            val residue =
                if (stickyBitsPow2EqZero) {
                    if (fracCmp < 0) {
                        if (halfUlpIsolated == 0L) EXACT else HALF
                    } else {
                        if (halfUlpIsolated == 0L) LT_HALF else GT_HALF
                    }
                } else {
                    if (halfUlpIsolated == 0L) LT_HALF else GT_HALF
                }
            return residue
        }

    }
}