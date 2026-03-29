// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

private const val EMPTY_STRING = ""
private val EMPTY_CHAR_ARRAY = CharArray(0)
private val EMPTY_BYTE_ARRAY = ByteArray(0)
/**
 * Abstract iterator over a sequence of characters stored in Latin-1 (ISO-8859-1) format.
 *
 * Provides sequential and reverse iteration over a contiguous range of characters,
 * as well as utility functions to inspect or reset the current position.
 *
 * The iterator is zero-based with respect to the underlying sequence and
 * tracks its position internally.
 *
 * @param len the length of the iteration window
 */
abstract class Latin1Iterator(len: Int) {
    protected var len = len
    protected var i = 0

    /** Returns true if there are more characters to read in the forward direction. */
    fun hasNext() = (i < len)

    /** Returns the current character without advancing the iterator, or '\u0000' if at end. */
    fun peek() = if (i < len) getCharAt(i) else '\u0000'

    /** Returns the number of remaining characters from the current position to the end. */
    fun remainingLen() = len - i

    /** Returns the next character and advances the iterator, or '\u0000' if at end. */
    fun nextChar() = if (i < len) getCharAt(i++) else '\u0000'

    fun nextCharCode() = nextChar().code

    /** Moves the iterator backward by one and returns that character, or '\u0000' if at start. */
    fun prevChar() = if (i > 0) getCharAt(--i) else '\u0000'

    /** Rewinds the iterator to the initial position and returns `this`. */
    fun rewind(): Latin1Iterator { i = 0; return this }

    /** Returns the length of the iteration window. */
    fun length() = len

    /** Returns the character at the specified absolute index in the underlying sequence. */
    abstract fun getCharAt(i: Int): Char

    abstract fun clear()

    /** Returns a string representation of the iterator's current window. */
    abstract override fun toString(): String

    init { require(len >= 0) { "Invalid offset/length range" } }
}

class StringLatin1Iterator(str: String): Latin1Iterator(str.length) {
    private var str = str

    /** Returns the character at the given absolute index in the underlying string. */
    override fun getCharAt(i: Int) = str[i]

    override fun clear() {
        super.i = 0
        super.len = 0
        str = EMPTY_STRING
    }

    fun reload(str: String): StringLatin1Iterator {
        this.str = str
        super.len = str.length
        super.i = 0
        return this
    }

    /** Returns the full string representation of this iterator. */
    override fun toString() = str

}

class CharSequenceLatin1Iterator(csq: CharSequence) : Latin1Iterator(csq.length) {
    private var csq = csq


    /** Returns the character at the given absolute index in the underlying [CharSequence]. */
    override fun getCharAt(i: Int) = csq[i]

    override fun clear() {
        i = 0
        len = 0
        csq = EMPTY_STRING
    }

    fun reload(csq: CharSequence): CharSequenceLatin1Iterator {
        this.csq = csq
        super.len = csq.length
        super.i = 0
        return this
    }

    /** Returns the string representation of the underlying [CharSequence]. */
    override fun toString() = csq.toString()
}


/**
 * [Latin1Iterator] implementation backed by a [CharArray].
 *
 * Provides sequential and reverse iteration over a contiguous subsequence of [chars],
 * using the specified offset and length.
 *
 * @property chars the underlying [CharArray] to iterate over
 * @param off the starting offset of the iteration window in the array
 * @param len the length of the iteration window
 *
 * @constructor Creates an iterator for the subsequence `chars[off..off+len-1]`.
 * @throws IllegalArgumentException if the offset and length are out of bounds
 */
class CharArrayLatin1Iterator(chars: CharArray, off: Int, len: Int) : Latin1Iterator(len) {
    private var chars = chars
    private var off = off

    /** Convenience constructor to iterate over the entire array. */
    constructor(chars: CharArray) : this(chars, 0, chars.size)

    /** Returns the character at the given absolute index in the underlying [CharArray]. */
    override fun getCharAt(i: Int) = chars[off + i]

    override fun clear() {
        i = 0
        len = 0
        chars = EMPTY_CHAR_ARRAY
    }

    fun reload(chars: CharArray, off: Int, len: Int): CharArrayLatin1Iterator {
        require(off >= 0 && len >= 0 && off + len <= chars.size)
        this.chars = chars
        this.off = off
        super.len = len
        super.i = 0
        return this
    }

    /** Returns the string representation of the subsequence covered by this iterator. */
    override fun toString() = chars.concatToString(off, off + len)

    init { require(off >= 0 && len >= 0 && off + len <= chars.size) }
}


/**
 * [Latin1Iterator] implementation backed by a [ByteArray].
 *
 * Provides sequential and reverse iteration over a contiguous subsequence of [bytes],
 * interpreting each byte as a single character. Suitable for ASCII, Latin-1, or
 * UTF-8 data where all characters are in the 0–255 range.
 *
 * @property bytes the underlying [ByteArray] to iterate over
 * @param off the starting offset of the iteration window in the array
 * @param len the length of the iteration window
 *
 * @constructor Creates an iterator for the subsequence `bytes[off..off+len-1]`.
 * @throws IllegalArgumentException if the offset and length are out of bounds
 */
class ByteArrayLatin1Iterator(bytes: ByteArray, off: Int, len: Int) : Latin1Iterator(len) {
    private var bytes = bytes
    private var off = off


    /** Convenience constructor to iterate over the entire array. */
    constructor(bytes: ByteArray) : this(bytes, 0, bytes.size)

    /** Returns the character at the given absolute index in the underlying [ByteArray]. */
    override fun getCharAt(i: Int) = (bytes[off + i].toInt() and 0xFF).toChar()

    override fun clear() {
        i = 0
        len = 0
        bytes = EMPTY_BYTE_ARRAY
    }

    fun reload(bytes: ByteArray, off: Int, len: Int): ByteArrayLatin1Iterator {
        require(off >= 0 && len >= 0 && off + len <= bytes.size)
        this.bytes = bytes
        this.off = off
        super.len = len
        super.i = 0
        return this
    }

    /** Returns the string representation of the subsequence covered by this iterator. */
    override fun toString() = bytes.decodeToString(off, off + len)

    init { require(off >= 0 && len >= 0 && off + len <= bytes.size) }

}
