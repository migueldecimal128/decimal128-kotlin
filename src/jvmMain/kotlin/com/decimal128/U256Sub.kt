package com.decimal128


object U256Sub {

    fun u256SubUnscaled(z: U256, x: U256, y: U256) { // minuend - subtrahend
        check(z.u256HasValidLengths())
        check(x.u256HasValidLengths())
        check(y.u256HasValidLengths())
        check(x.u256UnscaledCompareTo(y) >= 0)
        val xBitLen = x.bitLen
        check(xBitLen >= y.bitLen)

        val d0 = x.dw0 - y.dw0
        if (xBitLen <= 64) {
            z.u256Set64(d0)
            return
        }
        val carry0 = if (unsignedCompare(d0, x.dw0) > 0) 1L else 0L

        if (xBitLen <= 128) {
            val d1 = x.dw1 - y.dw1 - carry0
            z.u256Set128(d1, d0)
            return
        }
        val d1a = x.dw1 - y.dw1
        val carry1a = if (unsignedCompare(d1a, x.dw1) > 0) 1L else 0L
        val d1 = d1a - carry0
        val carry1 = if (unsignedCompare(d1, d1a) > 0) 1L else carry1a

        if (xBitLen <= 192) {
            val d2 = x.dw2 - y.dw2 - carry1
            z.u256Set192(d2, d1, d0)
            return
        }
        val d2a = x.dw2 - y.dw2
        val carry2a = if (unsignedCompare(d2a, x.dw2) > 0) 1L else 0L
        val d2 = d2a - carry1
        val carry2 = if (unsignedCompare(d2, d2a) > 0) 1L else carry2a

        val d3a = x.dw3 - y.dw3
        val carry3a = if (unsignedCompare(d3a, x.dw3) > 0) 1L else 0L
        val d3 = d3a - carry2
        val carry3 = if (unsignedCompare(d3, d3a) > 0) 1L else carry3a
        check(carry3 == 0L)

        z.u256Set256(d3, d2, d1, d0)
    }

    fun u256SubScaled(z: U256, x: U256, scaleDelta: Int, y: U256) {
        check(scaleDelta > 0)
        check(scaleDelta <= 40)
        check(x.digitLen + scaleDelta < 79)

        check((x.dw3 or x.dw2) == 0L)
        check((y.dw3 or y.dw2) == 0L)
        check(x.u256HasValidLengths())
        check(y.u256HasValidLengths())
        check(z.u256HasValidLengths())

        check(y.u256ScaledCompareTo(x, scaleDelta) <= 0)

        U256Fms.u256FmsPow10(z, x, scaleDelta, y)
    }

    fun u256SubScaled(z: U256, x: U256, y: U256, scaleDelta: Int) {
        check(scaleDelta > 0)
        check(scaleDelta < 34)

        check((x.dw3 or x.dw2) == 0L)
        check((y.dw3 or y.dw2) == 0L)
        check(x.u256HasValidLengths())
        check(y.u256HasValidLengths())
        check(z.u256HasValidLengths())

        check(x.u256ScaledCompareTo(y, scaleDelta) >= 0)

        U256Fms.u256FmsPow10(z, x, y, scaleDelta)
    }

}