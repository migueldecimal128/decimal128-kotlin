package com.decimal128.decimal

/**
 * Preferences controlling print behavior — converting [Decimal128] to text.
 *
 * Defaults follow GDAS (General Decimal Arithmetic Specification) canonical form,
 * which is also the convention used by `java.math.BigDecimal.toString` and Python's
 * `decimal.Decimal`. Override individual fields to match other conventions
 * (e.g., `Double.toString`, C `printf`) or for application-specific styling.
 *
 * @property negativeZero When `true`, `-0` prints with a leading minus (`-0`).
 *   When `false`, the sign is suppressed and `-0` prints as `0`.
 *   Default: `true` (preserve sign of zero).
 *
 * @property exponentPlusSign When `true`, positive exponents are prefixed with `+`
 *   (e.g., `1.23E+4`). When `false`, the `+` is omitted (e.g., `1.23E4`, matching
 *   `java.lang.Double.toString`). Negative exponents always use `-`.
 *   Default: `true` (GDAS canonical).
 *
 * @property exponentLowercaseE When `true`, exponent indicator is `e` (e.g., `1.23e+4`,
 *   matching `Double.toString` in Swift, C, Python `float`, JavaScript). When `false`,
 *   uppercase `E` is used (e.g., `1.23E+4`, matching `BigDecimal.toString` in Java/Kotlin,
 *   Python `Decimal`, GDAS canonical).
 *   Default: `false`.
 *
 * @property infinityShort When `true`, infinity prints as the 3-character form
 *   (`Inf`, `INF`, `inf` per [specialsCase]). When `false`, the full word `Infinity`
 *   is used.
 *   Default: `false` (full word, GDAS canonical).
 *
 * @property nanMinusSign When `true`, a NaN with its sign bit set prints with a
 *   leading minus (`-NaN`). When `false`, the sign is suppressed (`NaN`, matching
 *   `java.lang.Double.toString` and JavaScript).
 *   Default: `true` (GDAS canonical).
 *
 * @property nanPayload When `true`, NaN payload digits are appended to the output
 *   (e.g., `NaN123`). When `false`, the payload is hidden and only `NaN` prints.
 *   Default: `true` (GDAS canonical).
 *
 * @property collapseSNaN When `true`, signaling NaNs print as quiet NaNs (`NaN`
 *   instead of `sNaN`). Useful for consumers that don't distinguish the two.
 *   Default: `false`.
 *
 * @property style Selects between scientific, engineering, and raw-cohort forms.
 *   See [PrintStyle].
 *   Default: [PrintStyle.AUTO] (GDAS to-scientific-string).
 *
 * @property specialsCase Letter case used for `Infinity`, `NaN`, and `sNaN`.
 *   See [SpecialsCase].
 *   Default: [SpecialsCase.MIXEDCASE].
 *
 * @property minPlainExponent In [PrintStyle.AUTO], values whose adjusted exponent
 *   is below this threshold render in scientific form rather than plain decimal.
 *   GDAS specifies `-6`, meaning values smaller than `1E-6` use scientific notation.
 *   Default: `-6`.
 */
data class PrintPrefs(
    val negativeZero: Boolean = true,
    val exponentPlusSign: Boolean = true,
    val exponentLowercaseE: Boolean = false,
    val infinityShort: Boolean = false,
    val nanMinusSign: Boolean = true,
    val nanPayload: Boolean = true,
    val collapseSNaN: Boolean = false,
    val style: PrintStyle = PrintStyle.AUTO,
    val specialsCase: SpecialsCase = SpecialsCase.MIXEDCASE,
    val minPlainExponent: Int = -6,
)
