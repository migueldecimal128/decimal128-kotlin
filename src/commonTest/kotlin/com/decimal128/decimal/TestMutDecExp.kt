package com.decimal128.decimal

import kotlin.test.Test

class TestMutDecExp {
    @Test
    fun expRangeReduction() {
        val ctx38 = DecContext.decimal128Extended38()

        loadDecimal128ConstantTables()

        // Test 1: x = ln(10) -> n=1, r should be ~0
        val ln10 = MutDec().set("2.3025850929940456840179914546843642076", ctx38)
        // after range reduction, r should be very close to 0
        // specifically |r| < ln(10)/2
        val expLn10 = MutDec().setExp(ln10, ctx38)
        println("expLn10:$expLn10")

        // Test 2: x = 2*ln(10) = ln(100) -> n=2, r should be ~0
        val ln100 = MutDec().set("4.6051701859880913680359828938172284152", ctx38)
        val expLn100 = MutDec().setExp(ln100, ctx38)
        println("expLn100:$expLn100")

        // Test 3: x = 1 -> n=0, r=1 (since 1 < ln(10)/2 is false...
        // actually ln(10)/2 ≈ 1.151, so n=0, r=1)
        val one = MutDec().set("1", ctx38)
        val exp1 = MutDec().setExp(one, ctx38)
        println("exp1:$exp1")

        // Test 4: x = ln(10) + 0.1 -> n=1, r=0.1
        val ln10PlusPoint1 = MutDec().set("2.4025850929940456840179914546843642076", ctx38)
        val expLn10PlusPoint1 = MutDec().setExp(ln10PlusPoint1, ctx38)
        println("expLn10PlusPoint1:$expLn10PlusPoint1")
    }
}