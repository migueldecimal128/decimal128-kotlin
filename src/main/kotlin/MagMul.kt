package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.coeffMul(x, y)
        z.qExp = x.qExp + y.qExp
    }

}
