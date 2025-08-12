package com.decimal128.hugeint

import java.math.BigInteger


object HugeIntExtensions {

    fun HugeInt.toBigInteger(): BigInteger = BigInteger(this.toString())

    fun BigInteger.toHugeInt(): HugeInt = HugeInt.fromString(this.toString())

    fun HugeInt.compareTo(bi: BigInteger) = this.compareTo(bi.toHugeInt())

    infix fun HugeInt.EQ(bi: BigInteger) = this.compareTo(bi) == 0

    infix fun HugeInt.NE(bi: BigInteger) = this.compareTo(bi) != 0

}
