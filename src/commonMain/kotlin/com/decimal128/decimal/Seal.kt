@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

/**
 * Compact **S-E-A-L** encoding for a Decimal128 value.
 *
 * A `Seal` packs the **Sign**, **q-Exponent**, **Lengths**
 * (digit length and bit length) of the coefficient
 * into a single 32-bit word. This keeps size at `4 + 8 + 8 = 20`
 * bytes + a 12 byte header = 32 bytes.
 *
 * ### Layout (high → low):
 * ```
 * 31      Sign bit     (1 = negative)
 * 30..16  qExp         (15-bit signed, masked to 0x7FFF)
 * 15..9   digitLen     (0–38)
 * 8..0    bitLen       (0–127)
 * ```
 *
 * Construction checks enforce all invariants, including:
 *  * `signBit ∈ {0,1}`
 *  * `qExp ∈ [-6176, 6111]` or in the non-finite range `NON_FINITE_INF .. NON_FINITE_SNAN`
 *  * `digitLen ∈ [0, 38]`  (allows Decimal128-Extended precision)
 *  * `bitLen ∈ [0, 127]`
 *
 * Several overloads compute `bitLen` and `digitLen` automatically from
 * a 128-bit coefficient `(dw1, dw0)`.
 *
 * ### Derived fields
 *  * `isNegative` - sign as a boolean
 *  * `signBit`    – raw sign bit (0/1)
 *  * `signMask`   – arithmetic sign mask (`0` or `-1`)
 *  * `qExp`       – stored q-exponent
 *  * `eExp`       – normalized scientific exponent
 *  * `digitLen`   – number of base-10 digits in the coefficient
 *  * `bitLen`     – coefficient bit length
  *
 * ### Operations
 *  * `negate()` returns a `Seal` with the sign bit flipped.
 *
 * This class is a `value class`; no heap allocation occurs at call sites.
 */
@JvmInline
value class Seal private constructor(val seal: Int) {
    companion object {

        /** Builds a S-E-A-L word after validating all fields and packing them into 32 bits. */
        internal operator fun invoke(signBit: Int, qExp: Int, digitLen: Int, bitLen: Int): Seal {
            verify { signBit in 0..1 }
            verify { qExp in -6176..6111 || qExp in NON_FINITE_INF..NON_FINITE_SNAN }
            // allow 38 digits for DECIMAL128_EXTENDED precision
            verify { digitLen in 0..39 }
            verify { bitLen in 0..128 }
            return Seal(
                (signBit shl 31) or
                        ((qExp and 0x7FFF) shl 16) or
                        (digitLen shl 9) or
                        bitLen
            )
        }

        /** Boolean-sign overload delegating to the raw `signBit` constructor. */
        internal operator fun invoke(sign: Boolean, qExp: Int, digitLen: Int, bitLen: Int): Seal =
            Seal(if (sign) 1 else 0, qExp, digitLen, bitLen)

        /** Boolean-sign overload delegating to the raw `signBit` constructor. */
        internal operator fun invoke(signExp: Short, packedLengths: Short): Seal {
            val sign = signExp.toInt() ushr 31
            val qExp = signExp.toInt() and 0x7FFF
            val digitLen = packedLengths.toInt() shr 9
            val bitLen = packedLengths.toInt() and 0x1FF
            return Seal(sign, qExp, digitLen, bitLen)
        }

        /** Computes `bitLen` and `digitLen` from the
         * 128-bit coefficient and builds the `Seal`. */
        internal operator fun invoke(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong): Seal {
            val bitLen = calcBitLen128(dw1, dw0)
            val digitLen = calcDigitLen128(bitLen, dw1, dw0)
            return Seal(if (sign) 1 else 0, qExp, digitLen, bitLen)
        }

        /** Raw-sign overload computing lengths from the 128-bit coefficient. */
        internal operator fun invoke(signBit: Int, qExp: Int, dw1: ULong, dw0: ULong): Seal {
            val bitLen = calcBitLen128(dw1, dw0)
            val digitLen = calcDigitLen128(bitLen, dw1, dw0)
            return Seal(signBit, qExp, digitLen, bitLen)
        }

    }
    internal val bitLen: Int
        get() = seal and 0x1FF
    internal val digitLen: Int
        get() = (seal shr 9) and 0x7F

    internal val sign: Boolean
        get() = seal < 0
    internal val signBit: Int
        get() = seal ushr 31
    internal val signMask: Int
        get() = seal shr 31

    fun updateDigitLenBitLen(digitLen: Int, bitLen: Int): Seal =
        Seal((this.seal and 0xFFFF_0000.toInt()) or ((digitLen and 0x7F) shl 9) or (bitLen and 0x1FF))

    fun updateSignExp(signBit: Int, exp: Int): Seal =
        Seal((signBit shl 31) or ((exp and 0x7FFF) shl 16) or (this.seal and 0xFFFF))

    fun updateSignExp(sign: Boolean, exp: Int): Seal =
        Seal((if (sign) Int.MIN_VALUE else 0) or ((exp and 0x7FFF) shl 16) or (this.seal and 0xFFFF))

    /**
     * The unbiased **q-exponent** extracted from the SEAL word.
     *
     * The q-exponent is the power of 10 applied to the coefficient:
     * the represented value is `coeff * 10**qExp`.
     */
    internal val qExp: Int
        get() = (seal shl 1) shr 17

    /**
     * Normalized scientific exponent:
     * `qExp + (digitLen - 1)` for non-zero coefficients, otherwise just `qExp`.
     *
     * Implemented branchlessly via `-bitLen ushr 31`, which is 0 when `bitLen == 0`
     * and 1 when `bitLen != 0`.
     */
    internal val eExp: Int
        get() = qExp + (digitLen - (-bitLen ushr 31))

    fun negate() = Seal(seal xor Int.MIN_VALUE)

    fun abs() = Seal(seal and Int.MAX_VALUE)

}