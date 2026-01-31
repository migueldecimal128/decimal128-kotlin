package com.decimal128.decimal

object MagnitudeInv {

    private val ONE = MutDec().set(1)

    fun magInv(z: MutDec, x: MutDec, env: DecContext): Residue {
        // TODO implement this as a Newton-Raphson
        //  Double -> DoubleDouble -> Decimal2
        return MagnitudeDiv.magDiv(z, ONE, x, env)
    }
}