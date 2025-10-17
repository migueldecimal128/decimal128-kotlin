package com.decimal128.decimal

object MagnitudeInv {

    private val ONE = MutDec().set(1)

    fun magInv(z: MutDec, x: MutDec, env: DecEnv): Residue {
        // TODO implement this as a Newton-Raphson
        //  Double -> DoubleDouble -> Decimal
        return MagnitudeDiv.magDiv(z, ONE, x, env)
    }
}