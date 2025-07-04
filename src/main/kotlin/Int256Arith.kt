package com.decimal128

object Int256Arith {

    fun int256Add(z: Int256, x: Int256, y: Int256) = int256AddImpl(z, x, y.sign, y)

    fun int256Sub(z: Int256, x: Int256, y: Int256) = int256AddImpl(z, x, ! y.sign, y)

    private fun int256AddImpl(z: Int256, x: Int256, ySign: Boolean, y: Int256) {
        if (x.sign == ySign) {
            // FIXME do I need to worry about -0 here?
            z.sign = ySign
            CoeffAdd.coeffAddUnscaled(z, x, y)
        } else {
            val cmp = CoeffCompare.coeffUnscaledCompare(x, y)
            when {
                (cmp > 0) -> {
                    z.sign = x.sign
                    CoeffSub.coeffSubUnscaled(z, x, y)
                }
                (cmp < 0) -> {
                    z.sign = ySign
                    CoeffSub.coeffSubUnscaled(z, y, x)
                }
                else -> {
                    z.sign = false
                    z.coeffSetZero()
                }
            }
        }
    }

    fun int256Mul(z: Int256, x: Int256, y: Int256) {
        CoeffMul.coeffMul(z, x, y)
        z.sign = (x.sign xor y.sign) and (z.bitLen > 0)
    }

    fun int256Divx64(z: Int256, x: Int256, y0: Long) {
        CoeffDivide.coeffDivx64(z, x, y0)
        z.sign = x.sign
    }

    fun int256Div(z: Int256, x: Int256, y: Int256) {
        CoeffDivide.coeffDiv(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun int256Mod(z: Int256, x: Int256, y: Int256) {
        CoeffDivide.coeffMod(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun int256ScaleUpPow10(z: Int256, x: Int256, pow10: Int) {
        CoeffScalePow10.coeffScaleUpPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun int256ScaleDownPow10(z: Int256, x: Int256, pow10: Int) {
        CoeffScalePow10.coeffScaleDownPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun int256FmaPow10(z: Int256, x: Int256, pow10: Int, a: Int256) {
        if (! (x.sign xor a.sign)) {
            z.sign = x.sign
            CoeffScalePow10.coeffScaleFmaPow10(z, x, pow10, a)
            return
        }
        val prod = if (z === a) Int256() else z
        int256ScaleUpPow10(prod, x, pow10)
        int256Add(z, prod, a)
    }

}
