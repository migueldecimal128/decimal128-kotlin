package com.decimal128.decimal

abstract class Binop2(private val dispatchTable: Array<(Decimal, Decimal) -> Decimal>) {
    init {
        require (dispatchTable.size == 16)
    }
    fun dispatch(xCat: Category754, x: Decimal, yCat: Category754, y: Decimal): Decimal {
        val index = ((xCat.ordinal shl 2) + yCat.ordinal) and 0x0F
        return dispatchTable[index](x, y)
    }

}