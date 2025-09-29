package com.decimal128.decimal

private const val TEN_POW_15 = 1_000_000_000_000_000L
private const val TEN_POW_18 = 1_000_000_000_000_000_000L
private const val EIGHTEEN_NINES = TEN_POW_18 - 1L

object SerDeDpd128 {

    fun encodeBigEndianDpd128(bigEndianLongs: LongArray, d: MutDec): LongArray {
        require (d.digitLen <= 34)
        require (bigEndianLongs.size == 2)
        encodeDpd128(d, true, bigEndianLongs, null)
        return bigEndianLongs
    }

    fun encodeBigEndianDpd128(bigEndianBytes: ByteArray, d: MutDec): ByteArray {
        require (d.digitLen <= 34)
        require(bigEndianBytes.size == 16)
        encodeDpd128(d, true, null, bigEndianBytes)
        return bigEndianBytes
    }

    fun encodeLittleEndianDpd128(littleEndianLongs: LongArray, d: MutDec): LongArray {
        require (d.digitLen <= 34)
        require(littleEndianLongs.size == 2)
        encodeDpd128(d, false, littleEndianLongs, null)
        return littleEndianLongs
    }

    fun encodeLittleEndianDpd128(littleEndianBytes: ByteArray, d: MutDec): ByteArray {
        require (d.digitLen <= 34)
        require(littleEndianBytes.size == 16)
        encodeDpd128(d, false, null, littleEndianBytes)
        return littleEndianBytes
    }

    fun decodeBigEndianDpd128(d: MutDec, bigEndianLongs: LongArray): MutDec {
        require (d.digitLen <= 34)
        require (bigEndianLongs.size == 2)
        decodeDpd128Longs(d, bigEndianLongs[0], bigEndianLongs[1])
        return d
    }

    fun decodeBigEndianDpd128(d: MutDec, bigEndianBytes: ByteArray): MutDec {
        require (d.digitLen <= 34)
        require(bigEndianBytes.size == 16)
        var dpdHi = 0L
        var dpdLo = 0L
        var shift = 56
        for (i in 0..7) {
            dpdHi = dpdHi or ((bigEndianBytes[0 + i].toLong() and 0xFFL) shl shift)
            dpdLo = dpdLo or ((bigEndianBytes[8 + i].toLong() and 0xFFL) shl shift)
            shift -= 8
        }
        decodeDpd128Longs(d, dpdHi, dpdLo)
        return d
    }

    fun decodeLittleEndianDpd128(d: MutDec, littleEndianLongs: LongArray): MutDec {
        require (d.digitLen <= 34)
        require(littleEndianLongs.size == 2)
        decodeDpd128Longs(d, littleEndianLongs[1], littleEndianLongs[0])
        return d
    }

    fun decodeLittleEndianDpd128(d: MutDec, littleEndianBytes: ByteArray): MutDec {
        require (d.digitLen <= 34)
        require(littleEndianBytes.size == 16)
        var dpdHi = 0L
        var dpdLo = 0L
        var shift = 0
        for (i in 0..7) {
            dpdLo = dpdLo or ((littleEndianBytes[0 + i].toLong() and 0xFFL) shl shift)
            dpdHi = dpdHi or ((littleEndianBytes[8 + i].toLong() and 0xFFL) shl shift)
            shift += 8
        }
        decodeDpd128Longs(d, dpdHi, dpdLo)
        return d
    }

    fun decodeDpd128Longs(d: MutDec, dpd128Hi: Long, dpd128Lo: Long): MutDec {
        val decimal128 = DecFormat.DECIMAL_128
        val sign = dpd128Hi < 0
        val combination = (dpd128Hi ushr 46).toInt() and 0x1FFFF
        val combinationHi2 = combination ushr 15
        val combinationHi4 = combination ushr 13
        val significand110Hi = dpd128Hi and ((1L shl (110 - 64)) - 1L)
        when {
            combinationHi4 != 0x0F -> {
                var biasedExpHi2 = combinationHi2
                var mostSigBcd4 = (combination ushr 12) and 0x07
                if (combinationHi2 == 0x03) {
                    biasedExpHi2 = combinationHi4 and 0x03
                    mostSigBcd4 = 0x08 or ((combination ushr 12) and 1)
                }
                val biasedExponent = (biasedExpHi2 shl 12) or (combination and 0xFFF)
                val qExp = biasedExponent + decimal128.qTiny
                val decletsHi6 = (mostSigBcd4.toLong() shl 50) or (significand110Hi shl 4) or (dpd128Lo ushr 60)
                val decletsLo6 = dpd128Lo and ((1L shl 60) - 1L)
                val binHi = binFromDeclets7(decletsHi6)
                val binLo = binFromDeclets7(decletsLo6)
                if (binHi != 0L) {
                    d.u256Set64(binHi)
                    d.u256SetFmaPow10(d, 18, binLo)
                } else {
                    d.u256Set64(binLo)
                }
                d.qExp = qExp
                d.sign = sign
            }
            (combination and 0x1F000) == 0x1E000 ->
                d.setInfinite(sign)
            (combination and 0x1F000) == 0x1F000 -> {
                var payloadHi = 0L
                var payloadLo = 0L
                when {
                    significand110Hi == 0L && dpd128Lo != 0L -> {
                        payloadLo = binFromDeclets7(dpd128Lo)
                    }
                    significand110Hi != 0L -> {
                        val decletsHi5 = (significand110Hi shl 4) or (dpd128Lo ushr 60)
                        val decletsLo6 = dpd128Lo and ((1L shl 60) - 1L)
                        val binHi = binFromDeclets7(decletsHi5)
                        val binLo = binFromDeclets7(decletsLo6)
                        d.u256Set64(binHi)
                        d.u256SetFmaPow10(d, 18, binLo)
                        payloadHi = d.dw1
                        payloadLo = d.dw0
                    }
                }
                d.setNaN((combination and 0x800) == 0x800, sign, payloadHi, payloadLo)
            }
            else -> {
                // large-form finite pattern => non-canonical for decimal128:
                // E = bits [15:2] (G2..Gw+3), C = 0, keep sign S.
                val E = (combination ushr 1) and 0x3FFF   // 14 bits
                d.u256SetZero()
                d.qExp = E + decimal128.qTiny             // preserve exponent
                d.sign = sign                             // preserve sign (±0)
            }
        }
        return d
    }

    private fun encodeDpd128(d: MutDec, isBigEndian: Boolean, longs: LongArray?, bytes: ByteArray?) {
        check (d.digitLen <= 34)
        var mostSigBcd4 = 0
        var declets5Hi = 0L
        var binLo = d.dw0
        if (d.digitLen > 18) {
            val q = U256()
            val r = q.u256SetDivModX64(d, TEN_POW_18)
            binLo = r
            var binHi = q.dw0
            if (q.digitLen > 15) {
                binHi = q.dw0 % TEN_POW_15
                mostSigBcd4 = (q.dw0 / TEN_POW_15).toInt()
                check(mostSigBcd4 in 0..9)
            }
            check(binHi in 0L..<TEN_POW_15)
            declets5Hi = declets6FromBin(binHi)
        }
        val declets6Lo = declets6FromBin(binLo)
        val signCombo = encodeSignAndGCombinationFieldDpd128(d.sign, d.qExp, mostSigBcd4)
        val dpdLo = (declets5Hi shl 60) or declets6Lo
        val dpdHi = signCombo or (declets5Hi ushr 4)
        if (longs != null) {
            val endianMask = if (isBigEndian) 0 else 1
            longs[0 xor endianMask] = dpdHi
            longs[1 xor endianMask] = dpdLo
        } else if (bytes != null) {
            val endianMask = if (isBigEndian) 0 else 15
            var shift = 56
            for (i in 0..7) {
                bytes[(0 + i) xor endianMask] = (dpdHi ushr shift).toByte()
                bytes[(8 + i) xor endianMask] = (dpdLo ushr shift).toByte()
                shift -= 8
            }
        } else {
            throw IllegalStateException()
        }
    }

    fun encodeSignAndGCombinationFieldDpd128(sign: Boolean, qExp: Int, mostSigBcd4: Int) : Long {
        require (mostSigBcd4 in 0..9)
        val decimal128 = DecFormat.DECIMAL_128
        val signBit = if (sign) 1L shl 63 else 0L
        val gCombinationField = when {
            qExp < MIN_SPECIAL_VALUE -> {
                require(qExp in decimal128.qTiny..decimal128.qMax)
                val biasedQExp = qExp - decimal128.qTiny // remember qTiny is negative
                val biasedQExpLo12 = biasedQExp and 0xFFF
                val biasedQExpHi2 = biasedQExp ushr 12
                check (biasedQExpHi2 in 0..2)
                (if (mostSigBcd4 >= 0x08)
                    (0b11 shl 15) or (biasedQExpHi2 shl 13) or ((mostSigBcd4 and 1) shl 12)
                else
                    (biasedQExpHi2 shl 15) or (mostSigBcd4 shl 12)
                        ) or biasedQExpLo12
            }
            qExp == NON_FINITE_INF  -> 0b11110 shl 12
            qExp == NON_FINITE_QNAN -> 0b111110 shl 11
            qExp == NON_FINITE_SNAN -> 0b111111 shl 11
            else -> throw RuntimeException("unrecognized")
        }
        val signCombo = signBit or (gCombinationField.toLong() shl 46)
        return signCombo
    }

    fun declets6FromBin(bin: Long): Long {
        require (bin in 0L..EIGHTEEN_NINES)
        var t = bin
        var declets6 = 0L
        var shift = 0
        while (t != 0L) {
            val q = unsignedMulHi(t, 0x020C49BA5E353F7DL) ushr 3
            val r = t - (q * 1000L)
            val declet = encodeDpdDeclet(r)
            declets6 = declets6 or (declet shl shift)
            shift += 10
            t = q
        }
        return declets6
    }

    fun binFromDeclets6(declets6: Long): Long {
        if (declets6 == 0L)
            return 0L
        var bin = decodeDpdDeclet(declets6 ushr 50)
        for (shift in 40 downTo 0 step 10)
            bin = (bin * 1000L) + decodeDpdDeclet((declets6 ushr shift) and 0x3FFL)
        return bin
    }

    // there could be up to 7 declets with the top declet having 4 bits
    fun binFromDeclets7(declets7: Long): Long {
        var bin = 0L
        var decletsT = declets7
        var multiplier = 1L
        while (decletsT != 0L) {
            val declet = decletsT and 0x3FFL
            val bin1 = decodeDpdDeclet(declet)
            bin += bin1 * multiplier
            multiplier *= 1000L
            decletsT = decletsT ushr 10
        }
        return bin
    }

    fun encodeDpdDeclet(n: Long): Long {
        require(n in 0..999) { "n must be in 0..999" }

        if (n == 0L)
            return 0L

        // reciprocal-multiply digit extraction (optimized for n <= 999)
        val q10  = (n * 205) ushr 11      // n / 10
        val q100 = (n * 41)  ushr 12      // n / 100

        val d2 = q100
        val d1 = q10 - (q100 * 10)
        val d0 = n - (q10 * 10)

        // BCD bits (MSB→LSB: 8,4,2,1)
        val a = (d2 shr 3) and 1
        val b = (d2 shr 2) and 1
        val c = (d2 shr 1) and 1
        val d = d2 and 1

        val e = (d1 shr 3) and 1
        val f = (d1 shr 2) and 1
        val g = (d1 shr 1) and 1
        val h = d1 and 1

        val i = (d0 shr 3) and 1
        val j = (d0 shr 2) and 1
        val k = (d0 shr 1) and 1
        val m = d0 and 1

        val na = a xor 1
        val ne = e xor 1
        val ni = i xor 1

        val p = b or ((a and j) or ((a and f) and i))
        val q = c or ((a and k) or ((a and g) and i))
        val r = d
        val s = (f and (na or ni)) or ((na and (e and j)) or (e and i))
        val t = g or ((na and (e and k)) or (a and i))
        val u = h
        val v = a or (e or i)
        val w = a or ((e and i) or (ne and j))
        val x = e or ((a and i) or (na and k))
        val y = m

        return (p shl 9) or (q shl 8) or (r shl 7) or (s shl 6) or
                (t shl 5) or (u shl 4) or (v shl 3) or (w shl 2) or
                (x shl 1) or y
    }

    /**
     * Decode a 10-bit IEEE-754 DPD declet (bits 9..0) into [0,999], using pure booleans.
     * Mapping: bit9..bit0 → p q r s t u v w x y
     * Equations: Cowlishaw's dpd2bcd (handles canonical + non-canonical).
     */
    fun decodeDpdDeclet(declet: Long): Long {
        require(declet in 0..1023) { "declet must be 10 bits" }

        if (declet == 0L)
            return 0L

        // Unpack to bits p..y
        val p = (declet ushr 9) and 1
        val q = (declet ushr 8) and 1
        val r = (declet ushr 7) and 1
        val s = (declet ushr 6) and 1
        val t = (declet ushr 5) and 1
        val u = (declet ushr 4) and 1
        val v = (declet ushr 3) and 1
        val w = (declet ushr 2) and 1
        val x = (declet ushr 1) and 1
        val y =  declet         and 1

        val ns = s xor 1
        val nx = x xor 1
        val nv = v xor 1
        val nw = w xor 1
        val nt = t xor 1

        val a = (v and w) and (ns or t or nx)
        val b = p and (nv or nw or (s and t.inv() and x))
        val c = q and (nv or nw or (s and t.inv() and x))
        val d = r

        val e = v and ((nw and x) or (t.inv() and x) or (s and x))
        val f = (s and (nv or nx)) or (p and ns and t and v and w and x)
        val g = (t and (nv or nx)) or (q and ns and t and w)
        val h = u

        val i = v and ((nw and nx) or (w and x and (s or t)))
        val j = ((nv and w)
                or (s and v and nw and x)
                or (p and w and (nx or (ns and t.inv()))))
        val k = (nv and x) or (t and nw and x) or (q and v and w and (nx or (ns and nt)))
        val m = y

        // Pack BCD digit bits → three digits
        val d2 = (a shl 3) or (b shl 2) or (c shl 1) or d
        val d1 = (e shl 3) or (f shl 2) or (g shl 1) or h
        val d0 = (i shl 3) or (j shl 2) or (k shl 1) or m

        require(d2 in 0..9) { "d2 out of range: $d2" }
        require(d1 in 0..9) { "d1 out of range: $d1" }
        require(d0 in 0..9) { "d0 out of range: $d0" }

        return d2 * 100 + d1 * 10 + d0
    }

}
