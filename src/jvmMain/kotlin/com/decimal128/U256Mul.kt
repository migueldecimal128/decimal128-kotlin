package com.decimal128


internal object U256Mul {

    fun u256Mul(z: U256, x: U256, y: U256) {
        val xBitLen = x.bitLen
        val yBitLen = y.bitLen
        val maxBitLen = xBitLen + yBitLen
        if (maxBitLen <= 128) {
            val (p1, p0) = umul128x128to128(x.dw1, x.dw0, y.dw1, y.dw0)
            z.u256Set128(p1, p0)
            return
        }
        if ((xBitLen or yBitLen) <= 128 && maxBitLen <= 192) {
            val (p2, p1, p0) = umul128x128to192(x.dw1, x.dw0, y.dw1, y.dw0)
            z.u256Set192(p2, p1, p0)
            return
        }

        val flipFlop = xBitLen >= yBitLen
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val mBitLen = m.bitLen
        val nBitLen = n.bitLen
        val maxProdBitLen = mBitLen + nBitLen
        // mBitLen >= nBitLen
        check(m.bitLen >= n.bitLen)
        val m0 = m.dw0
        val n0 = n.dw0
        when {
            (nBitLen <= 64) -> {
                when {
                    (maxProdBitLen <= 192) -> {
                        val (p2, p1, p0) = umul192x64to192(m.dw2, m.dw1, m0, n0)
                        z.u256Set192(p2, p1, p0)
                        return
                    }
                    (nBitLen == 0) -> z.u256SetZero()
                    (n0 and (n0 - 1) == 0L) -> {
                        // also handles n0 == 1
                        // even power of 2 ... just shift
                        val ntz = n0.countTrailingZeroBits()
                        z.u256SetShiftLeft(m, ntz)
                    }

                    (m.bitLen <= 128) -> _mulCoeff2x1(z, maxProdBitLen, m.dw1, m.dw0, n0)
                    (m.bitLen <= 192) -> _mulCoeff3x1(z, maxProdBitLen, m.dw2, m.dw1, m.dw0, n0)
                    else -> _mulCoeff4x1(z, maxProdBitLen, m.dw3, m.dw2, m.dw1, m.dw0, n0)
                }
                return
            }

            (nBitLen <= 128 && m.bitLen <= 192) -> {
                _mulCoeff3x2(z, maxProdBitLen, m.dw2, m.dw1, m.dw0, n.dw1, n0)
                return
            }
        }
        throw RuntimeException("coeff mul overflow")
    }

    fun u256Mul(z: U256, x: U256, yBitLen: Int, y0: Long) {
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        if (maxBitLen <= 192) {
            val (p2, p1, p0) = umul192x64to192(x.dw2, x.dw1, x.dw0, y0)
            z.u256Set192(p2, p1, p0)
            return
        }
        when {
            (xBitLen <= 64) -> {
                val x0 = x.dw0
                val pHi = umulHigh(x0, y0)
                val pLo = x0 * y0
                z.u256Set128(pHi, pLo)
                return
            }
            (xBitLen <= 128) -> _mulCoeff2x1(z, maxBitLen, x.dw1, x.dw0, y0)
            (xBitLen <= 192) -> _mulCoeff3x1(z, maxBitLen, x.dw2, x.dw1, x.dw0, y0)
            else -> _mulCoeff4x1(z, maxBitLen, x.dw3, x.dw2, x.dw1, x.dw0, y0)
        }
    }

    fun u256Mul(z: U256, x: U256, yBitLen: Int, y1: Long, y0: Long) {
        check(yBitLen in 65..128)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (maxBitLen <= 192) -> {
                val (p2, p1, p0) = umul128x128to192(x.dw1, x.dw0, y1, y0)
                z.u256Set192(p2, p1, p0)
                return
            }
            //(xBitLen <= 64) -> when {
            //    (xBitLen > 1) -> _mulCoeff2x1(z, maxBitLen, y1, y0, x.dw0)
            //    (xBitLen == 1) -> z.coeffSet128(y1, y0)
            //    else -> z.coeffSetZero()
            //}
            (xBitLen <= 128) -> _mulCoeff2x2(z, maxBitLen, x.dw1, x.dw0, y1, y0)
            (xBitLen <= 192) -> _mulCoeff3x2(z, maxBitLen, x.dw2, x.dw1, x.dw0, y1, y0)
            else -> throw RuntimeException("coeff overflow")
        }
    }

    fun u256Mul(z: U256, x: U256, yBitLen: Int, y2: Long, y1: Long, y0: Long) {
        check(yBitLen in 129..192)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> when {
                (xBitLen > 1) -> _mulCoeff3x1(z, maxBitLen, y2, y1, y0, x.dw0)
                (xBitLen == 1) -> z.u256Set192(y2, y1, y0)
                else -> z.u256SetZero()
            }
            (xBitLen <= 128) -> _mulCoeff3x2(z, maxBitLen, y2, y1, y0, x.dw1, x.dw0)
            else -> throw RuntimeException("coeff overflow")
        }
    }

    fun u256Mul(z: U256, x: U256, yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long) {
        check(yBitLen in 193..256)
        val xBitLen = x.bitLen
        val maxBitLen = xBitLen + yBitLen
        when {
            (xBitLen <= 64) -> when {
                (xBitLen > 1) -> _mulCoeff4x1(z, maxBitLen, y3, y2, y1, y0, x.dw0)
                (xBitLen == 1) -> z.u256Set256(y3, y2, y1, y0)
                else -> z.u256SetZero()
            }
            else -> throw RuntimeException("coeff overflow")
        }
    }

    @Suppress("UNUSED")
    fun _mulCoeff4x4(
        p: U256,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y3: Long, y2: Long, y1: Long, y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp01Hi = umulHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = umulHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp02Hi = umulHigh(x0, y2)
        val pp02Lo = x0 * y2
        val pp20Hi = umulHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo
            p.u256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
        val pp12Hi = umulHigh(x1, y2)
        val pp12Lo = x1 * y2
        val pp21Hi = umulHigh(x2, y1)
        val pp21Lo = x2 * y1
        val pp03Hi = umulHigh(x0, y3)
        val pp03Lo = x0 * y3
        val pp30Hi = umulHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val pp22Lo = x2 * y2
        val (carry4, p4) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
        if ((carry4 or p4) == 0L) {
            check(maxBitLen == 257)
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    fun _mulCoeff4x1(
        p: U256,
        maxBitLen: Int,
        x3: Long, x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = umulHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.u256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)
        val pp30Hi = umulHigh(x3, y0)
        val pp30Lo = x3 * y0

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp20Hi + pp30Lo
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp20Hi, pp30Lo)
        if (carry3 == 0L) {
            check(maxBitLen == 257)
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff3x2(
        p: U256,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp01Hi = umulHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = umulHigh(x1, y1)
        val pp11Lo = x1 * y1
        val pp20Hi = umulHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo
            p.u256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
        val pp21Hi = umulHigh(x2, y1)
        val pp21Lo = x2 * y1

        if (maxBitLen <= 256) {
            val p3 = carry2 + pp11Hi + pp20Hi + pp21Lo
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        val (carry3, p3) = sumU64(carry2, pp11Hi, pp20Hi, pp21Lo)
        if (carry3 == 0L) {
            check(maxBitLen == 257)
            p.u256Set256(p3, p2, p1, p0)
            return
        }
        throw RuntimeException("coeff multiply overflow")
    }

    private fun _mulCoeff3x1(
        p: U256,
        maxBitLen: Int,
        x2: Long, x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val pp20Hi = umulHigh(x2, y0)
        val pp20Lo = x2 * y0
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp10Hi + pp20Lo
            p.u256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp10Hi, pp20Lo)

        val p3 = carry2 + pp20Hi
        p.u256Set256(p3, p2, p1, p0)
    }

    private fun _mulCoeff2x2(
        p: U256,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y1: Long, y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp01Hi = umulHigh(x0, y1)
        val pp01Lo = x0 * y1
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        val pp11Hi = umulHigh(x1, y1)
        val pp11Lo = x1 * y1
        if (maxBitLen <= 192) {
            val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
            p.u256Set192(p2, p1, p0)
            return
        }
        val (carry2, p2) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo)

        val p3 = carry2 + pp11Hi
        p.u256Set256(p3, p2, p1, p0)
    }

    private fun _mulCoeff2x1(
        p: U256,
        maxBitLen: Int,
        x1: Long, x0: Long,
        y0: Long
    ) {
        val pp00Hi = umulHigh(x0, y0)
        val pp00Lo = x0 * y0
        val p0 = pp00Lo
        if (maxBitLen <= 64) {
            p.u256Set64(p0)
            return
        }
        val pp10Hi = umulHigh(x1, y0)
        val pp10Lo = x1 * y0
        if (maxBitLen <= 128) {
            val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
            p.u256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val p2 = carry1 + pp10Hi
        p.u256Set192(p2, p1, p0)
    }

}