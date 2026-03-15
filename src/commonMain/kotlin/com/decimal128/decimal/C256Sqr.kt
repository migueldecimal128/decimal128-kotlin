package com.decimal128.decimal

internal fun c256SetSqr(z: C256, x: C256, pentad: Pentad) {
    val xBitLen = x.bitLen
    when {
        (xBitLen <= 64) -> {
            val p1 = unsignedMulHi(x.dw0, x.dw0)
            val p0 = x.dw0 * x.dw0
            z.c256Set128(p1, p0)
            return
        }

        (xBitLen <= 96) -> {
            usqr96to192(pentad, x.dw1, x.dw0)
            z.c256Set192(pentad.dw2, pentad.dw1, pentad.dw0)
        }

        (xBitLen <= 128) -> {
            _sqrC256_2to4(z, x.dw1, x.dw0, pentad)
        }

        else -> throw RuntimeException("coeff mul overflow")
    }
}

private fun _sqrC256_2to4(
    p: C256,
    x1: Long, x0: Long,
    pentad: Pentad
) {
    val pp00Hi = unsignedMulHi(x0, x0)
    val pp00Lo = x0 * x0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, x1)
    val pp01Lo = x0 * x1
    val pp10Hi = pp01Hi
    val pp10Lo = pp01Lo
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0

    val pp11Hi = unsignedMulHi(x1, x1)
    val pp11Lo = x1 * x1
    sumU64(pentad, carry1, pp01Hi, pp10Hi, pp11Lo)
    val carry2 = pentad.dw1
    val p2 = pentad.dw0

    val p3 = carry2 + pp11Hi
    p.c256Set256(p3, p2, p1, p0)
}

