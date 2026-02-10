package com.decimal128.decimal

object MagnitudeInv {

    private val ONE = MutDec().set(1)

    fun magInv(z: MutDec, sign: Boolean, x: MutDec, ctx: DecContext): Residue {
        // TODO implement this as a Newton-Raphson
        //  Double -> DoubleDouble -> Decimal2
        return MagnitudeDiv.magDivFnzFnz(z, sign, ONE, x, ctx)
    }
}