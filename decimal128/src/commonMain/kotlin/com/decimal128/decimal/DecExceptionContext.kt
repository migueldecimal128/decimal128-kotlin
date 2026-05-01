package com.decimal128.decimal

data class DecExceptionContext(
    val ctx: DecContext,
    val defaultReturn: Decimal,
    val exception: DecException,
    val exceptionReason: InvalidCause? = null
) {
}
