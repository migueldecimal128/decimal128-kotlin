package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.mul(x, y)
        z.qExp = x.qExp + y.qExp
    }

}
