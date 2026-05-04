package com.decimal128.decimal

/**
 * Preferences controlling parse behavior — converting text to [Decimal128].
 *
 * Defaults are lenient: parsing accepts any valid input form, including digit-separator
 * underscores (e.g., `9_999_999.99`) and either case for the exponent indicator
 * (`e` or `E`). On invalid or out-of-range input the parser returns NaN or ±Infinity
 * with the appropriate IEEE 754 condition flag set, rather than throwing.
 * Set the `throwOn*` flags for fail-fast error handling instead.
 *
 * @property collapseSNAN When `true`, parsing `"sNaN"` produces a quiet NaN instead
 *   of a signaling NaN. Useful for consumers that don't distinguish the two.
 *   Default: `false` (preserve sNaN as signaling).
 *
 * @property preserveNANPayload When `true`, parsing `"NaN123"` retains the payload
 *   `123` in the result. When `false`, payload is discarded and a canonical NaN is
 *   produced (matches `java.lang.Double.parseDouble` behavior).
 *   Default: `true` (GDAS-canonical).
 *
 * @property throwOnMalformedText When `true`, syntactically invalid input throws
 *   instead of returning NaN (as specified in IEEE 754) . Use for fail-fast
 *   parsing of trusted input. Default tries to conform to language-specific
 *   behavior.
 *   Kotlin/Java Default: `false`.
 *
 * @property throwOnInexact When `true`, input whose coefficient cannot be represented
 *   in 34 digits without discarding non-zero information throws instead of silently
 *   rounding. Trailing zeros never trigger inexact — they can be dropped from the
 *   coefficient (with a corresponding exponent adjustment) without loss of value.
 *   Default: `false`.
 *
 * @property throwOnOverflow When `true`, input whose adjusted exponent exceeds the
 *   maximum representable (6144) (e.g. `1e9999`) throws instead of returning ±Infinity.
 *   Default: `false`.
 *
 * @property throwOnUnderflow When `true`, input whose magnitude is too small
 *   (e.g. 1e-9999) to represent at all (rounds to zero) throws
 *   instead of returning zero.
 *   Default: `false`.
 */
data class ParsePrefs(
    val collapseSNAN: Boolean = false,
    val preserveNANPayload: Boolean = true,
    val throwOnMalformedText: Boolean = false,
    val throwOnInexact: Boolean = false,        // > 34 significant digits → rounded
    val throwOnOverflow: Boolean = false,       // exponent too large → infinity
    val throwOnUnderflow: Boolean = false,      // exponent too small → subnormal/zero
)

