package com.decimal128.decimal
/**
 * Preferences controlling arithmetic behavior — addition, multiplication, FMA,
 * division, and related operations.
 *
 * Defaults follow GDAS (General Decimal Arithmetic Specification, IBM Cowlishaw)
 * and IEEE 754-2008/2019. On exceptional conditions (invalid operation,
 * division by zero, overflow, underflow, inexact) the operation returns the
 * IEEE-specified result
 * (NaN, ±Infinity, or rounded value) and sets the corresponding condition flag.
 * Set the `throwOn*` flags for fail-fast error handling instead.
 *
 * Signed zero is preserved through arithmetic per IEEE 754; no preference is
 * required to control this.
 *
 * @property propagatePreferSNAN When two NaN operands meet, controls payload
 *   propagation. When `true`, signaling NaN's payload propagates over quiet NaN's
 *   (GDAS / DecTest behavior). When `false`, the first operand's payload propagates
 *   regardless of signaling-ness. In both cases, encountering a signaling NaN sets
 *   the Invalid Operation flag.
 *   Default: `true` (GDAS / DecTest).
 *
 * @property preserveNANPayload When `true`, NaN payloads propagate through arithmetic
 *   per GDAS. When `false`, all NaN results are canonical (zero payload), matching
 *   `java.lang.Double` and RISC-V semantics.
 *   Default: `true` (GDAS canonical).
 *
 * @property throwOnInvalidOperation When `true`, operations that produce an Invalid
 *   Operation condition (e.g., `0 × ∞`, `√-1`, signaling NaN consumed) throw instead
 *   of returning NaN.
 *   Default: `false`.
 *
 * @property throwOnDivisionByZero When `true`, dividing a finite non-zero value by
 *   zero throws instead of returning ±Infinity. Mirrors `BigDecimal` behavior on
 *   the JVM.
 *   Default: `false`.
 *
 * @property throwOnOverflow When `true`, operations whose result exceeds the maximum
 *   representable magnitude throw instead of returning ±Infinity.
 *   Default: `false`.
 *
 * @property throwOnUnderflow When `true`, operations whose result is too small to
 *   represent (rounds to zero) throw instead of returning zero.
 *   Default: `false`.
 *
 * Note: `throwOnInexact` is intentionally omitted. The Inexact condition fires on
 * virtually every non-trivial decimal computation, making throw-on-inexact too noisy
 * for practical use. Callers needing this behavior can install a custom trap handler.
 */
data class ArithmeticPrefs(
    val propagatePreferSNAN: Boolean = true,
    val preserveNANPayload: Boolean = true,
    val throwOnInvalidOperation: Boolean = false,
    val throwOnDivisionByZero: Boolean = false,
    val throwOnOverflow: Boolean = false,
    val throwOnUnderflow: Boolean = false,
) {
    companion object {
        val DEFAULT_IEEE = ArithmeticPrefs()
        val DEFAULT_INTEL = ArithmeticPrefs(propagatePreferSNAN = false)
        val DEFAULT_KOTLIN = ArithmeticPrefs(throwOnInvalidOperation = true,
            throwOnDivisionByZero = true, throwOnOverflow = true, throwOnUnderflow = true)
    }
}

