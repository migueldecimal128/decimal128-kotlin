package com.decimal128.decimal

data class DecExceptionContext(
    val ctx: DecContext,
    val exception: DecException,
    val exceptionReason: InvalidOperationReason? = null
) {
}
