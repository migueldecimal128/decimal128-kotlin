package com.decimal128.decimal

fun interface DecTrapHandler {
    fun signal(trapContext: DecExceptionContext) : Decimal
}
