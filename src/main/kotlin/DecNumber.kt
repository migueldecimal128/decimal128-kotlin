package com.decimal128
import com.decimal128.*
import java.nio.charset.StandardCharsets

typealias Bits = Int

val DECNEG:  Bits = 1 shl 31        /* Sign; 1=negative, 0=positive or zero */
val DECINF:  Bits = 0x40000000      /* 1=Infinity                           */
val DECNAN:  Bits = 0x20000000      /* 1=NaN                                */
val DECSNAN: Bits = 0x10000000      /* 1=sNaN                               */
/* The remaining bits are reserved; they must be 0                  */
val DECSPECIAL = (DECINF or DECNAN or DECSNAN) /* any special value     */
val DECNEG_OR_SPECIAL = (DECNEG or DECINF or DECNAN or DECSNAN) /* any special value     */

val DECDPUN = 8         /* DECimal Digits Per UNit [must be >0  */
                        /* and <10; 3 or powers of 2 are best]. */
val DECDPUNMAX = 99999999
val DEC1E8 = 100000000
val DEC1E8L = 100000000L
val DEC1E16L = 10000000000000000L
val DEC1E8u = DEC1E8.toUInt()
val DEC1E8uL = DEC1E8.toULong()
val DEC1E16uL = 10000000000000000uL
val D2UTABLE = intArrayOf(0,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,3,3,3,3,3,
    3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,6,6,6,
    6,6,6,6,6,7)

val DECNUMUNITS = 3

val DECNUMMAXP = 999999999  /* maximum precision code can handle  */
val DECNUMMAXE = 999999999  /* maximum adjusted exponent ditto    */
val DECNUMMINE = -999999999 /* minimum adjusted exponent ditto    */


public val DECPOWERS: IntArray = intArrayOf(
    1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
)

inline fun divModPow10(u: Int, pow10: Int) : Pair<Int, Int> {
    val divisor = DECPOWERS[pow10]
    val q = u / divisor
    val r = u % divisor
    return Pair(q, r)
}

private val INFINITY_BYTES = ("Infinity".toByteArray(StandardCharsets.UTF_8))

fun int32DivMod1e8(n: Int) : Pair<Int, Int> {
    val quotient = n / DEC1E8
    val remainder = n % DEC1E8
    return Pair(quotient, remainder)
}

fun int64DivMod1e8(l: Long) : Pair<Long, Int> {
    val quotient = l / DEC1E8L
    val remainder = l % DEC1E8L
    return Pair(quotient, remainder.toInt())
}

fun uint64DivMod1e8(ul: ULong) : Pair<Long, Int> {
    val quotient = ul / DEC1E8uL
    val remainder = ul % DEC1E8uL
    return Pair(quotient.toLong(), remainder.toInt())
}

inline fun D2U(digits: Int) : Int {
    return (digits + 7) ushr 3
}

inline fun msuDigitCount(d: Int) : Int {
    return ((d - 1) and 0x07) + 1
}

class DecNumber {
    /* The data structure... */
    var digits = 0      /* Count of digits in the coefficient; >0    */
    var exponent = 0;    /* Unadjusted exponent, unbiased, in         */

    /* range: -1999999997 through 999999999      */
    var bits = 0;        /* Indicator bits (see above)                */

    /* Coefficient, from least significant unit  */
    var lsu = IntArray(DECNUMUNITS);

    private fun ensureDigitCountWipe(minDigitCount: Int) {
        val minUnitCount = D2U(minDigitCount)
        if (lsu.size < minUnitCount)
            lsu = IntArray(minUnitCount)
    }

    private val BADINT: Int = 0x80000000.toInt()    // most-negative Int; error indicator

    fun isZero(): Boolean {
        // allows both + and - zero
        return (digits - 1) or lsu[0] or (bits and DECSPECIAL) == 0
    }

    fun isNegative() : Boolean {
        return bits < 0;
    }

    fun isSpecial() : Boolean {
        return (bits and DECSPECIAL) != 0
    }

    fun isInfinity() : Boolean {
        return (bits and DECINF) != 0
    }

    fun isSNAN() : Boolean {
        return (bits and DECSNAN) != 0
    }

    fun isNAN() : Boolean {
        return (bits and DECNAN) != 0
    }

    // if zero then return zero
    // if nonzero then return 0xFFFFFFFF
    // both +/- 0
    fun zeroOrFFFFFFFF(): Int {
        val z = (digits - 1) or lsu[0] or (bits and DECSPECIAL)
        val extended = -(z or -z) shr 31
        return extended
    }

    inline fun signum() : Int {
        val z = (digits - 1) or lsu[0]
        val zeroOrFFFFFFFF = z or -z
        val signExtended = bits shr 31
        val sig = signExtended and zeroOrFFFFFFFF
        return sig
    }

    /* ================================================================== */
    /* Conversions                                                        */
    /* ================================================================== */

    /* ------------------------------------------------------------------ */
    /* from-int32 -- conversion from Int or uInt                          */
    /*                                                                    */
    /*  dn is the decNumber to receive the integer                        */
    /*  in or uin is the integer to be converted                          */
    /*  returns dn                                                        */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    fun fromInt32(n: Int): DecNumber {
        zero()
        var t = n;
        if (n < 0) {
            if (n == Int.MIN_VALUE)
                return fromInt64(n.toLong())
            t = -n;
            bits = DECNEG;            // sign needed
        }
        val (d1, d0) = int32DivMod1e8(t)
        lsu[0] = d0
        lsu[1] = d1
        digits = getDigits(lsu, 2);
        return this
    }

    fun fromUInt32(u: UInt): DecNumber {
        return fromInt64(u.toLong())
    }

    fun fromInt64(l: Long): DecNumber {
        zero()
        var t = l
        if (l < 0L) {
            bits = DECNEG
            if (l == Long.MIN_VALUE) {
                val negInfinity = Long.MIN_VALUE.toULong()
                lsu[0] = (negInfinity % DEC1E8uL).toInt()
                lsu[1] = ((negInfinity / DEC1E8uL) % DEC1E8uL).toInt()
                lsu[2] = ((negInfinity / DEC1E8uL) / DEC1E8uL).toInt()
                digits = 19
                return this;
            }
            t = -l
        }
        val q = t / DEC1E16L
        val r = t % DEC1E16L
        lsu[0] = (r % DEC1E8L).toInt()
        lsu[1] = (r / DEC1E8L).toInt()
        lsu[2] = q.toInt()
        digits = getDigits(lsu, 3);
        return this
    } // decNumberFromInt64

    fun fromUInt64(ul: ULong): DecNumber {
        if ((ul and (1uL shl 63)) == 0uL) {
            return fromInt64(ul.toLong())
        }
        zero()
        val q = ul / DEC1E16uL
        val r = (ul % DEC1E16uL).toLong()
        lsu[0] = (r % DEC1E8L).toInt()
        lsu[1] = (r / DEC1E8L).toInt();
        lsu[2] = q.toInt()
        digits = getDigits(lsu, 3)
        return this
    } // decNumberFromUInt64

    /* ------------------------------------------------------------------ */
    /* decNumberZero -- set a number to 0                                 */
    /*                                                                    */
    /*   dn is the number to set, with space for one digit                */
    /*   returns dn                                                       */
    /*                                                                    */
    /* No error is possible.                                              */
    /* ------------------------------------------------------------------ */
    // Memset is not used as it is much slower in some environments.
    inline fun zero(): DecNumber {
        bits = 0;
        exponent = 0;
        digits = 1;
        lsu[0] = 0;
        return this
    } // decNumberZero

    /* ------------------------------------------------------------------ */
    /* to-int32 -- conversion to Int or uInt                              */
    /*                                                                    */
    /*  dn is the decNumber to convert                                    */
    /*  set is the context for reporting errors                           */
    /*  returns the converted decNumber, or 0 if Invalid is set           */
    /*                                                                    */
    /* Invalid is set if the decNumber does not have exponent==0 or if    */
    /* it is a NaN, Infinite, or out-of-range.                            */
    /* ------------------------------------------------------------------ */
    fun toInt32(set: DecContext): Int {
        // if (digits <= 10 && exponent == 0 && (bits and DECSPECIAL) == 0) {
        if (((10 - digits) shr 31) or exponent or (bits and DECSPECIAL) == 0) {
            val l = (lsu[1].toLong() * DEC1E8L) + lsu[0]
            if (l <= Int.MAX_VALUE) {
                val signExtended = bits shr 31;
                return (l.toInt() xor signExtended) - signExtended
            }
            if (l == -(Int.MIN_VALUE.toLong())) {
                return Int.MIN_VALUE
            }
        }
        // special or too many digits, or bad exponent
        set.setStatus(Invalid_operation); // [may not return]
        return 0;
    }

    fun toUInt32(set: DecContext): UInt {
        if ((((10 - digits) shr 31) or exponent or bits) and zeroOrFFFFFFFF() == 0) {
            val l = (lsu[1].toLong() * DEC1E8L) + lsu[0]
            if (l <= 0xFFFFFFFFL) {
                return l.toUInt()
            }
        }
        // special or too many digits, or bad exponent
        set.setStatus(Invalid_operation); // [may not return]
        return 0u;
    }

    fun toInt64(set: DecContext): Long {
        // if (digits <= 19 && exponent == 0 && (bits and DECSPECIAL) == 0) {
        if (((19 - digits) shr 31) or exponent or (bits and DECSPECIAL) == 0) {
            val d2 = lsu[2].toLong()
            val d1 = lsu[1].toLong()
            val d0 = lsu[0].toLong()
            val l = (d2 * DEC1E16L) + (d1 * DEC1E8L) + d0
            val t2 = (d2 * DEC1E8L) + d1
            val maxT2 = Long.MAX_VALUE / DEC1E8L
            if (t2 < maxT2) {
                val signExtended = bits.toLong() shr 63
                return (l xor signExtended) - signExtended
            }
            val maxD0 = Long.MAX_VALUE % DEC1E8L + 1
            if (t2 == maxT2) {
                if (d0 < maxD0) {
                    val signExtended = bits.toLong() shr 63
                    return (l xor signExtended) - signExtended
                }
                if (d0 == maxD0 && bits < 0) {
                    return Long.MIN_VALUE
                }
            }
        }
        // special or too many digits, or bad exponent
        set.setStatus(Invalid_operation); // [may not return]
        return 0L;
    }

    fun toUInt64(set: DecContext): ULong {
        // if (digits <= 19 && exponent == 0 && (bits and DECSPECIAL) == 0) {
        if ((((20 - digits) shr 31) or exponent or bits) and zeroOrFFFFFFFF() == 0) {
            val d2 = lsu[2].toLong()
            val d1 = lsu[1].toLong()
            val d0 = lsu[0].toLong()
            val l = ((d2 * DEC1E16L) + (d1 * DEC1E8L) + d0).toULong()
            val t2 = (d2 * DEC1E8L) + d1
            val maxT2 = (ULong.MAX_VALUE / DEC1E8uL).toLong()
            if (t2 < maxT2) {
                val signExtended = (bits.toLong() shr 63).toULong()
                return (l xor signExtended) - signExtended
            }
            val maxD0 = (ULong.MAX_VALUE % DEC1E8uL).toLong()
            if (t2 == maxT2) {
                if (d0 <= maxD0) {
                    return ULong.MAX_VALUE
                }
            }
        }
        // special or too many digits, or bad exponent
        set.setStatus(Invalid_operation); // [may not return]
        return 0uL;
    }

    /* ------------------------------------------------------------------ */
    /* to-scientific-string -- conversion to numeric string               */
    /* to-engineering-string -- conversion to numeric string              */
    /*                                                                    */
    /*   decNumberToString(dn, string);                                   */
    /*   decNumberToEngString(dn, string);                                */
    /*                                                                    */
    /*  dn is the decNumber to convert                                    */
    /*  string is the string where the result will be laid out            */
    /*                                                                    */
    /*  string must be at least dn->digits+14 characters long             */
    /*                                                                    */
    /*  No error is possible, and no status can be set.                   */
    /* ------------------------------------------------------------------ */
    override fun toString() : String {
        val bytes = ByteArray(digits + 14)
        val cb = decToString(bytes, false)
        return String(bytes, 0, cb, StandardCharsets.UTF_8)
    } // DecNumberToString

    fun toEngString() : String {
        val bytes = ByteArray(digits + 14)
        val cb = decToString(bytes, true)
        return String(bytes, 0, cb, StandardCharsets.UTF_8)
    } // DecNumberToEngString

    /* ------------------------------------------------------------------ */
    /* decToString -- lay out a number into a string                      */
    /*                                                                    */
    /*   dn     is the number to lay out                                  */
    /*   string is where to lay out the number                            */
    /*   eng    is 1 if Engineering, 0 if Scientific                      */
    /*                                                                    */
    /* string must be at least dn->digits+14 characters long              */
    /* No error is possible.                                              */
    /*                                                                    */
    /* Note that this routine can generate a -0 or 0.000.  These are      */
    /* never generated in subset to-number or arithmetic, but can occur   */
    /* in non-subset arithmetic (e.g., -1*0 or 1.234-1.234).              */
    /* ------------------------------------------------------------------ */
// If DECCHECK is enabled the string "?" is returned if a number is
// invalid.
    fun decToString(string : ByteArray, eng: Boolean) : Int {
        var ib = 0;
        val exp = exponent;       // local copy
        if (isNegative()) {   // Negatives get a minus
            string[ib++] = '-'.code.toByte()
        }
        if (isSpecial()) {       // Is a special value
            if (isInfinity()) {
                System.arraycopy(INFINITY_BYTES, 0, string, ib, INFINITY_BYTES.size)
                ib += INFINITY_BYTES.size;
                return ib
            }
            // a NaN
            if (isSNAN()) {        // signalling NaN
                string[ib++] = 's'.code.toByte()
            }
            string[ib++] = 'N'.code.toByte()
            string[ib++] = 'a'.code.toByte()
            string[ib++] = 'N'.code.toByte()
            // if not a clean non-zero coefficient, that's all there is in a
            // NaN string
            if (exp != 0 || (lsu[0] == 0 && digits == 1)) {
                return ib
            }
            // [drop through to add integer]
        }

        // calculate how many digits in msu, and hence first cut
        var cut = msuDigitCount(digits) - 1
        if (exp == 0) {                    // simple integer [common fastpath]
            for (i in (digits-1) shr 3 downTo 0) {// each Unit from msu
                var u = lsu[i]
                for (j in cut downTo 0) {
                    val (q, r) = divModPow10(u, j)
                    string[ib++] = ('0'.code + q).toByte()
                    u = r
                }
                cut = 7
            }
            return ib
        }

        /* non-0 exponent -- assume plain form */
        var pre = digits + exp           // digits before '.'
        var e = 0                        // no E
        if ((exp > 0) || (pre < -5)) {   // need exponential form
            e = exp + digits - 1         // calculate E value
            pre = 1                      // assume one digit before '.'
            if (eng && (e != 0)) {       // engineering: may need to adjust
                var adj = 0              // adjustment
                // The C remainder operator is undefined for negative numbers, so
                // a positive remainder calculation must be used here
                if (e < 0) {
                    adj = (-e) % 3;
                    if (adj != 0) {
                        adj = 3 - adj
                    }
                }
                else { // e>0
                    adj = e % 3
                }
                e -= adj
                // if dealing with zero still produce an exponent which is a
                // multiple of three, as expected, but there will only be the
                // one zero before the E, still.  Otherwise note the padding.
                if (!isZero()) {
                    pre += adj
                } else {  // is zero
                    if (adj != 0) {              // 0.00Esnn needed
                        e += 3;
                        pre = -(2 - adj)
                    }
                } // zero
            } // eng
        } // need exponent
        var iu = (digits-1) shr 3
        var u = lsu[iu]
        /* lay out the digits of the coefficient, adding 0s and . as needed */
        if (pre > 0) {                     // xxx.xxx or xx00 (engineering) form
            val n = pre
            // for (; pre>0; pre--, c++, cut--) {
            while (pre > 0) {
                if (cut < 0) {                 // need new Unit
                    if (iu == 0) break;    // out of input digits (pre>digits)
                    u = lsu[--iu]
                    cut = 7
                }
                // TODIGIT(u, cut, c, pow);
                val (q, r) = divModPow10(u, cut)
                string[ib++] = ('0'.code + q).toByte()
                u = r
                // for (; pre>0; pre--, c++, cut--) {
                --pre
                --cut
            }
            if (n < digits) {            // more to come, after '.'
                string[ib++] = '.'.code.toByte()
                //for (;; c++, cut--) {
                while (true) {
                    if (cut < 0) {                 // need new Unit
                        if (iu == 0) break;    // out of input digits
                        u = lsu[--iu]
                        cut = 7
                    }
                    // TODIGIT(u, cut, c, pow);
                    val (q, r) = divModPow10(u, cut)
                    string[ib++] = ('0'.code + q).toByte()
                    u = r
                    --cut
                }
            } else {
                while (pre-- > 0) { // 0 padding (for engineering) needed
                    string[ib++] = '0'.code.toByte()
                }
            }
        } else {                          // 0.xxx or 0.000xxx form
            string[ib++] = '0'.code.toByte()
            string[ib++] = '.'.code.toByte()
            while(pre < 0) {   // add any 0's after '.'
                string[ib++] = '0'.code.toByte()
                ++pre
            }
            while (true) {
                if (cut < 0) {                 // need new Unit
                    if (iu == 0) break;    // out of input digits
                    u = lsu[--iu]
                    cut = 7
                }
                // TODIGIT(u, cut, c, pow);
                val (q, r) = divModPow10(u, cut)
                string[ib++] = ('0'.code + q).toByte()
                u = r
                --cut
            }
        }

        /* Finally add the E-part, if needed.  It will never be 0, has a
           base maximum and minimum of +999999999 through -999999999, but
           could range down to -1999999998 for anormal numbers */
        if (e != 0) {
            var had = false               // 1=had non-zero
            string[ib++] = 'E'.code.toByte()
            string[ib++] = (if (e > 0) '+' else '-').code.toByte()
            u = Math.abs(e)
            // lay out the exponent [_itoa or equivalent is not ANSI C]
            for (j in getDigitCount(u) downTo 0) {
                // TODIGIT(u, cut, c, pow);
                val (q, r) = divModPow10(u, j)
                string[ib++] = ('0'.code + q).toByte()
                u = r
            }
        }
        return ib
    } // decToString

    /* ------------------------------------------------------------------ */
    /* to-number -- conversion from numeric string                        */
    /*                                                                    */
    /* decNumberFromString -- convert string to decNumber                 */
    /*   dn        -- the number structure to fill                        */
    /*   chars[]   -- the string to convert ('\0' terminated)             */
    /*   set       -- the context used for processing any error,          */
    /*                determining the maximum precision available         */
    /*                (set.digits), determining the maximum and minimum   */
    /*                exponent (set.emax and set.emin), determining if    */
    /*                extended values are allowed, and checking the       */
    /*                rounding mode if overflow occurs or rounding is     */
    /*                needed.                                             */
    /*                                                                    */
    /* The length of the coefficient and the size of the exponent are     */
    /* checked by this routine, so the correct error (Underflow or        */
    /* Overflow) can be reported or rounding applied, as necessary.       */
    /*                                                                    */
    /* If bad syntax is detected, the result will be a quiet NaN.         */
    /* ------------------------------------------------------------------ */
    /*
    fun fromString(chars: CharArray, cch: Int, set: DecContext) : DecNumber {
        var exponentT = 0                // working exponent [assume 0]
        var bitsT = 0                   // working flags [assume +ve]
        //Unit  *res;                      // where result will be built
        //Unit  resbuff[SD2U(DECBUFFER+9)];// local buffer in case need temporary
        // [+9 allows for ln() constants]
        //Unit  *allocres=NULL;            // -> allocated result, iff allocated
        var d = 0                       // count of digits found in decimal part
        var ichDot = -1
        var ichFirstDigit = 0
        var ichLastDigit = -1
        var status = 0                  // error code
        var ch = 0.toChar()

        do {                             // status & malloc protection
            var ich = -1
            while (++ich < cch) {
                ch = chars[ich]
                if (ch in '0'..'9') {    // test for Arabic digit
                    ichLastDigit = ich
                    d++;                      // count of real digits
                    continue;                  // still in decimal part
                }
                if (ch == '.' && ichDot == -1) { // first '.'
                    ichDot = ich               // record offset into decimal part
                    if (ich == ichFirstDigit)
                        ichFirstDigit++            // first digit must follow
                    continue
                }
                if (ich == 0) {              // first in string...
                    if (ch == '-') {             // valid - sign
                        ichFirstDigit++;
                        bitsT = DECNEG;
                        continue
                    }
                    if (ch == '+') {             // valid + sign
                        ichFirstDigit++
                        continue
                    }
                }
                // ch is not a digit, or a valid +, -, or '.'
                break;
            } // c

            // if (ichLastDigit == -1) {              // no digits yet
            if (d == 0) {              // no digits yet
                status = Conversion_syntax   // assume the worst
                if (ich == cch)
                    break;         // and no more to come...
                // Infinities and NaNs are possible, here
                if (ichDot != -1)
                    break;    // .. unless had a dot
                zero()        // be optimistic
                val cchRemaining = cch - ich;
                if (matchesCaseInsensitive(chars, ich, cchRemaining, "infinity", "inf")) {
                    bits = bitsT or DECINF
                    status = 0                  // is OK
                    break; // all done
                }
                // a NaN expected
                // 2003.09.10 NaNs are now permitted to have a sign
                if (startsWithCaseInsensitive(chars, ich, cchRemaining, "sNaN")) {
                    bits = bitsT or DECSNAN
                    ich += 4
                } else if (startsWithCaseInsensitive(chars, ich, cchRemaining, "NaN")) {
                    bits = bitsT or DECNAN      // simple NaN
                    ich += 3
                }
                // now either nothing, or nnnn payload, expected
                // -> start of integer and skip leading 0s [including plain 0]
                while (ich < cch && chars[ich] == '0')
                    ++ich
                if (ich == cch) {
                    status = 0;                  // it's good
                    break;                     // ..
                }
                ichFirstDigit = ich
                // something other than 0s; setup last and d as usual [no dots]
                while (ich < cch && chars[ich] in '0'..'9') {
                    ichLastDigit = ich++
                    ++d
                }
                if (ich != cch)
                    break;         // not all digits
                if (d > set.digits || d == set.digits && set.clamp) {
                    // [NB: payload in a decNumber can be full length unless
                    // clamped, in which case can only be digits-1]
                    break;
                } // too many digits?
                // good; drop through to convert the integer to coefficient
                status=0;                    // syntax is OK
                bitsT = bits;               // for copy-back
            } else /* had some digits */ if (ich < cch) {          // more to process...
                // had some digits; exponent is only valid sequence now
                status = Conversion_syntax;// assume the worst
                if (chars[ich].code or 0x20 != 'e'.code)
                    break;
                /* Found 'e' or 'E' -- now process explicit exponent */
                // 1998.07.11: sign no longer required
                var negExp = false                   // 1=negative exponent
                ++ich
                if (ich == cch)
                    break;
                ch = chars[ich]
                if (ch == '-') {
                    negExp = true;
                    ++ich
                } else if (ch == '+') {
                    ++ich
                }
                if (ich == cch)
                    break;
                while (ich < cch-1 && chars[ich] == '0') // strip insignificant leading zeros
                    ++ich
                val ichFirstExpDigit = ich               // save exponent digit place
                while (ich < cch && chars[ich] in '0'..'9') {
                    exponentT = exponentT*10 + chars[ich].code - '0'.code
                }
                // if not now on a '\0', *c must not be a digit
                if (ich != cch)
                    break;

                // (this next test must be after the syntax checks)
                // if it was too long the exponent may have wrapped, so check
                // carefully and set it to a certain overflow if wrap possible
                if (ich >= ichFirstExpDigit+9+1) {
                    if (ich > ichFirstExpDigit+9+1 || chars[ichFirstExpDigit] > '1')
                        exponentT = DECNUMMAXE*2;
                    // [up to 1999999999 is OK, for example 1E-1000000998]
                }
                if (negExp)
                    exponentT = -exponentT;     // was negative
                status=0;                         // is OK
            } // stuff after digits

            // Here when whole string has been inspected; syntax is good
            // cfirst->first digit (never dot), last->last digit (ditto)

            // strip leading zeros/dot [leave final 0 if all 0's]
            if (chars[ichFirstDigit] == '0') {                 // [cfirst has stepped over .]
                // for (c=cfirst; c<last; c++, cfirst++) {
                while (ichFirstDigit < ichLastDigit) {
                    ch = chars[ichFirstDigit]
                    if (ch == '.')
                        continue;          // ignore dots
                    if (ch != '0')
                        break;             // non-zero found
                    --d;                            // 0 stripped
                    ++ichFirstDigit
                } // c
            } // at least one leading 0


            // Handle decimal point...
            if (ichDot >= 0 && ichDot < ichLastDigit)  // non-trailing '.' found?
                exponentT -= (ichLastDigit - ichDot);         // adjust exponent
            // [we can now ignore the .]

            // OK, the digits string is good.  Assemble in the decNumber, or in
            // a temporary units array if rounding is needed

            // mth 20250228
            // MFC's code assumes that context.digits accurately describes the space avail in DecNumber.lsu
            // Seems like this is going to be a recurring/ongoing problem ... Yikes!
            // For now, I will ensure that lsu units >= d
            ensureDigitCountWipe(d)
            // res now -> number lsu, buffer, or allocated storage for Unit array
            // Place the coefficient into the selected Unit array
            // [this is often 70% of the cost of this function when DECDPUN>1]
            var out = 0                         // accumulator
            var iu = D2U(d) - 1            // -> msu
            var cut = msuDigitCount(d)        // digits in top unit
            //for (c=cfirst;; c++) {         // along the digits
            ich = ichFirstDigit
            while (true) {
                ch = chars[ich]
                if (ch != '.') {     // ignore '.' [don't decrement cut]
                    out = out * 10 + ch.code - '0'.code
                    if (ich == ichLastDigit)
                        break;          // done [never get to trailing '.']
                    --cut
                    if (cut == 0) {
                        lsu[iu--] = out               // write unit
                        cut = 8
                        out = 0
                    }
                }
            } // c
            assert(iu == 0)
            lsu[0] = out                 // write lsu

            bits = bitsT
            exponent = exponentT
            digits = d

            // if not in number (too long) shorten into the number
            val residue = IntArray(1)
            if (d > set.digits) {
                status = decSetCoeff(set, lsu, d, residue, status)
                // always check for overflow or subnormal and round as needed
                status = decFinalize(set, residue, status)
            } else { // no rounding, but may still have overflow or subnormal
                // [these tests are just for performance; finalize repeats them]
                if ((exponent - 1 < set.emin - digits) || (exponent - 1 > set.emax - set.digits)) {
                    status = decFinalize(set, residue, status)
                }
            }
            // decNumberShow(dn);
        } while (false)                         // [for break]

        if (status != 0)
            decStatus(status, set);
        return this
    } // decNumberFromString

     */

    /* ------------------------------------------------------------------ */
    /* decSetCoeff -- set the coefficient of a number                     */
    /*                                                                    */
    /*   dn    is the number whose coefficient array is to be set.        */
    /*         It must have space for set->digits digits                  */
    /*   set   is the context [for size]                                  */
    /*   lsu   -> lsu of the source coefficient [may be dn->lsu]          */
    /*   len   is digits in the source coefficient [may be dn->digits]    */
    /*   residue is the residue accumulator.  This has values as in       */
    /*         decApplyRound, and will be unchanged unless the            */
    /*         target size is less than len.  In this case, the           */
    /*         coefficient is truncated and the residue is updated to     */
    /*         reflect the previous residue and the dropped digits.       */
    /*   status is the status accumulator, as usual                       */
    /*                                                                    */
    /* The coefficient may already be in the number, or it can be an      */
    /* external intermediate array.  If it is in the number, lsu must ==  */
    /* dn->lsu and len must == dn->digits.                                */
    /*                                                                    */
    /* Note that the coefficient length (len) may be < set->digits, and   */
    /* in this case this merely copies the coefficient (or is a no-op     */
    /* if dn->lsu==lsu).                                                  */
    /*                                                                    */
    /* Note also that (only internally, from decQuantizeOp and            */
    /* decSetSubnormal) the value of set->digits may be less than one,    */
    /* indicating a round to left.  This routine handles that case        */
    /* correctly; caller ensures space.                                   */
    /*                                                                    */
    /* dn->digits, dn->lsu (and as required), and dn->exponent are        */
    /* updated as necessary.   dn->bits (sign) is unchanged.              */
    /*                                                                    */
    /* DEC_Rounded status is set if any digits are discarded.             */
    /* DEC_Inexact status is set if any non-zero digits are discarded, or */
    /*                       incoming residue was non-0 (implies rounded) */
    /* ------------------------------------------------------------------ */
// mapping array: maps 0-9 to canonical residues, so that a residue
// can be adjusted in the range [-1, +1] and achieve correct rounding
//                             0  1  2  3  4  5  6  7  8  9
/*
    private val resmap = intArrayOf(0, 3, 3, 3, 3, 5, 7, 7, 7, 7)

    fun decSetCoeff(set: DecContext, lsu: IntArray, len: Int, residue: IntArray, statusIn: Int) : Int {

        // Int   discard;              // number of digits to discard
        // uInt  cut;                  // cut point in Unit
        // const Unit *up;             // work
        // Unit  *target;              // ..
        // Int   count;                // ..

        var status = statusIn
        val srcUnitCount = D2U(len)
        val discard = len - set.digits    // digits to discard
        if (discard <= 0) {           // no digits are being discarded
            if (this.lsu !== lsu) {       // copy needed
                ensureDigitCountWipe(len)
                // copy the coefficient array to the result number; no shift needed
                System.arraycopy(lsu, 0, this.lsu, 0, srcUnitCount)
                this.digits = len;         // set the new length
            }
            // dn->exponent and residue are unchanged, record any inexactitude
            if (residue[0] != 0)
                status = status or (Inexact or Rounded)
            return status
        }

        // some digits must be discarded ...
        this.exponent += discard      // maintain numerical value
        status = status or Rounded       // accumulate Rounded status
        if (residue[0] > 1)
            residue[0] = 1  // previous residue now to right, so reduce

        if (discard > len) {          // everything, +1, is being discarded
            // guard digit is 0
            // residue is all the number [NB could be all 0s]
            if (residue[0] <= 0) {        // not already positive
                for (i in 0..srcUnitCount) {
                    if (lsu[i] != 0) { // found non-0
                        residue[0] = 1
                        break;                // no need to check any others
                    }
                }
            }
            if (residue[0] != 0)
                status = status or Inexact // record inexactitude
            this.lsu[0] = 0           // coefficient will now be 0
            this.digits = 1
            return status
        } // total discard

        // partial discard [most common case]
        // here, at least the first (most significant) discarded digit exists

        // spin up the number, noting residue during the spin, until get to
        // the Unit with the first discarded digit.  When reach it, extract
        // it and remember its position
        var count = 0;
        var iu = 0
        //for (up=lsu;; up++) {
        while (true) {
            count += DECDPUN
            if (count >= discard)
                break; // full ones all checked
            if (lsu[iu] != 0)
                residue[0] = 1
            ++iu
        } // up

        // here up -> Unit with first discarded digit
        var cut = discard - (count - DECDPUN) - 1
        if (cut == DECDPUN-1) {       // unit-boundary case (fast)
            val half = DECPOWERS[DECDPUN] shr 1
            // set residue directly
            if (lsu[iu] >= half) {
                if (lsu[iu] > half)
                    residue[0] = 7
                else
                    residue[0] += 5       // add sticky bit
            } else { // <half
                if (lsu[iu] != 0)
                    residue[0] = 3       // [else is 0, leave as sticky bit]
            }
            if (set.digits <= 0) {     // special for Quantize/Subnormal :-(
                this.lsu[0] = 0 // .. result is 0
                this.digits = 1 // ..
            } else {                   // shift to least
                count = set.digits      // now digits to end up with
                this.digits = count       // set the new length
                iu++                   // move to next
                // on unit boundary, so shift-down copy loop is simple
                System.arraycopy(lsu, iu, this.lsu, 0, D2U(count))
            }
        } // unit-boundary case
        else { // discard digit is in low digit(s), and not top digit
            /*
            uInt  discard1;                // first discarded digit
            uInt  quot, rem;               // for divisions
            */
            var quot = lsu[iu]
            // if (cut==0) quot=*up;          // is at bottom of unit
            if (cut >  0) {
                val power10 = DECPOWERS[cut]
                val rem = quot % power10
                quot = quot/power10
                if (rem != 0)
                    residue[0] = 1
            }
            // discard digit is now at bottom of quot
            val discard1 = quot % 10
            quot = quot / 10;

            // here, discard1 is the guard digit, and residue is everything
            // else [use mapping array to accumulate residue safely]
            residue[0] += resmap[discard1]
            cut++;                         // update cut
            // here: up -> Unit of the array with bottom digit
            //       cut is the division point for each Unit
            //       quot holds the uncut high-order digits for the current unit
            if (set.digits <= 0) {          // special for Quantize/Subnormal :-(
                this.lsu[0] = 0               // .. result is 0
                this.digits = 1               // ..
            } else {                        // shift to least needed
                count = set.digits           // now digits to end up with
                this.digits = count          // set the new length
                // shift-copy the coefficient array to the result number
                //for (target=dn->lsu; ; target++) {
                var iuTarget = 0
                val powerQuot = DECPOWERS[cut]
                val powerRem = DECPOWERS[DECDPUN - cut]
                do {
                    val quotPrev = quot
                    count -= (DECDPUN - cut)
                    if (count <= 0) {
                        this.lsu[iuTarget] = quotPrev
                        break
                    }
                    ++iu
                    quot = lsu[iu]
                    val rem = quot % powerQuot
                    quot = quot / powerQuot
                    this.lsu[iuTarget] = quotPrev + rem*powerRem
                    count -= cut;
                    ++iuTarget
                } while (count > 0)
            } // shift to least
        } // not unit boundary

        if (residue[0] != 0)
            status = status or Inexact
        return status
    } // decSetCoeff

 */

    /* ------------------------------------------------------------------ */
    /* decFinalize -- final check, clamp, and round of a number           */
    /*                                                                    */
    /*   dn is the number                                                 */
    /*   set is the context                                               */
    /*   residue is the rounding accumulator (as in decApplyRound)        */
    /*   status is the status accumulator                                 */
    /*                                                                    */
    /* This finishes off the current number by checking for subnormal     */
    /* results, applying any pending rounding, checking for overflow,     */
    /* and applying any clamping.                                         */
    /* Underflow and overflow conditions are raised as appropriate.       */
    /* All fields are updated as required.                                */
    /* ------------------------------------------------------------------ */
    /*
    fun decFinalize(set: DecContext, residue: IntArray, statusIn: Int) : Int {
        //Int shift;                            // shift needed if clamping
        val tinyexp = set.emin - this.digits + 1   // precalculate subnormal boundary

        // Must be careful, here, when checking the exponent as the
        // adjusted exponent could overflow 31 bits [because it may already
        // be up to twice the expected].

        // First test for subnormal.  This must be done before any final
        // round as the result could be rounded to Nmin or 0.
        if (this.exponent <= tinyexp) {          // prefilter
            //Int comp;
            //decNumber nmin;
            // A very nasty case here is dn == Nmin and residue<0
            if (this.exponent < tinyexp) {
                // Go handle subnormals; this will apply round if needed.
                return decSetSubnormal(set, residue, statusIn);
            }
            // Equals case: only subnormal if dn=Nmin and negative residue
            decNumberZero(&nmin);
            nmin.lsu[0]=1;
            nmin.exponent=set->emin;
            comp=decCompare(dn, &nmin, 1);                // (signless compare)
            if (comp==BADINT) {                           // oops
                *status|=DEC_Insufficient_storage;          // abandon...
                return;
            }
            if (*residue<0 && comp==0) {                  // neg residue and dn==Nmin
                decApplyRound(dn, set, *residue, status);   // might force down
                decSetSubnormal(dn, set, residue, status);
                return;
            }
        }

        // now apply any pending round (this could raise overflow).
        if (*residue!=0) decApplyRound(dn, set, *residue, status);

        // Check for overflow [redundant in the 'rare' case] or clamp
        if (dn->exponent<=set->emax-set->digits+1) return;   // neither needed


        // here when might have an overflow or clamp to do
        if (dn->exponent>set->emax-dn->digits+1) {           // too big
            decSetOverflow(dn, set, status);
            return;
        }
        // here when the result is normal but in clamp range
        if (!set->clamp) return;

        // here when need to apply the IEEE exponent clamp (fold-down)
        shift=dn->exponent-(set->emax-set->digits+1);

        // shift coefficient (if non-zero)
        if (!ISZERO(dn)) {
                dn->digits=decShiftToMost(dn->lsu, dn->digits, shift);
        }
        dn->exponent-=shift;   // adjust the exponent to match
        *status|=DEC_Clamped;  // and record the dirty deed
        return;
    } // decFinalize
*/


}

/* ------------------------------------------------------------------ */
/* decCompare -- compare two decNumbers by numerical value            */
/*                                                                    */
/*  This routine compares A ? B without altering them.                */
/*                                                                    */
/*  Arg1 is A, a decNumber which is not a NaN                         */
/*  Arg2 is B, a decNumber which is not a NaN                         */
/*  Arg3 is 1 for a sign-independent compare, 0 otherwise             */
/*                                                                    */
/*  returns -1, 0, or 1 for A<B, A==B, or A>B, or BADINT if failure   */
/*  (the only possible failure is an allocation error)                */
/* ------------------------------------------------------------------ */

/*
fun decCompare(lhs: DecNumber, rhs: DecNumber, abs: Boolean) : Int {
    //Int   result;                    // result value
    //Int   sigr;                      // rhs signum
    //Int   compare;                   // work

    var result = if (lhs.isZero()) 0 else 1 // assume signum(lhs)
    if (abs) {
        if (rhs.isZero())
            return result;          // LHS wins or both 0
        // RHS is non-zero
        if (result == 0)
            return -1;                // LHS is 0; RHS wins
        // [here, both non-zero, result=1]
    } else {                                    // signs matter
        if (result != 0 && lhs.isNegative())
            result=-1
        val sigr = rhs.signum()
        if (result > sigr)
            return +1            // L > R, return 1
        if (result < sigr)
            return -1            // L < R, return -1
        if (result==0)
            return 0                   // both 0
    }

    // signums are the same; both are non-zero
    if ((lhs.bits or rhs.bits) and DECINF != 0) {    // one or more infinities
        if (rhs.isInfinity()) {
            if (lhs.isInfinity())
                result=0;// both infinite
            else
                result=-result;                  // only rhs infinite
        }
        return result;
    }
    var alfa = lhs
    var beta = rhs
    // must compare the coefficients, allowing for exponents
    if (alfa.exponent > beta.exponent) {         // LHS exponent larger
        // swap sides, and sign
        val tmp = alfa
        alfa = beta
        beta = tmp
        result = -result;
    }
    // mth 2025-03-02
    // FIXME
    // rather than going straight to comparing units, he should have compared the digit counts
    // digitCount + exponent gives us the magnitude of the most-significant-digit
    // if we are working with a DECDPUN > 1 then this would/should pro-rata cut the number
    // of time we have to go into decUnitCompare ... which ends up doing a complete subtraction
    // just to get the sign of the result.
    var compare = decUnitCompare(
        alfa.lsu, D2U(alfa.digits),
        beta.lsu, D2U(beta.digits), beta.exponent - alfa.exponent)
    compare *= result      // comparison succeeded
    return compare;
} // decCompare

/* ------------------------------------------------------------------ */
/* decUnitCompare -- compare two >=0 integers in Unit arrays          */
/*                                                                    */
/*  This routine compares A ? B*10**E where A and B are unit arrays   */
/*  A is a plain integer                                              */
/*  B has an exponent of E (which must be non-negative)               */
/*                                                                    */
/*  Arg1 is A first Unit (lsu)                                        */
/*  Arg2 is A length in Units                                         */
/*  Arg3 is B first Unit (lsu)                                        */
/*  Arg4 is B length in Units                                         */
/*  Arg5 is E (0 if the units are aligned)                            */
/*                                                                    */
/*  returns -1, 0, or 1 for A<B, A==B, or A>B, or BADINT if failure   */
/*  (the only possible failure is an allocation error, which can      */
/*  only occur if E!=0)                                               */
/* ------------------------------------------------------------------ */
fun decUnitCompare(a: IntArray, alength: Int , b:IntArray, blength: Int, exp: Int) : Int {
    // Unit  *acc;                      // accumulator for result
    // Unit  accbuff[SD2U(DECBUFFER*2+1)]; // local buffer
    // Unit  *allocacc=NULL;            // -> allocated acc buffer, iff allocated
    // Int   accunits, need;            // units in use or needed for acc
    // const Unit *l, *r, *u;           // work
    // Int   expunits, exprem, result;  // ..

    if (exp==0) {                    // aligned; fastpath
        if (alength>blength) return 1;
        if (alength<blength) return -1;
        // same number of units in both -- need unit-by-unit compare
        for (i in alength-1 downTo 0) {
            if (a[i] > b[i])
                return 1
            if (a[i] < b[i])
                return -1
        }
        return 0                      // all units match
    } // aligned

    val expUnits = D2U(exp)


    // Unaligned.  If one is >1 unit longer than the other, padded
    // approximately, then can return easily
    if (alength > blength+expUnits) return 1
    if (alength+1 < blength+expUnits) return -1

    // Need to do a real subtract.  For this, a result buffer is needed
    // even though only the sign is of interest.  Its length needs
    // to be the larger of alength and padded blength, +2
    var need = blength + expUnits                // maximum real length of B
    if (need < alength)
        need=alength;
    need += 2;
    var acc = IntArray(need)
    // Calculate units and remainder from exponent.
    val expunits = exp/DECDPUN;
    val exprem = exp%DECDPUN;
    // subtract [A+B*(-m)]
    val accunits = decUnitAddSub(a, alength, b, blength, expunits, acc,-DECPOWERS[exprem]);
    // [UnitAddSub result may have leading zeros, even on zero]
    if (accunits<0) {
        return -1            // negative result
    } else {                               // non-negative result
        // check units of the result before freeing any storage
        for (iu in 0..<accunits) {
            if (acc[iu] > 0)
                return 1;
        }
    }
    return 0
} // decUnitCompare
*/




/* ------------------------------------------------------------------ */
/* decGetDigits -- count digits in a Units array                      */
/*                                                                    */
/*   uar is the Unit array holding the number (this is often an       */
/*          accumulator of some sort)                                 */
/*   len is the length of the array in units [>=1]                    */
/*                                                                    */
/*   returns the number of (significant) digits in the array          */
/*                                                                    */
/* All leading zeros are excluded, except the last if the array has   */
/* only zero Units.                                                   */
/* ------------------------------------------------------------------ */
// This may be called twice during some operations.
fun getDigits(uar: IntArray, len: Int) : Int {
    var i = len - 1
    var d = uar[i]
    while (d == 0 && i > 0)
        d = uar[--i]
    val previous = i * DECDPUN
    val final = getDigitCount(d)
    val digitCount = previous + final
    return digitCount
    /*
    val lastIndex = len - 1;
    val lastVal = uar[lastIndex]
    var digits = lastIndex * DECDPUN + 1
    // (at least 1 in final msu)
    for (; up>=uar; up--) {
        if (*up==0) {                  // unit is all 0s
        if (digits==1) break;        // a zero has one digit
        digits-=DECDPUN;             // adjust for 0 unit
        continue;}
        // found the first (most significant) non-zero Unit
        #if DECDPUN>1                  // not done yet
        if (*up<10) break;             // is 1-9
        digits++;
        #if DECDPUN>2                  // not done yet
        if (*up<100) break;            // is 10-99
        digits++;
        #if DECDPUN>3                  // not done yet
        if (*up<1000) break;           // is 100-999
        digits++;
        #if DECDPUN>4                  // count the rest ...
        for (pow=&powers[4]; *up>=*pow; pow++) digits++;
        #endif
        #endif
        #endif
        #endif
        break;
    } // up
    return digits;
    */
} // decGetDigits

fun getDigitCount(n: Int) : Int {
    val digitCount = 1 +
            ((9 - n) ushr 31) + ((99 - n) ushr 31) + ((999 - n) ushr 31) +
            ((9999 - n) ushr 31) + ((99999 - n) ushr 31) + ((999999 - n) ushr 31) +
            ((9999999 - n) ushr 31) + ((99999999 - n) ushr 31) + ((999999999 - n) ushr 31)
    return digitCount
}

fun startsWithCaseInsensitive(haystack: CharArray, offset: Int, length: Int, needle: String) : Boolean {
    val strLen = needle.length
    if (strLen > length)
        return false;
    for (ich in 0..<strLen) {
        if (needle[ich].code or 0x20 != haystack[offset + ich].code or 0x20)
            return false
    }
    return true
}

fun matchesCaseInsensitive(haystack: CharArray, offset: Int, length: Int, vararg needles: String) : Boolean {
    for (needle in needles)
        if (matchesCaseInsensitive(haystack, offset, length, needle))
            return true
    return false
}

fun matchesCaseInsensitive(haystack: CharArray, offset: Int, length: Int, needle: String) : Boolean {
    val strLen = needle.length
    if (strLen != length)
        return false
    for (ich in 0..<strLen) {
        if (needle[ich].code or 0x20 != haystack[offset + ich].code or 0x20)
            return false
    }
    return true
}