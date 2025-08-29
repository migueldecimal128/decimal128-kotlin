package com.decimal128.decimal

object Decimal128BidSerDe {

    fun encodeLittleEndianBid128(d: Decimal, littleEndianLongs: LongArray): LongArray {
        require(littleEndianLongs.size == 2)
        val bidDecimal128Hi = encodeBid128Hi(d)
        val bidDecimal128Lo = d.dw0
        littleEndianLongs[0] = bidDecimal128Lo
        littleEndianLongs[1] = bidDecimal128Hi
        return littleEndianLongs
    }

    fun encodeLittleEndianBid128(d: Decimal, littleEndianBytes: ByteArray): ByteArray {
        require(littleEndianBytes.size == 16)
        val bidDecimal128Hi = encodeBid128Hi(d)
        val bidDecimal128Lo = d.dw0
        for (i in 0..7) {
            littleEndianBytes[    i] = (bidDecimal128Lo ushr (i shl 3)).toByte()
            littleEndianBytes[8 + i] = (bidDecimal128Hi ushr (i shl 3)).toByte()
        }
        return littleEndianBytes
    }

    fun encodeBid128Hi(d: Decimal): Long {
        val decimal128 = DecimalFormat.DECIMAL_128
        val sign = if (d.sign) 1L shl 63 else 0L
        val qExp = d.qExp
        val significand110Hi = d.dw1 and ((1L shl (110 - 64)) - 1L)
        val combinationField = when {
            qExp < MIN_SPECIAL_VALUE -> {
                require(qExp in decimal128.qTiny..decimal128.qMax)
                val biasedQExp = qExp - decimal128.qTiny // remember qTiny is negative
                check((biasedQExp and 0x3000) != 0x3000)
                val mostSignificant3 = (d.dw1 ushr (110 - 64)).toInt() and 0x07
                ((biasedQExp shl 3) or mostSignificant3).toLong() shl 46
            }
            qExp == NON_FINITE_INF  -> 0b11110L shl 58
            qExp == NON_FINITE_QNAN -> 0b111110L shl 57
            qExp == NON_FINITE_SNAN -> 0b111111L shl 57
            else -> throw RuntimeException("unrecognized")
        }
        val bidDecimal128Hi = sign or combinationField or significand110Hi
        return bidDecimal128Hi
    }

    fun decodeLittleEndianBid128(littleEndianLongs: LongArray, d: Decimal): Decimal {
        require(littleEndianLongs.size == 2)
        return decodeBid128Longs(d, littleEndianLongs[1], littleEndianLongs[0])
    }

    fun decodeLittleEndianBid128(littleEndianBytes: ByteArray, d: Decimal): Decimal {
        require(littleEndianBytes.size == 16)
        var bid128Hi = 0L
        var bid128Lo = 0L
        for (i in 0..7) {
            bid128Lo = bid128Lo or ((littleEndianBytes[    i].toLong() and 0xFFL) shl (i shl 3))
            bid128Hi = bid128Hi or ((littleEndianBytes[8 + i].toLong() and 0xFFL) shl (i shl 3))
        }
    return decodeBid128Longs(d, bid128Hi, bid128Lo)
    }

    fun decodeBid128Longs(d: Decimal, bid128Hi: Long, bid128Lo: Long): Decimal {
        val decimal128 = DecimalFormat.DECIMAL_128
        val sign = bid128Hi < 0
        val combination = (bid128Hi ushr 46).toInt() and 0x1FFFF
        val significand110Hi = bid128Hi and ((1L shl (110 - 64)) - 1L)
        when {
            (combination and 0x18000) != 0x18000 -> {
                val biasedExponent = combination ushr 3
                val qExp = biasedExponent + decimal128.qTiny
                val mostSignificant3 = (combination and 0x07).toLong() shl (110 - 64)
                d.u256Set128(mostSignificant3 or significand110Hi, bid128Lo)
                // IEEE754-2019 3.5.2 p21
                //  If the value exceeds the maximum, the significand c is
                //  non-canonical and the value used for c is zero.
                if (d.digitLen > 34)
                    d.u256SetZero()
                d.qExp = qExp
                d.sign = sign
            }
            (combination and 0x1F000) == 0x1E000 ->
                d.setInfinite(sign)
            (combination and 0x1F000) == 0x1F000 ->
                d.setNaN((combination and 0x800) == 0x800, sign, significand110Hi, bid128Lo)
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