package com.decimal128.decimal

import kotlin.test.Test

class TestCompoundInterest1 {

    @Test
    fun testCompoundInterest1() {
        val percentRate = "5.5".toDecimal()
        println("percentRate:$percentRate")
        val rate = percentRate / "100".toDecimal()
        println("rate:$rate")
        val periods = 276
        println("periods:$periods")
        val ratePerPeriod = Decimal.ONE + rate / periods
        println("ratePerPeriod:$ratePerPeriod")
        val compoundRate = ratePerPeriod.pow(periods)
        println("compoundRate:$compoundRate")

    }
}