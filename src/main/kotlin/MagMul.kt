package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.c.mul(x.c, y.c)
        z.qExp = x.qExp + y.qExp
    }

}
