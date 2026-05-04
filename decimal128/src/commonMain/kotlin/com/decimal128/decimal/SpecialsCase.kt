package com.decimal128.decimal

/**
 * Specification of character case used for non-finite specials.
 *
 * @property LOWERCASE infinity/inf, nan, snan
 * @property MIXEDCASE Infinity/Inf, NaN, sNaN
 * @property UPPERCASE INFINITY/INF, NAN, SNAN
 */
enum class SpecialsCase {
    LOWERCASE, // inf, nan, snan
    MIXEDCASE, // Inf or Infinity, NaN, sNaN
    UPPERCASE  // INF or INFINITY, NAN, SNAN
}
