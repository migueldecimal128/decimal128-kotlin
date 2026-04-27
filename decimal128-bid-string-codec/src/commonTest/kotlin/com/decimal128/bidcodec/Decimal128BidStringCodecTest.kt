package com.decimal128.bidcodec

import kotlin.test.Test
import kotlin.test.assertFailsWith

class Decimal128BidStringCodecTest {

    /**
     * Smoke test: confirm the API surface is reachable from common code.
     * Each method currently throws NotImplementedError (TODO); this test
     * verifies the codec object is callable and the methods exist with the
     * expected signatures.
     */
    @Test
    fun apiIsReachable() {
        assertFailsWith<NotImplementedError> {
            val dest = LongArray(2)
            Decimal128BidStringCodec.parseReturnError(dest, "0")
        }
    }

    @Test
    fun parseReturnErrorRejectsTooSmallDest() {
        assertFailsWith<IllegalArgumentException> {
            Decimal128BidStringCodec.parseReturnError(LongArray(1), "0")
        }
    }
}
