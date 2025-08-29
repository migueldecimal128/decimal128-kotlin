package com.decimal128.decimal

object Decimal128DpdSerDe {

    fun decodeDpd128Longs(d: Decimal, dpd128Hi: Long, dpd128Lo: Long): Decimal {
        throw RuntimeException("not impl")
    }

    fun encodeDpdDeclet(n: Int): Int {
        require(n in 0..999) { "n must be in 0..999" }

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
    fun decodeDpdDeclet(declet: Int): Int {
        require(declet in 0..1023) { "declet must be 10 bits" }

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
