package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.coeffSetMul(x, y)
        z.qExp = x.qExp + y.qExp
    }

    fun magFma(z: Mag, x: Mag, y: Mag, a: Mag): Residue {
        // check the scaled to see what will happen
        // prob check in base 10

        throw RuntimeException("fma not impl")
    }

}
