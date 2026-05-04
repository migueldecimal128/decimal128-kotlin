package com.decimal128.decimal

enum class FormatStyle {
    AUTO,                 // GDAS to-scientific-string (sic)
    ENGINEERING,          // GDAS to-engineering-string
    ALWAYS_SCIENTIFIC,    // always scientific notaion
    COEFFICIENT_QEXPONENT // raw decimal qExp cohort
}
