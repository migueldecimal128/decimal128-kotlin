package com.decimal128.decimal

fun interface DecTrapHandler {
    companion object{
        fun defaultUnderflowHandler(trapContext: DecTrapContext): MutDec {
            // FIXME
            return MutDec()
        }
    }
    fun execute(trapContext: DecTrapContext) : MutDec
}
