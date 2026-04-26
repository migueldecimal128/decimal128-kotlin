package com.decimal128.bidcodec

/**
 * Thin codec converting between BID128 binary representations and decimal
 * string form, per IEEE 754-2019.
 *
 * BID128 values are represented as a pair of 64-bit longs: `dw1` (high 64 bits)
 * and `dw0` (low 64 bits). This object provides no arithmetic, no field
 * extraction, and no value-type wrapper — just string conversion in both
 * directions.
 *
 * For the full decimal128 implementation (arithmetic, comparison, rounding,
 * etc.), see the `decimal128-kotlin` artifact.
 */
public object Decimal128BidStringCodec {

    /**
     * Convert a BID128 value to its decimal string representation.
     *
     * @param bid128Hi high 64 bits of the BID128 encoding
     * @param bid128Lo low 64 bits of the BID128 encoding
     * @return the decimal string form of the BID128 value
     */
    public fun toString(bid128Hi: Long, bid128Lo: Long): String {
        TODO("not yet implemented")
    }

    /**
     * Parse a decimal string into a BID128 value, writing the result into [dest].
     *
     * On success: `dest[0]` is set to `dw1` (high 64 bits), `dest[1]` is set
     * to `dw0` (low 64 bits), and this method returns `null`.
     *
     * On failure: `dest[0]` and `dest[1]` are set to zero, and this method
     * returns a human-readable error message describing the failure. The
     * exact wording of error messages is not part of the API contract and
     * may change between versions.
     *
     * @param dest a `LongArray` of size at least 2, into which the parsed
     *             BID128 value is written on success
     * @param str  the decimal string to parse
     * @return `null` on success, or a human-readable error message on failure
     * @throws IllegalArgumentException if `dest.size < 2`
     */
    public fun parseReturnError(dest: LongArray, str: String): String? {
        require(dest.size >= 2) { "dest must have size >= 2, was ${dest.size}" }
        TODO("not yet implemented")
    }

    // 34 nines
    private val coeffMaxHi = 0x0001ED09BEAD87C0L
    private val coeffMaxLo = 0x378D8E63FFFFFFFFL


    private fun bid128Decode(bid128Hi: Long, bid128Lo: Long): String {
        // IEEE754-2019 Table 3.6-Decimal2 Interchange format parameters -- p 23
        val k = 128 // storage width in bits
        val p = 34 // precision in digits
        val emax = 6144
        val bias = 6176
        val w5 = 17 // w+5, combination field width in bits
        val t = 110 // trailing significand field width in bits
        verify { 1 + w5 + t == k }

        // 1 + 17 + 110 == 128
        // 1 bit for the sign
        val sign = bid128Hi < 0L
        // w5 bit combination field ... for bid128 0x1FFFF
        val combination = (bid128Hi shr (t - 64)).toInt() and 0x1FFFF
        val coeffTHi = bid128Hi and ((1L shl t) - 1L)

        val coeffHi: Long
        val qExp: Int
        when {
            // if the top 2 bits are not 0b11
            combination shr (w5 - 2) != 0b11 -> {
                //  IEEE754-2019 3.5.2 c) 2) i) -- p 21
                //   If G0 and G1 together are one of 00, 01, or 10, then the biased
                //   exponent E is formed from G0 through Gw+1 and the significand
                //   is formed from bits Gw+2 through the end of the encoding (including T).
                //
                // w+5=13 w=8 Gw+1=G9 Gw+2=G10
                // the top 10 bits of the combination field represent the biased exponent ...
                val biasedExponentE = combination shr 3
                qExp = biasedExponentE - bias
                // ... and the bottom 3 bits are the most significant bits of the significand
                // coeff/significant is 3 + 50 == 53 ... but that's not all ... see below
                coeffHi = (combination and 0x07).toLong()
            }

            combination shr (w5 - 4) != 0b1111 -> {
                // IEEE754-2019 3.5.2 c) 2) ii) -- p 21
                //  If G0 and G1 together are 11 and G2 and G3 together are one
                //  of 00, 01, or 10, then the biased exponent E is formed from
                //  G2 through Gw+3 and the significand is formed by prefixing
                //  the 4 bits (8 + Gw+4) to T.
                //
                // w+5=13 w=8 Gw+3=G11 Gw+4=G12
                //
                val biasedExponentE = (combination ushr 1) and ((1 shl (w5 - 5 + 2)) - 1)
                qExp = biasedExponentE - bias
                coeffHi = 8L + (combination and 1).toLong()
            }
            // if the top 5 bits are 0b11110 then Infinity
            (combination shr (w5 - 5)) == 0b11110 -> {
                return infString(sign)
            }
            // if the top 5 bits are 0x11111 then NaN
            (combination shr (w5 - 5)) == 0b11111 -> {
                // with the next bit determining signaling NaN
                val payloadHi = coeffTHi
                val payloadLo = bid128Lo
                val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
                return nanString(sign, isSignaling, payloadHi, payloadLo)
            }

            else -> {
                // all possible cases were covered above
                throw IllegalStateException()
            }
        }
        val dw1 = ((coeffHi shl (t - 64)) or coeffTHi)
        val dw0 = bid128Lo
        // IEEE754-2019 3.5.2 p21
        //  If the value exceeds the maximum, the significand c is
        //  non-canonical and the value used for c is zero.
        if ((dw1 or dw0) == 0L ||
            unsignedLT(coeffMaxHi, dw1) || dw1 == coeffMaxHi && unsignedLT(coeffMaxLo, dw0)) {
                return zerString(sign, qExp)
            }
        return fnzString(sign, qExp, dw1, dw0)
    }

    private fun infString(sign: Boolean) =
        if (sign) "-Infinity" else "Infinity"

    // 33 nines
    private val payloadMaxHi = 0x0000314DC6448D93L
    private val payloadMaxLo = 0x38C15B09FFFFFFFFL

    private fun nanString(sign: Boolean, isSignaling: Boolean, payloadHi: Long, payloadLo: Long): String {
        val base =
            if (!isSignaling)
                if (sign) "-NaN" else "NaN"
            else
                if (sign) "-sNaN" else "sNaN"
        if ((payloadHi or payloadLo) == 0L ||
            // non canonical IEEE 754-2019 3.5.2 c) 2)
            unsignedLT(payloadMaxHi, payloadHi) ||
            payloadHi == payloadMaxHi && unsignedLT(payloadMaxLo, payloadLo))
            return base
        if (payloadHi == 0L)
            return base + payloadLo.toString()
        error("large NaN payloads not yet implemented")
    }

    private fun unsignedLT(dwA: Long, dwB: Long): Boolean =
        (dwA xor Long.MAX_VALUE) < (dwB xor Long.MAX_VALUE)

    private val ZEROS = arrayOf(
        "0", "0.0", "0.00", "0.000", "0.0000", "0.00000", "0.000000", "",
        "-0", "-0.0", "-0.00", "-0.000", "-0.0000", "-0.00000", "-0.000000", "",
    )

    private fun zerString(sign: Boolean, qExp: Int): String {
        if (qExp <= 0 && qExp >= -6) {
            val signBit = if (sign) 1 else 0
            val index = -qExp + (signBit shl 3)
            return ZEROS[index and 0x0F]
        }
        val base = if (sign) "-0E" else "0E"
        return base + qExp.toString()
    }

    private fun fnzString(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): String {
        return when {
            qExp == 0 -> fnzToIntegerString(sign, dw1, dw0)
            qExp <= 0 && qExp >= -6 -> fnzToDecimalPointString(sign, qExp, dw1, dw0)
            else -> fnzToNormalizedScientificString(sign, qExp, dw1, dw0)
        }
    }

    private fun fnzToIntegerString(sign: Boolean, dw1: Long, dw0: Long): String {
        if (dw1 == 0L) {
            if (dw0 >= 0L)
                return (if (sign) -dw0 else dw0).toString()
            val base = dw0.toULong().toString()
            return if (sign) "-$base" else base
        }
        error("large integer coefficients not yet implemented")
    }

    private fun fnzToDecimalPointString(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): String {
        TODO()
    }

    private fun fnzToNormalizedScientificString(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): String {
        TODO()
    }

}
