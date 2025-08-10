package com.decimal128.cardinal

import com.decimal128.decimal.unsignedCmp
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max


object HugeIntExtensions {

    fun HugeInt.toBigInteger(): BigInteger = BigInteger(this.toString())

    fun BigInteger.toHugeInt(): HugeInt = HugeInt.fromString(this.toString())

    fun HugeInt.compareTo(bi: BigInteger) = this.compareTo(bi.toHugeInt())

    fun HugeInt.EQ(bi: BigInteger) = this.compareTo(bi) == 0

}
