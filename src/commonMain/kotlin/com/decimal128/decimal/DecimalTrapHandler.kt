package com.decimal128.decimal

fun interface DecimalTrapHandler {
    companion object{
        fun defaultUnderflowHandler(trapContext: DecimalTrapContext): MutDec {
            // FIXME
            return MutDec()
        }
    }
    fun execute(trapContext: DecimalTrapContext) : MutDec
}
