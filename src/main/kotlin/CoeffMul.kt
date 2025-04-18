package com.decimal128
import java.lang.Math.unsignedMultiplyHigh

class CoeffMul {
    companion object {

        fun mulCoeff(product: Coeff, x: Coeff, yDigitCount: Int, y0: Long) {
            when {
                (x.dw3 != 0L) -> _mulCoeff(product, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, y0)
                (x.dw2 != 0L) -> _mulCoeff(product, x.digitCount, x.dw2, x.dw1, x.dw0, yDigitCount, y0)
                (x.dw1 != 0L) -> _mulCoeff(product, x.digitCount, x.dw1, x.dw0, yDigitCount, y0)
                else -> _mulCoeff(product, x.digitCount, x.dw0, yDigitCount, y0)
            }
        }

        fun mulCoeff(product: Coeff, x: Coeff, yDigitCount: Int, y1: Long, y0: Long) {
            when {
                (x.dw3 != 0L) -> throw RuntimeException("?que?")
                (x.dw2 != 0L) -> _mulCoeff(product, x.digitCount, x.dw2, x.dw1, x.dw0, yDigitCount, y1, y0)
                (x.dw1 != 0L) -> _mulCoeff(product, x.digitCount, x.dw1, x.dw0, yDigitCount, y1, y0)
                else -> _mulCoeff(product, yDigitCount, y1, y0, x.digitCount, x.dw0)
            }
        }

        fun mulCoeff(product: Coeff, x: Coeff, yDigitCount: Int, y2: Long, y1: Long, y0: Long) {
            when {
                ((x.dw3 or x.dw2)!= 0L) -> assert(false)
                (x.dw1 != 0L) -> _mulCoeff(product, yDigitCount, y2, y1, y0, x.digitCount, x.dw1, x.dw0)
                else -> _mulCoeff(product, yDigitCount, y2, y1, y0, x.digitCount, x.dw0)
            }
        }

        fun mulCoeff(product: Coeff, x: Coeff, yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
            when {
                ((x.dw3 or x.dw2 or x.dw3)!= 0L) -> throw RuntimeException("?que?")
                else -> _mulCoeff(product, yDigitCount, y3, y2, y1, y0, x.digitCount, x.dw0)
            }
        }

        fun mulCoeffPow10(product: Coeff, x: Coeff, pow10: Int) {
            when {
                (pow10 < POW10_128_OFFSET) -> mulCoeff(product, x, pow10, POW10[pow10])
                (pow10 < POW10_192_OFFSET) -> {
                    val index = POW10_128_DWORD_INDEX + (pow10 - POW10_128_OFFSET)*2
                    mulCoeff(product, x, pow10, POW10[index + 1], POW10[index])
                }
                (pow10 < POW10_256_OFFSET) -> {
                    val index = POW10_192_DWORD_INDEX + (pow10 - POW10_192_OFFSET)*3
                    mulCoeff(product, x, pow10, POW10[index + 2], POW10[index + 1], POW10[index])
                }
                (pow10 < POW10_MAX_OFFSET) -> {
                    val index = POW10_256_DWORD_INDEX + (pow10 - POW10_256_OFFSET)*4
                    mulCoeff(product, x, pow10, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index])
                }
            }
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long, yDigitCount: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp01Lo = x0 * y1
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxMulDigitCount
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp11Lo = x1 * y1
            val pp02Lo = x0 * y2
            val pp20Lo = x2 * y0
            if (maxMulDigitCount < POW10_256_OFFSET) {
                val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = 0L;
                p.digitCount = calcDigitCount192(p2, p1, p0)
                return
            }
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp12Lo = x1 * y2
            val pp21Lo = x2 * y1
            val pp03Lo = x0 * y3
            val pp30Lo = x3 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            if (maxMulDigitCount < POW10_MAX_OFFSET) {
                val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp03Hi = unsignedMultiplyHigh(x0, y3)
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp22Lo = x2 * y2
            val dw4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
            if (dw4 == 0L) {
                // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
                assert (maxMulDigitCount == 78 || maxMulDigitCount == 79)
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            throw RuntimeException("coefficient multiply overflow")
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long, yDigitCount: Int, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp10Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp20Lo = x2 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
            if (maxMulDigitCount < POW10_256_OFFSET) {
                val p2 = carry1 + pp10Hi + pp20Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = 0L;
                p.digitCount = calcDigitCount192(p2, p1, p0)
                return
            }
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp30Lo = x3 * y0
            val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)

            if (maxMulDigitCount < POW10_MAX_OFFSET) {
                val p3 = carry2 + pp20Hi + pp30Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)
            val dw4 = carry3 + pp30Hi
            if (dw4 == 0L) {
                // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
                assert (maxMulDigitCount == 78 || maxMulDigitCount == 79)
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            throw RuntimeException("coefficient multiply overflow")
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x2: Long, x1: Long, x0: Long, yDigitCount: Int, y1: Long, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp01Lo = x0 * y1
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxMulDigitCount
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp11Lo = x1 * y1
            val pp20Lo = x2 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
            if (maxMulDigitCount < POW10_256_OFFSET) {
                val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = 0L;
                p.digitCount = calcDigitCount192(p2, p1, p0)
                return
            }
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp21Lo = x2 * y1
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)

            if (maxMulDigitCount < POW10_MAX_OFFSET) {
                val p3 = carry2 + pp11Hi + pp20Hi + pp21Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val (carry3, p3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo)
            val dw4 = carry3 + pp21Hi
            if (dw4 == 0L) {
                // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
                assert (maxMulDigitCount == 78 || maxMulDigitCount == 79)
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = p3
                p.digitCount = calcDigitCount256(p3, p2, p1, p0)
                return
            }
            throw RuntimeException("coefficient multiply overflow")
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x2: Long, x1: Long, x0: Long, yDigitCount: Int, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp10Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp20Lo = x2 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
            if (maxMulDigitCount < POW10_256_OFFSET) {
                val p2 = carry1 + pp10Hi + pp20Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = 0L;
                p.digitCount = calcDigitCount192(p2, p1, p0)
                return
            }
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
            val p3 = carry2 + pp20Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            p.digitCount = calcDigitCount256(p3, p2, p1, p0)
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x1: Long, x0: Long, yDigitCount: Int, y1: Long, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp01Lo = x0 * y1
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxMulDigitCount
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp11Lo = x1 * y1
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
            if (maxMulDigitCount < POW10_256_OFFSET) {
                val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = p2
                p.dw3 = 0L;
                p.digitCount = calcDigitCount192(p2, p1, p0)
                return
            }
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)
            val p3 = carry2 + pp11Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = p3
            p.digitCount = calcDigitCount256(p3, p2, p1, p0)
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x1: Long, x0: Long, yDigitCount: Int, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp10Lo = x1 * y0
            if (maxMulDigitCount < POW10_192_OFFSET) {
                val p1 = pp00Hi + pp10Lo // no carry possible because of maxMulDigitCount
                p.dw0 = p0
                p.dw1 = p1
                p.dw2 = 0L; p.dw3 = 0L;
                p.digitCount = calcDigitCount128(p1, p0)
                return
            }
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
            val p2 = carry1 + pp10Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = p2
            p.dw3 = 0L;
            p.digitCount = calcDigitCount192(p2, p1, p0)
        }

        private fun _mulCoeff(p: Coeff, xDigitCount: Int, x0: Long, yDigitCount: Int, y0: Long) {
            if (xDigitCount == 0 || yDigitCount == 0) {
                p.setZero()
                return
            }
            val maxMulDigitCount = xDigitCount + yDigitCount
            val loDigitCount = maxMulDigitCount - 1
            val pp00Lo = x0 * y0
            val p0 = pp00Lo
            if (maxMulDigitCount < POW10_128_OFFSET) {
                p.dw0 = p0
                p.dw1 = 0L; p.dw2 = 0L; p.dw3 = 0L
                p.digitCount = calcDigitCount64(p0)
                return
            }
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val p1 = pp00Hi
            p.dw0 = p0
            p.dw1 = p1
            p.dw2 = 0L; p.dw3 = 0L;
            p.digitCount = calcDigitCount128(p1, p0)
        }

        fun mulCoeff4x5shr320(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y4: Long, y3: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp03Hi = unsignedMultiplyHigh(x0, y3)
            val pp03Lo = x0 * y3
            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)

            val pp04Hi = unsignedMultiplyHigh(x0, y4)
            val pp04Lo = x0 * y4
            val pp13Hi = unsignedMultiplyHigh(x1, y3)
            val pp13Lo = x1 * y3
            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp03Hi, pp30Hi, pp12Hi, pp21Hi, pp22Lo, pp04Lo, pp13Lo, pp31Lo)

            val pp14Hi = unsignedMultiplyHigh(x1, y4)
            val pp14Lo = x1 * y4
            val pp23Hi = unsignedMultiplyHigh(x2, y3)
            val pp23Lo = x2 * y3
            val pp32Hi = unsignedMultiplyHigh(x3, y2)
            val pp32Lo = x3 * y2
            val (carry5, dw5T) = sumU64(carry4, pp04Hi, pp13Hi, pp31Hi, pp22Hi, pp14Lo, pp23Lo, pp32Lo)

            val pp24Hi = unsignedMultiplyHigh(x2, y4)
            val pp24Lo = x2 * y4
            val pp33Hi = unsignedMultiplyHigh(x3, y3)
            val pp33Lo = x3 * y3
            val (carry6, dw6T) = sumU64(carry5, pp14Hi, pp24Lo, pp23Hi, pp32Hi, pp33Lo)

            val pp34Hi = unsignedMultiplyHigh(x3, y4)
            val pp34Lo = x3 * y4
            val (carry7, dw7T) = sumU64(carry6, pp24Hi, pp33Hi, pp34Lo)

            val dw8T = carry7 + pp34Hi

            prodShifted.dw3 = dw8T
            prodShifted.dw2 = dw7T
            prodShifted.dw1 = dw6T
            prodShifted.dw0 = dw5T
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x5shr256(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y4: Long, y3: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp03Hi = unsignedMultiplyHigh(x0, y3)
            val pp03Lo = x0 * y3
            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)

            val pp04Hi = unsignedMultiplyHigh(x0, y4)
            val pp04Lo = x0 * y4
            val pp13Hi = unsignedMultiplyHigh(x1, y3)
            val pp13Lo = x1 * y3
            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp03Hi, pp30Hi, pp12Hi, pp21Hi, pp22Lo, pp04Lo, pp13Lo, pp31Lo)

            val pp14Hi = unsignedMultiplyHigh(x1, y4)
            val pp14Lo = x1 * y4
            val pp23Hi = unsignedMultiplyHigh(x2, y3)
            val pp23Lo = x2 * y3
            val pp32Hi = unsignedMultiplyHigh(x3, y2)
            val pp32Lo = x3 * y2
            val (carry5, dw5T) = sumU64(carry4, pp04Hi, pp13Hi, pp31Hi, pp22Hi, pp14Lo, pp23Lo, pp32Lo)

            val pp24Hi = unsignedMultiplyHigh(x2, y4)
            val pp24Lo = x2 * y4
            val pp33Hi = unsignedMultiplyHigh(x3, y3)
            val pp33Lo = x3 * y3
            val (carry6, dw6T) = sumU64(carry5, pp14Hi, pp24Lo, pp23Hi, pp32Hi, pp33Lo)

            val pp34Hi = unsignedMultiplyHigh(x3, y4)
            val pp34Lo = x3 * y4
            val (carry7, dw7T) = sumU64(carry6, pp24Hi, pp33Hi, pp34Lo)

            val dw8T = carry7 + pp34Hi

            assert(dw8T == 0L)
            prodShifted.dw3 = dw7T
            prodShifted.dw2 = dw6T
            prodShifted.dw1 = dw5T
            prodShifted.dw0 = dw4T
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x4shr256(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y3: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp03Hi = unsignedMultiplyHigh(x0, y3)
            val pp03Lo = x0 * y3
            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)

            val pp13Hi = unsignedMultiplyHigh(x1, y3)
            val pp13Lo = x1 * y3
            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp03Hi, pp30Hi, pp12Hi, pp21Hi, pp22Lo, pp13Lo, pp31Lo)

            val pp23Hi = unsignedMultiplyHigh(x2, y3)
            val pp23Lo = x2 * y3
            val pp32Hi = unsignedMultiplyHigh(x3, y2)
            val pp32Lo = x3 * y2
            val (carry5, dw5T) = sumU64(carry4, pp13Hi, pp31Hi, pp22Hi, pp32Lo, pp23Lo)

            val pp33Hi = unsignedMultiplyHigh(x3, y3)
            val pp33Lo = x3 * y3
            val (carry6, dw6T) = sumU64(carry5, pp23Hi, pp32Hi, pp33Lo)

            val dw7T = carry6 + pp33Hi

            prodShifted.dw3 = dw7T
            prodShifted.dw2 = dw6T
            prodShifted.dw1 = dw5T
            prodShifted.dw0 = dw4T
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x4shr192(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y3: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp03Hi = unsignedMultiplyHigh(x0, y3)
            val pp03Lo = x0 * y3
            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)

            val pp13Hi = unsignedMultiplyHigh(x1, y3)
            val pp13Lo = x1 * y3
            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp03Hi, pp30Hi, pp12Hi, pp21Hi, pp22Lo, pp13Lo, pp31Lo)

            val pp23Hi = unsignedMultiplyHigh(x2, y3)
            val pp23Lo = x2 * y3
            val pp32Hi = unsignedMultiplyHigh(x3, y2)
            val pp32Lo = x3 * y2
            val (carry5, dw5T) = sumU64(carry4, pp13Hi, pp31Hi, pp22Hi, pp32Lo, pp23Lo)

            val pp33Hi = unsignedMultiplyHigh(x3, y3)
            val pp33Lo = x3 * y3
            val (carry6, dw6T) = sumU64(carry5, pp23Hi, pp32Hi, pp33Lo)

            val dw7T = carry6 + pp33Hi

            assert(dw7T == 0L)

            prodShifted.dw3 = dw6T
            prodShifted.dw2 = dw5T
            prodShifted.dw1 = dw4T
            prodShifted.dw0 = p3
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x3shr192(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo, pp30Lo)

            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp30Hi, pp12Hi, pp21Hi, pp22Lo, pp31Lo)

            val pp32Hi = unsignedMultiplyHigh(x3, y2)
            val pp32Lo = x3 * y2
            val (carry5, dw5T) = sumU64(carry4, pp31Hi, pp22Hi, pp32Lo)

            val dw6T = carry5 + pp32Hi

            prodShifted.dw3 = dw6T
            prodShifted.dw2 = dw5T
            prodShifted.dw1 = dw4T
            prodShifted.dw0 = p3
            prodShifted.digitCount = -1
        }

        fun mulCoeff3x3shr128(prodShifted: Coeff, x2: Long, x1: Long, x0: Long, y2: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp02Hi = unsignedMultiplyHigh(x0, y2)
            val pp02Lo = x0 * y2
            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

            val pp12Hi = unsignedMultiplyHigh(x1, y2)
            val pp12Lo = x1 * y2
            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val (carry3, p3) = sumU64(carry2, pp02Hi, pp20Hi, pp11Hi, pp12Lo, pp21Lo)

            val pp22Hi = unsignedMultiplyHigh(x2, y2)
            val pp22Lo = x2 * y2
            val (carry4, dw4T) = sumU64(carry3, pp12Hi, pp21Hi, pp22Lo)

            val dw5T = carry4 + pp22Hi


            prodShifted.dw3 = dw5T
            prodShifted.dw2 = dw4T
            prodShifted.dw1 = p3
            prodShifted.dw0 = p2
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x2shr128(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y1: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp01Hi = unsignedMultiplyHigh(x0, y1)
            val pp01Lo = x0 * y1
            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

            val pp11Hi = unsignedMultiplyHigh(x1, y1)
            val pp11Lo = x1 * y1
            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)

            val pp21Hi = unsignedMultiplyHigh(x2, y1)
            val pp21Lo = x2 * y1
            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp20Hi, pp11Hi, pp21Lo, pp30Lo)

            val pp31Hi = unsignedMultiplyHigh(x3, y1)
            val pp31Lo = x3 * y1
            val (carry4, dw4T) = sumU64(carry3, pp30Hi, pp21Hi, pp31Lo)

            val dw5T = carry4 + pp31Hi


            prodShifted.dw3 = dw5T
            prodShifted.dw2 = dw4T
            prodShifted.dw1 = p3
            prodShifted.dw0 = p2
            prodShifted.digitCount = -1
        }

        fun mulCoeff4x1shr64(prodShifted: Coeff, x3: Long, x2: Long, x1: Long, x0: Long, y0: Long) {
            val pp00Hi = unsignedMultiplyHigh(x0, y0)
            val pp00Lo = x0 * y0
            val p0 = pp00Lo

            val pp10Hi = unsignedMultiplyHigh(x1, y0)
            val pp10Lo = x1 * y0
            val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

            val pp20Hi = unsignedMultiplyHigh(x2, y0)
            val pp20Lo = x2 * y0
            val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)

            val pp30Hi = unsignedMultiplyHigh(x3, y0)
            val pp30Lo = x3 * y0
            val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)

            val dw4T = carry3 + pp30Hi


            prodShifted.dw3 = dw4T
            prodShifted.dw2 = p3
            prodShifted.dw1 = p2
            prodShifted.dw0 = p1
            prodShifted.digitCount = -1
        }

    }
}