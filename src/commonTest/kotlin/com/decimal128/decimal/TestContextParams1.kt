package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TestContextParams1 {

    val verbose = false

    @Test
    fun testContextA() {
        val contexts = arrayOf(DecContext.DECIMAL128, DecContext.DECIMAL128_EXTENDED)
        for (ctx in contexts) {
            with(ctx) {
                val a = "1.0".toDecimal()
                val b = "3.0".toDecimal()
                val c = a / b
                if (verbose)
                    println("ctx.precision:${ctx.precision} c:$c, ${c.precision()}")
                assertEquals(ctx.precision, c.precision())
            }
        }
    }
}