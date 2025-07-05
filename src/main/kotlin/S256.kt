package com.decimal128

import com.decimal128.U256Compare.u256UnscaledCompare

internal open class S256() : U256() {
    var sign = false

    fun s256Add(x: S256, y: S256) = s256AddImpl(x, y.sign, y)

    fun s256Sub(x: S256, y: S256) = s256AddImpl(x, ! y.sign, y)

    private fun s256AddImpl(x: S256, ySign: Boolean, y: S256) {
        val xSign = x.sign
        if (xSign == ySign) {
            this.u256SetAdd(x, y)
            this.sign = ySign and (bitLen > 0)
        } else {
            val cmp = u256UnscaledCompare(x, y)
            when {
                (cmp > 0) -> {
                    this.u256SetSub(x, y)
                    this.sign = xSign and (this.bitLen > 0)
                }
                (cmp < 0) -> {
                    this.u256SetSub(y, x)
                    this.sign = ySign and (this.bitLen > 0)
                }
                else -> {
                    this.sign = false
                    this.u256SetZero()
                }
            }
        }
    }

    fun s256Mul(x: S256, y: S256) {
        this.u256SetMul(x, y)
        this.sign = (x.sign xor y.sign) and (bitLen > 0)
    }

    fun s256DivX64(x: S256, y0: Long): Residue {
        val residue = this.u256SetDivX64(x, y0)
        this.sign = x.sign and (bitLen > 0)
        return residue
    }

    fun s256Div(x: S256, y: S256): Residue {
        val residue = this.u256SetDiv(x, y)
        this.sign = (x.sign xor y.sign) and (bitLen > 0)
        return residue
    }

    fun s256Mod(x: S256, y: S256) {
        this.u256SetMod(x, y)
        this.sign = (x.sign xor y.sign) and (bitLen > 0)
    }

    fun s256SetScaleUpPow10(x: S256, pow10: Int) {
        this.u256SetScaleUpPow10(x, pow10)
        this.sign = x.sign
    }

    fun s256ScaleDownPow10(x: S256, pow10: Int): Residue {
        val residue = this.u256SetScaleDownPow10(x, pow10)
        this.sign = x.sign
        return residue
    }

    fun s256SetFmaPow10(x: S256, pow10: Int, a: S256) {
        if (! (x.sign xor a.sign)) {
            u256SetFmaPow10(x, pow10, a)
            this.sign = x.sign and (bitLen > 0)
            return
        }
        val prod = if (this === a) S256() else this
        prod.s256SetScaleUpPow10(x, pow10)
        s256Add(prod, a)
    }

}
