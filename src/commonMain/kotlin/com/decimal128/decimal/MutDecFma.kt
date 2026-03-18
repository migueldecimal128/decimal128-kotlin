package com.decimal128.decimal

import com.decimal128.decimal.MagnitudeDiv.magDivFnzFnz
import kotlin.math.max
import kotlin.math.min

internal fun mutDecFmaImpl(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val signatureXY = binopSignatureOf(x.type, y.type)
    if (signatureXY == FNZ_FNZ && a.isFinite())
        return fmaFnzFnzFinite(z, x, y, a, ctx)
    if (a.isNaN())
        return fmaNanAddend(z, x, y, a, ctx)
    when (signatureXY) {
        ZER_FNZ,
        FNZ_ZER,
        ZER_ZER -> fmaZeroProd(z, x, y, a, ctx)
        ZER_INF,
        INF_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.MUL_ZERO_BY_INFINITY)
        FNZ_FNZ -> {
            verify { a.isInfinite() }
            z.setInfinite(a.sign)
        }
        FNZ_INF,
        INF_FNZ,
        INF_INF -> fmaInfProd(z, x.sign xor y.sign, a, ctx)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}

private fun fmaFnzFnzFinite(z:MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val aT = if (z === a) ctx.tmps.mdecArg1.set(a) else a
    // multiply without roundAndFinalize .. remains exact
    c256SetMul(z, x, y, ctx.tmps.pentad1)
    verify { z.bitLen != 0 }
    z.type = STEAL_TYPE_FNZ
    z.qExp = x.qExp + y.qExp
    z.sign = x.sign xor y.sign
    // roundAndFinalize takes place here in z.setAdd
    return mutDecAddImpl(z, z, aT.sign, aT, ctx)
}

private fun fmaNanAddend(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val stealX = x.type
    val stealY = y.type
    val stealA = a.type
    verify { stealIsNAN(stealA) }
    val hasSNAN = stealIsSNAN(stealX) or stealIsSNAN(stealY) or stealIsSNAN(stealA)
    val targetNAN = if (hasSNAN) STEAL_NAN_SNAN else STEAL_NAN_QNAN
    val theNAN =
        if ((stealX and STEAL_NAN_MASK) == targetNAN) x
        else if ((stealY and STEAL_NAN_MASK) == targetNAN) y
        else a
    z.set(theNAN)
    if (!hasSNAN) {
        if ((stealIsZER(stealX) and stealIsINF(stealY)) or (stealIsINF(stealX) and stealIsZER(stealY)))
            return ctx.signalInvalid(InvalidOperationReason.MUL_ZERO_BY_INFINITY, z)
        return z
    }
    z.quietSNaN()
    return ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, z)
}

private fun fmaZeroProd(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val stealX = x.type
    val stealY = y.type
    val stealA = a.type
    verify { stealIsZER(stealX) || stealIsZER(stealY) }
    verify { !stealIsNAN(stealA) }
    val prodSign = x.sign xor y.sign
    val prodQ = x.qExp + y.qExp
    val aQ = a.qExp
    if (stealIsZER(stealA)) {
        val fmaSign =
            (prodSign and a.sign) or ((prodSign xor a.sign) and ctx.isRoundTowardNegative())
        return z.setZero(fmaSign, min(prodQ, aQ), ctx)
    }
    if (stealIsFNZ(stealA) && prodQ < aQ)
        return setScaleToMinQexp(z, a.sign, a, prodQ, ctx)
    return z.set(a)
}

private fun fmaInfProd(z: MutDec, infSign: Boolean, a: MutDec, ctx: DecContext): MutDec {
    verify { !a.isNaN() }
    if (a.isFinite() || a.sign == infSign)
        return z.setInfinite(infSign)
    return ctx.setNanSignalInvalid(z, InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)
}


internal fun mutDecFmdFnzFnzFnz(z:MutDec, x: MutDec, y: MutDec, d: MutDec, ctx: DecContext): MutDec {
    verify { max(max(x.qExp, y.qExp), d.qExp) < MIN_SPECIAL_VALUE &&
            (x.digitLen * y.digitLen * d.digitLen) != 0 }
    val resultSign = x.sign xor y.sign xor d.sign

    val pT = ctx.tmps.mdecFusedProduct

    // raw multiply without roundAndFinalize ... remains exact
    c256SetMul(pT, x, y, ctx.tmps.pentad1)
    pT.qExp = x.qExp + y.qExp
    val residue = magDivFnzFnz(z, resultSign, pT, d, ctx)
    return z.roundAndFinalize(residue, ctx)
}