@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Bits.calcBitLen64
import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen64
import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.Magia.renderChunk8
import com.decimal128.hugeint.Magia.renderChunkTail
import kotlin.math.max
import kotlin.math.min

internal inline fun packSeal(sign: Boolean, qExp: Int, digitLen: Int, bitLen: Int): Int =
    (if (sign) Int.MIN_VALUE else 0) or (((qExp and 0x7FFF) shl 16) or
            (digitLen shl 9) or bitLen)

internal inline fun packSeal(sign01: Int, qExp: Int, digitLen: Int, bitLen: Int): Int =
    (sign01 shl 31) or (((qExp and 0x7FFF) shl 16) or (digitLen shl 9) or bitLen)

internal fun calcSeal(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    return packSeal(sign, qExp, digitLen, bitLen)
}

internal fun calcSeal(sign01: Int, qExp: Int, dw1: ULong, dw0: ULong): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    return packSeal(sign01, qExp, digitLen, bitLen)
}

internal fun calcSeal(sign01: Int, dw0: ULong): Int {
    val bitLen = calcBitLen64(dw0)
    val digitLen = calcDigitLen64(bitLen, dw0)
    return packSeal(sign01, 0, digitLen, bitLen)
}

private const val SIGN_0 = 0
private const val SIGN_1 = 1

class Dec2 private constructor(
    // pronounced:
    // seal = Sign Exponent And Lengths
    internal val seal: Int,
    internal val dw1: ULong,
    internal val dw0: ULong
) {
    internal val bitLen: Int
        get() = seal and 0x1FF
    internal val digitLen: Int
        get() = (seal shr 9) and 0x7F

    internal val sign: Boolean
        get() = seal < 0
    internal val sign01: Int
        get() = seal ushr 31
    internal val sign0Neg1: Int
        get() = seal shr 31
    internal val qExp: Int
        get() = (seal shl 1) shr 17
    internal val sciExp: Int
        get() = qExp + (digitLen - (-digitLen ushr 31))

    constructor(sign: Boolean, dw1: ULong, dw0: ULong, qExp: Int) :
            this(calcSeal(sign, qExp, dw1, dw0), dw1, dw0)

    constructor(sign: Boolean, dw1: ULong, dw0: ULong, bitLen: Int, digitLen: Int, qExp: Int) :
            this(packSeal(sign, qExp, digitLen, bitLen), dw1, dw0)

    constructor(sign: Boolean, dw0: ULong) :
            this(calcSeal(if (sign) 1 else 0, dw0), 0uL, dw0)

    constructor(sign01: Int, dw1: ULong, dw0: ULong, bitLen: Int, digitLen: Int, qExp: Int) :
            this(packSeal(sign01, qExp, digitLen, bitLen), dw1, dw0)


    companion object {
        val POS_ZEROe0 = Dec2(SIGN_0, 0uL, 0uL)
        val NEG_ZEROe0 = POS_ZEROe0.negate()
        val POS_ONEe0 = from(1)
        val NEG_ONEe0 = POS_ONEe0.negate()
        val POS_INFINITY = Dec2(SIGN_0, 0uL, 0uL, 0, 0, NON_FINITE_INF)
        val NEG_INFINITY = POS_INFINITY.negate()
        val POS_QNAN = Dec2(SIGN_0, 0uL, 0uL, 0, 0, NON_FINITE_QNAN)
        val NEG_QNAN = POS_QNAN.negate()
        val POS_SNAN = Dec2(SIGN_0, 0uL, 0uL, 0, 0, NON_FINITE_SNAN)
        val NEG_SNAN = POS_SNAN.negate()

        fun from(n: Int): Dec2 = from(n.toLong())

        fun from(w: UInt): Dec2 = from(w.toULong())

        fun from(l: Long): Dec2 {
            val mask = l shr 63
            val abs = ((l xor mask) - mask).toULong()
            return Dec2(calcSeal(mask.toInt(), abs), 0uL, abs)
        }

        fun from(dw: ULong): Dec2 = Dec2(calcSeal(SIGN_0, dw), 0uL, dw)

        fun from(str: String): Dec2 {
            // parse only Decimal128
            // 34 digits ... exponent in range
            // unsignedMulHi will give up to 128 bits ... good
            // no rounding.
            // Any parse error throws IllegalArgumentException("invalid decimal format")
            // if someone wants something more complicated then they use DecEnv.parse()
            TODO()
        }

        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

        private val SPECIAL_VALUE_STRINGS = arrayOf(
            "Infinity", "-Infinity", "NaN", "-NaN", "sNaN", "-sNaN"
        )

        private val SMALL_INTEGER_STRINGS = arrayOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
            "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
            "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
        )

        fun parseInfinityText(str: String): Dec2? {
            if (str.length < 3)
                return null
            var ch = str[0].code
            var ich = 1
            val sign = ch == '-'.code
            if (ch == '-'.code || ch == '+'.code)
                ch = str[ich++].code
            ch = ch or 0x20
            for (target in "infinity") {
                if (ch != target.code)
                    return null
                if (ich == str.length)
                    break
                ch = str[ich++].code or 0x20
            }
            if (ich != str.length || (ch != 'f'.code && ch != 'y'.code))
                return null
            return infinity(sign)
        }

        inline fun tenPow(n: Int): ULong {
            var exp = n
            var pow = 1uL
            var base = 10uL
            while (exp != 0) {
                if ((exp and 1) != 0)
                    pow *= base
                exp = exp ushr 1
                if (exp != 0)
                    base *= base
            }
            return pow
        }

        fun parseNanText(str: String): Dec2? {
            if (str.length < 3)
                return null
            var ch = str[0].code
            var ich = 1
            val sign = ch == '-'.code
            if (ch == '-'.code || ch == '+'.code)
                ch = str[ich++].code
            val hasS = (ch or 0x20) == 's'.code
            if (hasS)
                ch = str[ich++].code
            if ((str.length - ich) < 2)
                return null
            if (((ch or 0x20) != 'n'.code) or
                ((str[ich].code or 0x20) != 'a'.code) or
                ((str[ich+1].code or 0x20) != 'n'.code))
                return null
            ich += 2
            var payloadDigitCount = 0
            var accumulator19 = 0uL
            var accumulator34 = 0uL
            while (ich < str.length) {
                val chDigit = str[ich++]
                if (chDigit !in '0'..'9')
                    return null
                val d = (chDigit - '0').toULong()
                if (payloadDigitCount > 0 || d != 0uL) {
                    when {
                        payloadDigitCount < 19 -> accumulator19 = (accumulator19 * 10uL) + d
                        payloadDigitCount < 34 -> accumulator34 = (accumulator34 * 10uL) + d
                        else -> return null
                    }
                    ++payloadDigitCount
                    }
            }
            var dw1 = 0uL
            var dw0 = accumulator19
            if (payloadDigitCount > 19) {
                val m = tenPow(payloadDigitCount - 19)
                dw0 = accumulator19 * m
                dw1 = unsignedMulHi(accumulator19, m)
                dw0 += accumulator34
                dw1 += if (dw0 < accumulator34) 1uL else 0uL
            }
            if ((dw1 or dw0) == 0uL) {
                return when {
                    !hasS && !sign -> POS_QNAN
                    !hasS && sign -> NEG_QNAN
                    sign -> NEG_SNAN
                    else -> POS_SNAN
                }
            }
            return Dec2(sign, dw1, dw0, if (hasS) NON_FINITE_SNAN else NON_FINITE_QNAN)
        }

        fun parseFiniteValueText(str: String): Dec2? {
            var hasCoefficientDigit = false
            var significantDigitCount = 0 // does not count leading zeros
            var hasDot = false
            var hasExp = false
            var hasExpDigit = false
            var expSign = false
            var expSignificantDigitCount = 0

            if (str.length == 0)
                return null
            var ch = str[0]
            var ich = 1
            var chLast = '\u0000'

            val sign = ch == '-'
            if (ch == '-' || ch == '+') {
                if (ich == str.length)
                    return null
                ch = str[ich++]
            }
            var fractionalDigitCount = 0
            var coeff19 = 0uL
            var coeff34 = 0uL
            var exp = 0

            while (ch in '0'..'9' || ch == '.' || ch == '_') {
                when {
                    ch in '0'..'9' -> {
                        val n = ch - '0'
                        hasCoefficientDigit = true
                        // increment if we have seen other digits or n != 0
                        significantDigitCount += (-(significantDigitCount or n)) ushr 31
                        if (significantDigitCount <= 19) {
                            coeff19 = coeff19 * 10uL + n.toULong()
                        } else if (significantDigitCount <= 34) {
                            coeff34 = coeff34 * 10uL + n.toULong()
                        } else {
                            return null
                        }
                        if (hasDot)
                            ++fractionalDigitCount
                    }
                    ch == '.' -> {
                        if (hasDot || chLast == '_')
                            return null
                        hasDot = true
                    }
                    ch == '_' -> {
                        if (! hasCoefficientDigit)
                            return null
                        if (hasDot && fractionalDigitCount == 0)
                            return null
                    }
                }
                chLast = ch
                ch = if (ich < str.length) str[ich++] else '\u0000'
            }
            if (ch == 'E' || ch == 'e') {
                if (chLast == '_')
                    return null
                hasExp = true
                ch = if (ich < str.length) str[ich++] else '\u0000'
                if (ch == '_')
                    return null
                if (ch == '+' || ch == '-') {
                    expSign = ch == '-'
                    ch = if (ich < str.length) str[ich++] else '\u0000'
                }
                while (ch in '0'..'9' || ch == '_') {
                    if (ch != '_') {
                        hasExpDigit = true
                        val eDigit = ch - '0'
                        expSignificantDigitCount +=
                            (eDigit or -expSignificantDigitCount) ushr 31
                        exp = exp * 10 + (ch - '0')
                    } else {
                        if (! hasExpDigit)
                            return null
                    }
                    chLast = ch
                    ch = if (ich < str.length) str[ich++] else '\u0000'
                }
            }
            if (ch != '\u0000' ||
                ich != str.length ||
                chLast == '_' ||
                ! hasCoefficientDigit ||
                hasExp && !hasExpDigit ||
                expSignificantDigitCount > 4)
                return null
            // we have at least one digit
            var dw0T = coeff19
            var dw1T = 0uL
            if (significantDigitCount > 19) {
                val m = tenPow(significantDigitCount - 19)
                dw0T = coeff19 * m
                dw1T = unsignedMulHi(coeff19, m)
                dw0T += coeff34
                dw1T += if (dw0T < coeff34) 1uL else 0uL
            }
            val signedExp = if (expSign) -exp else exp
            // fractionalDigitCount can exceed significantDigitCount ... 0.00000123
            val integerDigitCount = significantDigitCount - fractionalDigitCount
            val qExp = signedExp - fractionalDigitCount
            val bitLen = calcBitLen128(dw1T, dw0T)
            val digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
            // FIXME ... make sure that this is clamping properly
            // or just document that it won't accept subnormals
            if (qExp < -6176 || qExp > 6111)
                return null
            return Dec2(packSeal(sign, qExp, digitLen, bitLen), dw1T, dw0T)
        }


    }

    fun negate(): Dec2 = Dec2(seal xor Int.MIN_VALUE, dw1, dw0)

    override fun toString(): String {
        return when {
            qExp == 0 -> toIntegerString()
            qExp >= MIN_SPECIAL_VALUE -> toSpecialValueString()
            qExp < 0 && sciExp >= -6 -> toDecimalPointString()
            else -> toNormalizedScientificString()
        }
    }

    private fun toSpecialValueString(): String {
        if (qExp < NON_FINITE_QNAN)
            return SPECIAL_VALUE_STRINGS[sign01]
        val nanIndex = (if (qExp == NON_FINITE_QNAN) 2 else 4) + sign01
        val nanStr = SPECIAL_VALUE_STRINGS[nanIndex]
        if ((dw1 or dw0) == 0uL)
            return nanStr
        val utf8 = ByteArray(nanStr.length + digitLen)
        for (i in nanStr.indices)
            utf8[i] = nanStr[i].code.toByte()
        u128ToUtf8(utf8, nanStr.length, digitLen, dw1, dw0)
        return String(utf8)
    }

    private fun toIntegerString(): String {
        if (bitLen < 4) {
            val i = ((16 and sign0Neg1) + dw0.toInt()) and 0x1F // bounds-check-elimination
            return SMALL_INTEGER_STRINGS[i]
        }
        val utf8 = ByteArray(sign01 + digitLen)
        utf8[0] = '-'.code.toByte() // will be overwritten if positive
        u128ToUtf8(utf8, sign01, digitLen, dw1, dw0)
        return utf8.decodeToString()
    }

    private fun toDecimalPointString(): String {
        val digitsRightOfDecimal = -qExp
        val leadingZeroCount = max(1 + digitsRightOfDecimal - digitLen, 0)
        val signLen = sign01
        val decimalPointLen = 1
        val totalLen = signLen + leadingZeroCount + decimalPointLen + digitLen
        val utf8 = ByteArray(totalLen)
        utf8[0] = '-'.code.toByte() // overwritten when positive
        for (i in signLen..leadingZeroCount) // there is one extra here
            utf8[i] = '0'.code.toByte()
        u128ToUtf8(utf8, signLen + leadingZeroCount, digitLen, dw1, dw0)
        for (i in totalLen-1 downTo totalLen-digitsRightOfDecimal)
            utf8[i] = utf8[i - 1]
        utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
        return utf8.decodeToString()
    }

    private fun toNormalizedScientificString(): String {
        val eExp = sciExp
        val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
        val signLen = sign01
        val decimalPointLen = if (digitLen > 1) 1 else 0
        val printedDigitLen = max(digitLen, 1)
        val expELen = 1
        val expSignLen = 1
        val expSignByte = (if (eExp < 0) '-' else '+').code.toByte()
        val expDigitLen = max(calcDigitLen64(eExpAbs.toULong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        u128ToUtf8(utf8, signLen + decimalPointLen, printedDigitLen, dw1, dw0)
        if (decimalPointLen > 0) {
            utf8[signLen] = utf8[signLen + 1]
            utf8[signLen + 1] = '.'.code.toByte()
        }
        val iE = signLen + decimalPointLen + printedDigitLen
        utf8[iE] = 'E'.code.toByte()
        utf8[iE + 1] = expSignByte
        val j = renderChunkTail(eExpAbs.toUInt(), utf8, utf8.size)
        check (j == expDigitLen)
        return utf8.decodeToString()
    }

    private fun u128ToUtf8(utf8: ByteArray, off: Int, digitLen: Int, dw1: ULong, dw0: ULong) {
        var dw1T = dw1
        var dw0T = dw0
        var i = off + digitLen
        var remainingDigits = digitLen
        while (dw1T != 0uL) {
            val dw1Q = dw1T / 1_0000_0000uL
            val dw1R = dw1T % 1_0000_0000uL
            val limb1 = (dw1R shl 32) or (dw0T shr 32)
            val dw0Qmid = limb1 / 1_0000_0000uL
            val dw0Rmid = limb1 % 1_0000_0000uL
            val limb0 = (dw0Rmid shl 32) or (dw0T and 0xFFFF_FFFFuL)
            val dw0Qlo = limb0 / 1_0000_0000uL
            val dw0Rlo = limb0 % 1_0000_0000uL
            val straddle = (dw0Qmid shl 32) + dw0Qlo
            dw0T = straddle
            dw1T = dw1Q
            renderChunk8(dw0Rlo, utf8, i)
            i -= 8
            remainingDigits -= 8
        }
        while (remainingDigits >= 8) {
            val t0 = dw0T / 1_0000_0000uL
            val r0 = dw0T % 1_0000_0000uL
            dw0T = t0
            renderChunk8(r0, utf8, i)
            i -= 8
            remainingDigits -= 8
        }
        if (remainingDigits > 0) {
            renderChunkTail(dw0T.toUInt(), utf8, i)
        }
    }


}

