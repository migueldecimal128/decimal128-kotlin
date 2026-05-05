package com.decimal128.bidcodec

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test
import java.math.BigInteger

class TestDivByPow10 {

    val verbose = false

    data class TC(val strDividend: String, val pow10: Int)

    val tcs = arrayOf(
        // ── Trivial sanity ──
        TC("0", 1),
        TC("0", 4),
        TC("1", 1),
        TC("9", 1),
        TC("10", 1),
        TC("99", 2),
        TC("100", 2),
        TC("9999", 4),
        TC("10000", 4),

        // ── Each n with a small dividend ──
        TC("12345", 1),
        TC("12345", 2),
        TC("12345", 3),
        TC("12345", 4),

        // ── Remainder boundary: dividend just below / at / above 10^n ──
        TC("9", 1), TC("10", 1), TC("11", 1),
        TC("99", 2), TC("100", 2), TC("101", 2),
        TC("999", 3), TC("1000", 3), TC("1001", 3),
        TC("9999", 4), TC("10000", 4), TC("10001", 4),

        // ── Low bit = 0 vs low bit = 1 (exercises the 'b' channel) ──
        TC("12345678901234567890", 1),  // odd
        TC("12345678901234567891", 1),  // odd, different low bit pattern
        TC("12345678901234567892", 1),  // even

        // ── 64-bit boundary: dividend exactly fits in dLo ──
        TC("18446744073709551615", 1),  // 2^64 - 1
        TC("18446744073709551615", 4),
        TC("18446744073709551614", 1),
        TC("9223372036854775808", 1),   // 2^63 (high bit of dLo set)
        TC("9223372036854775807", 1),   // 2^63 - 1

        // ── Just above 64 bits (dHi = 1) ──
        TC("18446744073709551616", 1),  // 2^64
        TC("18446744073709551617", 1),  // 2^64 + 1
        TC("18446744073709551626", 1),  // 2^64 + 10

        // ── 112-bit values (one below the s = d>>1 ceiling) ──
        TC("5192296858534827628530496329220095", 4),  // 2^112 - 1
        TC("5192296858534827628530496329220096", 4),  // 2^112

        // ── 113-bit values (full input width) ──
        TC("10384593717069655257060992658440191", 1),  // 2^113 - 1
        TC("10384593717069655257060992658440191", 2),
        TC("10384593717069655257060992658440191", 3),
        TC("10384593717069655257060992658440191", 4),
        TC("10384593717069655257060992658440190", 4),  // 2^113 - 2 (even)

        // ── Powers of 2 near the top ──
        TC("5192296858534827628530496329220096", 1),   // 2^112
        TC("2596148429267413814265248164610048", 1),   // 2^111

        // ── Powers of 10 (clean quotients, zero remainders) ──
        TC("100000000000000000000000000000000", 4),   // 10^32
        TC("1000000000000000000000000000000000", 4),  // 10^33

        // ── Decimal128-ish coefficients (34 digits, the max for IEEE 754 decimal128) ──
        TC("9999999999999999999999999999999999", 1),  // 10^34 - 1
        TC("9999999999999999999999999999999999", 2),
        TC("9999999999999999999999999999999999", 3),
        TC("9999999999999999999999999999999999", 4),
        TC("1234567890123456789012345678901234", 4),
        TC("9876543210987654321098765432109876", 3),

        // ── Stress the 49-bit boundary in s = d>>1 ──
        // s with all 49 low bits set, hi63 also interesting
        TC("1125899906842623", 1),                    // 2^50 - 1 (after shift, lo49 = all ones)
        TC("1125899906842624", 1),                    // 2^50

        // ── Stress mid near its upper bound (forces large r1) ──
        // Crafted so hi63 % 5000 = 4999
        TC("4999", 4),                                 // hi63 = 0, r1 = 0; trivial
        TC("9999999999999999999999999999999999", 4),  // already covered, but worth re-listing for stress

        // ── Adjacent values to catch off-by-one ──
        TC("99999999999999999999", 4),
        TC("100000000000000000000", 4),
        TC("100000000000000000001", 4),

        // EXTEND to variable_shift for more range

        // ── Sanity for each n ──
        TC("0", 5), TC("0", 6), TC("0", 7), TC("0", 8), TC("0", 9),
        TC("1", 5), TC("1", 9),
        TC("12345678901234567890", 5),
        TC("12345678901234567890", 6),
        TC("12345678901234567890", 7),
        TC("12345678901234567890", 8),
        TC("12345678901234567890", 9),

        // ── Boundaries: dividend just below / at / above 10^n ──
        TC("99999", 5), TC("100000", 5), TC("100001", 5),
        TC("999999", 6), TC("1000000", 6), TC("1000001", 6),
        TC("9999999", 7), TC("10000000", 7), TC("10000001", 7),
        TC("99999999", 8), TC("100000000", 8), TC("100000001", 8),
        TC("999999999", 9), TC("1000000000", 9), TC("1000000001", 9),

        // ── Even / odd / low-n-bits patterns (exercises the 'b' channel for n bits) ──
        // For n=5, b is 5 bits (0..31)
        TC("12345678901234567890", 5),    // dLo low 5 bits = some pattern
        TC("12345678901234567919", 5),    // last digit shifted → different low 5 bits
        TC("12345678901234567904", 5),    // ends in ...0
        TC("12345678901234567935", 5),    // ends in ...5, low 5 bits all set if aligned

        // Specifically force the low n bits = all ones, for each n:
        TC("31", 5),                       // 0b11111 → b = 31
        TC("63", 6),                       // 0b111111
        TC("127", 7),
        TC("255", 8),
        TC("511", 9),

        // Specifically force the low n bits = 0:
        TC("32", 5),
        TC("64", 6),
        TC("128", 7),
        TC("256", 8),
        TC("512", 9),

        // ── 64-bit boundary cases (cross-limb shift stress for variable n) ──
        TC("18446744073709551615", 5),     // 2^64 - 1
        TC("18446744073709551615", 6),
        TC("18446744073709551615", 7),
        TC("18446744073709551615", 8),
        TC("18446744073709551615", 9),
        TC("18446744073709551616", 5),     // 2^64
        TC("18446744073709551616", 9),
        TC("9223372036854775808", 5),      // 2^63 (high bit of dLo set)
        TC("9223372036854775808", 9),

        // ── Full 113-bit inputs ──
        TC("10384593717069655257060992658440191", 5),  // 2^113 - 1
        TC("10384593717069655257060992658440191", 6),
        TC("10384593717069655257060992658440191", 7),
        TC("10384593717069655257060992658440191", 8),
        TC("10384593717069655257060992658440191", 9),
        TC("10384593717069655257060992658440190", 9),  // 2^113 - 2 (low bit clear)

        // Top-bit-of-dHi-set cases (49 bits used in dHi)
        TC("5192296858534827628530496329220095", 5),   // 2^112 - 1
        TC("5192296858534827628530496329220095", 9),
        TC("5192296858534827628530496329220096", 9),   // 2^112

        // ── Decimal128 coefficient max (34 nines, ~113 bits) ──
        TC("9999999999999999999999999999999999", 5),
        TC("9999999999999999999999999999999999", 6),
        TC("9999999999999999999999999999999999", 7),
        TC("9999999999999999999999999999999999", 8),
        TC("9999999999999999999999999999999999", 9),

        // Other realistic 34-digit coefficients
        TC("1234567890123456789012345678901234", 9),
        TC("9876543210987654321098765432109876", 7),
        TC("5000000000000000000000000000000000", 9),

        // ── Powers of 10 (exact division, remainder = 0) ──
        TC("100000", 5),                                       // 10^5 / 10^5 = 1
        TC("1000000000", 9),                                   // 10^9 / 10^9 = 1
        TC("100000000000000000000", 9),                        // 10^20
        TC("1000000000000000000000000000000000", 9),           // 10^33
        TC("9999999999999999999999999999999999", 9),           // 10^34 - 1, just below 10^34

        // ── Stress the worst-case mid (forces r1 = D - 1 = 5^n - 1) ──
        // For n=9, D = 1953125. We want hi63 % D = 1953124.
        // Easiest: pick s = D * something + (D-1). Then d = 2*s + b.
        // Quick way: hi63 itself = D - 1 (small dividend, but tests the boundary)
        TC("3905249", 9),     // 2 * (5^9 - 1) + 1 = 2 * 1953124 + 1 = 3906249... let me redo
        // Actually: s = 5^9 - 1 = 1953124, d = 2*s = 3906248 (b=0) or 2*s+1 = 3906249
        TC("3906248", 9),     // d such that s = 5^9 - 1, b = 0
        TC("3906249", 9),     // d such that s = 5^9 - 1, b = 1

        // ── Off-by-one neighborhood ──
        TC("999999999999999999999", 9),
        TC("1000000000000000000000", 9),
        TC("1000000000000000000001", 9),

        // ── 2^113 - 10^k for various k (ensures the quotient and remainder both span limbs) ──
        TC("10384593717069655257060992658430191", 5),   // 2^113 - 1 - 10000
        TC("10384593717069655257060992658430192", 5),
    )

    @Test
    fun testDivByPow10() {
        for (tc in tcs)
            test(tc)
    }

    val POW10 = longArrayOf(1L, 10L, 100L, 1000L, 10000L, 100000L,
        1000000L, 10000000L, 100000000L, 1000000000L)

    fun test(tc: TC) {
        if (verbose)
            println(tc)

        val pow10 = tc.pow10
        val biDividend = BigInteger(tc.strDividend)
        val biDivisor = BigInteger.valueOf(POW10[pow10])

        val dividendHi = (biDividend shr 64).toLong()
        val dividendLo = biDividend.toLong()

        val biQuotient = biDividend / biDivisor
        val biRemainder = biDividend % biDivisor

        val expectedQuotHi = (biQuotient shr 64).toLong()
        val expectedQuotLo = biQuotient.toLong()
        val expectedRemainder = biRemainder.toLong()

        val longs = LongArray(2)
        val remainder = u128DivModPow10(longs, dividendHi, dividendLo, pow10)

        assertEquals(expectedQuotHi, longs[0])
        assertEquals(expectedQuotLo, longs[1])
        assertEquals(expectedRemainder, remainder)


    }

    private fun divModPow10_fixed(quotLongs: LongArray, dwHi: Long, dwLo: Long, pow10: Int): Long {
        require(pow10 in 1..4)
        require(dwHi.countLeadingZeroBits() >= 15)

        val b = dwLo and 1L
        val sHi = dwHi ushr 1
        val sLo = (dwLo ushr 1) or (dwHi shl 63)

        val hi63 = (sHi shl 15) or (sLo ushr 49)
        val lo49 = sLo and ((1L shl 49) - 1)

        val d = POW10[pow10] ushr 1
        val q1  = hi63 / d
        val r1  = hi63 % d
        val mid = (r1 shl 49) or lo49
        val q0  = mid / d
        val r0  = mid % d

        val qLo = (q1 shl 49) + q0
        val qHi = q1 ushr 15
        val remainder = (r0 shl 1) or b

        quotLongs[0] = qHi
        quotLongs[1] = qLo
        return remainder
    }

    /**
     * Divides a 113-bit unsigned dividend by `10^pow10`, returning both quotient and remainder.
     *
     * The dividend is supplied as two 64-bit limbs forming the unsigned value
     * `(dwHi shl 64) or dwLo`, where `dwHi` holds the upper 49 bits and `dwLo`
     * holds the lower 64 bits. The quotient is written into `quotLongs` as
     * `quotLongs[0] = qHi` (upper limb) and `quotLongs[1] = qLo` (lower limb),
     * representing the unsigned 128-bit value `(qHi shl 64) or qLo`. The
     * remainder is returned directly and lies in `[0, 10^pow10)`.
     *
     * ## Algorithm
     *
     * Direct schoolbook division by `10^pow10` would require a divisor up to
     * `10^9`, which combined with a 49-bit low limb chunk would overflow the
     * `2^63` ceiling that signed `Long` division requires. To avoid this, the
     * routine factors `10^n = 2^n * 5^n` and divides in two stages:
     *
     * 1. Peel off the bottom `n` bits as `b = d mod 2^n` (free — just a mask).
     *    The shifted value `s = d / 2^n` is `(113 - n)` bits wide.
     * 2. Divide `s` by `5^n` using a two-limb schoolbook divide. The dividend
     *    `s` is split into a top 63-bit chunk `hi63` and a low `(50 - n)`-bit
     *    chunk `loK`. Two signed `Long` divisions by `5^n` produce the full
     *    quotient `q` and remainder `r`.
     * 3. Reconstruct: the original quotient is `q` (unchanged by the factoring),
     *    and the original remainder is `r * 2^n + b`, which fits in
     *    `[0, 10^n)` and is returned in a single `Long`.
     *
     * Because `5^n * 2^(50-n) < 2^63` for all `n` in `1..9`, every intermediate
     * value stays in non-negative `Long` range and signed `/` / `%` produce
     * correct unsigned results. The HotSpot JIT recognizes adjacent `a / b`
     * and `a % b` on the same operands and emits a single `idiv` instruction
     * per pair, so the routine costs two hardware divides plus a handful of
     * shifts and masks.
     *
     * ## Quotient reconstruction
     *
     * The two-limb assembly `qLo = (q1 shl k) + q0`, `qHi = q1 ushr (14 + n)`
     * relies on a bit-disjointness invariant: the low `k` bits of `q1 shl k`
     * are zero by construction, and `q0 < 2^k` follows from
     * `mid < 5^n * 2^k` and integer division. The two summands therefore
     * occupy disjoint bit ranges within the 64-bit register, so the addition
     * never carries into bit 64 and `qHi` is the exact upper limb of `q`.
     *
     * ## Range
     *
     * - Dividend: any unsigned value `< 2^113`, supplied as `(dwHi, dwLo)`
     *   with `dwHi in [0, 2^49)`.
     * - Scale: `pow10` in `1..9`. For larger scales, chain two calls
     *   (e.g., dividing by `10^15` = dividing by `10^9` then by `10^6`).
     * - Quotient: fits in 113 bits, returned as `(qHi, qLo)` with
     *   `qHi < 2^49`.
     * - Remainder: in `[0, 10^pow10)`, fits in a single positive `Long`.
     *
     * ## Performance
     *
     * On JVM with HotSpot C2: two paired `idiv` instructions plus straight-line
     * shifts and masks. No `unsignedMulHi`, no `ULong`, no magic constants,
     * no table beyond the existing `POW10` powers-of-ten table.
     *
     * @param quotLongs Output array of length ≥ 2. On return, `quotLongs[0]`
     *   holds the upper 64 bits of the quotient and `quotLongs[1]` holds the
     *   lower 64 bits.
     * @param dwHi Upper 49 bits of the 113-bit unsigned dividend. Must satisfy
     *   `0 <= dwHi < 2^49`.
     * @param dwLo Lower 64 bits of the dividend, treated as unsigned (any
     *   `Long` bit pattern is valid).
     * @param pow10 The exponent `n` such that the divisor is `10^n`. Must be
     *   in `1..9`.
     * @return The remainder `d mod 10^pow10`, a non-negative `Long` strictly
     *   less than `10^pow10`.
     *
     * @throws IllegalArgumentException if `pow10` is outside `1..9` or if
     *   `dwHi` is negative or has a bit set above bit 48.
     */
    fun u128DivModPow10(quotLongs: LongArray, dwHi: Long, dwLo: Long, pow10: Int): Long {
        require(pow10 in 1..9) { "n must be in 1..9: pow10=$pow10" }
        require(dwHi in 0 until (1L shl 49)) { "dHi exceeds 49 bits: dHi=$dwHi" }

        val mask = (1L shl pow10) - 1
        val b = dwLo and mask
        val sHi = dwHi ushr pow10
        val sLo = (dwLo ushr pow10) or (dwHi shl (64 - pow10))

        val k = 50 - pow10
        val maskK = (1L shl k) - 1
        val hi63 = (sHi shl (14 + pow10)) or (sLo ushr k)
        val loK = sLo and maskK

        val d = POW10[pow10] ushr pow10     // = 5^n
        val q1 = hi63 / d
        val r1 = hi63 % d
        val mid = (r1 shl k) or loK
        val q0 = mid / d
        val r0 = mid % d

        val qLo = (q1 shl k) + q0
        val qHi = q1 ushr (14 + pow10)
        val remainder = (r0 shl pow10) or b

        quotLongs[0] = qHi
        quotLongs[1] = qLo
        return remainder
    }
}