package com.decimal128.decimal

import com.decimal128.decimal.DecContext.Companion.decimal128Extended38
import com.decimal128.decimal.DecContext.Companion.decimal128IEEE
import com.decimal128.decimal.DecContext.Companion.decimal128Kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TestContextParams1 {

    val verbose = false

    @Test
    fun testContextA() {
        val contexts = arrayOf(decimal128Kotlin(), decimal128IEEE(), decimal128Extended38())
        for (ctx in contexts) {
            ctx.context {
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