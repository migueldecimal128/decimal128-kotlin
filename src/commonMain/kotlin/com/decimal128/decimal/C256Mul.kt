// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

/**
 * Multiplies two decimal coefficients, each at most 127 bits (38 digits).
 * Fast path for both operands <= 64 bits. Maximum product is 254 bits.
 * Properly defends against aliasing.
 */
internal fun c256SetMul(z: C256, x: C256, y: C256, pentad: Pentad) {
    val xBitLen = x.bitLen
    val yBitLen = y.bitLen
    val maxProdBitLen = xBitLen + yBitLen
    val x0 = x.dw0; val x1 = x.dw1
    val y0 = y.dw0; val y1 = y.dw1
    when {
        xBitLen == 0 || yBitLen == 0 -> z.c256SetZero()

        (xBitLen or yBitLen) <= 64 -> { //if ((xBitLen <= 64) and (yBitLen <= 64))
            val pHi = unsignedMulHi(x0, y0)
            val pLo = x0 * y0
            z.c256Set128(pHi, pLo)
            return
        }
        (xBitLen or yBitLen) <= 128 ->
            _mulCoeff2x2(z, maxProdBitLen, x1, x0, y1, y0, pentad)

        (xBitLen <= 192 && yBitLen <= 64) ->
            _mulCoeff3x1(z, maxProdBitLen, x.dw2, x1, x0, y0, pentad)

        else -> throwCoeffMultiplyOverflow()
    }
}

/**
 * Multiplies a decimal coefficient by a 64-bit unsigned integer (1 limb).
 * Dispatches to [_mulCoeff2x1] or handles inline for 64-bit x.
 */
internal fun c256SetMul(z: C256, x: C256, yBitLen: Int, y0: Long, pentad: Pentad) {
    val xBitLen = x.bitLen
    verify { xBitLen <= 127 }
    val maxBitLen = xBitLen + yBitLen
    val x0 = x.dw0
    val x1 = x.dw1
    when {
        xBitLen <= 64 -> {
            val pHi = unsignedMulHi(x0, y0)
            val pLo = x0 * y0
            z.c256Set128(pHi, pLo)
        }

        xBitLen <= 128 -> _mulCoeff2x1(z, maxBitLen, x1, x0, y0, pentad)
        xBitLen <= 192 ->
            _mulCoeff3x1(z, maxBitLen, x.dw2, x1, x0, y0, pentad)

        else ->
            _mulCoeff4x1(z, maxBitLen, x.dw3, x.dw2, x1, x0, y0, pentad)
    }
}

/**
 * Multiplies a decimal coefficient by a 128-bit unsigned integer (2 limbs, 65..128 bits).
 * Dispatches to [_mulCoeff2x1] or [_mulCoeff2x2] based on x limb count.
 */
internal fun c256SetMul(z: C256, x: C256, yBitLen: Int, y1: Long, y0: Long, pentad: Pentad) {
    verify { yBitLen in 65..128 }
    val xBitLen = x.bitLen
    verify { xBitLen <= 127 }
    val maxBitLen = xBitLen + yBitLen
    verify { maxBitLen <= 254 }
    val x0 = x.dw0;
    when {
        xBitLen <= 64 -> _mulCoeff2x1(z, maxBitLen, y1, y0, x0, pentad)
        xBitLen <= 127 -> _mulCoeff2x2(z, maxBitLen, x.dw1, x0, y1, y0, pentad)
        else -> throwCoeffMultiplyOverflow()
    }
}

/**
 * Multiplies a decimal coefficient by a 192-bit unsigned integer (3 limbs, 129..192 bits).
 * Dispatches to [_mulCoeff3x1] or [_mulCoeff3x2] based on x limb count.
 */
internal fun c256SetMul(z: C256, x: C256, yBitLen: Int, y2: Long, y1: Long, y0: Long,
                        pentad: Pentad) {
    verify { yBitLen in 129..192 }
    val xBitLen = x.bitLen
    verify { xBitLen <= 127 }
    val maxBitLen = xBitLen + yBitLen
    verify { maxBitLen <= 257 }
    val x0 = x.dw0;
    when {
        xBitLen <= 64 -> _mulCoeff3x1(z, maxBitLen, y2, y1, y0, x0, pentad)
        xBitLen <= 127 -> _mulCoeff3x2(z, maxBitLen, y2, y1, y0, x.dw1, x0, pentad)
        else -> throwCoeffMultiplyOverflow()
    }
}

/**
 * Multiplies a decimal coefficient by a 256-bit unsigned integer (4 limbs, 193..256 bits).
 * x is constrained to 64 bits (1 limb); dispatches to [_mulCoeff4x1].
 */
internal fun c256SetMul(z: C256, x: C256, yBitLen: Int, y3: Long, y2: Long, y1: Long, y0: Long,
                        pentad: Pentad) {
    verify { yBitLen in 193..256 }
    val xBitLen = x.bitLen
    verify { xBitLen <= 64 }
    val maxBitLen = xBitLen + yBitLen
    verify { maxBitLen <= 256 }
    if (xBitLen <= 64)
        _mulCoeff4x1(z, maxBitLen, y3, y2, y1, y0, x.dw0, pentad)
    else
        throwCoeffMultiplyOverflow()
}

/**
 * provided only as a reference implementation
 */
/*
@Suppress("UNUSED")
private fun _mulCoeff4x4(
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
        verify { maxBitLen == 257 }
        p.c256Set256(p3, p2, p1, p0)
        return
    }
    throwCoeffMultiplyOverflow()
}
*/

/**
 * 4-limb × 1-limb multiply. Product at most 254 bits (10^76 - 1 requires 253 bits).
 */
private inline fun _mulCoeff4x1(
    p: C256,
    maxBitLen: Int,
    x3: Long, x2: Long, x1: Long, x0: Long,
    y0: Long,
    pentad: Pentad
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
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxBitLen <= 192) {
        val p2 = carry1 + pp10Hi + pp20Lo
        p.c256Set192(p2, p1, p0)
        return
    }
    sumU64(pentad, carry1, pp10Hi, pp20Lo)
    val carry2 = pentad.dw1
    val p2 = pentad.dw0
    val pp30Lo = x3 * y0

    if (maxBitLen <= 256) {
        val p3 = carry2 + pp20Hi + pp30Lo
        p.c256Set256(p3, p2, p1, p0)
        return
    }
    sumU64(pentad, carry2, pp20Hi, pp30Lo)
    val carry3 = pentad.dw1
    val p3 = pentad.dw0
    if (carry3 == 0L) {
        verify { maxBitLen == 257 }
        p.c256Set256(p3, p2, p1, p0)
        return
    }
    throwCoeffMultiplyOverflow()
}

/**
 * 3-limb × 2-limb multiply. Product at most 254 bits (10^76 - 1 requires 253 bits).
 */
private inline fun _mulCoeff3x2(
    p: C256,
    maxBitLen: Int,
    x2: Long, x1: Long, x0: Long,
    y1: Long, y0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxBitLen <= 128) {
        val p1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxBitLen
        p.c256Set128(p1, p0)
        return
    }
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0
    val pp11Hi = unsignedMulHi(x1, y1)
    val pp11Lo = x1 * y1
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxBitLen <= 192) {
        val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo
        p.c256Set192(p2, p1, p0)
        return
    }
    sumU64(pentad, carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
    val carry2 = pentad.dw1
    val p2 = pentad.dw0
    val pp21Lo = x2 * y1

    if (maxBitLen <= 256) {
        val p3 = carry2 + pp11Hi + pp20Hi + pp21Lo
        p.c256Set256(p3, p2, p1, p0)
        return
    }
    sumU64(pentad, carry2, pp11Hi, pp20Hi, pp21Lo)
    val carry3 = pentad.dw1
    val p3 = pentad.dw0
    if (carry3 == 0L) {
        verify { maxBitLen == 257 }
        p.c256Set256(p3, p2, p1, p0)
        return
    }
    throwCoeffMultiplyOverflow()
}

/**
 * 3-limb × 1-limb multiply. Product at most 192 bits.
 */
private inline fun _mulCoeff3x1(
    p: C256,
    maxBitLen: Int,
    x2: Long, x1: Long, x0: Long,
    y0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxBitLen <= 128) {
        val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
        p.c256Set128(p1, p0)
        return
    }
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxBitLen <= 192) {
        val p2 = carry1 + pp10Hi + pp20Lo
        p.c256Set192(p2, p1, p0)
        return
    }
    sumU64(pentad, carry1, pp10Hi, pp20Lo)
    val carry2 = pentad.dw1
    val p2 = pentad.dw0

    val p3 = carry2 + pp20Hi
    p.c256Set256(p3, p2, p1, p0)
}

/**
 * 2-limb × 2-limb multiply. Product at most 254 bits (10^76 - 1 requires 253 bits).
 */
private fun _mulCoeff2x2(
    c: C256,
    maxBitLen: Int,
    x1: Long, x0: Long,
    y1: Long, y0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxBitLen <= 128) {
        val p1 = pp00Hi + pp01Lo + pp10Lo
        c.c256Set128(p1, p0)
        return
    }
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0
    val pp11Hi = unsignedMulHi(x1, y1)
    val pp11Lo = x1 * y1
    if (maxBitLen <= 192) {
        val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo
        c.c256Set192(p2, p1, p0)
        return
    }
    sumU64(pentad, carry1, pp01Hi, pp10Hi, pp11Lo)
    val carry2 = pentad.dw1
    val p2 = pentad.dw0
    val p3 = carry2 + pp11Hi

    c.c256Set256(p3, p2, p1, p0)
}

/**
 * 2-limb × 1-limb multiply. Product at most 192 bits.
 */
private inline fun _mulCoeff2x1(
    c: C256,
    maxBitLen: Int,
    x1: Long, x0: Long,
    y0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxBitLen <= 128) {
        val p1 = pp00Hi + pp10Lo // no carry possible because of maxBitLen
        c.c256Set128(p1, p0)
        return
    }
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0
    val p2 = carry1 + pp10Hi
    c.c256Set192(p2, p1, p0)
}

/**
 * Thrown when a coefficient product exceeds the supported range.
 */
private fun throwCoeffMultiplyOverflow() {
    throw RuntimeException("coeff multiply overflow")
}

