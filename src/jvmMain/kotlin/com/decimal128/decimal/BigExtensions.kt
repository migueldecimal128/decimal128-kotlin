package com.decimal128.decimal

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

fun C256.u256Set(bi: BigInteger) {
    require(bi.signum() >= 0)
    require(bi.bitLength() <= 256)
    this.c256Set256(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
}

fun newCoeff(bi: BigInteger): C256 {
    val c = C256()
    c.u256Set(bi)
    return c
}

fun C256.coeffToBigInteger(): BigInteger {
    var bi = BigInteger.ZERO
    val dw0Lo = this.dw0 and 0xFFFFFFFFL
    val dw0Hi = this.dw0 ushr 32
    val dw1Lo = this.dw1 and 0xFFFFFFFFL
    val dw1Hi = this.dw1 ushr 32
    val dw2Lo = this.dw2 and 0xFFFFFFFFL
    val dw2Hi = this.dw2 ushr 32
    val dw3Lo = this.dw3 and 0xFFFFFFFFL
    val dw3Hi = this.dw3 ushr 32
    bi = bi or BigInteger(dw0Lo.toString()).shiftLeft(0)
    bi = bi or BigInteger(dw0Hi.toString()).shiftLeft(32)
    bi = bi or BigInteger(dw1Lo.toString()).shiftLeft(64)
    bi = bi or BigInteger(dw1Hi.toString()).shiftLeft(96)
    bi = bi or BigInteger(dw2Lo.toString()).shiftLeft(128)
    bi = bi or BigInteger(dw2Hi.toString()).shiftLeft(160)
    bi = bi or BigInteger(dw3Lo.toString()).shiftLeft(192)
    bi = bi or BigInteger(dw3Hi.toString()).shiftLeft(224)
    return bi
}

fun Decimal.coeffToBigInteger(): BigInteger {
    var bi = BigInteger.ZERO
    val dw0Lo = this.dw0 and 0xFFFFFFFFL
    val dw0Hi = this.dw0 ushr 32
    val dw1Lo = this.dw1 and 0xFFFFFFFFL
    val dw1Hi = this.dw1 ushr 32
    bi = bi or BigInteger(dw0Lo.toString()).shiftLeft(0)
    bi = bi or BigInteger(dw0Hi.toString()).shiftLeft(32)
    bi = bi or BigInteger(dw1Lo.toString()).shiftLeft(64)
    bi = bi or BigInteger(dw1Hi.toString()).shiftLeft(96)
    return bi
}

fun newMutDec(bd: BigDecimal): MutDec = newMutDec(bd, DecContext())

fun newMutDec(bd: BigDecimal, env: DecContext): MutDec {
    val mutdec = MutDec()
    mutdec.set(bd, env)
    return mutdec
}

fun MutDec.set(bd: BigDecimal) = this.set(bd, DecContext())

fun MutDec.set(bd: BigDecimal, env: DecContext) {
    this.u256Set(bd.abs().unscaledValue())
    this.qExp = -bd.scale()
    this.sign = bd.signum() < 0
    this.finalize(env)
}

fun MutDec.set(bi: BigInteger, env: DecContext) {
    if (bi.bitLength() <= 256) {
        this.qExp = 0
        val sign = bi.signum() < 0
        this.sign = sign
        val biT = if (sign) bi.abs() else bi
        val d0 = biT.toLong()
        val d1 = biT.shiftRight( 64).toLong()
        val d2 = biT.shiftRight(128).toLong()
        val d3 = biT.shiftRight(192).toLong()
        c256Set256(d3, d2, d1, d0)
    }
    val bd = BigDecimal(bi, MathContext(70, RoundingMode.HALF_EVEN))
    set(bd, DecContext())
}

fun newDoubleDoubleFromBigInteger(bi: BigInteger): DoubleDouble {
    val dd = DoubleDouble()
    dd.setBigInteger(bi)
    return dd
}

fun DoubleDouble.setBigInteger(n: BigInteger) {
    this.hi = 0.0
    this.lo = 0.0
    val sign = n.signum()
    if (sign == 0) {
        return
    }
    val abs   = n.abs()
    val L     = abs.bitLength()
    if (L <= 53) {
        this.hi = sign * abs.toDouble()
        return
    }
    val shift = L - 53
    val top53 = abs.shiftRight(shift)
    val m     = top53.toLong() and ((1L shl 53) - 1)
    val exp   = L - 1
    val mant  = m and ((1L shl 52) - 1)
    val bits  = ((if (sign<0)1L else 0L) shl 63) or
            ((exp+1023).toLong() shl 52) or
            mant
    val hi    = Double.fromBits(bits)
    val rem   = abs.subtract(top53.shiftLeft(shift))
    val lo0   = sign * rem.toDouble()
    // final normalization!
    this.setQuickTwoSum(hi, lo0)
}

private val ROUNDING_MODE_MAP = arrayOf(RoundingMode.HALF_EVEN, RoundingMode.HALF_UP,
    RoundingMode.DOWN, RoundingMode.CEILING, RoundingMode.FLOOR)

fun DecRounding.mapToRoundingMode() = ROUNDING_MODE_MAP[value]



