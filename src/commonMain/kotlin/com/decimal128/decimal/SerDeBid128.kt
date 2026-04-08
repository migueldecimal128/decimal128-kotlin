package com.decimal128.decimal

internal fun encodeLittleEndianBid128(d: MutDec, littleEndianLongs: LongArray): LongArray {
    require(littleEndianLongs.size == 2)
    val bidDecimal128Hi = encodeBid128Hi(d)
    val bidDecimal128Lo = d.dw0
    littleEndianLongs[0] = bidDecimal128Lo
    littleEndianLongs[1] = bidDecimal128Hi
    return littleEndianLongs
}

internal fun encodeLittleEndianBid128(d: MutDec, littleEndianBytes: ByteArray): ByteArray {
    require(littleEndianBytes.size == 16)
    val bidDecimal128Hi = encodeBid128Hi(d)
    val bidDecimal128Lo = d.dw0
    for (i in 0..7) {
        littleEndianBytes[i] = (bidDecimal128Lo ushr (i shl 3)).toByte()
        littleEndianBytes[8 + i] = (bidDecimal128Hi ushr (i shl 3)).toByte()
    }
    return littleEndianBytes
}

internal fun encodeBigEndianBid128(d: MutDec, bigEndianLongs: LongArray): LongArray {
    require(bigEndianLongs.size == 2)
    val bidDecimal128Hi = encodeBid128Hi(d)
    val bidDecimal128Lo = d.dw0
    bigEndianLongs[0] = bidDecimal128Hi
    bigEndianLongs[1] = bidDecimal128Lo
    return bigEndianLongs
}

internal fun encodeBigEndianBid128(d: MutDec, bigEndianBytes: ByteArray): ByteArray {
    require(bigEndianBytes.size == 16)
    val bidDecimal128Hi = encodeBid128Hi(d)
    val bidDecimal128Lo = d.dw0
    var shift = 56
    for (i in 0..7) {
        bigEndianBytes[i] = (bidDecimal128Hi ushr shift).toByte()
        bigEndianBytes[8 + i] = (bidDecimal128Lo ushr shift).toByte()
        shift -= 8
    }
    return bigEndianBytes
}

@Suppress("NOTHING_TO_INLINE")
private inline fun encodeSignAndGCombinationFieldBid128(type: Int, sign: Boolean, qExp: Int, mostSigBits4: Int): Long {
    require(mostSigBits4 in 0..9)
    val signBit = if (sign) 1L shl 63 else 0L
    val gCombinationField = when (stealTyp(type)) {
        STEAL_TYP_ZER,
        STEAL_TYP_FNZ -> {
            require(qExp in Q_TINY..Q_MAX)
            val biasedQExp = qExp - Q_TINY // remember qTiny is negative
            verify { (biasedQExp and 0x3000) != 0x3000 }
            if ((mostSigBits4 and 0x08) == 0)
                (biasedQExp shl 3) or mostSigBits4
            else
                0x18000 or (biasedQExp shl 1) or (mostSigBits4 and 1)
        }

        STEAL_TYP_INF -> 0b11110 shl 12
        STEAL_NAN_QNAN -> 0b111110 shl 11
        STEAL_NAN_SNAN -> 0b111111 shl 11
        else -> throw RuntimeException("unrecognized")
    }
    val signCombo = signBit or (gCombinationField.toLong() shl 46)
    return signCombo
}

internal fun encodeBid128Hi(d: MutDec): Long {
    val mostSignificant3 = (d.dw1 ushr (110 - 64)).toInt() and 0x07
    val steal = d.steal
    val signCombo = encodeSignAndGCombinationFieldBid128(stealTyp(steal),
        stealSignFlag(steal), stealQExp(steal), mostSignificant3)
    val significand110Hi = d.dw1 and ((1L shl (110 - 64)) - 1L)
    val bidDecimal128Hi = signCombo or significand110Hi
    return bidDecimal128Hi
}

internal fun decodeLittleEndianBid128(d: MutDec, littleEndianLongs: LongArray): MutDec {
    require(littleEndianLongs.size == 2)
    return decodeBid128Longs(d, littleEndianLongs[1], littleEndianLongs[0])
}

internal fun decodeLittleEndianBid128(d: MutDec, littleEndianBytes: ByteArray): MutDec {
    require(littleEndianBytes.size == 16)
    var bid128Hi = 0L
    var bid128Lo = 0L
    for (i in 0..7) {
        bid128Lo = bid128Lo or ((littleEndianBytes[i].toLong() and 0xFFL) shl (i shl 3))
        bid128Hi = bid128Hi or ((littleEndianBytes[8 + i].toLong() and 0xFFL) shl (i shl 3))
    }
    return decodeBid128Longs(d, bid128Hi, bid128Lo)
}

internal fun decodeBigEndianBid128(d: MutDec, bigEndianLongs: LongArray): MutDec {
    require(bigEndianLongs.size == 2)
    return decodeBid128Longs(d, bigEndianLongs[0], bigEndianLongs[1])
}

internal fun decodeBigEndianBid128(d: MutDec, bigEndianBytes: ByteArray): MutDec {
    require(bigEndianBytes.size == 16)
    var bidHi = 0L
    var bidLo = 0L
    var shift = 56
    for (i in 0..7) {
        bidHi = bidHi or ((bigEndianBytes[0 + i].toLong() and 0xFFL) shl shift)
        bidLo = bidLo or ((bigEndianBytes[8 + i].toLong() and 0xFFL) shl shift)
        shift -= 8
    }
    return decodeBid128Longs(d, bidHi, bidLo)
}

internal fun decodeBid128Longs(d: MutDec, bid128Hi: Long, bid128Lo: Long): MutDec {
    val sign = bid128Hi < 0
    val combination = (bid128Hi ushr 46).toInt() and 0x1FFFF
    val significand110Hi = bid128Hi and ((1L shl (110 - 64)) - 1L)
    when {
        (combination and 0x18000) != 0x18000 -> { // finite
            val biasedExponent = combination ushr 3
            val qExp = biasedExponent + Q_TINY
            val mostSignificant3 = (combination and 0x07).toLong() shl (110 - 64)
            d.c256Set128(mostSignificant3 or significand110Hi, bid128Lo)
            val digitLen = d.digitLen
            if (digitLen == 0 || digitLen > 34) {
                // IEEE754-2019 3.5.2 p21
                //  If the value exceeds the maximum, the significand c is
                //  non-canonical and the value used for c is zero.
                d.setZero(sign, qExp)
            } else {
                d.steal = stealEncodeFNZ(sign, qExp, stealPackedLengths(d.steal))
            }
        }

        (combination and 0x1F000) == 0x1E000 ->
            d.setInfinite(sign)

        (combination and 0x1F000) == 0x1F000 ->
            d.setNaN(sign, (combination and 0x800) == 0x800, significand110Hi, bid128Lo)

        else -> {
            // large-form finite pattern => non-canonical for decimal128:
            // E = bits [15:2] (G2..Gw+3), C = 0, keep sign S.
            val E = (combination ushr 1) and 0x3FFF   // 14 bits
            d.setZero(sign, E + Q_TINY)     // preserve sign & exponent
        }
    }
    return d
}
