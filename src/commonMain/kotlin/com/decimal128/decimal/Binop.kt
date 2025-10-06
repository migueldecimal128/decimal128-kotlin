package com.decimal128.decimal

abstract class Binop {
    abstract fun opZerZer(xZer: Decimal, yZer: Decimal): Decimal
    abstract fun opFnzZer(xFnz: Decimal, yZer: Decimal): Decimal
    abstract fun opInfZer(xInf: Decimal, yZer: Decimal): Decimal
    abstract fun opNanZer(xNan: Decimal, yZer: Decimal): Decimal

    abstract fun opZerFnz(xZer: Decimal, yFnz: Decimal): Decimal
    abstract fun opFnzFnz(xFnz: Decimal, yFnz: Decimal): Decimal
    abstract fun opInfFnz(xInf: Decimal, yFnz: Decimal): Decimal
    abstract fun opNanFnz(xNan: Decimal, yFnz: Decimal): Decimal

    abstract fun opZerInf(xZer: Decimal, yInf: Decimal): Decimal
    abstract fun opFnzInf(xFnz: Decimal, yInf: Decimal): Decimal
    abstract fun opInfInf(xInf: Decimal, yInf: Decimal): Decimal
    abstract fun opNanInf(xNan: Decimal, yInf: Decimal): Decimal

    abstract fun opZerNan(xZer: Decimal, yNan: Decimal): Decimal
    abstract fun opFnzNan(xFnz: Decimal, yNan: Decimal): Decimal
    abstract fun opInfNan(xInf: Decimal, yNan: Decimal): Decimal
    abstract fun opNanNan(xNan: Decimal, yNan: Decimal): Decimal

}