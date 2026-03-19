package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.ONE
import com.decimal128.decimal.Decimal.Companion.ZERO
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class TestDecTrapHandlers {

    val verbose = false

    @Test
    fun TestInvalidOperation() {
        val z = ONE / ZERO
        assertTrue(z.isInfinite())

        val catchCtx = DecContext.decimal128Kotlin().withThrownException(DecException.DIVIDE_BY_ZERO)

        assertThrows<DivideByZeroException> {
            with (catchCtx) {
                val z1 = ONE / ZERO
            }
        }

        val sentinal99Ctx = catchCtx.withTrapHandler(
            DecTrapHandler { _ -> Decimal.from(99) },
            DecException.DIVIDE_BY_ZERO
        )

        with (sentinal99Ctx) {
            val z2 = ONE / ZERO
            assertTrue(z2 == Decimal.from(99))
        }
    }
}
