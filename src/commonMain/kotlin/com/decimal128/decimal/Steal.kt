// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

/*
STEAL == Sign Type Exponent And Lengths
 */

/*
b31: sign
b30-b17: qExp (signed)
b16-b11: digitLen
b10-b2: bitLen
b1-b0: type
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

internal inline fun stealIsFinite(steal: Int): Boolean  = (steal and STEAL_NONFINITE_BIT) == 0
internal inline fun stealNotFinite(steal: Int): Boolean = (steal and STEAL_NONFINITE_BIT) != 0

internal inline fun stealSignFlag(steal: Int): Boolean = steal < 0
internal inline fun stealSignBit(steal: Int): Int = steal ushr 31
internal inline fun stealSignMask(steal: Int): Int = steal shr 31

internal const val STEAL_NAN_MASK      = 0b111
internal const val STEAL_NAN_SNAN      = 0b111
internal const val STEAL_NAN_QNAN      = 0b011

internal inline fun stealIsQNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_QNAN
internal inline fun stealIsSNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_SNAN


private const val STEAL_BITLEN_SHIFT = 16
private const val STEAL_BITLEN_MASK = 0xFF
private const val STEAL_BITLEN_UNSHIFTED_MASK = STEAL_BITLEN_MASK shl STEAL_BITLEN_SHIFT
internal inline fun stealBitLen(steal: Int) =
    (steal ushr STEAL_BITLEN_SHIFT) and STEAL_BITLEN_MASK

private const val STEAL_DIGITLEN_SHIFT = 24
private const val STEAL_DIGITLEN_MASK = 0x7F
private const val STEAL_DIGITLEN_UNSHIFTED_MASK = STEAL_DIGITLEN_MASK shl STEAL_DIGITLEN_SHIFT
internal inline fun stealDigitLen(steal: Int) =
    (steal ushr STEAL_DIGITLEN_SHIFT) and STEAL_DIGITLEN_MASK

internal const val STEAL_PACKED_LENGTHS_MASK = 0x7FFF_0000

private const val STEAL_QEXP_DECODE_SHL = 16
private const val STEAL_QEXP_DECODE_SHR = 18
private const val STEAL_QEXP_ENCODE_SHL = 2
private const val STEAL_QEXP_ENCODE_MASK = 0x3FFF
private const val STEAL_QEXP_MASK_UNSHIFTED = STEAL_QEXP_ENCODE_MASK shl STEAL_QEXP_ENCODE_SHL
internal inline fun stealQExp(steal: Int) =
    (steal shl STEAL_QEXP_DECODE_SHL) shr STEAL_QEXP_DECODE_SHR

internal inline fun stealWithQExp(oldSteal: Int, qExp: Int) =
    (oldSteal and STEAL_QEXP_MASK_UNSHIFTED.inv()) or ((qExp and STEAL_QEXP_ENCODE_MASK) shl STEAL_QEXP_ENCODE_SHL)

internal inline fun stealEexp(steal: Int): Int {
    // eExp = qExp + (digitLen - (-digitLen ushr 31))
    // eExp = qExp + (digitLen - (-bitLen ushr 31))
    return ((steal shl STEAL_QEXP_DECODE_SHL) shr STEAL_QEXP_DECODE_SHR) +
            ((steal ushr STEAL_DIGITLEN_SHIFT) and STEAL_DIGITLEN_MASK) -
            (-(steal and STEAL_BITLEN_UNSHIFTED_MASK) ushr 31)
}

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

internal fun stealEncodeFinite(signBit: Int, qExp: Int, dw1: Long, dw0: Long): Int =
    if ((dw1 or dw0) != 0L)
        stealEncodeFNZ(signBit, qExp, dw1, dw0)
    else
        stealEncodeZER(signBit, qExp)


internal inline fun stealEncodeINF(signBit: Int) =
    (signBit shl 31) or STEAL_TYP_INF

// FIXME - do I allow non-canonical encodings ...
//  ... e.g. link INF + coeff ... at this point I think not

internal inline fun stealEncodeSNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or calcStealPackedLengths128(payloadDw1, payloadDw0) or STEAL_NAN_SNAN

internal inline fun stealEncodeQNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or calcStealPackedLengths128(payloadDw1, payloadDw0) or STEAL_NAN_QNAN

internal fun stealWithSignFlag(oldSteal: Int, signFlag: Boolean) =
    (oldSteal and 0x7FFF_FFFF) or (if (signFlag) Int.MIN_VALUE else 0)

internal fun stealWithTyp(oldSteal: Int, typ: Int) =
    (oldSteal and STEAL_TYP_MASK.inv()) or typ

internal fun stealWithPackedLengths(oldSteal: Int, packedLengths: Int): Int =
    (oldSteal and STEAL_PACKED_LENGTHS_MASK.inv()) or packedLengths

internal fun stealWithDigitLenBitLen(oldSteal: Int, digitLen: Int, bitLen: Int): Int {
    verify { digitLen >= 0 && digitLen <= 76 && bitLen >= 0 && bitLen <= 253 }
    return (oldSteal and STEAL_PACKED_LENGTHS_MASK.inv()) or
            (digitLen shl STEAL_DIGITLEN_SHIFT) or
            (bitLen shl STEAL_BITLEN_SHIFT)
}

internal fun stealPackLengths(digitLen: Int, bitLen: Int): Int {
    verify { digitLen >= 0 && digitLen <= 76 && bitLen >= 0 && bitLen <= 253 }
    return (digitLen shl STEAL_DIGITLEN_SHIFT) or (bitLen shl STEAL_BITLEN_SHIFT)
}