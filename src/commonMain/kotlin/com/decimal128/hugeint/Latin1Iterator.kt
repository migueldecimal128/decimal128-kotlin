package com.decimal128.hugeint

abstract class Latin1Iterator(size: Int, val off: Int, val len: Int) {
    private var i = 0
    fun hasNext() = (i < len)
    fun peek() = if (i < len) getCharAt(off + i) else '\u0000'
    fun remainingLen() = len - i
    fun nextChar() = if (i < len) getCharAt(off + i++) else '\u0000'
    fun prevChar() = if (i > 0) getCharAt(off + --i) else '\u0000'
    fun reset(): Latin1Iterator { i = 0; return this }
    abstract fun getCharAt(i: Int): Char
    abstract override fun toString(): String
    init { require (off >= 0 && len >= 0 && off <= size - len) }
}

class StringLatin1Iterator(private val s: String, off: Int, len: Int) : Latin1Iterator(s.length, off, len) {
    override fun getCharAt(i: Int) = s[i]
    override fun toString() = s.toString()
}

class CharSequenceLatin1Iterator(private val csq: CharSequence, off: Int, len: Int) : Latin1Iterator(csq.length, off, len) {
    override fun getCharAt(i: Int) = csq[i]
    override fun toString() = csq.toString()
}

class CharArrayLatin1Iterator(private val chars: CharArray, off: Int, len: Int) : Latin1Iterator(chars.size, off, len) {
    override fun getCharAt(i: Int) = chars[i]
    override fun toString() = String(chars, off, len)
}

class ByteArrayLatin1Iterator(private val bytes: ByteArray, off: Int, len: Int) : Latin1Iterator(bytes.size, off, len) {
    override fun getCharAt(i: Int) = (bytes[i].toInt() and 0xFF).toChar()
    override fun toString() = String(bytes, off, len)
}
