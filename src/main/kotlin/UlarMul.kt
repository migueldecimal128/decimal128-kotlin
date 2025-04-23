package com.decimal128

import java.lang.Math.unsignedMultiplyHigh

class UlarMul {
    companion object {

        fun ularMul(z:LongArray, zOff:Int, zLen:Int, m:LongArray, mOff:Int, mLen:Int, n:LongArray, nOff:Int, nLen:Int) {
            require(zLen >= mLen + nLen)
            assert(nLen <= mLen)
            when (nLen) {
                0 -> Ular.setZero(z, zOff, zLen)
                1 -> ularMul1(z, zOff, zLen, m, mOff, mLen, n[nOff+0])
                2 -> ularMul2(z, zOff, zLen, m, mOff, mLen, n[nOff+1], n[nOff+0])
                3 -> ularMul3(z, zOff, zLen, m, mOff, mLen, n[nOff+2], n[nOff+1], n[nOff+0])
                4 -> ularMul4(z, zOff, zLen, m, mOff, mLen, n[nOff+3], n[nOff+2], n[nOff+1], n[nOff+0])
                else -> ularMulN(z, zOff, zLen, m, mOff, mLen, n, nOff, nLen)
            }
        }


        fun ularMul4(z:LongArray, zOff:Int, zLen:Int,
                     m:LongArray, mOff:Int, mLen:Int,
                     n3:Long, n2:Long, n1:Long, n0:Long) {
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

            var carry_0 = 0L
            var i = 0
            while (i < mLen) {
                val mX = m[mOff + i]

                val pp01_0 = unsignedMultiplyHigh(mX, n0)
                val pp00_0 = mX * n0

                val pp11_0 = unsignedMultiplyHigh(mX, n1)
                val pp10_0 = mX * n1

                val pp21_0 = unsignedMultiplyHigh(mX, n2)
                val pp20_0 = mX * n2

                val pp31_0 = unsignedMultiplyHigh(mX, n3)
                val pp30_0 = mX * n3

                val (carry0, z0) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_0)
                z[zOff + i] = z0

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

                carry_0 = carry0
                ++i
            }
            val (carry1, z1) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_0)
            z[zOff + i] = z1
            ++i
            val (carry2, z2) = sumU64(pp31_3, pp30_2, pp21_2, pp20_1, pp11_1, carry1)
            z[zOff + i] = z2
            ++i
            val (carry3, z3) = sumU64(pp31_2, pp30_1, pp21_1, carry2)
            z[zOff + i] = z3
            ++i
            //val (carry4, z4) = sumU64(pp31_1, carry3)
            //require(carry4 == 0L)
            val z4 = pp31_1 + carry3
            if (z4 != 0L) {
                z[zOff + i] = z4
                ++i
            }
            while (i < zLen) {
                z[zOff + i] = 0L
                ++i
            }
        }

        fun ularMul3(z:LongArray, zOff:Int, zLen:Int,
                     m:LongArray, mOff:Int, mLen:Int,
                     n2:Long, n1:Long, n0:Long) {
            var pp21_3 = 0L
            var pp21_2 = 0L
            var pp21_1 = 0L

            var pp20_2 = 0L
            var pp20_1 = 0L

            var pp11_2 = 0L
            var pp11_1 = 0L

            var pp10_1 = 0L

            var pp01_1 = 0L

            var carry_0 = 0L
            var i = 0
            while (i < mLen) {
                val mX = m[mOff + i]

                val pp01_0 = unsignedMultiplyHigh(mX, n0)
                val pp00_0 = mX * n0

                val pp11_0 = unsignedMultiplyHigh(mX, n1)
                val pp10_0 = mX * n1

                val pp21_0 = unsignedMultiplyHigh(mX, n2)
                val pp20_0 = mX * n2

                val (carry0, z0) = sumU64(pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_0)
                z[zOff + i] = z0

                pp21_3 = pp21_2
                pp21_2 = pp21_1
                pp21_1 = pp21_0

                pp20_2 = pp20_1
                pp20_1 = pp20_0

                pp11_2 = pp11_1
                pp11_1 = pp11_0

                pp10_1 = pp10_0

                pp01_1 = pp01_0

                carry_0 = carry0
                ++i
            }
            val (carry1, z1) = sumU64(pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_0)
            z[zOff + i] = z1
            ++i
            val (carry2, z2) = sumU64(pp21_2, pp20_1, pp11_1, carry1)
            z[zOff + i] = z2
            ++i
            //val (carry3, z3) = sumU64(pp21_1, carry2)
            //require(carry3 == 0L)
            val z3 = pp21_1 + carry2
            if (z3 != 0L) {
                z[zOff + i] = z3
                ++i
            }
            while (i < zLen) {
                z[zOff + i] = 0L
                ++i
            }
        }

        fun ularMul2(z:LongArray, zOff:Int, zLen:Int,
                     m:LongArray, mOff:Int, mLen:Int,
                     n1:Long, n0:Long) {
            var pp11_2 = 0L
            var pp11_1 = 0L

            var pp10_1 = 0L

            var pp01_1 = 0L

            var carry_0 = 0L
            var i = 0
            while (i < mLen) {
                val mX = m[mOff + i]

                val pp01_0 = unsignedMultiplyHigh(mX, n0)
                val pp00_0 = mX * n0

                val pp11_0 = unsignedMultiplyHigh(mX, n1)
                val pp10_0 = mX * n1

                val (carry0, z0) = sumU64(pp11_2, pp10_1, pp01_1, pp00_0, carry_0)
                z[zOff + i] = z0

                pp11_2 = pp11_1
                pp11_1 = pp11_0

                pp10_1 = pp10_0

                pp01_1 = pp01_0

                carry_0 = carry0
                ++i
            }
            val (carry1, z1) = sumU64(pp11_2, pp10_1, pp01_1, carry_0)
            z[zOff + i] = z1
            ++i
            //val (carry2, z2) = sumU64(pp11_1, carry1)
            //require(carry2 == 0L)
            val z2 = pp11_1 + carry1
            if (z2 != 0L) {
                z[zOff + i] = z2
                ++i
            }
            while (i < zLen) {
                z[zOff + i] = 0L
                ++i
            }
        }

        fun ularMul1(z:LongArray, zOff:Int, zLen:Int,
                     m:LongArray, mOff:Int, mLen:Int,
                     n0:Long) {

            var pp01_1 = 0L

            var carry_0 = 0L
            var i = 0
            while (i < mLen) {
                val mX = m[mOff + i]

                val pp01_0 = unsignedMultiplyHigh(mX, n0)
                val pp00_0 = mX * n0

                val (carry0, z0) = sumU64(pp01_1, pp00_0, carry_0)
                z[zOff + i] = z0

                pp01_1 = pp01_0

                carry_0 = carry0
                ++i
            }
            //val (carry1, z1) = sumU64(pp01_1, carry_0)
            //require(carry1 == 0L)
            val z1 = pp01_1 + carry_0
            if (z1 != 0L) {
                z[zOff + i] = z1
                ++i
            }
            while (i < zLen) {
                z[zOff + i] = 0L
                ++i
            }
        }


        fun ularMulN(z:LongArray, zOff:Int, zLen:Int,
                     m:LongArray, mOff:Int, mLen:Int,
                     n:LongArray, nOff:Int, nLen:Int) {
            require (z !== m || zOff != mOff)
            require (z !== n || zOff != nOff)

            for (i in zOff..<zOff+zLen)
                z[i] = 0L
            for (i in 0..<mLen) {
                val mX = m[mOff + i]
                var carry_0 = 0L
                for (j in 0..<nLen) {
                    val nX = n[nOff + j]
                    val pp01_0 = unsignedMultiplyHigh(mX, nX)
                    val pp00_0 = mX * nX
                    val zIndex = zOff + i + j
                    val zPrev = z[zIndex]
                    val (carry0, z0) = sumU64(zPrev, pp00_0, carry_0)
                    z[zIndex] = z0
                    carry_0 = pp01_0 + carry0 // cannot overflow ?!?
                }
                z[zOff + i + nLen] = carry_0
            }
        }


    }
}