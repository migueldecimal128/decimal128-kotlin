package com.decimal128

import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.c.mul(x.c, y.c)
        z.expQ = x.expQ + y.expQ
    }

}
