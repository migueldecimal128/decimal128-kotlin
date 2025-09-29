package com.decimal128.decimal


operator fun MutDec.plus(other: MutDec) = this.add(other)
operator fun MutDec.minus(other: MutDec) = this.subtract(other)
operator fun MutDec.times(other: MutDec) = this.multiply(other)
operator fun MutDec.div(other: MutDec) = this.divide(other)

