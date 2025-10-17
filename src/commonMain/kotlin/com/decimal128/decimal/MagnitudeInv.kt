package com.decimal128.decimal

object MagnitudeInv {

    private val ONE = MutDec(false, 0, 0L, 0L, 0L, 1L)

    fun magInv(z: MutDec, x: MutDec, env: env): Residue {
        // TODO implement this as a Newton-Raphson
        //  Double -> DoubleDouble -> Decimal
        return MagnitudeDiv.magDiv(z, ONE, x, env)
    }
}