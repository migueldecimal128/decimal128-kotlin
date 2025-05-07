package com.decimal128

import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled
import java.lang.Long.compareUnsigned
import com.decimal128.CoeffScalePow10.coeffScaleFmaPow10
import com.decimal128.Residue.Companion.EXACT

object MagAdd {

    private fun addAbsorbSmallOperand(z: Mag, x: Mag, y: Mag, sign: Boolean, ctx: Decimal128Context) {
        val expDelta = x.exp - y.exp
        val residue = (
                if (expDelta == PRECISION_34)
                    Residue.residueFrom(y.c)
                else
                    Residue.LT_HALF
                )
        z.magSet(x)
        z.finalize(residue, sign, ctx)
    }

    fun magAdd(z: Mag, a: Mag, b: Mag, sign: Boolean, ctx: Decimal128Context) {
        val flipFlop = a.exp >= b.exp
        val x = if (flipFlop) a else b
        val y = if (flipFlop) b else a
        assert(x.exp >= y.exp)
        val minExp = y.exp
        val expDelta = x.exp - y.exp
        if (expDelta >= PRECISION_34) {
            addAbsorbSmallOperand(z, x, y, sign, ctx)
            return
        }
        when {
            expDelta == 0 -> coeffAddUnscaled(z.c, x.c, y.c)
            expDelta > 0 -> coeffAddScaled(z.c, x.c, expDelta, y.c)
            else -> coeffAddScaled(z.c, y.c, -expDelta, x.c)
        }
        z.exp = minExp
    }

    fun magSub(z: Mag, sign: Boolean, x: Mag, y: Mag, ctx: Decimal128Context) {
        throw RuntimeException("not impl")
    }
}
