package com.decimal128

object XInt256Arith {

    fun int256Add(z: XInt256, x: XInt256, y: XInt256) = int256AddImpl(z, x, y.sign, y)

    fun int256Sub(z: XInt256, x: XInt256, y: XInt256) = int256AddImpl(z, x, ! y.sign, y)

    private fun int256AddImpl(z: XInt256, x: XInt256, ySign: Boolean, y: XInt256) {
        if (x.sign == ySign) {
            // FIXME do I need to worry about -0 here?
            z.sign = ySign
            U256Add.u256AddUnscaled(z, x, y)
        } else {
            val cmp = U256Compare.u256UnscaledCompare(x, y)
            when {
                (cmp > 0) -> {
                    z.sign = x.sign
                    U256Sub.u256SubUnscaled(z, x, y)
                }
                (cmp < 0) -> {
                    z.sign = ySign
                    U256Sub.u256SubUnscaled(z, y, x)
                }
                else -> {
                    z.sign = false
                    z.u256SetZero()
                }
            }
        }
    }

    fun int256Mul(z: XInt256, x: XInt256, y: XInt256) {
        U256Mul.u256Mul(z, x, y)
        z.sign = (x.sign xor y.sign) and (z.bitLen > 0)
    }

    fun int256Divx64(z: XInt256, x: XInt256, y0: Long) {
        U256Divide.u256Divx64(z, x, y0)
        z.sign = x.sign
    }

    fun int256Div(z: XInt256, x: XInt256, y: XInt256) {
        U256Divide.u256Div(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun int256Mod(z: XInt256, x: XInt256, y: XInt256) {
        U256Divide.u256Mod(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun int256ScaleUpPow10(z: XInt256, x: XInt256, pow10: Int) {
        U256ScalePow10.u256ScaleUpPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun int256ScaleDownPow10(z: XInt256, x: XInt256, pow10: Int) {
        U256ScalePow10.u256ScaleDownPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun int256FmaPow10(z: XInt256, x: XInt256, pow10: Int, a: XInt256) {
        if (! (x.sign xor a.sign)) {
            z.sign = x.sign
            U256ScalePow10.u256ScaleFmaPow10(z, x, pow10, a)
            return
        }
        val prod = if (z === a) XInt256() else z
        int256ScaleUpPow10(prod, x, pow10)
        int256Add(z, prod, a)
    }

}
