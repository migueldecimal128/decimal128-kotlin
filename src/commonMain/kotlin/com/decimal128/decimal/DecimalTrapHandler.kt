package com.decimal128.decimal

fun interface DecimalTrapHandler {
    companion object{
        fun defaultUnderflowHandler(trapContext: DecimalTrapContext): Decimal {
            // FIXME
            return Decimal()
        }
    }
    fun execute(trapContext: DecimalTrapContext) : Decimal
}
