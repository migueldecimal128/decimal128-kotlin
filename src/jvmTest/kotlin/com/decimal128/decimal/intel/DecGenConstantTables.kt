package com.decimal128.decimal.intel

import com.decimal128.bigint.BigInt
import com.decimal128.decimal.BARRETT_POW10_MAXX
import com.decimal128.decimal.BARRETT_POW5_MU_BASE
import com.decimal128.decimal.BARRETT_POW5_MU_MAXX
import com.decimal128.decimal.MAGIC_FLAG_AND_SHIFT_BASE
import com.decimal128.decimal.MAGIC_POW10_M_BASE
import com.decimal128.decimal.MAGIC_POW10_M_MAXX
import com.decimal128.decimal.MAXX_DIGIT_LEN
import com.decimal128.decimal.MIN_POW10_DIGIT_LEN_128
import com.decimal128.decimal.POW5_64_BASE
import com.decimal128.decimal.POW5_64_MAXX
import com.decimal128.decimal.RANGE_RECIP_MUL_PARAMS_BASE
import com.decimal128.decimal.RRMP10_K_MAXX
import com.decimal128.decimal.RRMP10_K_MIN
import com.decimal128.decimal.RRMP10_LOOKUP_BASE
import com.decimal128.decimal.RRMP10_LOOKUP_ROW_SIZE
import com.decimal128.decimal.RRMP10_LOOKUP_SHIFT
import com.decimal128.decimal.RRMP10_LOOKUP_TABLE_SIZE
import com.decimal128.decimal.RRMP10_Q_MAXX
import com.decimal128.decimal.RRMP10_Q_MIN
import com.decimal128.decimal.Residue
import com.decimal128.decimal.verify
import java.io.DataOutputStream
import java.io.File
import kotlin.math.min
import kotlin.test.Test

class DecGenConstantTables {

    val resourcePath = "src/main/resources/com/decimal128/decimal/decimal128_tables.bin"
    val X_RESOURCE_TABLE_VERSION = 0x0001_D128
    val X_DWORD_TABLES_SIZE = 986 // DWORD_TABLES_SIZE
    val X_BYTE_TABLES_SIZE = 1986 // BYTE_TABLES_SIZE
    var X_DWORD_TABLES_FNV1A = 0
    var X_BYTE_TABLES_FNV1A = 0


    @Test
    fun generateConstantTables() {
        initializeTables()
        X_DWORD_TABLES_FNV1A = Fnv1aChecksum.fnv1a(X_DWORD_TABLES)
        X_BYTE_TABLES_FNV1A = Fnv1aChecksum.fnv1a(X_BYTE_TABLES)
        println("X_DWORD_TABLES_FNV1A:$X_DWORD_TABLES_FNV1A X_BYTE_TABLES_FNV1A:$X_BYTE_TABLES_FNV1A")
        check(X_DWORD_TABLES_FNV1A == 739413891)
        check(X_BYTE_TABLES_FNV1A == 1447633196)
        saveConstantTablesAsTestResource()
    }

    fun saveConstantTablesAsTestResource() {
        val tableHeader = intArrayOf(
            X_RESOURCE_TABLE_VERSION,
            X_BYTE_TABLES_SIZE,
            X_DWORD_TABLES_SIZE,
            X_BYTE_TABLES_FNV1A,
            X_DWORD_TABLES_FNV1A,
        )
        val file = File(resourcePath)
        file.parentFile.mkdirs()
        val dos = DataOutputStream(file.outputStream().buffered())
        dos.use {
            for (n in tableHeader)
                dos.writeInt(n)
            for (i in 0..<X_DWORD_TABLES_SIZE)
                dos.writeLong(X_DWORD_TABLES[i])
            for (i in 0..<X_BYTE_TABLES_SIZE)
                dos.writeByte(X_BYTE_TABLES[i].toInt())
        }
    }


    fun initializeTables() {
        initPow10Pow5()
        initBarrett()
        initMagicPow10_64()
        initRangeRecipMulPow10()
    }

    val X_DWORD_TABLES = LongArray(1024)
    val X_BYTE_TABLES = ByteArray(2048)

    private fun initPow10Pow5() {
        var hiPow10 = BigInt.ONE
        var j = 0
        for (i in 0..<MAXX_DIGIT_LEN) {
            X_BYTE_TABLES[i] = (hiPow10.magnitudeBitLen() - 1).toByte()
            for (dw in hiPow10.magnitudeToLittleEndianLongArray())
                X_DWORD_TABLES[j++] = dw
            if (i < MIN_POW10_DIGIT_LEN_128)
                ++j
            hiPow10 *= 10
        }

        // initialize powers of 5 that fit in 64 bits
        X_DWORD_TABLES[POW5_64_BASE] = 1L
        for (i in 1..<POW5_64_MAXX)
            X_DWORD_TABLES[POW5_64_BASE + i] = X_DWORD_TABLES[POW5_64_BASE + i - 1] * 5L

        // initialization of Magic multipliers M is in DivMagic
    }

    private fun initBarrett() {
        // initialize Barrett division
        val biTwoPow64 = BigInt.ONE.shl(64)
        var biPow5 = BigInt.ONE

        // mu for 10**0 == 0 ... used for checking div by 1 case
        for (i in 1..<BARRETT_POW10_MAXX) {
            biPow5 *= 5
            val mu5 = biTwoPow64 / biPow5
            X_DWORD_TABLES[BARRETT_POW5_MU_BASE + i] = mu5.toLong()
        }
        for (i in 1..<BARRETT_POW5_MU_MAXX) {
            val t = BigInt.fromUnsigned(X_DWORD_TABLES[POW5_64_BASE + i])
            val mu = biTwoPow64 / t
            X_DWORD_TABLES[BARRETT_POW5_MU_BASE + i] = mu.toLong()
        }
    }

    fun initMagicPow10_64() {
        X_DWORD_TABLES[MAGIC_POW10_M_BASE + 0] = 1
        X_BYTE_TABLES[MAGIC_FLAG_AND_SHIFT_BASE + 0] = Byte.MIN_VALUE
        for (k in 1..<MAGIC_POW10_M_MAXX) {
            val d = pow10_64(k)
            val magic = magicu64(d)
            X_DWORD_TABLES[MAGIC_POW10_M_BASE + k] = magic.m
            val flagAndShift = magic.s or if (magic.add) 0x80 else 0
            X_BYTE_TABLES[MAGIC_FLAG_AND_SHIFT_BASE + k] = flagAndShift.toByte()
        }
    }

    private fun pow10_64(pow10: Int): Long {
        return X_DWORD_TABLES[2 * pow10]
    }

    /**
     * magic division of a ULong by powers of 10 using
     * unsignedMulHi() that do *not* need correction
     *
     * 1e1 0xCCCCCCCCCCCCCCCD s=3
     * 1e4 0x346DC5D63886594B s=11
     * 1e6 0x431BDE82D7B634DB s = 18
     * 1e7 0xD6BF94D5E57A42BD s = 23
     * 1e8 0xABCC77118461CEFD s = 26
     * 1e10 0xDBE6FECEBDEDD5BF	s = 33
     * 1e11 0xAFEBFF0BCB24AAFF	s = 36
     * 1e12 0x232F33025BD42233	s = 37
     * 1e13 0x384B84D092ED0385	s = 41
     * 1e14 0xB424DC35095CD81 s = 42
     * 1e16 0x39A5652FB1137857	s = 51
     * 1e19 0x760F253EDB4AB0D3	s = 62
     *
     */

    data class Magic(val m: Long, val add: Boolean, val s: Int)

    /**
     * Compute magic number and shift for unsigned division by d (1 ≤ d < 2^64),
     * using BigInt instead of BigInteger.
     */
    fun magicu64(d: Long): Magic {
        require(d != 0L) { "divisor must be nonzero" }
        val N = 64

        // 1) build BigInt version of 2^N and of the unsigned divisor
        val hiD        = BigInt.fromUnsigned(d)

        // 2) anc = (2^N − 1) − ((2^N − 1) mod d)
        val nBitMask   = BigInt.withBitMask(N)
        val anc      = nBitMask - (nBitMask % hiD)

        // 3) initialize p, q1/r1 for anc and q2/r2 for d
        var p          = (N - 1).toLong()
        val twoPowN1 = BigInt.withSetBit(N - 1)         // 2^(N−1)

        //var (q1, r1) = twoPowN1.divMod(anc)
        var q1 = twoPowN1 / anc
        var r1 = twoPowN1 % anc
        //var (q2, r2) = twoPowN1.divMod(hiD)
        var q2 = twoPowN1 / hiD
        var r2 = twoPowN1 % hiD
        lateinit var delta: BigInt

        do {
            p += 1

            // double q1/r1 mod anc
            q1 = q1 shl 1
            r1 = r1 shl 1
            if (r1 >= anc) {
                q1 += 1
                r1 -= anc
            }

            // double q2/r2 mod biD
            q2 = q2 shl 1
            r2 = r2 shl 1
            if (r2 >= hiD) {
                q2 += 1
                r2 -= hiD
            }

            delta = hiD - r2
        } while (q1 < delta || q1 == delta && r1.isZero())

        // 5) extract the “true” multiplier and shift
        val Mtrue   = q2 + 1
        val addFlag = Mtrue.isBitSet(N)            // bit-64 set?
        val m_mod   = Mtrue.toLong()           // low 64 bits
        val s       = (p - N).toInt()

        return Magic(m_mod, addFlag, s)
    }


    //RRMP10

    private var POW_10 = Array<BigInt>(RRMP10_Q_MAXX) { BigInt.ONE }
    private var POW_5 = Array<BigInt>(RRMP10_K_MAXX) { BigInt.ONE }

    private fun calcPowTables() {
        for (i in 1..<RRMP10_Q_MAXX)
            POW_10[i] = POW_10[i - 1] * 10
        for (i in 1..<RRMP10_K_MAXX)
            POW_5[i] = POW_5[i - 1] * 5
    }

    private data class TableEntry(val qMax: Int, val k: Int, val prodBitLen: Int, val M: BigInt, val S: Int) {
        var qMin = qMax
        fun prodDwordLen() = (prodBitLen + 0x3f) ushr 6

        override fun toString() = if (qMax == -1) "[NULL]" else "[q:$qMin-$qMax k:$k bitLen:$prodBitLen M:$M S:$S]"
    }

    // qMin = 8 bits
// qMax = 8 bits
// k = 8 bits
// maxProdDwordLen = 8 bits
// S = 16 bits
// mDwordLen = 16 bits
    private fun packDescriptor(te: TableEntry): Long {
        val prodDwordLen = te.prodDwordLen()
        val MDwordLen = te.M.magnitudeLongArrayLen()
        val descriptor = (te.qMin.toLong() shl 56) or
                (te.qMax.toLong() shl 48) or
                (te.k.toLong() shl 40) or
                (prodDwordLen.toLong() shl 32) or
                (te.S shl 16).toLong() or
                (MDwordLen).toLong()
        return descriptor
    }

    private fun unpackQMin(d: Long) = (d ushr 56).toInt()
    private fun unpackQMax(d: Long) = (d ushr 48).toInt() and 0xFF
    private fun unpackK(d: Long) = (d ushr 40).toInt() and 0xFF
    private fun unpackProdDwordLen(d: Long) = (d ushr 32).toInt() and 0xFF
    private fun unpackS(d: Long) = (d ushr 16).toInt() and 0xFFFF
    private fun unpackMDwordLen(d: Long) = d.toInt() and 0xFFFF

    private val NULL_TABLE_ENTRY = TableEntry(-1, -1, -1, BigInt.ONE, -1)

    private var recipTable: Array<Array<TableEntry>> = Array(RRMP10_Q_MAXX) { Array<TableEntry>(RRMP10_K_MAXX) { NULL_TABLE_ENTRY } }

    private fun populateTable() {
        for (j in RRMP10_Q_MIN..<RRMP10_Q_MAXX) {
            var prev = recipTable[j - 1][RRMP10_K_MIN]
            for (k in RRMP10_K_MIN..<min(j, RRMP10_K_MAXX)) {
                val yPrev = if (prev != NULL_TABLE_ENTRY) prev.S else 5 * RRMP10_K_MIN
                val te = findTableEntry(j, k, yPrev + 2)
                if (te != null) {
                    //println("($j, $k) => minimalY:${te.S}")
                    recipTable[j][k] = te
                    prev = te
                    //println(te)
                }
            }
        }
    }

    private fun findTableEntry(q: Int, k: Int, yStart: Int): TableEntry? {
        var y = yStart
        var te = computeMSIfValid(q, k, y)
        if (te != null) {
            while (true) {
                --y
                if (y == 0)
                    throw IllegalStateException()
                val teDown = computeMSIfValid(q, k, y) ?: return te
                te = teDown
            }
        } else {
            var yUp = yStart + 1
            while (true) {
                val teUp = computeMSIfValid(q, k, yUp)
                if (teUp != null)
                    return teUp
                ++yUp
                if (yUp == 500)
                    throw IllegalStateException()
                //println("q:$q k:$k yUp:$yUp")
            }
        }
    }

    private fun computeMSIfValid(q: Int, k: Int, y: Int): TableEntry? {
        val twoPowY = BigInt.ONE shl y
        val fivePowK = POW_5[k]
        val tenPowK = POW_10[k]
        val tenPowQ = POW_10[q]
        val x = twoPowY + fivePowK - 1
        val M = x / fivePowK

        val S = y

        val C_max = tenPowQ
        val C_max_prime = C_max shr (k - 1)
        val maxProd = C_max_prime * M

        if (!isValid(tenPowQ, k, M, S))
            return null
        //println("tenPowQ is valid")
        val half = tenPowK shr 1

        if (!isValid(half, k, M, S))
            return null
        //println("half is valid")
        val nines5 = tenPowQ - half

        if (!isValid(nines5, k, M, S))
            return null
        //println("nines5 is valid")
        val nines5down = nines5 - 1

        if (!isValid(nines5down, k, M, S))
            return null
        //println("nines5down is valid")
        val nines5up = nines5 + 1

        if (!isValid(nines5up, k, M, S))
            return null
        //println("nines5up is valid")

        val bitLen = maxProd.magnitudeBitLen()
        return TableEntry(q, k, bitLen, M, y)
    }

    private fun isValid(dividend: BigInt, k: Int, M: BigInt, S: Int): Boolean {
        val tenPowK = POW_10[k]
        //val (expectedQuot, expectedRem) = dividend.divMod(tenPowK)
        val expectedQuot = dividend / tenPowK
        val expectedRem = dividend % tenPowK
        val expectedResidue = Residue.fromRemainderDivisor(expectedRem, tenPowK)

        val dividendAlfa = dividend shr (k - 1)
        val maskAlfa = BigInt.withBitMask(k - 1)
        val fracAlfa = dividend and maskAlfa
        val stickyAlfa = fracAlfa.isNotZero()

        val prod = dividendAlfa * M
        val quotPlusRound = prod ushr S
        val quot = quotPlusRound ushr 1
        val round = quotPlusRound.isBitSet(0)

        if (expectedQuot != quot)
            return false

        val fracBeta = prod - (quotPlusRound shl S)
        val stickyBeta = fracBeta >= M

        val sticky = stickyAlfa or stickyBeta

        val residue = when {
            round and sticky -> Residue.GT_HALF
            round -> Residue.HALF
            sticky -> Residue.LT_HALF
            else -> Residue.EXACT
        }

        return expectedResidue == residue
    }

    private fun tableMerge() {
        for (k in RRMP10_K_MIN..<RRMP10_K_MAXX) {
            var tePrev = recipTable[RRMP10_Q_MAXX - 1][k]
            for (q in RRMP10_Q_MAXX - 2 downTo RRMP10_Q_MIN) {
                val te = recipTable[q][k]
                if (tePrev != NULL_TABLE_ENTRY && te.prodDwordLen() == tePrev.prodDwordLen()) {
                    tePrev.qMin = q
                    recipTable[q][k] = tePrev
                    //println("merge($q, $k)")
                } else {
                    tePrev = te
                }
            }
        }
    }

// I tried setting this up to use triangle indexing
// it only saved 15% of the table size and required
// more calculation to find the offsetIndex, esp because
// of the upper triangle vs the lower rectangle

    private fun offsetIndex(digitCount: Int, pow10: Int): Int {
        check(RRMP10_K_MAXX - RRMP10_K_MIN <= RRMP10_LOOKUP_ROW_SIZE)
        check(digitCount in RRMP10_Q_MIN..<RRMP10_Q_MAXX)
        check(pow10 in RRMP10_K_MIN..<RRMP10_K_MAXX)
        val index = ((digitCount - RRMP10_Q_MIN) shl RRMP10_LOOKUP_SHIFT) + (pow10 - RRMP10_K_MIN)
        return index
    }

    private fun paramsIndex(digitCount: Int, pow10: Int): Int {
        //  return paramsIndex_x(digitCount, pow10)
        return paramsIndex_y(digitCount, pow10)
    }

    private fun storeParamsIndex(digitCount: Int, pow10: Int, paramsIndex: Int) {
//    storeParamsIndex_x(digitCount, pow10, paramsIndex)
        storeParamsIndex_y(digitCount, pow10, paramsIndex)
    }


    private val OFFSETS = ShortArray(RRMP10_LOOKUP_TABLE_SIZE)

    private fun paramsIndex_x(digitCount: Int, pow10: Int): Int {
        return OFFSETS[offsetIndex(digitCount, pow10)].toInt()
    }

    private fun storeParamsIndex_x(digitCount: Int, pow10: Int, paramsIndex: Int) {
        val offsetIndex = offsetIndex(digitCount, pow10)
        OFFSETS[offsetIndex] = paramsIndex.toShort()
    }

    private val BASE_INTERCEPT = 768

    private fun storeParamsIndex_y(digitCount: Int, pow10: Int, paramsIndex: Int) {
        val offsetIndex = offsetIndex(digitCount, pow10)
        val baseMask = (BASE_INTERCEPT - offsetIndex) shr 31
        val block = (offsetIndex - (BASE_INTERCEPT - 128)) ushr 7
        val base = (block shl 6) - (block shl 3)  // base = block * 56
        val effectiveBase = base and baseMask
        val encodedIndex = paramsIndex - effectiveBase
        verify { encodedIndex in 0..255 }
        X_BYTE_TABLES[RRMP10_LOOKUP_BASE + offsetIndex] = encodedIndex.toByte()
    }

    private fun paramsIndex_y(digitCount: Int, pow10: Int): Int {
        val offsetIndex = offsetIndex(digitCount, pow10)
        val encodedIndex =
            X_BYTE_TABLES[(RRMP10_LOOKUP_BASE + offsetIndex)].toInt() and 0xFF
        val baseMask = (BASE_INTERCEPT - offsetIndex) shr 31
        val block = (offsetIndex - (BASE_INTERCEPT - 128)) ushr 7
        val base = (block shl 6) - (block shl 3)  // base = block * 56
        val effectiveBase = base and baseMask
        return effectiveBase + encodedIndex
    }

    private var iRRP = 1
    private val RANGE_RECIP_PARAMS = LongArray(709)

    private fun serializeTable() {
        for (q in RRMP10_Q_MIN..<RRMP10_Q_MAXX) {
            for (k in RRMP10_K_MIN..<min(q, RRMP10_K_MAXX)) {
                val te = recipTable[q][k]
                val i = offsetIndex(q, k)
                val paramsIndex = when {
                    te == NULL_TABLE_ENTRY -> -1
                    q > te.qMin -> paramsIndex(te.qMin, k)
                    else -> serialize(te)
                }
                storeParamsIndex(q, k, paramsIndex)
            }
        }

        if (RANGE_RECIP_PARAMS.size != iRRP) {
            println("RANGE_RECIP_PARAMS.size should be $iRRP")
            throw IllegalStateException()
        }

        for (i in RANGE_RECIP_PARAMS.indices)
            X_DWORD_TABLES[RANGE_RECIP_MUL_PARAMS_BASE + i] = RANGE_RECIP_PARAMS[i]

        for (i in OFFSETS.indices) {
            val value = OFFSETS[i].toInt() and 0xFFFF
            if (value != 0) {
                val base = when {
                    i < 768 -> 0
                    (i - 768) < 128 * 1 -> 56 * 1
                    (i - 768) < 128 * 2 -> 56 * 2
                    (i - 768) < 128 * 3 -> 56 * 3
                    (i - 768) < 128 * 4 -> 56 * 4
                    (i - 768) < 128 * 5 -> 56 * 5
                    (i - 768) < 128 * 6 -> 56 * 6
                    (i - 768) < 128 * 7 -> 56 * 7
                    (i - 768) < 128 * 8 -> 56 * 8
                    (i - 768) < 128 * 9 -> 56 * 9
                    else -> 0
                }
                val diff = value - base
                if (diff < 0 || diff > 255)
                    println("FAIL at index $i: value=$value base=$base diff=$diff")
            }
        }
    }

    private fun serialize(te: TableEntry): Int {
        val offset = iRRP
        appendLong(packDescriptor(te))
        val mLimbs = te.M.magnitudeToLittleEndianLongArray()
        for (mLimb in mLimbs)
            appendLong(mLimb)
        return offset
    }

    private fun appendLong(dw: Long) {
        if (iRRP == RANGE_RECIP_PARAMS.size)
            throw RuntimeException("RANGE_RECIP_PARAMS is too small")
        RANGE_RECIP_PARAMS[iRRP] = dw
        ++iRRP
    }

    private fun initRangeRecipMulPow10() {
        calcPowTables()
        populateTable()
        tableMerge()
        serializeTable()
    }


    object Fnv1aChecksum {
        private const val FNV_OFFSET_BASIS = -0x7ee3ad4b // 2166136261 as signed Int
        private const val FNV_PRIME = 16777619

        fun fnv1a(bytes: ByteArray): Int {
            var hash = FNV_OFFSET_BASIS
            for (b in bytes) {
                hash = hash xor (b.toInt() and 0xff)
                hash *= FNV_PRIME
            }
            return hash
        }

        fun fnv1a(longs: LongArray): Int {
            var hash = FNV_OFFSET_BASIS
            for (value in longs) {
                // Process the 8 bytes within the Long
                for (i in 0..7) {
                    val byte = ((value shr (i * 8)) and 0xFF).toInt()
                    hash = hash xor byte
                    hash *= FNV_PRIME
                }
            }
            return hash
        }
    }
}
