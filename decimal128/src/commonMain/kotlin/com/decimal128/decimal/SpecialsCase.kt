package com.decimal128.decimal

/**
 * Character case used when rendering non-finite specials (`Infinity`, `NaN`, `sNaN`).
 *
 * The choice of long vs. short form for infinity (`Infinity` vs. `Inf`) is controlled
 * separately by [PrintPrefs.infinityShort].
 *
 * @property LOWERCASE Lowercase form: `infinity`/`inf`, `nan`, `snan`. Matches
 *   `Double.toString` in C, Swift, Python `float`, and JavaScript's lowercase form.
 *
 * @property MIXEDCASE Mixed-case form: `Infinity`/`Inf`, `NaN`, `sNaN`. GDAS
 *   canonical, also used by `BigDecimal.toString` in Java/Kotlin and Python's
 *   `decimal.Decimal`.
 *
 * @property UPPERCASE Uppercase form: `INFINITY`/`INF`, `NAN`, `SNAN`. Used by
 *   IBM mainframe environments (Db2 SQL, COBOL on z/OS, PL/I) and matches
 *   C `printf("%F")` / `printf("%G")` output.
 */
enum class SpecialsCase {
    LOWERCASE,
    MIXEDCASE,
    UPPERCASE
}