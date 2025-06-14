package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.coeffSetMul(x, y)
        z.qExp = x.qExp + y.qExp
    }

    fun magSqr(z: Mag, x: Mag) {
        z.coeffSetSqr(x)
        z.qExp = x.qExp shl 1
    }

}
