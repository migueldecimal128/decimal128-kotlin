package com.decimal128.decimal

import java.text.spi.DecimalFormatSymbolsProvider

data class DecExceptionContext(
    val ctx: DecContext,
    val defaultReturn: Decimal,
    val exception: DecException,
    val exceptionReason: InvalidOperationReason? = null
) {
}
