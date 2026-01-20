package com.decimal128.decimal


internal fun c256SetSubUnscaled(z: C256, x: C256, y: C256) { // minuend - subtrahend
    verify { z.c256HasValidLengths() }
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { x.c256UnscaledCompareTo(y) >= 0 }
    val xBitLen = x.bitLen
    verify { xBitLen >= y.bitLen }

    val d0 = x.dw0 - y.dw0
    if (xBitLen <= 64) {
        z.c256Set64(d0)
        return
    }
    val carry0 = if (unsignedCmp(d0, x.dw0) > 0) 1L else 0L

    if (xBitLen <= 128) {
        val d1 = x.dw1 - y.dw1 - carry0
        z.c256Set128(d1, d0)
        return
    }
    val d1a = x.dw1 - y.dw1
    val carry1a = if (unsignedCmp(d1a, x.dw1) > 0) 1L else 0L
    val d1 = d1a - carry0
    val carry1 = if (unsignedCmp(d1, d1a) > 0) 1L else carry1a

    if (xBitLen <= 192) {
        val d2 = x.dw2 - y.dw2 - carry1
        z.c256Set192(d2, d1, d0)
        return
    }
    val d2a = x.dw2 - y.dw2
    val carry2a = if (unsignedCmp(d2a, x.dw2) > 0) 1L else 0L
    val d2 = d2a - carry1
    val carry2 = if (unsignedCmp(d2, d2a) > 0) 1L else carry2a

    val d3a = x.dw3 - y.dw3
    val carry3a = if (unsignedCmp(d3a, x.dw3) > 0) 1L else 0L
    val d3 = d3a - carry2
    val carry3 = if (unsignedCmp(d3, d3a) > 0) 1L else carry3a
    verify { carry3 == 0L }

    z.c256Set256(d3, d2, d1, d0)
}

internal fun c256SetSubScaled(z: C256, x: C256, scaleDelta: Int, y: C256) {
    verify { scaleDelta > 0 }
    verify { scaleDelta <= 40 }
    verify { x.digitLen + scaleDelta <= 77 }

    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { z.c256HasValidLengths() }

    verify { y.c256ScaledCompareTo(x, scaleDelta) <= 0 }

    c256FmsPow10(z, x, scaleDelta, y)
}

internal fun c256SetSubScaled(z: C256, x: C256, y: C256, scaleDelta: Int) {
    verify { !x.c256IsZero() }
    verify { !y.c256IsZero() }
    verify { scaleDelta > 0 }
    verify { scaleDelta < 34 }

    //check((x.dw3 or x.dw2) == 0L) allow longer because of FMA
    //check((y.dw3 or y.dw2) == 0L)
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { z.c256HasValidLengths() }

    verify { x.c256ScaledCompareTo(y, scaleDelta) >= 0 }

    c256FmsPow10(z, x, y, scaleDelta)
}
