package com.decimal128.decimal


internal object U256Mul {

    fun u256Mul(z: C256, x: C256, y: C256) {
        if ((x.bitLen < 128) and (y.bitLen < 128)) {
            val maxProdBitLen = x.bitLen + y.bitLen
            _mulCoeff2x2(z, maxProdBitLen, x.dw1, x.dw0, y.dw1, y.dw0)
        }
        else
            throw IllegalArgumentException("mul arg >=128 bits ... overflow")
    }

    fun u256Mul(z: C256, x: C256, yBitLen: Int, y0: Long) {
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> {
                val x0 = x.dw0
                val pHi = unsignedMulHi(x0, y0)
                val pLo = x0 * y0
                z.c256Set128(pHi, pLo)
            }
            (xBitLen <= 128) -> _mulCoeff2x1(z, maxBitLen, x.dw1, x.dw0, y0)
            else -> throw IllegalArgumentException("mul arg >=128 bits ... overflow")
        }
    }

    fun u256Mul(z: C256, x: C256, yBitLen: Int, y1: Long, y0: Long) {
        check(yBitLen in 65..128)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> when {
                (xBitLen > 1) -> _mulCoeff2x1(z, maxBitLen, y1, y0, x.dw0)
                (xBitLen == 1) -> z.c256Set128(y1, y0)
                else -> z.c256SetZero()
            }
            (xBitLen <= 128) -> _mulCoeff2x2(z, maxBitLen, x.dw1, x.dw0, y1, y0)
            else -> throw IllegalArgumentException("mul arg >=128 bits ... overflow")
        }
    }

    fun u256Mul(z: C256, x: C256, yBitLen: Int, y2: Long, y1: Long, y0: Long) {
        check(yBitLen in 129..192)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> when {
                (xBitLen > 1) -> _mulCoeff3x1(z, maxBitLen, y2, y1, y0, x.dw0)
                (xBitLen == 1) -> z.c256Set192(y2, y1, y0)
                else -> z.c256SetZero()
            }
            (xBitLen <= 128) -> _mulCoeff3x2(z, maxBitLen, y2, y1, y0, x.dw1, x.dw0)
            else -> throw RuntimeException("coeff overflow")
        }
    }

    fun u256Mul(z: C256, x: C256, yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
        check(yBitLen in 193..256)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> when {
                (xBitLen > 1) -> _mulCoeff4x1(z, maxBitLen, y3, y2, y1, y0, x.dw0)
                (xBitLen == 1) -> z.c256Set256(y3, y2, y1, y0)
                else -> z.c256SetZero()
            }
            else -> throw RuntimeException("coeff overflow")
        }
    }

    @Suppress("UNUSED")
    fun _mulCoeff4x4(
        p: C256,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp01Hi = unsignedMulHi(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMulHi(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = unsignedMulHi(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = unsignedMulHi(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo
            p.c256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
        val pp12Hi = unsignedMulHi(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = unsignedMulHi(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = unsignedMulHi(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = unsignedMulHi(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val pp22Lo = x2 * y2
        val (carry4, p4) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        if ((carry4 or p4) == 0L) {
            check(maxBitLen == 257)
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    fun _mulCoeff4x1(
        p: C256,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = unsignedMulHi(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.c256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
        val pp30Hi = unsignedMulHi(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp20Hi + pp30Lo
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)
        if (carry3 == 0L) {
            check(maxBitLen == 257)
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff3x2(
        p: C256,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp01Hi = unsignedMulHi(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMulHi(x1, y1)
        val pp11Lo = x1 * y1
        val pp20Hi = unsignedMulHi(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo
            p.c256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
        val pp21Hi = unsignedMulHi(x2, y1)
        val pp21Lo = x2 * y1

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp20Hi + pp21Lo
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo)
        if (carry3 == 0L) {
            check(maxBitLen == 257)
            p.c256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff3x1(
        p: C256,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = unsignedMulHi(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.c256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)

        val p3 = carry2 + pp20Hi
        p.c256Set256(p3, p2, p1, p0)
    }

    private fun _mulCoeff2x2(
        p: C256,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp01Hi = unsignedMulHi(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = unsignedMulHi(x1, y1)
        val pp11Lo = x1 * y1
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
            p.c256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)

        val p3 = carry2 + pp11Hi
        p.c256Set256(p3, p2, p1, p0)
    }

    private fun _mulCoeff2x1(
        p: C256,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.c256Set64(p0)
            return
        }
        val pp10Hi = unsignedMulHi(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val p2 = carry1 + pp10Hi
        p.c256Set192(p2, p1, p0)
    }

}