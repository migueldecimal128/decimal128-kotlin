package com.decimal128.decimal

import com.decimal128.decimal.DecContext.Companion.decimal128Extended38
import com.decimal128.decimal.DecContext.Companion.decimal128IEEE
import com.decimal128.decimal.DecContext.Companion.decimal128Kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class TestContextParams1 {

    val verbose = true

    @Test
    fun testContextA() {
        val contexts = arrayOf(decimal128Kotlin(), decimal128IEEE(), decimal128Extended38())
        for (ctx in contexts) {
            for (i in 0..4) {
                val rounding = DecRounding.fromValue(i)
                val decPrefs = DecPrefs().copy(printValuePlusSign = true, printStyle = DecPrefs.PrintStyle.ALWAYS_SCIENTIFIC)
                val ctxT = ctx.with(rounding).with(decPrefs)
                ctxT.eval {
                    val a = "1.0".toDecimal()
                    val b = "3.0".toDecimal()
                    val c = a / b
                    if (verbose)
                        println("ctx.precision:${ctx.precision} rounding:$rounding c:$c, ${c.precision()}")
                    assertEquals(ctx.precision, c.precision())
                }
            }
        }
    }
}