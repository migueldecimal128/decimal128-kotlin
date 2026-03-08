// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_/*inline*/")

package com.decimal128.decimal

/*
STEAL == Sign Type Exponent And Lengths
 */

/*
b31: sign
b30: isNonFinite
b29: isNonZero/isNaN
b28-b15: qExp (signed)
b14-b10: digitLenCorrection
b9:b0: bitLen
 */
internal const val STEAL_SIGN = Int.MIN_VALUE
internal const val STEAL_NONFINITE_BIT = 0x4000_0000
internal const val STEAL_NONZERO_BIT   = 0x2000_0000

internal const val STEAL_TYPE_MASK     = 0x6000_0000

internal const val STEAL_TYPE_ZER      = 0x0000_0000
internal const val STEAL_TYPE_FNZ      = STEAL_NONZERO_BIT
internal const val STEAL_TYPE_INF      = STEAL_NONFINITE_BIT
internal const val STEAL_TYPE_NAN      = STEAL_NONFINITE_BIT or STEAL_NONZERO_BIT

internal /*inline*/ fun stealIsZER(steal: Int): Boolean = (steal and STEAL_TYPE_MASK) == STEAL_TYPE_ZER
internal /*inline*/ fun stealIsFNZ(steal: Int): Boolean = (steal and STEAL_TYPE_MASK) == STEAL_TYPE_FNZ
internal /*inline*/ fun stealIsINF(steal: Int): Boolean = (steal and STEAL_TYPE_MASK) == STEAL_TYPE_INF
internal /*inline*/ fun stealIsNAN(steal: Int): Boolean = (steal and STEAL_TYPE_MASK) == STEAL_TYPE_NAN

internal /*inline*/ fun stealBothFNZ(stealX: Int, stealY: Int) =
    stealIsFNZ(stealX) and stealIsFNZ(stealY)

internal /*inline*/ fun stealType(steal: Int): Int = (steal shr 29) and 0x03

internal /*inline*/ fun stealIsFinite(steal: Int): Boolean  = (steal and STEAL_NONFINITE_BIT) == 0
internal /*inline*/ fun stealNotFinite(steal: Int): Boolean = (steal and STEAL_NONFINITE_BIT) != 0

internal const val STEAL_QNAN_BIT      = 0x1000_0000
internal const val STEAL_NAN_MASK      = 0x7000_0000
internal const val STEAL_NAN_SNAN      = 0x6000_0000
internal const val STEAL_NAN_QNAN      = 0x7000_0000

internal /*inline*/ fun stealIsQNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_QNAN
internal /*inline*/ fun stealIsSNAN(steal: Int): Boolean = (steal and STEAL_NAN_MASK) == STEAL_NAN_SNAN


internal const val STEAL_BITLEN_MASK = 0x1FF
internal /*inline*/ fun stealBitLen(steal: Int) = steal and STEAL_BITLEN_MASK

internal const val STEAL_DIGITLEN_SHIFT = 9
internal const val STEAL_DIGITLEN_MASK = 0x3F
internal /*inline*/ fun stealDigitLen(steal: Int) =
    (steal ushr STEAL_DIGITLEN_SHIFT) and STEAL_DIGITLEN_MASK

internal const val STEAL_QEXP_SHL = 3
internal const val STEAL_QEXP_SHR = 18
internal const val STEAL_QEXP_MASK = 0x3FFF
internal /*inline*/ fun stealQexp(steal: Int) =
    ((steal shl STEAL_QEXP_SHL) shr STEAL_QEXP_SHR) and STEAL_QEXP_MASK


internal /*inline*/ fun stealEncodeZER(signBit: Int, qExp: Int) =
    (signBit shl 31) or ((qExp shl STEAL_QEXP_SHR) ushr STEAL_QEXP_SHL)

internal /*inline*/ fun stealEncodeFNZ(signBit: Int, qExp: Int, dw1: Long, dw0: Long) =
    (signBit shl 31) or STEAL_TYPE_FNZ or
            ((qExp shl STEAL_QEXP_SHR) ushr STEAL_QEXP_SHL) or
            calcPackedLengths128(dw1, dw0)

internal /*inline*/ fun stealEncodeINF(signBit: Int) =
    (signBit shl 31) or STEAL_TYPE_INF

// FIXME - do I allow non-canonical encodings ...
//  ... e.g. link INF + coeff ... at this point I think not

internal /*inline*/ fun stealEncodeSNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or STEAL_NAN_SNAN or calcPackedLengths128(payloadDw1, payloadDw0)

internal /*inline*/ fun stealEncodeQNAN(signBit: Int, payloadDw1: Long, payloadDw0: Long): Int =
    (signBit shl 31) or STEAL_NAN_QNAN or calcPackedLengths128(payloadDw1, payloadDw0)

internal fun stealRaw(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): Int {
    val signBit = if (sign) 1 else 0
    return when {
        qExp < NON_FINITE_INF   ->
            if ((dw1 or dw0) == 0L) stealEncodeZER(signBit, qExp)
            else stealEncodeFNZ(signBit, qExp, dw1, dw0)
        qExp == NON_FINITE_INF  -> stealEncodeINF(signBit)
        qExp == NON_FINITE_SNAN -> stealEncodeSNAN(signBit, dw1, dw0)
        qExp == NON_FINITE_QNAN -> stealEncodeQNAN(signBit, dw1, dw0)
        else -> throw IllegalArgumentException()
    }
}