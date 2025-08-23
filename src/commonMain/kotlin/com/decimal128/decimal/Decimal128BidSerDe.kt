package com.decimal128.decimal

object Decimal128BidSerDe {

    fun toBidDecimal128(d: Decimal, longs: LongArray): LongArray {
        val decimal128 = DecimalFormat.DECIMAL_128
        val sign = if (d.sign) 1L shl 63 else 0L
        val qExp = d.qExp
        if (qExp < MIN_SPECIAL_VALUE) {
            require(qExp in decimal128.qTiny..decimal128.qMax)
            val biasedQExp = qExp - decimal128.qTiny // remember qTiny is negative
            check ((biasedQExp and 0x3000) != 0x3000)
            val significand110 = d.dw1 and ((1L shl (110 - 64)) - 1L)
            val mostSignificant3 = (d.dw1 ushr (110 - 64)).toInt() and 0x07
            val combinationField = (biasedQExp shl 3) or mostSignificant3
            longs[1] = d.dw0
            longs[0] = sign or (combinationField.toLong() shl 46) or significand110
        } else {
            longs[1] = d.dw0
            val combination = when {
                qExp == NON_FINITE_INF -> {
                    longs[1] = 0L
                    0b11110L shl 58
                }

                qExp == NON_FINITE_QNAN -> {
                    0b111110L shl 57
                }

                qExp == NON_FINITE_SNAN -> {
                    0b111111L shl 57
                }

                else -> throw RuntimeException("unrecognized")
            }
            longs[0] = sign or combination
        }
        return longs
    }

    fun fromBidDecimal128(longs: LongArray, d: Decimal, ctx: DecimalContext): Decimal {
        val decimal128 = DecimalFormat.DECIMAL_128
        val dwLo = longs[1]
        val dwHi = longs[0]
        val sign = dwHi < 0
        val combination = (dwHi ushr 46).toInt() and 0x1FFFF
        when {
            (combination and 0x18000) != 0x18000 -> {
                val biasedExponent = combination ushr 3
                val qExp = biasedExponent + decimal128.qTiny
                val mostSignificant3 = (combination and 0x07).toLong() shl (110 - 64)
                val significand110 = dwHi and ((1L shl (110 - 64)) - 1L)
                d.u256Set128(mostSignificant3 or significand110, dwLo)
                d.qExp = qExp
                d.sign = sign
            }
            (combination and 0x1F000) == 0x1E000 -> d.setInfinite(sign)
            (combination and 0x1F800) == 0x1F000 -> d.setNaN(dwLo.toInt(), ctx)
            (combination and 0x1F800) == 0x1F800 -> d.setSNaN(dwLo.toInt(), ctx)
            else -> {
                // large-form finite pattern ⇒ non-canonical for decimal128:
                // E = bits [15:2] (G2..Gw+3), C := 0, keep sign S.
                val E = (combination ushr 1) and 0x3FFF   // 14 bits
                d.u256SetZero()
                d.qExp = E + decimal128.qTiny             // preserve exponent
                d.sign = sign                             // preserve sign (±0)
            }
        }
        return d
    }

}