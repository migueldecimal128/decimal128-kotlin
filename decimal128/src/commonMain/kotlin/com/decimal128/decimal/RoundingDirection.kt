package com.decimal128.decimal

expect value class RoundingDirection private constructor(val value:Int) {
    companion object {
        val TIES_TO_EVEN: RoundingDirection
        val TIES_TO_AWAY: RoundingDirection
        val TOWARD_ZERO: RoundingDirection
        val TOWARD_POSITIVE: RoundingDirection
        val TOWARD_NEGATIVE: RoundingDirection

        fun fromValue(value: Int): RoundingDirection
    }

}

/**
 * Returns the rounding direction to apply when operating on the magnitude
 * of a signed value with this rounding direction.
 *
 * For sign-magnitude representations, arithmetic is performed on the
 * unsigned magnitude with the sign reattached at the end. This requires
 * mapping the original (signed-space) rounding direction to an equivalent
 * direction in magnitude space:
 *
 * | Direction          | Positive sign | Negative sign     |
 * |--------------------|---------------|-------------------|
 * | `TIES_TO_EVEN`     | unchanged     | unchanged         |
 * | `TIES_TO_AWAY`     | unchanged     | unchanged         |
 * | `TOWARD_ZERO`      | unchanged     | unchanged         |
 * | `TOWARD_POSITIVE`  | unchanged     | `TOWARD_NEGATIVE` |
 * | `TOWARD_NEGATIVE`  | unchanged     | `TOWARD_POSITIVE` |
 *
 * Rounding toward +∞ on a negative value moves toward zero in magnitude
 * space — equivalent to rounding toward −∞ on the magnitude. The two
 * directional rules swap; the three sign-symmetric rules are unchanged.
 *
 * ## Encoding
 *
 * Branchless via a packed lookup. The map holds 4-bit nybbles indexed by
 * `(sign ? 8 : 0) or value`:
 *
 *   Positive sign (indices 0..4):  identity map (low 32 bits)
 *   Negative sign (indices 8..12): swap of 3 and 4 (high 32 bits)
 *
 * @param sign `true` if the operand is negative, `false` if positive.
 */
internal fun RoundingDirection.forMagnitude(sign: Boolean): RoundingDirection {
    val magnitudeMap: Long = 0x00034210_00043210L
    val index = (if (sign) 8 else 0) or value
    val shift = index shl 2  // index * 4
    val result = ((magnitudeMap shr shift) and 0xFL).toInt()
    return RoundingDirection.fromValue(result)
}

/**
 * Whether overflow under this rounding direction produces infinity (vs. the
 * largest representable finite value).
 *
 * | Rounding direction       | Result   |
 * |--------------------------|----------|
 * | `ROUND_TIES_TO_EVEN`     | `true`   |
 * | `ROUND_TIES_TO_AWAY`     | `true`   |
 * | `ROUND_TOWARD_ZERO`      | `false`  |
 * | `ROUND_TOWARD_POSITIVE`  | `!sign`  |
 * | `ROUND_TOWARD_NEGATIVE`  | `sign`   |
 *
 * ## Encoding
 *
 * Branchless via a packed lookup. Each rounding mode is assigned a 4-bit
 * nibble; within a nibble:
 *
 * ```
 *   bit 0 = answer when sign = false
 *   bit 1 = answer when sign = true
 *   bits 2, 3 = unused (zero)
 * ```
 *
 * Per-mode nibble values:
 *
 * | Mode (value)              | sign=false | sign=true | nibble |
 * |---------------------------|------------|-----------|--------|
 * | `TIES_TO_EVEN`     (0)    | 1          | 1         | `0x3`  |
 * | `TIES_TO_AWAY`     (1)    | 1          | 1         | `0x3`  |
 * | `TOWARD_ZERO`      (2)    | 0          | 0         | `0x0`  |
 * | `TOWARD_POSITIVE`  (3)    | 1          | 0         | `0x1`  |
 * | `TOWARD_NEGATIVE`  (4)    | 0          | 1         | `0x2`  |
 *
 * The nibbles are packed into [table] with mode 0 occupying the lowest
 * nibble (bits 0..3), mode 1 the next (bits 4..7), and so on:
 *
 * ```
 *   value:    4     3     2     1     0
 *   nibble:  0x2   0x1   0x0   0x3   0x3
 *   bits:   16-19 12-15  8-11  4-7   0-3
 *
 *   table = 0x21033
 * ```
 *
 * Lookup is `(table >>> (value * 4 + signBit)) & 1`: the shift selects
 * the correct nibble's correct bit in one step. Returns `false` for any
 * [value] outside `0..4` (the corresponding bits in [table] are zero).
 */
internal fun RoundingDirection.overflowsToInfinity(sign: Boolean): Boolean {
    val table: Int = 0x21033
    val signBit = if (sign) 1 else 0
    return ((table ushr (this.value * 4 + signBit)) and 1) != 0
}

/**
 * Whether underflow under this rounding direction collapses to zero (vs. the
 * smallest representable subnormal magnitude).
 *
 * | Rounding direction       | Result   |
 * |--------------------------|----------|
 * | `ROUND_TIES_TO_EVEN`     | `true`   |
 * | `ROUND_TIES_TO_AWAY`     | `true`   |
 * | `ROUND_TOWARD_ZERO`      | `true`   |
 * | `ROUND_TOWARD_POSITIVE`  | `sign`   |
 * | `ROUND_TOWARD_NEGATIVE`  | `!sign`  |
 *
 * ## Encoding
 *
 * Branchless via a packed lookup. Each rounding mode is assigned a 4-bit
 * nibble; within a nibble:
 *
 * ```
 *   bit 0 = answer when sign = false
 *   bit 1 = answer when sign = true
 *   bits 2, 3 = unused (zero)
 * ```
 *
 * Per-mode nibble values:
 *
 * | Mode (value)              | sign=false | sign=true | nibble |
 * |---------------------------|------------|-----------|--------|
 * | `TIES_TO_EVEN`     (0)    | 1          | 1         | `0x3`  |
 * | `TIES_TO_AWAY`     (1)    | 1          | 1         | `0x3`  |
 * | `TOWARD_ZERO`      (2)    | 1          | 1         | `0x3`  |
 * | `TOWARD_POSITIVE`  (3)    | 0          | 1         | `0x2`  |
 * | `TOWARD_NEGATIVE`  (4)    | 1          | 0         | `0x1`  |
 *
 * The nibbles are packed into [table] with mode 0 occupying the lowest
 * nibble (bits 0..3), mode 1 the next (bits 4..7), and so on:
 *
 * ```
 *   value:    4     3     2     1     0
 *   nibble:  0x1   0x2   0x3   0x3   0x3
 *   bits:   16-19 12-15  8-11  4-7   0-3
 *
 *   table = 0x12333
 * ```
 *
 * Lookup is `(table >>> (value * 4 + signBit)) & 1`: the shift selects
 * the correct nibble's correct bit in one step. Returns `false` for any
 * [value] outside `0..4` (the corresponding bits in [table] are zero).
 */
internal fun RoundingDirection.underflowsToZero(sign: Boolean): Boolean {
    val table: Int = 0x12333
    val signBit = if (sign) 1 else 0
    return ((table ushr (this.value * 4 + signBit)) and 1) != 0
}

private val TO_STRING_MAP = arrayOf("ROUND_TIES_TO_EVEN", "ROUND_TIES_TO_AWAY",
    "ROUND_TOWARD_ZERO", "ROUND_TOWARD_POSITIVE", "ROUND_TOWARD_NEGATIVE")

internal fun RoundingDirection.toDebugString(): String = TO_STRING_MAP[value]
