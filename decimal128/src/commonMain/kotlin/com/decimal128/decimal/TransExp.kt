package com.decimal128.decimal

// TransExp.kt

private val LN10 = MutDec().set("2.302585092994045684017991454684364207601")
private val NEG_LN10 = MutDec().setNegate(LN10)

/*
internal fun expImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val decTmps = ctx.decTmps
    val quot = decTmps.mdecTrans1
    val rem  = decTmps.mdecTrans2

    // ── Range reduction ──────────────────────────────────────────────────────
    // quot = nint(x / LN10),  rem = x - quot * LN10  via FMS
    quot.setDiv(x, LN10, ctx)
    quot.setRoundToIntegralExact(quot, ctx)
    rem.setFma(x, quot, NEG_LN10, ctx)

    // Remember sign of rem, work with |rem|
    val negRem = rem.sign
    rem.sign = false

    // ── Compute R = number of halvings needed ────────────────────────────────
    // R = bitLen( absRem.coefficient << 8 / 10^38 )
    val t = decTmps.mdecTrans3
    val remShifted = decTmps.mdecTrans4
    c256SetShiftLeft(remShifted, rem, 8)
    c256SetDivPow10(t, remShifted, 38)
    val R = t.bitLen

    // ── divide rem 2**R nearest ───────────────────────────────────────────────────
    if (R > 0) {
        val residue = calcLoBitsResidue(rem, R)
        c256SetShiftRight(rem, rem, R)
        rem.roundAndFinalize(residue, ctx)
    }

    // ── Taylor series for expm1(rem), Horner's method, T=16 terms ─────────
    // seed: y = zSmall / T
    val T = 16
    val y = ctx.decTmps.mdecExpY
    val tDec = ctx.decTmps.mdecExpT
    tDec.setInt(T.toLong())
    y.setDiv(rem, tDec, ctx)

    // Horner loop: y = zSmall * (1 + y) / i  for i = T-1 downTo 1
    for (i in (T - 1) downTo 2) {
        // 1 + y: fast coefficient addition
        c256AddPow10(y, -y.qExp)
        // y = zSmall * (1+y) / i  via FMD
        val iDec = ctx.decTmps.mdecExpI
        iDec.setInt(i.toLong())
        y.setFmdFnzFnzFnz(zSmall, y, iDec, ctx)
    }
    // i = 1: y = zSmall * (1 + y) / 1 = zSmall * (1 + y)
    c256AddPow10(y, -y.qExp)
    y.setMul(zSmall, y, ctx)
    // y = expm1(zSmall)

    // ── Square back up R times ───────────────────────────────────────────────
    // expm1(2z) = expm1(z) * (expm1(z) + 2)
    val two = ctx.decTmps.mdecExpTwo
    two.setInt(2)
    val tmp2 = ctx.decTmps.mdecExpTmp
    for (k in 0 until R) {
        c256AddPow10(y, -y.qExp)  // y + 1  ... wait, we need y + 2 not 1 + y
        // FIXME
    }

    // ── result = 1 + expm1(rem) ──────────────────────────────────────────────
    c256AddPow10(y, -y.qExp)      // 1 + y = exp(absRem)

    // ── If rem was negative, take reciprocal ─────────────────────────────────
    if (negRem) z.setReciprocal(y, ctx)
    else z.set(y)

    // ── Reattach powers-of-10 factor ─────────────────────────────────────────
    z.qExp += quot.toIntExact()

    return z
}


 */
private fun calcLoBitsResidue(x: MutDec, bitWidth: Int): Residue {
    verify { bitWidth > 0 && bitWidth <= 32 }
    val dw0 = x.dw0
    val bitWidthMinus1 = bitWidth - 1
    val roundBitMask = 1L shl bitWidthMinus1
    val isolatedRoundBit = dw0 and roundBitMask
    val stickyBits = dw0 and (roundBitMask - 1L)
    return Residue.fromRoundBitStickyBits(isolatedRoundBit, stickyBits)
}
