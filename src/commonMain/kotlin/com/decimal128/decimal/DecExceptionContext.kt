package com.decimal128.decimal

data class DecExceptionContext(
    val exception: DecException,
    val exceptionReason: DecExceptionReason,
    val operation: String,
    val env: DecContext) {
}
