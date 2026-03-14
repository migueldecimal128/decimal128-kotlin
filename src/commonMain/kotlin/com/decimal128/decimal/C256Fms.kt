package com.decimal128.decimal

import kotlin.math.max

internal fun c256SetFms(z: C256, x: C256, y: C256, s: C256, pentad: Pentad) {
  verify { z.c256HasValidLengths() }
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { s.c256HasValidLengths() }
    val flipFlop = x.bitLen >= y.bitLen
    val m = if (flipFlop) x else y
    val n = if (flipFlop) y else x
    val mBitLen = m.bitLen
    val nBitLen = n.bitLen
    verify { mBitLen >= nBitLen }
    val m2 = m.dw2; val m1 = m.dw1; val m0 = m.dw0
    val n0 = n.dw0
    val s3 = s.dw3; val s2 = s.dw2; val s1 = s.dw1; val s0 = s.dw0
    val sBitLen = s.bitLen

    val maxFusedBitLen = max(mBitLen + nBitLen, s.bitLen) + 1
    if (nBitLen <= 64) {
        when {
            (mBitLen <= 64 && sBitLen <= 128) ->
                _fms1x1x2(
                    z, maxFusedBitLen,
                    m0,
                    n0,
                    s1, s0,
                    pentad
                )

            (mBitLen <= 128 && sBitLen <= 192) ->
                _fms2x1x3(
                    z, maxFusedBitLen,
                    m1, m0,
                    n0,
                    s2, s1, s0,
                    pentad
                )

            (mBitLen <= 192) ->
                _fms3x1x4(
                    z, maxFusedBitLen,
                    m2, m1, m0,
                    n0,
                    s3, s2, s1, s0,
                    pentad
                )

            else ->
                _fms4x1x4(
                    z, maxFusedBitLen,
                    m.dw3, m2, m1, m0,
                    n0,
                    s3, s2, s1, s0,
                    pentad
                )
        }
        return
    }
    val n1 = n.dw1
    if (nBitLen <= 128) {
        when {
            (mBitLen <= 128) ->
                _fms2x2x4(
                    z, maxFusedBitLen,
                    m1, m0,
                    n1, n0,
                    s3, s2, s1, s0,
                    pentad
                )

            (mBitLen <= 192) ->
                _fms3x2x4(
                    z, maxFusedBitLen,
                    m2, m1, m0,
                    n1, n0,
                    s3, s2, s1, s0,
                    pentad
                )

            else -> throw RuntimeException("U256 overflow")
        }
        return
    }
    throw RuntimeException("U256 overflow")
}


internal fun c256FmsPow10(z: C256, x: C256, pow10: Int, y: C256, pentad: Pentad) {
    verify { pow10 > 0 }
    verify { z.c256HasValidLengths() }
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { c256ScaledCompare(y, x, pow10, pentad) <= 0 }
    val xBitLen = x.bitLen
    val x0 = x.dw0
    val x1 = x.dw1
    val yBitLen = y.bitLen
    val p10BitLen = pow10BitLen(pow10)
    val pow10Offset = pow10Offset(pow10) and POW10_BCE
    val p0 = POW10[pow10Offset    ]
    val p1 = POW10[pow10Offset + 1] and ((64 - p10BitLen) shr 31).toLong()
    val maxFusedBitLen = xBitLen + p10BitLen
    /*
    if (maxFusedBitLen <= 128) {
        val (f1, f0) = umul128x128to128(x1, x0, p1, p0)
        val (d1, d0) = diffU128(f1, f0, y1, y0)
        z.coeffSet128(d1, d0)
        return
    }

     */
    if (p10BitLen <= 64) {
        when {
            (xBitLen <= 64 && yBitLen <= 128) ->
                _fms1x1x2(
                    z, maxFusedBitLen,
                    x0,
                    p0,
                    y.dw1, y.dw0,
                    pentad
                )

            (xBitLen <= 128 && yBitLen <= 192) ->
                _fms2x1x3(
                    z, maxFusedBitLen,
                    x1, x0,
                    p0,
                    y.dw2, y.dw1, y.dw0,
                    pentad
                )

            (xBitLen <= 192) ->
                _fms3x1x4(
                    z, maxFusedBitLen,
                    x.dw2, x1, x0,
                    p0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )

            else ->
                _fms4x1x4(
                    z, maxFusedBitLen,
                    x.dw3, x.dw2, x1, x0,
                    p0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )
        }
        return
    }
    if (p10BitLen <= 128) {
        when {
            (xBitLen <= 64 && yBitLen <= 192) ->
                _fms2x1x3(
                    z, maxFusedBitLen,
                    p1, p0,
                    x0,
                    y.dw2, y.dw1, y.dw0,
                    pentad
                )

            (xBitLen <= 128) ->
                _fms2x2x4(
                    z, maxFusedBitLen,
                    x1, x0,
                    p1, p0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )

            (xBitLen <= 192) ->
                _fms3x2x4(
                    z, maxFusedBitLen,
                    x.dw2, x1, x0,
                    p1, p0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )

            else -> throw RuntimeException("coeff overflow")
        }
        return
    }
    val p2 = POW10[pow10Offset + 2]
    if (p10BitLen <= 192) {
        when {
            (xBitLen <= 64) ->
                _fms3x1x4(
                    z, maxFusedBitLen,
                    p2, p1, p0,
                    x0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )

            (xBitLen <= 128) ->
                _fms3x2x4(
                    z, maxFusedBitLen,
                    p2, p1, p0,
                    x1, x0,
                    y.dw3, y.dw2, y.dw1, y.dw0,
                    pentad
                )

            else -> throw RuntimeException("coeff overflow")
        }
        return
    }
    val p3 = POW10[pow10Offset + 3]
    if (xBitLen <= 64) {
        _fms4x1x4(
            z, maxFusedBitLen,
            p3, p2, p1, p0,
            x0,
            y.dw3, y.dw2, y.dw1, y.dw0,
            pentad
        )
        return
    } else {
        throw RuntimeException("coeff overflow")
    }
}

internal fun c256FusedSubMulPow10(z: C256, x: C256, y: C256, pow10: Int, pentad: Pentad) {
    verify { pow10 > 0 }
    verify { z.c256HasValidLengths() }
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { c256ScaledCompare(x, y, pow10, pentad) >= 0 }
    val y0 = y.dw0
    val y1 = y.dw1
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val p1 = POW10[pow10Offset + 1]
    val p0 = POW10[pow10Offset    ]

    val pp00Hi = unsignedMulHi(y0, p0)
    val pp00Lo = y0 * p0

    val pp10Hi = unsignedMulHi(y1, p0)
    val pp10Lo = y1 * p0

    val pp01Hi = unsignedMulHi(y0, p1)
    val pp01Lo = y0 * p1

    val pp11Hi = unsignedMulHi(y1, p1)  // NEW!
    val pp11Lo = y1 * p1                // NEW!

// Accumulate properly across all 4 limbs
    val f0 = pp00Lo
    sumU64(pentad, pp00Hi, pp10Lo, pp01Lo)
    val carry1 = pentad.dw1
    val f1 = pentad.dw0

    sumU64(pentad, carry1, pp10Hi, pp01Hi, pp11Lo)
    val carry2 = pentad.dw1
    val f2 = pentad.dw0

    val f3 = carry2 + pp11Hi

// Now subtract from x
    diffU64(pentad, x.dw0, f0)
    val borrow0 = pentad.dw1
    val d0 = pentad.dw0
    diffU64withBorrow(pentad, x.dw1, f1, borrow0)
    val borrow1 = pentad.dw1
    val d1 = pentad.dw0
    diffU64withBorrow(pentad, x.dw2, f2, borrow1)
    val borrow2 = pentad.dw1
    val d2 = pentad.dw0
    //diffU64withBorrow(pentad, x.dw3, f3, borrow2)
    //val borrow3 = pentad.dw1
    //val d3 = pentad.dw0
    val d3 = x.dw3 - f3 - borrow2

    z.c256Set256(d3, d2, d1, d0)
}

/*
@Suppress("UNUSED")
private fun _fms4x4x4(
    f: C256,
    maxFusedBitLen: Int,
    x3: Long, x2: Long, x1: Long, x0: Long,
    y3: Long, y2: Long, y1: Long, y0: Long,
    s3: Long, s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    val (borrow0, f0) = diffU64(pp00Lo, s0)
    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp01Lo + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    val (carry1, f1p) = sumU64(pp00Hi, pp01Lo, pp10Lo)
    val (borrow1, f1) = diffU64withBorrow(f1p, s1, borrow0)

    val pp11Hi = unsignedMulHi(x1, y1)
    val pp11Lo = x1 * y1
    val pp02Hi = unsignedMulHi(x0, y2)
    val pp02Lo = x0 * y2
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    val (carry2, f2p) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)
    val (borrow2, f2) = diffU64withBorrow(f2p, s2, borrow1)

    val pp12Hi = unsignedMulHi(x1, y2)
    val pp12Lo = x1 * y2
    val pp21Hi = unsignedMulHi(x2, y1)
    val pp21Lo = x2 * y1
    val pp03Hi = unsignedMulHi(x0, y3)
    val pp03Lo = x0 * y3
    val pp30Hi = unsignedMulHi(x3, y0)
    val pp30Lo = x3 * y0

    if (maxFusedBitLen <= 256) {
        val f3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo - s3 - borrow2
        f.c256Set256(f3, f2, f1, f0)
        return
    }
    val (carry3, f3p) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
    val (borrow3, f3) = diffU64withBorrow(f3p, s3, borrow2)
    val pp22Lo = x2 * y2
    val (carry4, f4t) = sumU64(carry3, pp12Hi, pp21Hi, pp03Hi, pp30Hi, pp22Lo)
    val (borrow4, f4) = diffU64(f4t, borrow3)
    if (carry4 == 0L && borrow4 == 0L && f4 == 0L) {
        f.c256Set256(f3, f2, f1, f0)
        return
    }
    throw RuntimeException("U256 overflow")
}
*/

@Suppress("NOTHING_TO_INLINE")
private inline fun _fms1x1x2(
    f: C256,
    maxFusedBitLen: Int,
    x0: Long,
    y0: Long,
    s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    // maxFusedBitLen can be at most 129 (64+64+1); f2 absorbs residual borrow
    diffU64withBorrow(pentad, pp00Hi, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    val f2 = -borrow1
    f.c256Set192(f2, f1, f0)
}

private fun _fms2x1x3(
    f: C256,
    maxFusedBitLen: Int,
    x1: Long, x0: Long,
    y0: Long,
    s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val f1p = pentad.dw0
    diffU64withBorrow(pentad, f1p, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp10Hi - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    sumU64(pentad, carry1, pp10Hi)
    val carry2 = pentad.dw1
    val f2p = pentad.dw0
    diffU64withBorrow(pentad, f2p, s2, borrow1)
    val borrow2 = pentad.dw1
    val f2 = pentad.dw0
    // maxFusedBitLen <= 193 (128+64+1); f3 absorbs residual
    val f3 = carry2 - borrow2
    f.c256Set256(f3, f2, f1, f0)
}

private fun _fms3x1x4(
    f: C256,
    maxFusedBitLen: Int,
    x2: Long, x1: Long, x0: Long,
    y0: Long,
    s3: Long, s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val f1p = pentad.dw0
    diffU64withBorrow(pentad, f1p, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp10Hi + pp20Lo - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    sumU64(pentad, carry1, pp10Hi, pp20Lo)
    val carry2 = pentad.dw1
    val f2p = pentad.dw0
    diffU64withBorrow(pentad, f2p, s2, borrow1)
    val borrow2 = pentad.dw1
    val f2 = pentad.dw0

    verify { maxFusedBitLen <= 256 }

    val f3 = carry2 + pp20Hi - s3 - borrow2
    f.c256Set256(f3, f2, f1, f0)
}

private fun _fms4x1x4(
    f: C256,
    maxFusedBitLen: Int,
    x3: Long, x2: Long, x1: Long, x0: Long,
    y0: Long,
    s3: Long, s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val f1p = pentad.dw0
    diffU64withBorrow(pentad, f1p, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp10Hi + pp20Lo - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    sumU64(pentad, carry1, pp10Hi, pp20Lo)
    val carry2 = pentad.dw1
    val f2p = pentad.dw0
    diffU64withBorrow(pentad, f2p, s2, borrow1)
    val borrow2 = pentad.dw1
    val f2 = pentad.dw0
    val pp30Lo = x3 * y0

    verify { maxFusedBitLen <= 256 }

    val f3 = carry2 + pp20Hi + pp30Lo - s3 - borrow2
    f.c256Set256(f3, f2, f1, f0)
}

private fun _fms2x2x4(
    f: C256,
    maxFusedBitLen: Int,
    x1: Long, x0: Long,
    y1: Long, y0: Long,
    s3: Long, s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp01Lo + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val f1p = pentad.dw0
    diffU64withBorrow(pentad, f1p, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    val pp11Hi = unsignedMulHi(x1, y1)
    val pp11Lo = x1 * y1
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    sumU64(pentad, carry1, pp01Hi, pp10Hi, pp11Lo)
    val carry2 = pentad.dw1
    val f2p = pentad.dw0
    diffU64withBorrow(pentad, f2p, s2, borrow1)
    val borrow2 = pentad.dw1
    val f2 = pentad.dw0

    verify { maxFusedBitLen <= 256 }

    val f3 = carry2 + pp11Hi - s3 - borrow2
    f.c256Set256(f3, f2, f1, f0)
}

private fun _fms3x2x4(
    f: C256,
    maxFusedBitLen: Int,
    x2: Long, x1: Long, x0: Long,
    y1: Long, y0: Long,
    s3: Long, s2: Long, s1: Long, s0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    if (maxFusedBitLen <= 64) {
        val f0 = pp00Lo - s0
        f.c256Set64(f0)
        return
    }
    diffU64(pentad, pp00Lo, s0)
    val borrow0 = pentad.dw1
    val f0 = pentad.dw0
    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    if (maxFusedBitLen <= 128) {
        val f1 = pp00Hi + pp01Lo + pp10Lo - s1 - borrow0
        f.c256Set128(f1, f0)
        return
    }
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val f1p = pentad.dw0
    diffU64withBorrow(pentad, f1p, s1, borrow0)
    val borrow1 = pentad.dw1
    val f1 = pentad.dw0
    val pp11Hi = unsignedMulHi(x1, y1)
    val pp11Lo = x1 * y1
    val pp20Hi = unsignedMulHi(x2, y0)
    val pp20Lo = x2 * y0
    if (maxFusedBitLen <= 192) {
        val f2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp20Lo - s2 - borrow1
        f.c256Set192(f2, f1, f0)
        return
    }
    sumU64(pentad, carry1, pp01Hi, pp10Hi, pp11Lo, pp20Lo)
    val carry2 = pentad.dw1
    val f2p = pentad.dw0
    diffU64withBorrow(pentad, f2p, s2, borrow1)
    val borrow2 = pentad.dw1
    val f2 = pentad.dw0
    val pp21Hi = unsignedMulHi(x2, y1)
    val pp21Lo = x2 * y1

    verify { maxFusedBitLen <= 256 }

    val f3 = carry2 + pp11Hi + pp20Hi + pp21Lo - s3 - borrow2
    f.c256Set256(f3, f2, f1, f0)
}
