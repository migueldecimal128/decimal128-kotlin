package com.decimal128.hugeint

abstract class Latin1Iterator(val len: Int) {
    private var i = 0
    fun hasNext() = (i < len)
    //fun peek() = if (i < len) getCharAt(i) else '\u0000'
    fun remainingLen() = len - i
    fun nextChar() = if (i < len) getCharAt(i++) else '\u0000'
    fun prevChar() = if (i > 0) getCharAt(--i) else '\u0000'
    fun reset(): Latin1Iterator { i = 0; return this }
    abstract fun getCharAt(i: Int): Char
    abstract override fun toString(): String
}

class StringLatin1Iterator(private val s: String) : Latin1Iterator(s.length) {
    override fun getCharAt(i: Int) = s[i]
    override fun toString() = s.toString()
}

class CharSequenceLatin1Iterator(private val csq: CharSequence) : Latin1Iterator(csq.length) {
    override fun getCharAt(i: Int) = csq[i]
    override fun toString() = csq.toString()
}

class CharArrayLatin1Iterator(private val chars: CharArray, private val off: Int, len: Int) : Latin1Iterator(len) {
    override fun getCharAt(i: Int) = chars[off + i]
    override fun toString() = String(chars, off, len)
    init { require (off >= 0 && len >= 0 && off <= chars.size - len) }
}

class ByteArrayLatin1Iterator(private val bytes: ByteArray, private val off: Int, len: Int) : Latin1Iterator(len) {
    override fun getCharAt(i: Int) = (bytes[off + i].toInt() and 0xFF).toChar()
    override fun toString() = String(bytes, off, len)
    init { require (off >= 0 && len >= 0 && off <= bytes.size - len) }
}
