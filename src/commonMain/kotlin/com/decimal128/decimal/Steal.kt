// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

/*
STEAL == Sign Type Exponent And Lengths
 */

/*
b31: sign
b30: unused
b29-b16: qExp (signed)
bitLenDelta = digitLen * 4 - bitLen
bitLen = digitLen * 4 - bitLenDelta
b15: isSNaN
b14-b9: bitLenDelta 6 bits
b8-b2: digitLen 7 bits
b1-b0: type 2 bits
b1: isNonZero/isNaN
b0: isNonFinite
 */
private const val STEAL_NONFINITE_BIT = 0x0000_0001
private const val STEAL_IS_ZERO_BIT   = 0x0000_0002

private const val STEAL_TYP_MASK     = 0x0000_0003

internal const val STEAL_TYP_FNZ      = 0x0000_0000
internal const val STEAL_TYP_ZER      = STEAL_IS_ZERO_BIT
internal const val STEAL_TYP_INF      = STEAL_NONFINITE_BIT
internal const val STEAL_TYP_NAN      = STEAL_NONFINITE_BIT or STEAL_IS_ZERO_BIT

internal inline fun stealTyp(steal: Int): Int = steal and 0x03

internal inline fun stealIsFNZ(steal: Int): Boolean = (steal and STEAL_TYP_MASK) == STEAL_TYP_FNZ
internal inline fun stealIsZER(steal: Int): Boolean = (steal and STEAL_TYP_MASK) == STEAL_TYP_ZER
internal inline fun stealIsINF(steal: Int): Boolean = (steal and STEAL_TYP_MASK) == STEAL_TYP_INF
internal inline fun stealIsNAN(steal: Int): Boolean = (steal and STEAL_TYP_MASK) == STEAL_TYP_NAN

internal inline fun stealBothFNZ(stealX: Int, stealY: Int) =
    // below only works because FNZ == 0  //stealIsFNZ(stealX) and stealIsFNZ(stealY)
    stealIsFNZ(stealX or stealY)

internal inline fun stealBothZER(stealX: Int, stealY: Int) =
    stealIsZER(stealX) and stealIsZER(stealY)
internal inline fun stealBothFinite(stealX: Int, stealY: Int) =
    // below only works because it is a NON-FINITE bit // stealIsFinite(stealX) and stealIsFinite(stealY)
    stealIsFinite(stealX or stealY)
internal inline fun stealBothINF(stealX: Int, stealY: Int) =
    stealIsINF(stealX) and stealIsINF(stealY)
internal inline fun stealBothNAN(stealX: Int, stealY: Int) =
    stealIsNAN(stealX) and stealIsNAN(stealY)

internal inline fun stealHasINF(stealX: Int, stealY: Int) =
    stealIsINF(stealX) or stealIsINF(stealY)
internal inline fun stealHasNAN(stealX: Int, stealY: Int) =
    stealIsNAN(stealX) or stealIsNAN(stealY)

internal inline fun stealIsFinite(steal: Int): Boolean = (steal and STEAL_NONFINITE_BIT) == 0
internal inline fun stealNotFinite(steal: Int): Boolean = (steal and STEAL_NONFINITE_BIT) != 0
internal inline fun stealIsNormal(steal: Int): Boolean {
    if (! stealIsFNZ(steal))
        return false
    val qExp = stealQExp(steal)
    if (qExp >= -6143)
        return true
    val digitLen = stealDigitLen(steal)
    val eExp = qExp + digitLen - 1
    return eExp >= -6143
}

internal inline fun stealIsSubnormal(steal: Int): Boolean {
    if (! stealIsFNZ(steal))
        return false
    val qExp = stealQExp(steal)
    if (qExp >= -6143)
        return false
    val digitLen = stealDigitLen(steal)
    val eExp = qExp + digitLen - 1
    return eExp < -6143
}

internal inline fun stealSignFlag(steal: Int): Boolean = steal < 0
internal inline fun stealSignBit(steal: Int): Int = steal ushr 31
internal inline fun stealSignMask(steal: Int): Int = steal shr 31

private const val STEAL_QUIETING_MASK  = 0x0000_8000.inv()
internal const val STEAL_NAN_MASK      = 0x0000_8003
internal const val STEAL_NAN_SNAN      = 0x0000_8003
internal const val STEAL_NAN_QNAN      = 0x0000_0003

internal const val STEAL_SIGNALING_SHL = 15

internal inline fun stealIsQNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_QNAN
internal inline fun stealIsSNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_SNAN


private const val STEAL_DIGITLEN_SHIFT = 2
private const val STEAL_DIGITLEN_MASK = 0x7F
private const val STEAL_DIGITLEN_UNSHIFTED_MASK = STEAL_DIGITLEN_MASK shl STEAL_DIGITLEN_SHIFT
internal inline fun stealDigitLen(steal: Int) =
    (steal ushr STEAL_DIGITLEN_SHIFT) and STEAL_DIGITLEN_MASK

private const val STEAL_BITLEN_CORRECTION_SHIFT = 9
private const val STEAL_BITLEN_CORRECTION_MASK = 0x3F
internal inline fun stealBitLen(steal: Int): Int {
    //(steal and STEAL_DIGITLEN_UNSHIFTED_MASK) - ((steal shr STEAL_BITLEN_CORRECTION_SHIFT) and STEAL_BITLEN_CORRECTION_MASK)
    val digitLen = stealDigitLen(steal)
    val stealUnshifted = steal and STEAL_DIGITLEN_UNSHIFTED_MASK
    val correction = ((steal shr STEAL_BITLEN_CORRECTION_SHIFT) and STEAL_BITLEN_CORRECTION_MASK)
    val bitLen = stealUnshifted - correction
    return bitLen
}


internal const val STEAL_PACKED_LENGTHS_MASK = 0x0000_7FFC

private const val CLAMPED_EXP_MIN = -7000
private const val CLAMPED_EXP_MAX = 7000

private const val STEAL_QEXP_DECODE_SHL = 2
private const val STEAL_QEXP_DECODE_SHR = 18
private const val STEAL_QEXP_ENCODE_SHL = 16
private const val STEAL_QEXP_ENCODE_MASK = 0x3FFF
private const val STEAL_QEXP_MASK_UNSHIFTED = STEAL_QEXP_ENCODE_MASK shl STEAL_QEXP_ENCODE_SHL
internal inline fun stealQExp(steal: Int) =
    (steal shl STEAL_QEXP_DECODE_SHL) shr STEAL_QEXP_DECODE_SHR

internal inline fun stealWithQExp(oldSteal: Int, qExp: Int) =
    (oldSteal and STEAL_QEXP_MASK_UNSHIFTED.inv()) or ((qExp and STEAL_QEXP_ENCODE_MASK) shl STEAL_QEXP_ENCODE_SHL)

internal inline fun stealSciExp(steal: Int): Int {
    // if the digitLen is non-zero then subtract 1
    // if digitLen == 0 then sciExp stays 0 ... 0e0
    // eExp = qExp + (digitLen - (-digitLen ushr 31))
    // eExp = qExp + (digitLen - (-bitLen ushr 31))
    return ((steal shl STEAL_QEXP_DECODE_SHL) shr STEAL_QEXP_DECODE_SHR) +
            ((steal ushr STEAL_DIGITLEN_SHIFT) and STEAL_DIGITLEN_MASK) -
            (-(steal and STEAL_DIGITLEN_UNSHIFTED_MASK) ushr 31)
}

internal fun stealBExpMin(steal: Int): Int =
    calcBExpMin(stealBitLen(steal), stealQExp(steal))

internal fun stealBExpMax(steal: Int): Int =
    calcBExpMax(stealBitLen(steal), stealQExp(steal))

internal inline fun stealEncodeZER(sign: Boolean, qExp: Int): Int =
    stealEncodeZER(if (sign) 1 else 0, qExp)

internal inline fun stealEncodeZER(signBit: Int, qExp: Int): Int {
    verify { qExp >= Q_TINY && qExp <= Q_MAX }
    return (signBit shl 31) or
            ((qExp and STEAL_QEXP_ENCODE_MASK) shl STEAL_QEXP_ENCODE_SHL) or
            STEAL_TYP_ZER
}

internal inline fun stealEncodeFNZ(signBit: Int, qExp: Int, dw1: Long, dw0: Long): Int {
    verify { qExp >= Q_TINY && qExp <= Q_MAX }
    verify { (dw1 or dw0) != 0L }
    return (signBit shl 31) or
            ((qExp and STEAL_QEXP_ENCODE_MASK) shl STEAL_QEXP_ENCODE_SHL) or
            calcStealPackedLengths128(dw1, dw0) or
            STEAL_TYP_FNZ
}

internal inline fun stealEncodeFNZ(sign: Boolean, qExp: Int, packedLengths: Int) =
    stealEncodeFNZ(if (sign) 1 else 0, qExp, packedLengths)

internal inline fun stealEncodeFNZ(signBit: Int, qExp: Int, packedLengths: Int): Int {
    verify { qExp >= -8192 && qExp <= 8191 }
    verify { packedLengths != 0 }
    return (signBit shl 31) or
            ((qExp and STEAL_QEXP_ENCODE_MASK) shl STEAL_QEXP_ENCODE_SHL) or
            packedLengths or
            STEAL_TYP_FNZ
}

internal fun stealEncodeFinite(signBit: Int, qExp: Int, dw1: Long, dw0: Long): Int =
    if ((dw1 or dw0) != 0L)
        stealEncodeFNZ(signBit, qExp, dw1, dw0)
    else
        stealEncodeZER(signBit, qExp)


internal inline fun stealEncodeINF(signBit: Int) =
    (signBit shl 31) or STEAL_TYP_INF

// FIXME - do I allow non-canonical encodings ...
//  ... e.g. link INF + coeff ... at this point I think not

internal inline fun stealEncodeNAN(signBit: Int, signalingBit: Int, payloadDw1: Long, payloadDw0: Long) =
    (signBit shl 31) or
            calcStealPackedLengths128(payloadDw1, payloadDw0) or
            (signalingBit shl STEAL_SIGNALING_SHL) or STEAL_TYP_NAN

internal inline fun stealEncodeSNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or calcStealPackedLengths128(payloadDw1, payloadDw0) or STEAL_NAN_SNAN

internal inline fun stealEncodeQNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or calcStealPackedLengths128(payloadDw1, payloadDw0) or STEAL_NAN_QNAN

internal inline fun stealWithSignFlag(oldSteal: Int, signFlag: Boolean) =
    (oldSteal and 0x7FFF_FFFF) or (if (signFlag) Int.MIN_VALUE else 0)

internal inline fun stealWithNegation(oldSteal: Int) = oldSteal xor Int.MIN_VALUE

internal inline fun stealWithAbsValue(oldSteal: Int) = oldSteal and 0x7FFF_FFFF

internal fun stealWithTyp(oldSteal: Int, typ: Int) =
    (oldSteal and STEAL_TYP_MASK.inv()) or typ

internal fun stealWithQuietedSNAN(oldSteal: Int): Int =
    (oldSteal and STEAL_QUIETING_MASK)

internal fun stealWithPackedLengths(oldSteal: Int, packedLengths: Int): Int =
    (oldSteal and STEAL_PACKED_LENGTHS_MASK.inv()) or packedLengths

internal fun stealWithDigitLenBitLen(oldSteal: Int, digitLen: Int, bitLen: Int): Int {
    verify { digitLen >= 0 && digitLen <= 77 && bitLen >= 0 && bitLen <= 256 ||
    digitLen == 100 }
    val digitLenShifted = digitLen shl 2
    return (oldSteal and STEAL_PACKED_LENGTHS_MASK.inv()) or
            digitLenShifted or
            ((digitLenShifted - bitLen) shl STEAL_BITLEN_CORRECTION_SHIFT)
}

internal const val PACKED_LENGTHS_1_1 = (1 shl STEAL_DIGITLEN_SHIFT) or (3 shl STEAL_BITLEN_CORRECTION_SHIFT)

internal fun stealPackedLengths(steal: Int): Int =
    steal and STEAL_PACKED_LENGTHS_MASK

internal fun stealPackLengths(digitLen: Int, bitLen: Int): Int {
    verify { digitLen >= 0 && digitLen <= 77 && bitLen >= 0 && bitLen <= 256 }
    val digitLenShifted = digitLen shl 2
    return digitLenShifted or
            ((digitLenShifted - bitLen) shl STEAL_BITLEN_CORRECTION_SHIFT)

}

internal inline fun clampQExponentRange(q: Int): Int {
    return min(max(q, CLAMPED_EXP_MIN), CLAMPED_EXP_MAX)
}

internal fun stealCompareMagnitudeFnzFnz(xSteal: Int, ySteal: Int): Int {
    verify { binopSignatureOf(xSteal, ySteal) == FNZ_FNZ }
    val xE = stealSciExp(xSteal)
    val yE = stealSciExp(ySteal)
    if (xE != yE)
        return ((xE - yE) shr 31) or 1
    if (stealBExpMin(xSteal) > stealBExpMax(ySteal))
        return 1
    if (stealBExpMax(xSteal) < stealBExpMin(ySteal))
        return -1
    return 0
}
