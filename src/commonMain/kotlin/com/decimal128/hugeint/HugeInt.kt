@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.hugeint

import kotlin.math.absoluteValue


/**
 * Arbitrary-precision signed integers for Kotlin multi-platform, providing
 * a replacement for [java.math.BigInteger].
 *
 * Provides basic arithmetic operations `+ - * / %` thru kotlin operator
 * overloading. Also provides overloaded operators functions where the
 * other operand is a primitive integer type, allowing standard expression
 * syntax for arithmetic expressions involving a mixture of HugeInt and
 * Int/UInt/Long/ULong values.
 *
 * Implementation is sign-magnitude, with the zero value always
 * non-negative.
 *
 * Comparison with java.math.BigInteger:
 *
 * HugeInt is a smaller, simpler implementation than []java.math.BigInteger].
 * It is generally intended for hundreds of digits, not tens-of-thousands.
 * All multiplication is performed using the schoolbook method, and Knuth
 * Algorithm-D is used for all divisions. HugeInt does not offer the prime
 * number generation and testing functions found in java.math.BigInteger.
 *
 * HugeInt differs from java.math.BigInteger in the handling of binary and bit
 * manipulation functions. HugeInt binary boolean and bit manipulation
 * functions work only on the magnitude, ignore the sign, and return
 * non-negative results. This differs from java.math.BigInteger which claims:
 * _All operations behave as if BigIntegers were represented in twos-complement
 * notation (like Java's primitive integer types)._
 *
 * HugeInt arithmetic operator functions allow primitive types
 * `Int/UInt/Long/ULong` as operands. Contrast this with BigInteger
 * where arguments must be boxed as BigInteger before being passed to
 * BigInteger methods. This reduces heap allocation pressure.
 */
class HugeInt private constructor(val sign: Boolean, val magia: IntArray) {

    companion object {
        // all zero values *must* point to this instance of ZERO
        @JvmStatic
        val ZERO = HugeInt(false, Magia.ZERO)

        @JvmStatic
        val ONE = HugeInt(false, Magia.ONE)

        @JvmStatic
        val TEN = HugeInt(false, Magia.TEN)

        @JvmStatic
        val NEG_ONE = HugeInt(true, Magia.ONE) // share magia .. but no mutation allowed

        private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

        /**
         * Converts a 32-bit signed Int into a signed HugeInt.
         */
        @JvmStatic
        fun from(n: Int): HugeInt = when {
            n > 0 -> HugeInt(false, intArrayOf(n))
            n < 0 -> HugeInt(true, intArrayOf(-n))
            else -> ZERO
        }

        /**
         * Converts a 32-bit unsigned value stored in an Int primitive into a
         * non-negative HugeInt.
         */
        @JvmStatic
        fun fromUnsigned(n: Int) = from(n.toUInt())

        /**
         * Converts a 32-bits unsigned UInt into a non-negative HugeInt.
         */
        @JvmStatic
        fun from(un: UInt) = if (un != 0u) HugeInt(false, intArrayOf(un.toInt())) else ZERO

        /**
         * Converts a 64-bit signed Long into a signed HugeInt.
         */
        @JvmStatic
        fun from(l: Long) = when {
            l > 0L -> HugeInt(false, intArrayOf(l.toInt(), (l ushr 32).toInt()))
            l < 0L -> HugeInt(true, intArrayOf(-l.toInt(), (-l ushr 32).toInt()))
            else -> ZERO
        }

        /**
         * Converts a 64-bit unsigned value stored in an Int primitive into a
         * non-negative HugeInt.
         */
        @JvmStatic
        fun fromUnsigned(l: Long) = from(l.toULong())

        /**
         * Converts a 64-bit unsigned ULong into a non-negative HugeInt.
         */
        @JvmStatic
        fun from(ul: ULong) =
            if (ul != 0uL) HugeInt(false, intArrayOf(ul.toInt(), (ul shr 32).toInt())) else ZERO

        /**
         * Converts [String] representation of an integer to a
         * HugeInt.
         *
         * Standard decimal integer notation with optional leading `+
         * -`sign.
         * Embedded underscores allowed as separators.
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         * Also supports standard hex notation of leading `0x` after
         * the optional sign character. `-0xDEAD_BEEF`
         *
         * @param str The [String] to be parsed
         * @throws [IllegalArgumentException] empty [String] or invalid chars
         */
        @JvmStatic
        fun from(str: String) =
            from(StringLatin1Iterator(str, 0, str.length))

        /**
         * Converts range of chars within a [String] to a [HugeInt].
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param str The [String] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */
        @JvmStatic
        fun from(str: String, offset: Int, length: Int) =
            from(StringLatin1Iterator(str, offset, length))

        /**
         * Converts [CharSequence] representation of an integer to a
         * HugeInt.
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param csq The [CharSequence] to be parsed
         * @throws [IllegalArgumentException] empty [CharSequence] or
         * invalid chars
         */
        @JvmStatic
        fun from(csq: CharSequence) =
            from(CharSequenceLatin1Iterator(csq, 0, csq.length))

        /**
         * Converts range of chars within a [CharSequence] to a [HugeInt].
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param csq The [CharSequence] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */        @JvmStatic
        fun from(csq: CharSequence, offset: Int, length: Int) =
            from(CharSequenceLatin1Iterator(csq, offset, length))

        /**
         * Converts [CharArray] representation of an integer to a
         * HugeInt.
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param chars The [CharArray] to be parsed
         * @throws [IllegalArgumentException] empty [CharArray] or
         * invalid chars
         */
        @JvmStatic
        fun from(chars: CharArray) =
            from(CharArrayLatin1Iterator(chars, 0, chars.size))

        /**
         * Converts range of chars within a [CharArray] to a [HugeInt].
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param chars The [CharArray] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */        @JvmStatic
        fun from(chars: CharArray, offset: Int, length: Int) =
            from(CharArrayLatin1Iterator(chars, offset, length))

        /**
         * Converts UTF-8/ASCII [ByteArray] representation of an integer to a
         * HugeInt.
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param bytes The [ByteArray] to be parsed
         * @throws [IllegalArgumentException] empty [ByteArray] or
         * invalid chars
         */
        @JvmStatic
        fun fromAscii(bytes: ByteArray) =
            from(ByteArrayLatin1Iterator(bytes, 0, bytes.size))

        /**
         * Converts range of chars within a UTF-8/ASCII [ByteArray] to a
         * [HugeInt].
         *
         * Standard decimal integer notation with optional leading `+ -`sign.
         * Embedded underscores `978_654_321` allowed as separators.
         * Supports standard hexadecimal indicator of leading `0x/0X`
         * after the optional sign character. `-0xDEAD_BEEF`
         * Trailing decimal point dot `123.` is not allowed.
         * Scientific notation `6.02E23` is not allowed.
         *
         * @param bytes The UTF-8/ASCII [ByteArray] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */       @JvmStatic
        fun fromAscii(bytes: ByteArray, offset: Int, length: Int) =
            from(ByteArrayLatin1Iterator(bytes, offset, length))

        /**
         * Converts hexadecimal [String] to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param str The [String] to be parsed
         * @throws [IllegalArgumentException] empty [String] or invalid chars
         */
        @JvmStatic
        fun fromHex(str: String) = fromHex(str, 0, str.length)

        /**
         * Converts hexadecimal range of chars within a UTF-8/ASCII [String]
         * to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param str The [String] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */
        @JvmStatic
        fun fromHex(str: String, offset: Int, length: Int) =
            fromHex(StringLatin1Iterator(str, offset, length))

        /**
         * Converts hexadecimal [CharSequence] to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param csq The [CharSequence] to be parsed
         * @throws [IllegalArgumentException] empty [CharSequence] or
         * invalid chars
         */
        @JvmStatic
        fun fromHex(csq: CharSequence) = fromHex(csq, 0, csq.length)

        /**
         * Converts hexadecimal range of chars within a [CharSequence]
         * to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param csq The [CharSequence] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */        @JvmStatic
        fun fromHex(csq: CharSequence, offset: Int, length: Int) =
            fromHex(CharSequenceLatin1Iterator(csq, offset, length))

        /**
         * Converts hexadecimal [CharArray] representation of an integer to a
         * HugeInt.
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param chars The [CharArray] to be parsed
         * @throws [IllegalArgumentException] empty [CharArray] or
         * invalid chars
         */
        @JvmStatic
        fun fromHex(chars: CharArray) = fromHex(chars, 0, chars.size)

        /**
         * Converts hexadecimal range of chars within a [CharArray]
         * to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param chars The [CharArray] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */        @JvmStatic
        fun fromHex(chars: CharArray, offset: Int, length: Int) =
            fromHex(CharArrayLatin1Iterator(chars, offset, length))

        /**
         * Converts hexadecimal chars within a UTF-8/ASCII [ByteArray]
         * to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param bytes The [ByteArray] to be parsed
         * @throws [IllegalArgumentException] empty [ByteArray] or
         * invalid chars
         */
        @JvmStatic
        fun fromHexAscii(bytes: ByteArray) = fromHexAscii(bytes, 0, bytes.size)

        /**
         * Converts hexadecimal range of chars within a UTF-8/ASCII [ByteArray]
         * to a [HugeInt].
         *
         * Hexadecimal `[0-9A-Fa-f]` representation, optional leading `0x/0X`
         * after optional `+ -` sign character ... `DEADBEEF` or `+0xDEAD_BEEF`
         *
         * @param bytes The UTF-8/ASCII [ByteArray] to be parsed
         * @param offset Starting index of the range
         * @param length Number of chars in the range
         * @throws [IllegalArgumentException] Empty range, invalid range, or
         * invalid chars
         */       @JvmStatic
        fun fromHexAscii(bytes: ByteArray, offset: Int, length: Int) =
            fromHex(ByteArrayLatin1Iterator(bytes, offset, length))

        /**
         * Parse a HugeInt thru a standard iterator for different text
         * representations.
         */
        private fun from(src: Latin1Iterator): HugeInt {
            val isNegative = src.peek() == '-'
            val magia = Magia.from(src)
            return if (magia.isNotEmpty()) HugeInt(isNegative, magia) else ZERO
        }

        /**
         * Parse a HugeInt thru a standard iterator for different text
         * representations.
         */
        private fun fromHex(src: Latin1Iterator): HugeInt {
            val isNegative = src.peek() == '-'
            val magia = Magia.fromHex(src)
            return if (magia.isNotEmpty()) HugeInt(isNegative, magia) else ZERO
        }

        /**
         * Converts the ByteArray in the twos-complement big-endian order
         * to a HugeInt.
         *
         * This is equivalent to the [java.math.BigInteger] constructor
         * `BigInteger(byte[] val)`.
         *
         * Zero length array returns HugeInt.ZERO
         */
        @JvmStatic
        fun fromTwosComplementBigEndianBytes(bytes: ByteArray) =
            fromTwosComplementBigEndianBytes(bytes, 0, bytes.size)

        /**
         * Converts the specified range of bytes in twos-complement big-endian
         * order to a HugeInt.
         *
         * Sign is determined by the highest bit of most significant byte.
         *
         * This is a direct replacement for the constructor `BigInteger(byte[] val)`
         *
         * @throws kotlin.IllegalArgumentException for invalid offset and/or length
         */
        @JvmStatic
        fun fromTwosComplementBigEndianBytes(bytes: ByteArray, offset: Int, length: Int): HugeInt {
            if (offset < 0 || length < 0 || length > bytes.size - offset)
                throw IllegalArgumentException()
            return when {
                length == 0 -> ZERO
                bytes[offset] >= 0 ->
                    HugeInt(false, Magia.fromNonNegativeBigEndianBytes(bytes, offset, length))
                else ->
                    HugeInt(true,
                            Magia.fromNegativeTwosComplementBigEndianBytes(bytes, offset, length))
            }
        }

        /**
         * Converts the ByteArray in twos-complement little-endian order to a
         * HugeInt.
         */
        @JvmStatic
        fun fromTwosComplementLittleEndianBytes(bytes: ByteArray) =
            fromTwosComplementLittleEndianBytes(bytes, 0, bytes.size)

        /**
         * Converts the binary integer in the specified range of bytes in twos-complement
         * little-endian order to a HugeInt.
         *
         * Sign is determined by the highest bit of most significant byte.
         *
         * @throws kotlin.IllegalArgumentException for invalid offset and/or length
         */
        @JvmStatic
        fun fromTwosComplementLittleEndianBytes(bytes: ByteArray, offset: Int, length: Int):
                HugeInt {
            if (offset < 0 || length < 0 || length > bytes.size - offset)
                throw IllegalArgumentException()
            return when {
                length == 0 -> ZERO
                bytes[offset + length - 1] >= 0 ->
                    HugeInt(false, Magia.fromNonNegativeLittleEndianBytes(bytes, offset, length))
                else ->
                    HugeInt(true,
                            Magia.fromNegativeTwosComplementLittleEndianBytes(bytes, offset, length))
            }
        }

        fun fromTwosComplementBigEndianBytesX(bytes: ByteArray, offset: Int, length: Int): HugeInt {
            val limit = offset + length
            if (offset < 0 || limit > bytes.size)
                throw IllegalArgumentException(
                    "invalid offset:$offset or length:$length for bytes" +
                            ".size:${bytes.size}"
                )
            if (length == 0)
                return ZERO
            val signMask = bytes[offset].toInt() shr 7 // 0 or 0xFFFF_FFFF
            var ib = offset
            while (ib < length && bytes[ib].toInt() == signMask) // flush leading 0's or FF's
                ++ib
            if (ib == limit)
                return if (signMask == 0) ZERO else NEG_ONE
            val magia: IntArray = Magia.fromBigEndianBytes(signMask, bytes, ib, length - ib)
            if (signMask != 0) {
                var carry = 1L
                for (i in magia.indices) {
                    val t = (magia[i].inv().toLong() and 0xFFFF_FFFFL) + carry
                    magia[i] = t.toInt()
                    carry = t ushr 32
                }
            }
            return HugeInt(signMask != 0, magia)
        }

        /**
         * Converts little-endian IntArray into a HugeInt with the specified sign.
         */
        @JvmStatic
        fun fromLittleEndianIntArray(sign: Boolean, littleEndianIntArray: IntArray): HugeInt {
            val magia = Magia.newMinimumCopy(littleEndianIntArray)
            return if (magia.isNotEmpty()) HugeInt(sign, magia) else ZERO
        }

        /**
         * Constructs a positive HugeInt with a single bit turned on at the zero-based bitIndex.
         *
         * @throws kotlin.IllegalArgumentException for a negative bitIndex
         */
        @JvmStatic
        fun withSetBit(bitIndex: Int): HugeInt {
            if (bitIndex < 0)
                throw IllegalArgumentException("negative bitIndex:$bitIndex")
            if (bitIndex == 0)
                return ONE
            val magia = Magia.newWithBitLen(bitIndex + 1)
            magia[magia.lastIndex] = 1 shl (bitIndex and 0x1F)
            return HugeInt(false, magia)
        }

        /**
         * Constructs a non-negative HugeInt with`bitWidth` bits turned on.
         */
        @JvmStatic
        fun withBitMask(bitWidth: Int): HugeInt {
            if (bitWidth < 0)
                throw IllegalArgumentException("negative bitWidth:$bitWidth")
            if (bitWidth == 0)
                return ZERO
            val magia = Magia.newWithBitLen(bitWidth)
            magia.fill(-1)
            val leadingZeroCount = (magia.size * 32) - bitWidth
            magia[magia.lastIndex] = -1 ushr leadingZeroCount
            return HugeInt(false, magia)
        }

        /**
         * Constructs a non-negative HugeInt with `bitWidth` bits set starting and `bitIndex`.
         *
         * @param bitIndex 0-based indexing starting at the least-significant-bit
         * @param bitWidth number of bits to turn on
         *
         * @throws kotlin.IllegalArgumentException on negative bitIndex or negative bitWidth
         */
        @JvmStatic
        fun withIndexedBitMask(bitIndex: Int, bitWidth: Int): HugeInt {
            if (bitIndex < 0 || bitWidth < 0)
                throw IllegalArgumentException(
                    "illegal negative arg bitIndex:$bitIndex bitCount:$bitWidth"
                )
            if (bitWidth <= 1)
                return if (bitWidth == 0) ZERO else withSetBit(bitIndex)
            if (bitIndex == 0)
                return withBitMask(bitWidth)
            val bitLen = bitIndex + bitWidth
            val magia = Magia.newWithBitLen(bitLen)
            var wordIndex = bitIndex ushr 5
            val initialInnerIndex = bitIndex and 0x1F
            val initialBitCount = Math.min(bitWidth, 32 - initialInnerIndex)
            val initialMask = (((1L shl initialBitCount) - 1) shl initialInnerIndex).toInt()
            magia[wordIndex++] = initialMask
            var remainingBitCount = bitWidth - initialBitCount
            if (remainingBitCount > 0) {
                while (remainingBitCount > 32) {
                    magia[wordIndex++] = -1
                    remainingBitCount -= 32
                }
                if (remainingBitCount > 0)
                    magia[wordIndex] = (1 shl remainingBitCount) - 1
            }
            return HugeInt(false, magia)
        }

    }

    /**
     * Predicate to test for zero value.
     */
    fun isZero() = this === ZERO

    /**
     * Predicate to test for non-zero value.
     */
    fun isNotZero() = this !== ZERO

    /**
     * Predicate to test if < 0.
     */
    fun isNegative() = sign

    /**
     * Standard signum function.
     */
    fun signum() = if (sign) -1 else if (isZero()) 0 else 1

    fun magnitudeIntArrayLen() = (Magia.bitLen(magia) + 31) ushr 5
    fun magnitudeLongArrayLen() = (Magia.bitLen(magia) + 63) ushr 6

    /**
     * Returns true if there is exactly one bit set in the magnitude.
     */
    fun isMagnitudePowerOfTwo(): Boolean {
        var bitSeen = false
        for (w in this.magia) {
            if (w == 0)
                continue
            if ((w and (w - 1)) != 0)
                return false
            if (bitSeen)
                return false
            bitSeen = true
        }
        return bitSeen
    }

    /**
     * Predicate to test if HugeInt in Int.MIN_VALUE..Int.MAX_VALUE
     */
    fun fitsInt(): Boolean {
        val bitLen = Magia.bitLen(magia)
        return bitLen <= 31 || sign && bitLen == 32 && magia[0] == Int.MIN_VALUE
    }

    /**
     * Predicate to test if HugeInt in 0..UInt.MAX_VALUE
     */
    fun fitsUInt() = !sign && Magia.bitLen(magia) <= 32

    /**
     * Predicate to test if HugeInt in Long.MIN_VALUE..Long.MAX_VALUE
     */
    fun fitsLong(): Boolean {
        val bitLen = Magia.bitLen(magia)
        return bitLen <= 63 || sign && bitLen == 64 && magia[0] == 0 && magia[1] == Int.MIN_VALUE
    }

    /**
     * Predicate to test if HugeInt in 0..ULong.MAX_VALUE
     */
    fun fitsULong() = !sign && Magia.bitLen(magia) <= 64

    /**
     * Returns lowest 32 bits of the magnitude of this HugeInt.
     */
    fun magnitudeRawInt() = if (magia.size != 0) magia[0] else 0

    /**
     * Returns lowest 32 bits of the magnitude of this HugeInt.
     */
    fun magnitudeRawUInt() = magnitudeRawInt().toUInt()

    /**
     * Returns lowest 64 bits of the magnitude of this HugeInt.
     */
    fun magnitudeRawLong() = when {
        magia.size == 0 -> 0L
        magia.size == 1 -> U32(magia[0])
        else -> (U32(magia[1]) shl 32) or (U32(magia[0]))
    }

    /**
     * Returns lowest 64 bits of the magnitude of this HugeInt.
     */
    fun magnitudeRawULong() = magnitudeRawLong().toULong()


    /**
     * Returns HugeInt value as a signed Int
     * clamped to the valid Int range.
     *
     * Values > Int.MAX_VALUE return Int.MAX_VALUE.
     * Values < Int.MIN_VALUE return Int.MIN_VALUE.
     */
    fun toInt(): Int {
        val bitLen = Magia.bitLen(magia)
        val magnitude = magia[0]
        return if (!sign) {
            if (bitLen <= 31) magnitude else Int.MAX_VALUE
        } else {
            if (bitLen <= 31) -magnitude else Int.MIN_VALUE
        }
    }

    /**
     * Returns HugeInt value as an unsigned UInt
     * clamped to the valid UInt range.
     *
     * Values > UInt.MAX_VALUE return UInt.MAX_VALUE.
     * Values < 0 return 0.
     */
    fun toUInt(): UInt {
        val bitLen = Magia.bitLen(magia)
        val magnitude = magia[0]
        return if (!sign) {
            if (bitLen <= 32) magnitude.toUInt() else UInt.MAX_VALUE
        } else {
            0u
        }
    }

    /**
     * Returns HugeInt as a signed Long
     * clamped to the valid Long range.
     *
     * Values > Int.MAX_VALUE return Int.MAX_VALUE.
     * Values < Int.MIN_VALUE return Int.MIN_VALUE.
     */
    fun toLong(): Long {
        val bitLen = Magia.bitLen(magia)
        val magnitude = when (magia.size) {
            0 -> 0L
            1 -> magia[0].toLong() and 0xFFFF_FFFFL
            else -> (magia[1].toLong() shl 32) or (magia[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 63) magnitude else Long.MAX_VALUE
        } else {
            if (bitLen <= 63) -magnitude else Long.MIN_VALUE
        }
    }

    /**
     * Returns HugeInt value as an unsigned ULong
     * clamped to the valid ULong range.
     *
     * Values > ULong.MAX_VALUE return ULong.MAX_VALUE.
     * Values < 0 return 0.
     */
    fun toULong(): ULong {
        val bitLen = Magia.bitLen(magia)
        val magnitude = when (magia.size) {
            0 -> 0L
            1 -> magia[0].toLong() and 0xFFFF_FFFFL
            else -> (magia[1].toLong() shl 32) or (magia[0].toLong() and 0xFFFF_FFFFL)
        }
        return if (!sign) {
            if (bitLen <= 64) magnitude.toULong() else ULong.MAX_VALUE
        } else {
            0uL
        }
    }

    // note that I am sharing the magia with negate and abs
    // but *no* mutation of magia is allowed
    /**
     * Returns a HugeInt with complementary sign and equivalent magnitude
     */
    fun negate() = if (isNotZero()) HugeInt(!sign, magia) else ZERO

    /**
     * Returns the absolute value of this HugeInt.
     */
    fun abs() = if (sign) HugeInt(false, magia) else this

    operator fun plus(other: HugeInt): HugeInt = this.addSubImpl(false, other)
    operator fun plus(n: Int): HugeInt = this.addSubImpl(false, n)
    operator fun plus(un: UInt): HugeInt = this.addSubImpl(false, un)
    operator fun plus(l: Long): HugeInt = this.addSubImpl(false, l)
    operator fun plus(ul: ULong): HugeInt = this.addSubImpl(false, ul)
    operator fun Int.plus(other: HugeInt) = other.addSubImpl(false, this)
    operator fun UInt.plus(other: HugeInt) = other.addSubImpl(false, this)
    operator fun Long.plus(other: HugeInt) = other.addSubImpl(false, this)
    operator fun ULong.plus(other: HugeInt) = other.addSubImpl(false, this)

    operator fun minus(other: HugeInt): HugeInt = this.addSubImpl(true, other)
    operator fun minus(n: Int): HugeInt = this.addSubImpl(true, n)
    operator fun minus(un: UInt): HugeInt = this.addSubImpl(true, un)
    operator fun minus(l: Long): HugeInt = this.addSubImpl(true, l)
    operator fun minus(ul: ULong): HugeInt = this.addSubImpl(true, ul)
    operator fun Int.minus(other: HugeInt) = other.addSubImpl(true, this)
    operator fun UInt.minus(other: HugeInt) = other.addSubImpl(true, this)
    operator fun Long.minus(other: HugeInt) = other.addSubImpl(true, this)
    operator fun ULong.minus(other: HugeInt) = other.addSubImpl(true, this)

    operator fun times(other: HugeInt): HugeInt {
        return if (isNotZero() && other.isNotZero())
            HugeInt(this.sign xor other.sign, Magia.newMul(this.magia, other.magia))
        else
            ZERO
    }

    operator fun times(n: Int): HugeInt {
        return if (isNotZero() && n != 0)
            HugeInt(this.sign xor (n < 0), Magia.newMul(this.magia, n.absoluteValue))
        else
            ZERO
    }

    operator fun times(un: UInt): HugeInt {
        return if (isNotZero() && un != 0u)
            ZERO
        else
            HugeInt(this.sign, Magia.newMul(this.magia, un.toInt()))
    }

    operator fun times(l: Long): HugeInt {
        return if (isNotZero() && l != 0L)
            HugeInt(this.sign xor (l < 0), Magia.newMul(this.magia, l.absoluteValue))
        else
            ZERO
    }

    operator fun times(ul: ULong): HugeInt {
        return if (isNotZero() && ul != 0uL)
            HugeInt(this.sign, Magia.newMul(this.magia, ul.toLong()))
        else
            ZERO
    }

    operator fun Int.times(other: HugeInt) = other.times(this)
    operator fun UInt.times(other: HugeInt) = other.times(this)
    operator fun Long.times(other: HugeInt) = other.times(this)
    operator fun ULong.times(other: HugeInt) = other.times(this)

    operator fun div(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, other.magia)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor other.sign, quot)
        }
        return ZERO
    }

    operator fun div(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, n.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (n < 0), quot)
        }
        return ZERO
    }

    operator fun div(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, un.toInt())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }

    operator fun div(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val quot = Magia.newDiv(this.magia, l.absoluteValue)
            if (quot.isNotEmpty())
                return HugeInt(this.sign xor (l < 0), quot)
        }
        return ZERO
    }

    operator fun div(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isZero()) {
            val quot = Magia.newDiv(this.magia, ul.toLong())
            if (quot.isNotEmpty())
                return HugeInt(this.sign, quot)
        }
        return ZERO
    }

    operator fun rem(other: HugeInt): HugeInt {
        if (other.isZero())
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, other.magia)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    operator fun rem(n: Int): HugeInt {
        if (n == 0)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, n.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    operator fun rem(un: UInt): HugeInt {
        if (un == 0u)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, un.toInt())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    operator fun rem(l: Long): HugeInt {
        if (l == 0L)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, l.absoluteValue)
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    operator fun rem(ul: ULong): HugeInt {
        if (ul == 0uL)
            throw ArithmeticException("div by zero")
        if (isNotZero()) {
            val rem = Magia.newMod(this.magia, ul.toLong())
            if (rem.isNotEmpty())
                return HugeInt(this.sign, rem)
        }
        return ZERO
    }

    /**
     * Perform an integer division, returning quotient and remainder.
     */
    fun divMod(other: HugeInt): Pair<HugeInt, HugeInt> {
        return when {
            other.isZero() -> throw ArithmeticException("div by zero")
            this.isNotZero() -> divModHelper(other.sign, other.magia)
            else -> ZERO to other
        }
    }

    fun divMod(n: Int): Pair<HugeInt, HugeInt> = divModIntHelper(n < 0, n.absoluteValue)
    fun divMod(un: UInt): Pair<HugeInt, HugeInt> = divModIntHelper(false, un.toInt())
    fun divMod(l: Long): Pair<HugeInt, HugeInt> = divModLongHelper(l < 0, l.absoluteValue)
    fun divMod(ul: ULong): Pair<HugeInt, HugeInt> = divModLongHelper(false, ul.toLong())

    /**
     * Compute the square of this HugeInt.
     */

    fun sqr(): HugeInt {
        if (this.isNotZero()) {
            check(Magia.nonZeroLimbLen(this.magia) > 0)
            val magiaSqr = Magia.newSqr(this.magia)
            return HugeInt(false, magiaSqr)
        }
        return ZERO
    }

    /**
     * Raise this HugeInt to the n power.
     *
     * @param n must be >= 0
     * @throws kotlin.IllegalArgumentException when n < 0
     */
    fun pow(n: Int): HugeInt {
        return when {
            n < 0 -> throw ArithmeticException("cannot raise HugeInt to negative power:$n")
            n == 0 -> ONE
            this.isZero() -> ZERO
            n == 1 -> this
            n == 2 -> sqr()
            else -> {
                val resultSign = this.sign && ((n and 1) != 0)
                var baseMag = this.magia
                var resultMag = Magia.ONE
                var exp = n
                while (true) {
                    if ((exp and 1) != 0)
                        resultMag = Magia.newMul(resultMag, baseMag)
                    exp = exp ushr 1
                    if (exp == 0)
                        break
                    baseMag = Magia.newSqr(baseMag)
                }
                HugeInt(resultSign, resultMag)
            }
        }
    }

    /**
     * Bit-length of the magnitude.
     */
    fun magnitudeBitLen() = Magia.bitLen(magia)

    /**
     * Gives the same answer as BigInteger.bitLength().
     *
     * BigInteger.bitLength() tries to give a pseudo-twos-complement answer.
     * They give the number of bits required, minus the sign bit.
     * For non-negative values there is no confusion.
     * For negative values BigInteger.bitLength() things become a little strange.
     * `BigInteger("-1").bitLength() == 0` ... think about it :)
     */
    fun bitLengthBigIntegerStyle(): Int {
        val magBitLen = magnitudeBitLen()
        val isNegPowerOfTwo = sign && isMagnitudePowerOfTwo()
        val bitLengthBigIntegerStyle = magBitLen - if (isNegPowerOfTwo) 1 else 0
        return bitLengthBigIntegerStyle
    }

    /**
     * Calculates the number of bytes required to represent this HugeInt in twos-complement.
     */
    fun calc2sComplementByteLength(): Int {
        if (isZero())
            return 1
        val bitLen2sComplement = bitLengthBigIntegerStyle() + 1
        val byteLength = (bitLen2sComplement + 7) ushr 3
        return byteLength
    }

    /**
     * Predicate to test whether the bit at the specified bitIndex is set.
     */
    fun testBit(bitIndex: Int): Boolean = Magia.testBit(this.magia, bitIndex)

    /**
     * Perform a boolean AND of the two magnitudes, while ignoring the signs.
     *
     * Sign of the result is non-negative.
     */
    infix fun and(other: HugeInt): HugeInt {
        val magiaAnd = Magia.newAnd(this.magia, other.magia)
        return if (magiaAnd.isNotEmpty()) HugeInt(false, magiaAnd) else ZERO
    }

    /**
     * Perform a boolean OR of the two magnitudes, while ignoring the signs.
     *
     * Sign of the result is non-negative.
     */
    infix fun or(other: HugeInt): HugeInt {
        val magiaOr = Magia.newOr(this.magia, other.magia)
        return if (magiaOr.isNotEmpty()) HugeInt(false, magiaOr) else ZERO
    }

    /**
     * Perform a boolean XOR of the two magnitudes, while ignoring the signs.
     *
     * Sign of the result is non-negative.
     */
    infix fun xor(other: HugeInt): HugeInt {
        val magiaXor = Magia.newXor(this.magia, other.magia)
        return if (magiaXor.isNotEmpty()) HugeInt(false, magiaXor) else ZERO
    }

    /**
     * Shifts the magnitude to the right, ignores the sign bit, and always
     * return a non-negative result.
     *
     * @param bitCount >= 0
     * @throws kotlin.IllegalArgumentException when bitCount < 0
     */
    infix fun ushr(bitCount: Int): HugeInt {
        return when {
            bitCount < 0 -> throw IllegalArgumentException()
            bitCount == 0 -> this
            else -> {
                val magia = Magia.newShiftRight(this.magia, bitCount)
                return if (magia.isNotEmpty()) HugeInt(false, magia) else ZERO
            }
        }
    }

    /**
     * Performs a signed shift to the right, giving the same results
     * as twos-complement for negative input values.
     *
     * @param bitCount >= 0
     * @throws kotlin.IllegalArgumentException when bitCount < 0
     */
    infix fun shr(bitCount: Int): HugeInt {
        return when {
            bitCount < 0 -> throw IllegalArgumentException()
            bitCount == 0 -> this
            else -> {
                var magia = Magia.newShiftRight(this.magia, bitCount)
                if (sign) {
                    // mimic twos-complement behavior
                    val roundDown = Magia.testAnyBitInLowerN(this.magia, bitCount)
                    if (roundDown)
                        magia = Magia.newOrMutateAdd(magia, 1)
                }
                return if (magia.isNotEmpty()) HugeInt(this.sign, magia) else ZERO
            }
        }
    }

    /**
     * Shifts left by bitCount, retaining sign of the input.
     *
     * @param bitCount >= 0
     * @throws kotlin.IllegalArgumentException when bitCount < 0
     */
    infix fun shl(bitCount: Int): HugeInt {
        return when {
            bitCount < 0 -> throw IllegalArgumentException()
            bitCount == 0 -> this
            isZero() -> this
            else -> HugeInt(this.sign, Magia.newShiftLeft(this.magia, bitCount))
        }
    }

    operator fun compareTo(other: HugeInt): Int {
        if (this.sign != other.sign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, other.magia)
        return if (this.sign) -cmp else cmp
    }

    operator fun compareTo(n: Int) = compareToHelper(n < 0, n.absoluteValue.toUInt().toULong())
    operator fun compareTo(un: UInt) = compareToHelper(false, un.toULong())
    operator fun compareTo(l: Long) = compareToHelper(l < 0, l.absoluteValue.toULong())
    operator fun compareTo(ul: ULong) = compareToHelper(false, ul)

    /**
     * Compares magnitudes, disregarding sign flags.
     *
     * @return -1,0,1
     */
    fun magnitudeCompareTo(other: HugeInt) = Magia.compare(this.magia, other.magia)
    fun magnitudeCompareTo(n: Int) = Magia.compare(this.magia, n)
    fun magnitudeCompareTo(un: UInt) = Magia.compare(this.magia, un.toInt())
    fun magnitudeCompareTo(l: Long) = Magia.compare(this.magia, l)
    fun magnitudeCompareTo(ul: ULong) = Magia.compare(this.magia, ul.toLong())
    fun magnitudeCompareTo(littleEndianIntArray: IntArray) =
        Magia.compare(this.magia, littleEndianIntArray)

    /**
     * Comparison predicate for numerical equality.
     */
    infix fun EQ(other: HugeInt): Boolean =
        (this.sign == other.sign) && Magia.EQ(this.magia, other.magia)

    infix fun EQ(n: Int): Boolean = compareTo(n) == 0
    infix fun EQ(un: UInt): Boolean = compareTo(un) == 0
    infix fun EQ(l: Long): Boolean = compareTo(l) == 0
    infix fun EQ(ul: ULong): Boolean = compareTo(ul) == 0

    /**
     * Comparison predicate for numerical inequality.
     */
    infix fun NE(other: HugeInt): Boolean =
        (this.sign != other.sign) || !Magia.EQ(this.magia, other.magia)

    infix fun NE(n: Int): Boolean = compareTo(n) != 0
    infix fun NE(un: UInt): Boolean = compareTo(un) != 0
    infix fun NE(l: Long): Boolean = compareTo(l) != 0
    infix fun NE(ul: ULong): Boolean = compareTo(ul) != 0

    override fun equals(other: Any?): Boolean {
        return ((other is HugeInt) &&
                (this.sign == other.sign) &&
                Magia.EQ(this.magia, other.magia))
    }

    override fun toString() = Magia.toString(this.sign, this.magia)
    fun toHexString() = Magia.toHexString(this.sign, this.magia)

    fun toBigEndianTwosComplementByteArray(): ByteArray {
        val byteLen = calc2sComplementByteLength()
        val bytes = ByteArray(byteLen)
        val magBitLen = magnitudeBitLen()
        val magByteLen = (magBitLen + 7) ushr 3
        writeBigEndianTwosComplementBytes(bytes, byteLen - magByteLen, magByteLen)
        return bytes
    }

    fun toBigEndianTwosComplementByteArray(bytes: ByteArray): Int {
        val byteLen = calc2sComplementByteLength()
        if (bytes.size < byteLen)
            throw IllegalArgumentException("target ByteArray too small")
        val magBitLen = magnitudeBitLen()
        val magByteLen = (magBitLen + 7) ushr 3
        writeBigEndianTwosComplementBytes(bytes, byteLen - magByteLen, magByteLen)
        return byteLen
    }

    fun toLittleEndianIntArray(): IntArray {
        if (sign)
            throw RuntimeException("not implemented for negative value")
        return Magia.newMinimumCopy(magia)
    }

    fun toLittleEndianLongArray(): LongArray {
        if (sign)
            throw RuntimeException("not implemented for negative value")
        val intLen = Magia.nonZeroLimbLen(magia)
        val longLen = (intLen + 1) ushr 1
        val z = LongArray(longLen)
        var iw = 0
        var il = 0
        while (intLen - iw >= 2) {
            val lo = U32(magia[iw])
            val hi = magia[iw + 1].toLong() shl 32
            val l = hi or lo
            z[il] = l
            ++il
            iw += 2
        }
        if (iw < intLen)
            z[il] = U32(magia[iw])
        return z
    }

    private fun addSubImpl(isSub: Boolean, other: HugeInt): HugeInt {
        if (other === ZERO)
            return this
        if (this === ZERO)
            return if (isSub) other.negate() else other
        val otherSign = isSub xor other.sign
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, other.magia))
        val cmp = this.magnitudeCompareTo(other)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, other.magia))
            cmp < 0 -> HugeInt(otherSign, Magia.newSub(other.magia, this.magia))
            else -> ZERO
        }
        return ret
    }

    private fun addSubImpl(isSub: Boolean, n: Int): HugeInt {
        val otherSign = n < 0
        val otherMag = n.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toUInt())
    }

    private fun addSubImpl(isSub: Boolean, un: UInt): HugeInt {
        if (un == 0u)
            return this
        if (isZero()) {
            val magia = intArrayOf(un.toInt())
            return HugeInt(isSub, magia)
        }
        val otherSign = isSub
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, un.toInt()))
        val cmp = this.magnitudeCompareTo(un)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, un.toInt()))
            cmp < 0 -> HugeInt(otherSign, intArrayOf(un.toInt() - this.magia[0]))
            else -> ZERO
        }
        return ret
    }

    private fun addSubImpl(isSub: Boolean, l: Long): HugeInt {
        val otherSign = l < 0L
        val otherMag = l.absoluteValue
        return addSubImpl(isSub xor otherSign, otherMag.toULong())
    }

    private fun addSubImpl(isSub: Boolean, ul: ULong): HugeInt {
        val hi = ul shr 32
        if (hi == 0uL)
            return addSubImpl(isSub, hi.toUInt())
        val otherSign = isSub
        if (this.sign == otherSign)
            return HugeInt(this.sign, Magia.newAdd(this.magia, ul.toLong()))
        val cmp = this.magnitudeCompareTo(ul)
        val ret = when {
            cmp > 0 -> HugeInt(sign, Magia.newSub(this.magia, ul.toLong()))
            cmp < 0 -> {
                val thisMag = this.toULong()
                val diff = ul - thisMag
                HugeInt(otherSign, Magia.newFromLong(diff.toLong()))
            }

            else -> ZERO
        }
        return ret
    }

    private fun canonicalizeZero(sign: Boolean, magia: IntArray): HugeInt {
        if (magia === Magia.ZERO)
            return ZERO
        check(Magia.nonZeroLimbLen(magia) > 0)
        return HugeInt(sign, magia)
    }


    private fun divModHelper(otherSign: Boolean, otherMagia: IntArray): Pair<HugeInt, HugeInt> {
        val (magiaQuot, magiaRem) = Magia.newDivMod(this.magia, otherMagia)
        val hiQuot =
            if (magiaQuot.isNotEmpty()) HugeInt(this.sign xor otherSign, magiaQuot) else ZERO
        val hiRem = if (magiaRem.isNotEmpty()) HugeInt(this.sign, magiaRem) else ZERO
        return hiQuot to hiRem
    }

    private fun divModIntHelper(nSign: Boolean, nMag: Int): Pair<HugeInt, HugeInt> {
        return when {
            nMag == 0 -> throw ArithmeticException("div by zero")
            this.isNotZero() -> {
                val quot = Magia.newMinimumCopy(this.magia)
                val remN = Magia.mutateDivideRemainder(quot, nMag).toInt()
                val hiQuot =
                    if (Magia.nonZeroLimbLen(quot) > 0) HugeInt(this.sign xor nSign, quot) else ZERO
                val hiRem = if (remN != 0) HugeInt(this.sign, intArrayOf(remN)) else ZERO
                return hiQuot to hiRem
            }

            else -> ZERO to HugeInt(nSign, intArrayOf(nMag))
        }
    }

    private fun divModLongHelper(lSign: Boolean, lMag: Long): Pair<HugeInt, HugeInt> {
        val lo = lMag.toInt()
        val hi = (lMag ushr 32).toInt()
        return when {
            lMag == 0L -> throw ArithmeticException("div by zero")
            hi == 0 -> divModIntHelper(lSign, lo)
            else -> divModHelper(lSign, intArrayOf(lo, hi))
        }
    }

    private fun compareToHelper(ulSign: Boolean, ulMag: ULong): Int {
        if (this.sign != ulSign)
            return if (this.sign) -1 else 1
        val cmp = Magia.compare(this.magia, ulMag.toLong())
        return if (!ulSign) cmp else -cmp
    }

    private fun writeBigEndianTwosComplementBytes(bytes: ByteArray, offset: Int, magByteLen: Int) {
        val last = offset + magByteLen - 1
        var dest = last
        var remaining = magByteLen
        var i = 0
        val complementMask = if (sign) -1 else 0
        bytes[0] = complementMask.toByte()
        while (remaining > 0 && i < magia.size) {
            var w = magia[i] xor complementMask
            var j = 4
            do {
                bytes[dest--] = w.toByte()
                w = w ushr 8
                --remaining
                --j
            } while (remaining > 0 && j > 0)
            ++i
        }
        if (sign) {
            var carry = 1
            var k = last
            while (carry > 0 && k >= 0) {
                val t = (bytes[k].toInt() and 0xFF) + carry
                bytes[k] = t.toByte()
                carry = t ushr 8
                --k
            }
        }
    }

    override fun hashCode(): Int {
        var result = sign.hashCode()
        result = 31 * result + magia.contentHashCode()
        return result
    }


}