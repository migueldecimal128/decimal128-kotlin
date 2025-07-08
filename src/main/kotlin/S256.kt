package com.decimal128

import com.decimal128.U256Compare.u256UnscaledCompare

@Suppress("NOTHING_TO_INLINE")
open class S256 : U256 {
    constructor(): super()
    constructor(x: S256): super() {
        s256Set(x)
    }
    constructor(sign: Boolean, dw3: Long, dw2: Long, dw1: Long, dw0: Long): super(dw3, dw2, dw1, dw0) {
        s256Set(sign, dw3, dw2, dw1, dw0)
        this.sign = sign
    }
    constructor(str: String) : super() {
        val sign = Int256ParsePrint.u256FromString(this, true, str)
        this.sign = sign and (bitLen > 0)
    }

    @JvmField
    internal var sign = false

    fun s256SetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L;
        bitLen = 0; digitLen = 0;
        sign = false
    }

    fun s256SetSigned(n: Long) {
        if (n >= 0)
            s256Set(false, 0L, 0L, 0L, n)
        else
            s256Set(true, 0L, 0L, 0L, -n) // this works for Long.MIN_VALUE too
    }

    fun s256SetUnsigned(u: Long) = s256Set(false, 0L, 0L, 0L, u)

    fun s256Set(x: S256) {
        dw3 = x.dw3; dw2 = x.dw2; dw1 = x.dw1; dw0 = x.dw0
        bitLen = x.bitLen; digitLen = x.digitLen
        sign = x.sign
    }

    fun s256Set(sign: Boolean, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        u256Set256(dw3, dw2, dw1, dw0)
        this.sign = sign and (bitLen > 0)
    }

    internal inline fun s256Add(x: S256, y: S256) = s256AddImpl(x, y.sign, y)

    internal inline fun s256Sub(x: S256, y: S256) = s256AddImpl(x, ! y.sign, y)

    fun s256AddImpl(x: S256, ySign: Boolean, y: S256) {
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

    internal inline fun s256Mul(x: S256, y: S256) {
        this.u256SetMul(x, y)
        this.sign = (x.sign xor y.sign) and (bitLen > 0)
    }

    internal inline fun s256DivX64(x: S256, y0: Long): Residue {
        val residue = this.u256SetDivX64(x, y0)
        this.sign = x.sign and (bitLen > 0)
        return residue
    }

    internal inline fun s256Div(x: S256, y: S256): Residue {
        val residue = this.u256SetDiv(x, y)
        this.sign = (x.sign xor y.sign) and (bitLen > 0)
        return residue
    }

    internal inline fun s256Mod(x: S256, y: S256) {
        this.u256SetMod(x, y)
        this.sign = x.sign and (bitLen > 0)
    }

    internal fun s256DivMod(mod: S256, x: S256, y: S256) {
        val signQ = x.sign xor y.sign
        val signR = x.sign
        this.u256SetDivMod(mod, x, y)
        mod.sign = signR and (mod.bitLen > 0)
        this.sign = signQ and (this.bitLen > 0)
    }

    internal inline fun s256SetScaleUpPow10(x: S256, pow10: Int) {
        this.u256SetScaleUpPow10(x, pow10)
        this.sign = x.sign and (bitLen > 0)
    }

    internal inline fun s256ScaleDownPow10(x: S256, pow10: Int): Residue {
        val residue = this.u256SetScaleDownPow10(x, pow10)
        this.sign = x.sign and (bitLen > 0)
        return residue
    }

    internal inline fun s256SetFmaPow10(x: S256, pow10: Int, a: S256) {
        if (! (x.sign xor a.sign)) {
            u256SetFmaPow10(x, pow10, a)
            this.sign = x.sign and (bitLen > 0)
            return
        }
        val prod = if (this === a) S256() else this
        prod.s256SetScaleUpPow10(x, pow10)
        s256Add(prod, a)
    }

    override fun toHexString() = Int256ParsePrint.int256ToHexString(sign, this)
    override fun toString() = Int256ParsePrint.int256ToString(sign, this)

    internal inline fun signum(): Int {
        val nonZero = (-this.bitLen) shr 31
        val s = if (this.sign) -1 else 1
        return nonZero and s
    }

    fun s256UnscaledCompareTo(other: S256): Int {
        val thisSignum = this.signum()
        val otherSignum = other.signum()

        // If both are non-zero
        if ((thisSignum and otherSignum) != 0) {
            if (thisSignum == otherSignum) {
                val cmp = u256UnscaledCompareTo(other)
                return thisSignum * cmp
            }
            return thisSignum
        }
        // at least one is zero
        return (thisSignum and ((otherSignum and 1) - 1)) or (otherSignum and ((thisSignum and 1) - 1))
    }

    override fun equals(other: Any?) = other is S256 && sign == other.sign && u256UnscaledEQ(other)

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + if (sign) 1 else 0
        return result
    }
}
