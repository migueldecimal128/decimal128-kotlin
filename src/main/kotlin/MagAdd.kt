package com.decimal128

import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled

object MagAdd {

    fun magAdd(z: Mag, x: Mag, y: Mag): Residue {
        val flipFlop = x.expQ >= y.expQ
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        assert(m.expQ >= n.expQ)
        val minExp = n.expQ
        val expDelta = m.expQ - n.expQ
        when {
            (expDelta == 0) -> coeffAddUnscaled(z.c, m.c, n.c)
            (expDelta >= PRECISION_34) -> {
                val residue =
                    if (expDelta == PRECISION_34)
                        Residue.residueFrom(y.c)
                    else
                        Residue.LT_HALF
                z.magSet(m)
                return residue
            }

            (expDelta > 0) -> coeffAddScaled(z.c, m.c, expDelta, n.c)
            else -> coeffAddScaled(z.c, n.c, -expDelta, m.c)
        }
        z.expQ = minExp
        return Residue.EXACT
    }

    fun magSub(z: Mag, x: Mag, y: Mag): Pair<Boolean, Residue> {
        throw RuntimeException("not impl")
    }
}
