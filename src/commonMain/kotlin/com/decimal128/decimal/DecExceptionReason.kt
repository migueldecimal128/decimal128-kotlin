package com.decimal128.decimal

enum class DecExceptionReason {
    SIGNALING_NAN_OPERAND,      // An operand was an sNaN
    MAGNITUDE_SUBTRACTION_OF_INFINITIES, // e.g., (+∞) + (-∞)
    MULTIPLICATION_OF_ZERO_BY_INFINITY, // e.g., 0 * ∞
    DIVISION_OF_ZERO_BY_ZERO,
    DIVISION_OF_INFINITY_BY_INFINITY,
    INVALID_CONVERSION,         // e.g., fromString("abc")
    INVALID_SQUARE_ROOT,        // e.g., sqrt(-1)
    INVALID_QUANTIZE,           // e.g., quantize(Infinity, finite)
    OTHER,                       // A catch-all
}