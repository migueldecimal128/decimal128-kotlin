package com.decimal128.decimal

import com.decimal128.decimal.DecEnv.Companion.DECIMAL128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestDecEnvOperators {

    @Test
    fun test1() {
        with(DECIMAL128) {
            val a = Decimal.from("2")
            val b = Decimal.from(3)
            val c = Decimal.from(4)
            val x = b * c
            val d = a + b * c
            println("a:$a + b:$b * c:$c = d:$d")
            assertEquals("14", "$d")
        }

        DECIMAL128.context {
            val e = Decimal.from(5)
            val f = Decimal.from(6)
            val g = Decimal.from(7)
            val h = (e + f) * g
            println("(e:$e + f:$f) * g:$g = h:$h")
            assertEquals("77", "$h")
        }
    }

}
