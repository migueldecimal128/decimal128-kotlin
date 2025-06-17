package com.decimal128

import java.math.BigDecimal
import java.math.BigInteger

fun Coeff.coeffSet(bi: BigInteger) {
    require(bi.signum() >= 0)
    require(bi.bitLength() <= 256)
    this.coeffSet256(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
}

fun newCoeff(bi: BigInteger): Coeff {
    val c = Coeff()
    c.coeffSet(bi)
    return c
}

fun Coeff.coeffToBigInteger(): BigInteger {
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

fun newDecimal(bd: BigDecimal): Decimal = newDecimal(bd, DecimalContext.newDecimal128Context())

fun newDecimal(bd: BigDecimal, ctx: DecimalContext): Decimal {
    val dec = Decimal()
    dec.magSet(bd.abs(), ctx)
    dec.sign = bd.signum() ushr 31
    return dec
}

fun Decimal.set(bd: BigDecimal, ctx: DecimalContext) {
    this.magSet(bd.abs(), ctx)
    this.sign = bd.signum() ushr 31
    this.roundAndFinalize(Residue.EXACT, ctx)
}
