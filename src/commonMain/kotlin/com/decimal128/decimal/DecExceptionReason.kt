package com.decimal128.decimal

enum class DecExceptionReason {
    SNAN_OPERAND,      // An operand was an sNaN
    MAGNITUDE_SUBTRACTION_OF_INFINITIES, // e.g., (+∞) + (-∞)
    MULTIPLICATION_OF_ZERO_BY_INFINITY, // e.g., 0 * ∞
    DIVISION_OF_ZERO_BY_ZERO,
    DIVISION_OF_INFINITY_BY_INFINITY,
    INVALID_CONVERSION,         // e.g., fromString("abc")
    INVALID_SQUARE_ROOT,        // e.g., sqrt(-1)
    INVALID_QUANTIZE,           // e.g., quantize(Infinity, finite)

    IS_TINY,
    IS_INEXACT,

    OTHER,                       // A catch-all
    PARSE_EMPTY_STRING,
    PARSE_SIGN_ONLY,
    PARSE_DOUBLE_DOT,
    PARSE_BAD_UNDERSCORE,
    PARSE_NO_EXPONENT_DIGIT,
    PARSE_UNEXPECTED_CHAR,
}