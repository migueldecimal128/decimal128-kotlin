package com.decimal128.decimal


operator fun Decimal.plus(other: Decimal) = this.add(other)
operator fun Decimal.minus(other: Decimal) = this.subtract(other)
operator fun Decimal.times(other: Decimal) = this.multiply(other)
operator fun Decimal.div(other: Decimal) = this.divide(other)

