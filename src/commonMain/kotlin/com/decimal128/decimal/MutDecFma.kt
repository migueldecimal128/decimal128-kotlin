// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.min

internal fun mutDecFmaImpl(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val aSteal = a.steal
    val signatureXY = binopSignatureOf(xSteal, ySteal)
    if (signatureXY == FNZ_FNZ && stealIsFinite(aSteal))
        return fmaFnzFnzFinite(z, x, y, a, ctx)
    if (stealIsNAN(aSteal))
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
        else -> z.setNanOperandFound(x, y, ctx)
    }
    return z
}

private inline fun fmaFnzFnzFinite(z:MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val aT = if (z === a) ctx.tmps.mdecArg1.set(a) else a
    // multiply without roundAndFinalize .. remains exact
    c256SetMul(z, x, y, ctx.tmps.pentad1)
    verify { stealBitLen(z.steal) != 0 }
    // since finalize is not called yet, we must clamp
    // the exponent to a representable range that does not overflow
    // the 14 bits allocated in the steal.
    // slight overflow/underflow are not a problem.
    // the add/sub will fix it or finalize will take care of it.
    val zQ = clampQExponentRange(stealQExp(xSteal) + stealQExp(ySteal))
    val zSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    z.steal = stealEncodeFNZ(zSign, zQ, stealPackedLengths(z.steal))
    // roundAndFinalize takes place here in z.setAdd
    return mutDecAddImpl(z, z, aT.sign, aT, ctx)
}

private inline fun fmaNanAddend(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val aSteal = a.steal
    verify { stealIsNAN(aSteal) }
    val hasSNAN = stealIsSNAN(xSteal) or stealIsSNAN(ySteal) or stealIsSNAN(aSteal)
    val theNAN =
        if (x.isNaN(hasSNAN)) x
        else if (y.isNaN(hasSNAN)) y
        else a
    verify { theNAN.isNaN(hasSNAN) }
    z.set(theNAN)
    if (z.isSignaling()) {
        z.quietSNaN()
        return ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, z)
    }
    if (theNAN === a) {
        if ((stealIsZER(xSteal) and stealIsINF(ySteal)) or
            (stealIsINF(xSteal) and stealIsZER(ySteal)))
            return ctx.signalInvalid(InvalidOperationReason.MUL_ZERO_BY_INFINITY, z)
    }
    return z
}

private inline fun fmaZeroProd(z: MutDec, x: MutDec, y: MutDec, a: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val aSteal = a.steal
    val aSign = stealSignFlag(aSteal)
    val aQ = stealQExp(aSteal)
    val prodSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val prodQ = stealQExp(xSteal) + stealQExp(ySteal)
    verify { stealIsZER(xSteal) || stealIsZER(ySteal) }
    verify { !stealIsNAN(aSteal) }
    if (stealIsZER(aSteal)) {
        val fmaSign =
            (prodSign and aSign) or ((prodSign xor aSign) and ctx.isRoundTowardNegative())
        return z.setZero(fmaSign, min(prodQ, aQ))
    }
    if (stealIsFNZ(aSteal) && prodQ < aQ)
        return setScaleToMinQexp(z, aSign, a, prodQ, ctx)
    return z.set(a)
}

private inline fun fmaInfProd(z: MutDec, infSign: Boolean, a: MutDec, ctx: DecContext): MutDec {
    val aSteal = a.steal
    verify { !stealIsNAN(aSteal) }
    if (stealIsFinite(aSteal) || stealSignFlag(aSteal) == infSign)
        return z.setInfinite(infSign)
    return ctx.setNanSignalInvalid(z, InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)
}

