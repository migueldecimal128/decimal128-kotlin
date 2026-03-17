package com.decimal128.decimal

enum class InvalidOperationReason {
    NAN_OPERAND,       // an operand was a NaN
    SNAN_OPERAND,      // An operand was an sNaN
    MAGNITUDE_SUBTRACTION_OF_INFINITIES, // e.g., (+∞) + (-∞)
    MUL_ZERO_BY_INFINITY, // e.g., 0 * ∞
    DIV_ZERO_BY_ZERO,
    DIV_INF_BY_INF,
    UNABLE_TO_SCALE,
    PARSE_MALFORMED,
    PARSE_EMPTY_STRING,
    PARSE_DOUBLE_DOT,
    PARSE_INVALID_UNDERSCORE_LOCATION,
    PARSE_NO_EXPONENT_DIGIT,
    PARSE_UNEXPECTED_CHAR,
    PARSE_NO_COEFFICIENT_DIGIT,
    PARSE_COEFFICIENT_EXCEEDS_MAX_PRECISION,
    PARSE_VALUE_OUT_OF_RANGE,
    NEG_SQUARE_ROOT,        // e.g., sqrt(-1)
    QUANTIZE,           // e.g., quantize(Infinity, finite)
    QEXP_OF_NON_FINITE,
    CONV_TO_INTEGER,
    CONVERT_NON_FINITE_TO_INTEGER,
    OTHER,
}