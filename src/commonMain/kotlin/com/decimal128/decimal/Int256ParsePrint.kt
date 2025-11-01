@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.StringLatin1Iterator
import kotlin.math.max

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
private const val S_U64_DIV_1E4 = 11 // + 64 high

private const val M_U64_DIV_1E8 = -6067343680855748867 // (0xABCC77118461CEFD)
private const val S_U64_DIV_1E8 = 26 // + 64 high

private const val M_U64_DIV_1E10 = -2601111570856684097 // (0xDBE6FECEBDEDD5BF)
private const val S_U64_DIV_1E10 = 33

private const val M_U64_DIV_1E12 = 2535301200456458803L // (0x232F33025BD42233)
private const val S_U64_DIV_1E12 = 37

private val SINGLE_DIGIT_INTEGERS =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7", "-8", "-9")

internal object Int256ParsePrint {

    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    fun int256ToString(sign: Boolean, u: C256): String {
        val s = if (sign) 1 else 0
        if (u.digitLen > 1) {
            val utf8 = ByteArray(u.digitLen + s)
            utf8[0] = '-'.code.toByte() // if positive then this will be overwritten
            u256ToUtf8(u, utf8, s)
            return String(utf8)
        } else {
            return SINGLE_DIGIT_INTEGERS[(10 and -s) + u.dw0.toInt()]
        }
    }

    fun int32ToUtf8(n: Int, utf8: ByteArray, off: Int): Int {
        val dwAbs = U32((n xor (n shr 31)) - (n shr 31))
        val offT = off + (n ushr 31)
        utf8[off] = '-'.code.toByte()
        val digitLen = U256Pow10.calcDigitLen64(dwAbs)
        val digitPrintCount = digitLen + 1 + (-digitLen shr 31)
        zeroTo10DigitsToUtf8(digitPrintCount, dwAbs, utf8, offT)
        return digitPrintCount + (n ushr 31) // add 1 if it is negative
    }

    fun int256ToUtf8(sign: Boolean, u: C256, utf8: ByteArray, off: Int): Int {
        val s = if (sign) 1 else 0
        utf8[off] = '-'.code.toByte() // if positive then this will be overwritten
        u256ToUtf8(u, utf8, off + s)
        return off + s + Math.max(1, u.digitLen)
    }

    fun u256ToUtf8(u: C256, utf8: ByteArray, off: Int, tmp: C256? = null): Int {
        // minimum printDigitLen is 1
        // add 1, then add -1 if the inbound digitLen was non-zero
        val printDigitLen = u.digitLen + 1 + (-u.digitLen shr 31)
        if (u.bitLen <= 64) {
            oneTo20DigitsToUtf8(printDigitLen, u.dw0, utf8, off)
            return printDigitLen
        }
        val t: C256 = tmp?.c256Set(u) ?: C256(u)
        while (t.bitLen > 192) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_192(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        oneTo20DigitsToUtf8(t.digitLen, t.dw0, utf8, off)
        return printDigitLen
    }

    private fun nineDigitsToUtf8(dw: Long, utf8: ByteArray, off: Int) =
        nineDigitsToUtf8_tree(dw, utf8, off)

    private fun nineDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val abcde = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val fghi  = dw - (abcde * 10000L)

        val abc = (abcde * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val de = abcde - (abc * 100L)

        val fg = (fghi * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val hi = fghi - (fg * 100L)

        val a = (abc * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val bc = abc - (a * 100L)

        val b = (bc * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = bc - (b * 10L)

        val d = (de * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = de - (d * 10L)

        val f = (fg * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = fg - (f * 10L)

        val h = (hi * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = hi - (h * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
        utf8[off + 4] = (e.toInt() + '0'.code).toByte()
        utf8[off + 5] = (f.toInt() + '0'.code).toByte()
        utf8[off + 6] = (g.toInt() + '0'.code).toByte()
        utf8[off + 7] = (h.toInt() + '0'.code).toByte()
        utf8[off + 8] = (i.toInt() + '0'.code).toByte()

    }

    private fun eightDigitsToUtf8(dw: Long, utf8: ByteArray, off: Int) {
        val abcdefgh = dw

        val abcd = unsignedMulHi(abcdefgh, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val efgh  = abcdefgh - (abcd * 10000L)

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val ef = (efgh * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val gh = efgh - (ef * 100L)

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val d = cd - (c * 10L)

        val e = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val f = ef - (e * 10L)

        val g = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val h = gh - (g * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
        utf8[off + 4] = (e.toInt() + '0'.code).toByte()
        utf8[off + 5] = (f.toInt() + '0'.code).toByte()
        utf8[off + 6] = (g.toInt() + '0'.code).toByte()
        utf8[off + 7] = (h.toInt() + '0'.code).toByte()
    }

    private fun fourDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val abcd = dw

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val d = cd - (c * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
    }

    private inline fun zeroTo4DigitsToUtf8(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcd = dw

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()

        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val d = (cd - (cDw * 10L)).toInt()

        val tA = digitPrintCount - 4; val maskA = -tA shr 31; val iA = tA and maskA
        val tB = digitPrintCount - 3; val maskB = -tB shr 31; val iB = tB and maskB
        val tC = digitPrintCount - 2; val maskC = -tC shr 31; val iC = tC and maskC
        val tD = digitPrintCount - 1; val maskD = -tD shr 31; val iD = tD and maskD

        val firstByte = utf8[off].toInt()

        utf8[off + iA] = (a + '0'.code).toByte()
        utf8[off + iB] = (b + '0'.code).toByte()
        utf8[off + iC] = (c + '0'.code).toByte()
        val digitCountNonZeroMask = -digitPrintCount shr 31
        val bD = d + '0'.code
        utf8[off + iD] = ((bD and digitCountNonZeroMask) or (firstByte and digitCountNonZeroMask.inv())).toByte()
     }

    private inline fun zeroTo8DigitsToUtf8(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcdefgh = dw

        val abcd = unsignedMulHi(abcdefgh, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val efgh  = abcdefgh - (abcd * 10000L)

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val ef = (efgh * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val gh = efgh - (ef * 100L)

        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()

        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val d = (cd - (cDw * 10L)).toInt()

        val eDw = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = eDw.toInt()
        val f = (ef - (eDw * 10L)).toInt()

        val gDw = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = gDw.toInt()
        val h = (gh - (gDw * 10L)).toInt()

        val tA = digitPrintCount - 8; val maskA = -tA shr 31; val iA = tA and maskA
        val tB = digitPrintCount - 7; val maskB = -tB shr 31; val iB = tB and maskB
        val tC = digitPrintCount - 6; val maskC = -tC shr 31; val iC = tC and maskC
        val tD = digitPrintCount - 5; val maskD = -tD shr 31; val iD = tD and maskD
        val tE = digitPrintCount - 4; val maskE = -tE shr 31; val iE = tE and maskE
        val tF = digitPrintCount - 3; val maskF = -tF shr 31; val iF = tF and maskF
        val tG = digitPrintCount - 2; val maskG = -tG shr 31; val iG = tG and maskG
        val tH = digitPrintCount - 1; val maskH = -tH shr 31; val iH = tH and maskH

        val firstByte = utf8[off].toInt()

        utf8[off + iA] = (a + '0'.code).toByte()
        utf8[off + iB] = (b + '0'.code).toByte()
        utf8[off + iC] = (c + '0'.code).toByte()
        utf8[off + iD] = (d + '0'.code).toByte()
        utf8[off + iE] = (e + '0'.code).toByte()
        utf8[off + iF] = (f + '0'.code).toByte()
        utf8[off + iG] = (g + '0'.code).toByte()
        val digitCountNonZeroMask = -digitPrintCount shr 31
        val bH = h + '0'.code
        utf8[off + iH] = ((bH and digitCountNonZeroMask) or (firstByte and digitCountNonZeroMask.inv())).toByte()
    }

    private fun tenDigitsToUtf8(dw: Long, utf8: ByteArray, off: Int) {
        val abcdefghij = dw

        val abcdef = unsignedMulHi(abcdefghij, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val ab     = unsignedMulHi(abcdef,     M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val cdef   = abcdef     - (ab     * 10000L)
        val ghij   = abcdefghij - (abcdef * 10000L)

        val cd = (cdef * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val ef = cdef - (cd * 100L)

        val gh = (ghij * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val ij = ghij - (gh * 100L)

        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()

        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val d = (cd - (cDw * 10L)).toInt()

        val eDw = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = eDw.toInt()
        val f = (ef - (eDw * 10L)).toInt()

        val gDw = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = gDw.toInt()
        val h = (gh - (gDw * 10L)).toInt()

        val iDw = (ij * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = iDw.toInt()
        val j = (ij - (iDw * 10L)).toInt()

        utf8[off + 0] = (a + '0'.code).toByte()
        utf8[off + 1] = (b + '0'.code).toByte()
        utf8[off + 2] = (c + '0'.code).toByte()
        utf8[off + 3] = (d + '0'.code).toByte()
        utf8[off + 4] = (e + '0'.code).toByte()
        utf8[off + 5] = (f + '0'.code).toByte()
        utf8[off + 6] = (g + '0'.code).toByte()
        utf8[off + 7] = (h + '0'.code).toByte()
        utf8[off + 8] = (i + '0'.code).toByte()
        utf8[off + 9] = (j + '0'.code).toByte()
    }

    private fun zeroTo10DigitsToUtf8(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcdefghij = dw

        val abcdef = unsignedMulHi(abcdefghij, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val ab     = unsignedMulHi(abcdef,     M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val cdef   = abcdef     - (ab     * 10000L)
        val ghij   = abcdefghij - (abcdef * 10000L)

        val cd = (cdef * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val ef = cdef - (cd * 100L)

        val gh = (ghij * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val ij = ghij - (gh * 100L)

        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()

        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val d = (cd - (cDw * 10L)).toInt()

        val eDw = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = eDw.toInt()
        val f = (ef - (eDw * 10L)).toInt()

        val gDw = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = gDw.toInt()
        val h = (gh - (gDw * 10L)).toInt()

        val iDw = (ij * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = iDw.toInt()
        val j = (ij - (iDw * 10L)).toInt()

        val tA = digitPrintCount - 10; val maskA = -tA shr 31; val iA = tA and maskA
        val tB = digitPrintCount -  9; val maskB = -tB shr 31; val iB = tB and maskB
        val tC = digitPrintCount -  8; val maskC = -tC shr 31; val iC = tC and maskC
        val tD = digitPrintCount -  7; val maskD = -tD shr 31; val iD = tD and maskD
        val tE = digitPrintCount -  6; val maskE = -tE shr 31; val iE = tE and maskE
        val tF = digitPrintCount -  5; val maskF = -tF shr 31; val iF = tF and maskF
        val tG = digitPrintCount -  4; val maskG = -tG shr 31; val iG = tG and maskG
        val tH = digitPrintCount -  3; val maskH = -tH shr 31; val iH = tH and maskH
        val tI = digitPrintCount -  2; val maskI = -tI shr 31; val iI = tI and maskI
        val tJ = digitPrintCount -  1; val maskJ = -tJ shr 31; val iJ = tJ and maskJ

        val firstByte = utf8[off].toInt()

        utf8[off + iA] = (a + '0'.code).toByte()
        utf8[off + iB] = (b + '0'.code).toByte()
        utf8[off + iC] = (c + '0'.code).toByte()
        utf8[off + iD] = (d + '0'.code).toByte()
        utf8[off + iE] = (e + '0'.code).toByte()
        utf8[off + iF] = (f + '0'.code).toByte()
        utf8[off + iG] = (g + '0'.code).toByte()
        utf8[off + iH] = (h + '0'.code).toByte()
        utf8[off + iI] = (i + '0'.code).toByte()
        val digitCountNonZeroMask = -digitPrintCount shr 31
        val bJ = j + '0'.code
        utf8[off + iJ] = ((bJ and digitCountNonZeroMask) or (firstByte and digitCountNonZeroMask.inv())).toByte()
    }

    private fun oneTo20DigitsToUtf8(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcdefghijklmnopqrst = dw
        val abcdefgh = unsignedMulHi(abcdefghijklmnopqrst, M_U64_DIV_1E12) ushr S_U64_DIV_1E12
        val abcd = unsignedMulHi(abcdefgh, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val efgh  = abcdefgh - (abcd * 10000L)
        val ijklmnopqrst = abcdefghijklmnopqrst - (abcdefgh * 1_000_000_000_000L)
        val ijklmnop = unsignedMulHi(ijklmnopqrst, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val ijkl = unsignedMulHi(ijklmnop, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val mnop = ijklmnop - (ijkl * 10000L)
        val qrst = ijklmnopqrst - (ijklmnop * 10000L)

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val ef = (efgh * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val gh = efgh - (ef * 100L)

        val ij = (ijkl * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val kl = ijkl - (ij * 100L)

        val mn = (mnop * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val op = mnop - (mn * 100L)

        val qr = (qrst * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val st = qrst - (qr * 100L)

        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()

        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val d = (cd - (cDw * 10L)).toInt()

        val eDw = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = eDw.toInt()
        val f = (ef - (eDw * 10L)).toInt()

        val gDw = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = gDw.toInt()
        val h = (gh - (gDw * 10L)).toInt()

        val iDw = (ij * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = iDw.toInt()
        val j = (ij - (iDw * 10L)).toInt()

        val kDw = (kl * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val k = kDw.toInt()
        val l = (kl - (kDw * 10L)).toInt()

        val mDw = (mn * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val m = mDw.toInt()
        val n = (mn - (mDw * 10L)).toInt()

        val oDw = (op * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val o = oDw.toInt()
        val p = (op - (oDw * 10L)).toInt()

        val qDw = (qr * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val q = qDw.toInt()
        val r = (qr - (qDw * 10L)).toInt()

        val sDw = (st * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val s = sDw.toInt()
        val t = (st - (sDw * 10L)).toInt()

        val tA = digitPrintCount - 20; val maskA = -tA shr 31; val iA = tA and maskA
        val tB = digitPrintCount - 19; val maskB = -tB shr 31; val iB = tB and maskB
        val tC = digitPrintCount - 18; val maskC = -tC shr 31; val iC = tC and maskC
        val tD = digitPrintCount - 17; val maskD = -tD shr 31; val iD = tD and maskD
        val tE = digitPrintCount - 16; val maskE = -tE shr 31; val iE = tE and maskE
        val tF = digitPrintCount - 15; val maskF = -tF shr 31; val iF = tF and maskF
        val tG = digitPrintCount - 14; val maskG = -tG shr 31; val iG = tG and maskG
        val tH = digitPrintCount - 13; val maskH = -tH shr 31; val iH = tH and maskH
        val tI = digitPrintCount - 12; val maskI = -tI shr 31; val iI = tI and maskI
        val tJ = digitPrintCount - 11; val maskJ = -tJ shr 31; val iJ = tJ and maskJ
        val tK = digitPrintCount - 10; val maskK = -tK shr 31; val iK = tK and maskK
        val tL = digitPrintCount -  9; val maskL = -tL shr 31; val iL = tL and maskL
        val tM = digitPrintCount -  8; val maskM = -tM shr 31; val iM = tM and maskM
        val tN = digitPrintCount -  7; val maskN = -tN shr 31; val iN = tN and maskN
        val tO = digitPrintCount -  6; val maskO = -tO shr 31; val iO = tO and maskO
        val tP = digitPrintCount -  5; val maskP = -tP shr 31; val iP = tP and maskP
        val tQ = digitPrintCount -  4; val maskQ = -tQ shr 31; val iQ = tQ and maskQ
        val tR = digitPrintCount -  3; val maskR = -tR shr 31; val iR = tR and maskR
        val tS = digitPrintCount -  2; val maskS = -tS shr 31; val iS = tS and maskS
        val tT = digitPrintCount -  1; val maskT = -tT shr 31; val iT = tT and maskT

        val firstByte = utf8[off].toInt()

        utf8[off + iA] = (a + '0'.code).toByte()
        utf8[off + iB] = (b + '0'.code).toByte()
        utf8[off + iC] = (c + '0'.code).toByte()
        utf8[off + iD] = (d + '0'.code).toByte()
        utf8[off + iE] = (e + '0'.code).toByte()
        utf8[off + iF] = (f + '0'.code).toByte()
        utf8[off + iG] = (g + '0'.code).toByte()
        utf8[off + iH] = (h + '0'.code).toByte()
        utf8[off + iI] = (i + '0'.code).toByte()
        utf8[off + iJ] = (j + '0'.code).toByte()
        utf8[off + iK] = (k + '0'.code).toByte()
        utf8[off + iL] = (l + '0'.code).toByte()
        utf8[off + iM] = (m + '0'.code).toByte()
        utf8[off + iN] = (n + '0'.code).toByte()
        utf8[off + iO] = (o + '0'.code).toByte()
        utf8[off + iP] = (p + '0'.code).toByte()
        utf8[off + iQ] = (q + '0'.code).toByte()
        utf8[off + iR] = (r + '0'.code).toByte()
        utf8[off + iS] = (s + '0'.code).toByte()
        utf8[off + iT] = (t + '0'.code).toByte()
    }

    internal fun u32ToUtf8(digitPrintCount: Int, w: Int, utf8: ByteArray, off: Int): Int {
        if (digitPrintCount in 1..10) {
            var d = U32(w)
            var i = digitPrintCount - 1
            do {
                val qA = (d * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitA = (( d - (qA * 10L)) + '0'.code).toByte()
                val qB = (qA * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = (qB * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()

                val tC = i - 2; val maskC = -tC shr 31; val iC = tC and maskC
                val tB = i - 1; val maskB = -tB shr 31; val iB = tB and maskB

                utf8[off + iC] = digitC
                utf8[off + iB] = digitB
                utf8[off + i] = digitA

                d = qC
                i -= 3
            } while (i >= 0)
            return off + digitPrintCount
        }
        throw IllegalArgumentException()
    }

    internal fun u64ToUtf8(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int =
        u64ToUtf8_sequential(digitPrintCount, dw0, utf8, off)

    internal fun u64ToUtf8_tree(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int {
        if (digitPrintCount in 1..20) {
            var dw = dw0
            var remainingCount = digitPrintCount
            var offT = off + digitPrintCount
            while (remainingCount >= 8) {
                val dwT = unsignedMulHi(dw, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
                val low8 = dw - (dwT * 100000000L)
                dw = dwT
                remainingCount -= 8
                offT -= 8
                eightDigitsToUtf8(low8, utf8, offT)
            }
            if (remainingCount > 4) {
                val dwT = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
                val low4 = dw - (dwT * 10000L)
                dw = dwT
                remainingCount -= 4
                offT -= 4
                fourDigitsToUtf8_tree(low4, utf8, offT)
            }
            zeroTo4DigitsToUtf8(remainingCount, dw, utf8, off)
            return digitPrintCount
        }
        throw IllegalArgumentException()
    }

    internal fun u64ToUtf8_wot(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int {
        check (digitPrintCount in 1..20)
        if (digitPrintCount <= 10) {
            zeroTo10DigitsToUtf8(digitPrintCount, dw0, utf8, off)
            return digitPrintCount
        }
        val hi10 = unsignedMulHi(dw0, M_U64_DIV_1E10) ushr S_U64_DIV_1E10
        val lo10 = dw0 - (hi10 * 10000000000L)
        check (hi10 == unsignedDiv(dw0, 10000000000L))
        zeroTo10DigitsToUtf8(digitPrintCount - 10, hi10, utf8, off)
        zeroTo10DigitsToUtf8(10, lo10, utf8, off + digitPrintCount - 10)
        return digitPrintCount
    }

    internal fun u64ToUtf8_sequential(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int {
        val z = C256()
        DivMagic.magicDivPow10_64(z, dw0, 8)
        if (digitPrintCount in 1..20) {
            var d = dw0
            var i = digitPrintCount - 1
            while (i >= 3) {
                val qA = unsignedMulHi(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitA = ((d - (qA * 10L)) + '0'.code).toByte()
                val qB = unsignedMulHi(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = unsignedMulHi(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()
                val qD = unsignedMulHi(qC, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitD = ((qC - (qD * 10L)) + '0'.code).toByte()

                utf8[off + i - 3] = digitD
                utf8[off + i - 2] = digitC
                utf8[off + i - 1] = digitB
                utf8[off + i    ] = digitA

                d = qD
                i -= 4
            }
            if (i >= 0) {
                val qA = unsignedMulHi(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitA = ((d - (qA * 10L)) + '0'.code).toByte()
                val qB = unsignedMulHi(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = unsignedMulHi(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()

                val tB = i - 1;
                val maskB = -tB shr 31;
                val iB = tB and maskB

                utf8[off     ] = digitC
                utf8[off + iB] = digitB
                utf8[off + i ] = digitA

            }
            return off + digitPrintCount
        }
        throw IllegalArgumentException()
    }

    fun int256ToHexString(sign: Boolean, u: C256): String {
        val s = if (sign) 1 else 0
        if (u.bitLen == 0)
            return "0x0"
        val hexitCount = (u.bitLen + 3) ushr 2
        val bytes = ByteArray(s + 2 + hexitCount)
        bytes[0    ] = '-'.code.toByte()
        bytes[s    ] = '0'.code.toByte()
        bytes[s + 1] = 'x'.code.toByte()
        u256ToHexUtf8(u, hexitCount, bytes, s + 2)
        return String(bytes)
    }

    fun u256ToHexUtf8(u: C256, hexitCount: Int, utf8: ByteArray, off: Int) {
        var hexitsRemaining = hexitCount
        var ich = off
        for (i in (u.bitLen - 1) ushr 6 downTo 0) {
            val dw = u[i]
            val thisHexitCount = if ((hexitsRemaining and 0x0F) != 0) (hexitsRemaining and 0x0F) else 16
            u64ToHexUtf8(dw, thisHexitCount, utf8, ich)
            hexitsRemaining -= thisHexitCount
            ich += thisHexitCount
        }
        check(hexitsRemaining == 0)
        check(ich == utf8.size)
    }

    fun u64ToHexUtf8(dw: Long, hexitCount: Int, utf8: ByteArray, off: Int) {
        var t = dw
        check(hexitCount in 1..16)
        for (i in hexitCount - 1 downTo 0) {
            val h = (t and 0x0FL).toInt()
            val ch = if (h < 10) '0' + h else 'A' - 10 + h
            utf8[off + i] = ch.code.toByte()
            t = t ushr 4
        }
    }

    fun u256FromString(u: C256, allowSign: Boolean, str: String) =
        u256FromLatin1Iterator(u, allowSign, StringLatin1Iterator(str))

    fun u256FromLatin1Iterator(u: C256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.c256SetZero()
        invalid_syntax@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            val sign = allowSign && ch == '-'
            if (allowSign && (ch == '-' || ch == '+')) // discard leading sign
                ch = src.nextChar()
            if (ch == '0') { // discard leading zero
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    return u256FromHexLatin1Iterator(u, allowSign, src.reset())
                leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@invalid_syntax
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar() // discard all leading zeros
            }
            if (ch == '\u0000') {
                if (leadingZeroSeen)
                    return sign
                break@invalid_syntax
            }
            var accumulator = 0L
            var accumulatorDigitCount = 0
            var chLast = '\u0000'
            src.prevChar() // back up one char
            while (true) {
                chLast = ch
                ch = src.nextChar()
                if (ch !in '0'..'9') {
                    if (ch == '_')
                        continue
                    break
                }
                val n = (ch - '0').toLong()
                accumulator = accumulator * 10L + n
                ++accumulatorDigitCount
                if (accumulatorDigitCount < 19)
                    continue
                u.u256MutateFmaPow10(19, accumulator)
                accumulator = 0L
                accumulatorDigitCount = 0
            }
            if (ch != '\u0000' || chLast == '_')
                break@invalid_syntax
            if (accumulatorDigitCount > 0)
                u.u256MutateFmaPow10(accumulatorDigitCount, accumulator)
            return sign
        } while (false)
        throw IllegalArgumentException("invalid integer syntax:$src")
    }

    private fun u256FromHexLatin1Iterator(u: C256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.c256SetZero()
        invalid_syntax@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            val sign = allowSign && ch == '-'
            if (allowSign && (ch == '-' || ch == '+'))
                ch = src.nextChar()
            if (ch == '0') {
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    ch = src.nextChar()
                else
                    leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@invalid_syntax
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar()
            }
            if (ch != '\u0000')
                src.prevChar() // back up 1 char
            var accumulator = 0L
            var accumulatorHexitCount = 0
            while (true) {
                ch = src.nextChar()
                val n = when {
                    ch in '0'..'9' -> (ch - '0').toLong()
                    (ch.code or 0x20) in 'a'.code..'f'.code ->
                        (ch.code or 0x20) - 'a'.code + 10L

                    ch == '_' -> continue
                    ch == '\u0000' -> break
                    else -> break@invalid_syntax
                }
                accumulator = (accumulator shl 4) or n
                ++accumulatorHexitCount
                if (accumulatorHexitCount < 16)
                    continue
                u.c256MutateShiftLeftOr(64, accumulator)
                accumulator = 0
                accumulatorHexitCount = 0
            }
            if (accumulatorHexitCount > 0)
                u.c256MutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
            return sign
        } while (false)
        throw NumberFormatException("invalid hex integer syntax:$src")
    }

}
