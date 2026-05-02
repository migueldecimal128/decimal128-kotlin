package com.decimal128.decimal

private val TO_STRING_MAP = arrayOf("ROUND_TIES_TO_EVEN", "ROUND_TIES_TO_AWAY",
    "ROUND_TOWARD_ZERO", "ROUND_TOWARD_POSITIVE", "ROUND_TOWARD_NEGATIVE")



@JvmInline
actual value class RoundingDirection private constructor(val value:Int) {
    actual companion object {
        actual val TIES_TO_EVEN = RoundingDirection(0)
        actual val TIES_TO_AWAY = RoundingDirection(1)
        actual val TOWARD_ZERO = RoundingDirection(2)
        actual val TOWARD_POSITIVE = RoundingDirection(3)
        actual val TOWARD_NEGATIVE = RoundingDirection(4)

        actual fun fromValue(value:Int) : RoundingDirection {
            return when (value) {
                0 -> TIES_TO_EVEN
                1 -> TIES_TO_AWAY
                2 -> TOWARD_ZERO
                3 -> TOWARD_POSITIVE
                4 -> TOWARD_NEGATIVE
                else -> throw RuntimeException("value:$value out of range for RoundingDirection")
            }
        }

    }

    // roundTiesToEve, roundTiesToAway, and roundTiesTowardZero are the same independent of sign
    // roundTowardPositive and roundTowardNegative are complementary
    actual fun negated() :RoundingDirection {
        return if (value < 3) this else if (value == 3) TOWARD_NEGATIVE else TOWARD_POSITIVE
    }

    actual fun negated(sign: Boolean) = if (sign) negated() else this

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
    actual fun overflowsToInfinity(sign: Boolean): Boolean {
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
    actual fun underflowsToZero(sign: Boolean): Boolean {
        val table: Int = 0x12333
        val signBit = if (sign) 1 else 0
        return ((table ushr (this.value * 4 + signBit)) and 1) != 0
    }

    override fun toString() = TO_STRING_MAP[value]

}
