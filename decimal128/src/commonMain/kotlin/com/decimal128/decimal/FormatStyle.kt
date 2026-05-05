package com.decimal128.decimal

/**
 * Selects between scientific, engineering, and raw-cohort output forms.
 *
 * @property AUTO GDAS to-scientific-string: plain decimal when adjusted exponent is
 *   within `[minPlainExponent, 0]`, scientific notation otherwise. The default form
 *   used by `BigDecimal.toString` and Python `decimal.Decimal`. Note that despite
 *   its name, this GDAS form is the auto-select form, not always-scientific.
 *
 * @property ENGINEERING GDAS to-engineering-string: scientific notation with the
 *   exponent constrained to a multiple of 3, so the integer part has 1, 2, or 3
 *   digits. Useful for displaying values with SI-style magnitudes.
 *
 * @property EXPONENTIAL Forces exponential notation regardless of magnitude.
 *   Not GDAS-canonical; useful for tabular alignment or when consistent format is
 *   required across a range of magnitudes.
 *
 * @property RAW Renders the raw cohort representation as
 *   `coefficient × 10^qExp`. Preserves the exact internal state of the value.
 *   Useful for debugging and for testing cohort-preservation invariants.
 */
enum class FormatStyle {
    AUTO,
    ENGINEERING,
    EXPONENTIAL,
    RAW
}