package com.decimal128.decimal

import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul1
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul2
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul3
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul4
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.lang.Math.min

object GenerateRangeRecipPow5_take2 {

    val verbose = false

    //private const val Q_MIN = POW10_64_COUNT
    //private const val Q_MAXX = 79
    //private const val K_MIN = BARRETT_POW10_MAXX
    //private const val K_MAXX = com.decimal128.Q_MAXX - 34

    private const val EXACT = 0
    private const val LT_HALF = 1
    private const val HALF = 2
    private const val GT_HALF = 3

    private fun getResidue(remainder: IntArray, denominator: IntArray): Int {
        if (Car.isZero(remainder))
            return EXACT
        val remainderDoubled = Car.newShiftLeft(remainder, 1)
        val cmp = Car.compare(remainderDoubled, denominator)
        return when {
            cmp < 0 -> LT_HALF
            cmp == 0 -> HALF
            else -> GT_HALF
        }
    }

    private var EMPTY: Array<IntArray> = emptyArray()
    private var ONE = intArrayOf(1)
    private var POW_10 = Array<IntArray>(Q_MAXX) { ONE }
    private var POW_5 = Array<IntArray>(K_MAXX) { ONE }

    private fun calcPowTables() {
        for (i in 1..<Q_MAXX)
            POW_10[i] = Car.newMul(POW_10[i-1], 10)
        for (i in 1..<K_MAXX)
            POW_5[i] = Car.newMul(POW_5[i-1], 5)
    }

    private data class TableEntry(val qMax: Int, val k: Int, val prodBitLen: Int, val M: IntArray, val S: Int) {
        var qMin = qMax
        fun prodDwordLen() = (prodBitLen + 0x3f) ushr 6

        override fun toString() = if (qMax == -1) "[NULL]" else "[q:$qMin-$qMax k:$k bitLen:$prodBitLen M:${Car.toString(M)} S:$S]"
    }

    // qMin = 8 bits
    // qMax = 8 bits
    // k = 8 bits
    // maxProdDwordLen = 8 bits
    // S = 16 bits
    // mDwordLen = 16 bits
    private fun packDescriptor(te: TableEntry): Long {
        val prodDwordLen = te.prodDwordLen()
        val MDwordLen = (te.M.size + 1) ushr 1
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

    private val NULL_TABLE_ENTRY = TableEntry(-1, -1, -1, ONE, -1)

    private var recipTable : Array<Array<TableEntry>> = Array(Q_MAXX) { Array<TableEntry>(K_MAXX) { NULL_TABLE_ENTRY} }

    private fun populateTable() {
        for (j in Q_MIN..<Q_MAXX) {
            var prev = recipTable[j-1][K_MIN]
            for (k in K_MIN..<min(j, K_MAXX)) {
                val yPrev = if (prev != NULL_TABLE_ENTRY) prev.S else 5 * K_MIN
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
        val twoPowY = Car.newShiftLeft(ONE, y)
        val fivePowK = POW_5[k]
        val tenPowK = POW_10[k]
        val tenPowQ = POW_10[q]
        val x = Car.newAdd(twoPowY, fivePowK)
        Car.mutateSub(x, 1)
        val M = Car.newCopy(Car.newDiv(x, fivePowK)) // eliminate leading zero words
        //println("q:$q k:$k y:$y x:${Car.toString(x)} / fivePowK:${Car.toString(fivePowK)} => M:${Car.toString(M)}")
        val S = y

        val C_max = tenPowQ // for me C_max == 10**q
        val C_max_prime = if (k == 1) C_max else Car.newShiftRight(C_max, k - 1)
        val maxProd = Car.newMul(C_max_prime, M)

        if (!isValid(tenPowQ, k, M, S))
            return null
        //println("tenPowQ is valid")
        val half = Car.newShiftRight(tenPowK, 1)
        if (!isValid(half, k, M, S))
            return null
        //println("half is valid")
        val nines5 = Car.newSub(tenPowQ, half)
        if (!isValid(nines5, k, M, S))
            return null
        //println("nines5 is valid")
        val nines5down = Car.newSub(nines5, 1)
        if (!isValid(nines5down, k, M, S))
            return null
        //println("nines5down is valid")
        val nines5up = Car.newAdd(nines5, 1)
        if (!isValid(nines5up, k, M, S))
            return null
        //println("nines5up is valid")

        val bitLen = Car.bitLen(maxProd)
        return TableEntry(q, k, bitLen, M, y)
    }

    private fun isValid(dividend: IntArray, k: Int, M: IntArray, S: Int): Boolean {
        val tenPowK = POW_10[k]
        val quotRem = Car.newDivMod(dividend, tenPowK)
        val expectedQuot = quotRem[0]
        val expectedResidue = getResidue(quotRem[1], tenPowK)
        //println("expectedQuot:${Car.toString(expectedQuot)} rem:${Car.toString(quotRem[1])} expectedResidue:$expectedResidue")

        val dividendAlfa = if (k == 1) dividend else Car.newShiftRight(dividend, k - 1)
        val maskAlfa = Car.mutateSub(Car.newShiftLeft(ONE, k - 1), 1)
        val fracAlfa = Car.newAnd(dividend, maskAlfa)
        val stickyAlfa = Car.bitLen(fracAlfa) > 0
        //println("dividendAlfa:${Car.toString(dividendAlfa)} fracAlfa:${Car.toString(fracAlfa)} stickyAlfa:$stickyAlfa")

        val prod = Car.newMul(dividendAlfa, M)
        val quotPlusRound = Car.newShiftRight(prod, S)
        val quot = Car.newShiftRight(quotPlusRound, 1)
        val round = (quotPlusRound[0] and 1) > 0
        //println("prod:${Car.toString(prod)} quotPlusRound:${Car.toString(quotPlusRound)} quot:${Car.toString(quot)} round:$round")
        if (! Car.EQ(expectedQuot, quot))
            return false
        val fracBeta = Car.newSub(prod, Car.newShiftLeft(quotPlusRound, S))
        val stickyBeta = Car.compare(fracBeta, M) >= 0

        val sticky = stickyAlfa or stickyBeta

        val residue = when {
            round && sticky -> GT_HALF
            round -> HALF
            sticky -> LT_HALF
            else -> EXACT
        }

        return expectedResidue == residue
    }

    private fun tableMerge() {
        for (k in K_MIN..<K_MAXX) {
            var tePrev = recipTable[Q_MAXX-1][k]
            for (q in Q_MAXX-2 downTo Q_MIN) {
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

    private const val ROW_SIZE = K_MAXX - K_MIN
    private const val TABLE_SIZE = (Q_MAXX - Q_MIN) * ROW_SIZE
    private val OFFSETS = ShortArray(TABLE_SIZE)
    // I tried setting this up to use triangle indexing
    // it only saved 15% of the table size and required
    // more calculation to find the offsetIndex, esp because
    // of the upper triangle vs the lower rectangle
    private fun offsetIndex(digitCount: Int, pow10: Int): Int {
        assert(digitCount in Q_MIN..<Q_MAXX)
        assert(pow10 in K_MIN..<K_MAXX)
        val index = (digitCount - Q_MIN) * ROW_SIZE + (pow10 - K_MIN)
        return index
    }

    var iRRP = 1
    val RANGE_RECIP_PARAMS = LongArray(814)

    private fun serializeTable() {
        for (q in Q_MIN..<Q_MAXX) {
            for (k in K_MIN..<min(q, K_MAXX)) {
                val te = recipTable[q][k]
                val i = offsetIndex(q, k)
                val offset = when {
                    te == NULL_TABLE_ENTRY -> -1
                    q > te.qMin -> OFFSETS[offsetIndex(te.qMin, k)]
                    else -> serialize(te)
                }
                OFFSETS[i] = offset.toShort()
            }
        }

        if (RANGE_RECIP_PARAMS.size != iRRP) {
            println("RANGE_RECIP_PARAMS.size should be $iRRP")
        }
    }

    private fun serialize(te: TableEntry): Int {
        val offset = iRRP
        appendLong(packDescriptor(te))
        val M = te.M
        for (i in 0..<(M.size and 0x7FFF_FFFE) step 2) {
            val mLimb = (M[i + 1].toLong() shl 32) + (M[i].toLong() and 0xFFFF_FFFFL)
            appendLong(mLimb)
        }
        if ((M.size and 1) != 0) {
            val mLimb = M[M.size - 1].toLong() and 0xFFFF_FFFFL
            appendLong(mLimb)
        }
        return offset
    }

    private fun appendLong(dw: Long) {
        if (iRRP == RANGE_RECIP_PARAMS.size)
            throw RuntimeException("RANGE_RECIP_PARAMS is too small")
        RANGE_RECIP_PARAMS[iRRP] = dw
        ++iRRP
    }

    private fun releaseTemporaryStorage() {
        POW_5 = EMPTY
        POW_10 = EMPTY
        recipTable = emptyArray()
    }

    private var initialized = false
    fun initialize() {
        if (! initialized) {
            calcPowTables()
            populateTable()
            tableMerge()
            serializeTable()
            //releaseTemporaryStorage()
            initialized = true
        }
    }

    fun dumpTable() {
        if (verbose) {
            print("        ")
            for (k in K_MIN..<K_MAXX) {
                print("$k  ")
            }
            println()
            for (j in Q_MIN..<Q_MAXX) {
                print("$j : ")
                for (k in K_MIN..<K_MAXX) {
                    val te = recipTable[j][k]
                    print("$te ")
                }
                println()
            }
        }
    }

    fun dumpDeltas() {
        var maxIntraRowDelta = 0
        var maxInterRowDelta = 0
        var uniqueEntryCount = 0
        var nonNullEntryCount = 0
        val maxProdBitLens = IntArray(Q_MAXX)
        var maxProdBitLen = 0
        var maxS = 0
        for (q in Q_MIN..<Q_MAXX) {
            if (q > Q_MIN) {
                val prevRow = recipTable[q - 1][K_MIN]
                val thisRow = recipTable[q][K_MIN]
                if (prevRow != NULL_TABLE_ENTRY && thisRow != NULL_TABLE_ENTRY) {
                    val interRowDelta = thisRow.S - prevRow.S
                    maxInterRowDelta = java.lang.Math.max(maxInterRowDelta, interRowDelta)
                }
            }
            var intraRowSum = 0
            var prevTE: TableEntry = NULL_TABLE_ENTRY
            for (k in K_MIN + 1..<K_MAXX) {
                val te = recipTable[q][k]
                if (te != NULL_TABLE_ENTRY && prevTE != NULL_TABLE_ENTRY) {
                    val intraRowDelta = te.S - prevTE.S
                    maxIntraRowDelta = java.lang.Math.max(maxIntraRowDelta, intraRowDelta)
                    intraRowSum += intraRowDelta
                    maxProdBitLens[q] = java.lang.Math.max(maxProdBitLens[q], te.prodBitLen)
                }
                if (te != NULL_TABLE_ENTRY) {
                    ++nonNullEntryCount
                    if (q == te.qMin)
                        ++uniqueEntryCount
                    maxS = java.lang.Math.max(maxS, te.S)
                    maxProdBitLen = java.lang.Math.max(maxProdBitLen, te.prodBitLen)
                }
                prevTE = te
            }
            val intraRowCount = min(K_MAXX, q) - K_MIN - 1
            if (intraRowCount > 0) {
                val avg = intraRowSum.toDouble() / intraRowCount
                if (verbose)
                    println("j:$q intraRowCount:$intraRowCount avg:$avg maxBitLen:${maxProdBitLens[q]}")
            }
        }
        if (verbose) {
            println("maxIntraRowDelta:$maxIntraRowDelta")
            println("maxInterRowDelta:$maxInterRowDelta")
            println("nonNullEntryCount:$nonNullEntryCount")
            println("uniqueEntryCount:$uniqueEntryCount")
            println("maxS:$maxS")
            println("maxProdBitLen:$maxProdBitLen")
        }
    }

    fun dumpOffsets() {
        var i = 0
        for (q in Q_MIN..<Q_MAXX) {
            for (k in K_MIN..<min(q, K_MAXX)) {
                val offsetIndex = offsetIndex(q, k)
                val offset = OFFSETS[offsetIndex]
                if (verbose)
                    println("$q,$k => $offset")
                //assert(i == offsetIndex)
                ++i
            }
        }
    }

    fun dumpSerialized() {
        for (q in Q_MIN..<Q_MAXX) {
            for (k in K_MIN..<min(q, K_MAXX)) {
                val i = offsetIndex(q, k)
                val offset = OFFSETS[i]
                val descriptor = RANGE_RECIP_PARAMS[offset.toInt()]
                val qMin = unpackQMin(descriptor)
                val qMax = unpackQMax(descriptor)
                val k2 = unpackK(descriptor)
                val prodDwordLen = unpackProdDwordLen(descriptor)
                val S = unpackS(descriptor)
                val mDwordLen = unpackMDwordLen(descriptor)
                val mOffset = offset + 1
                val mLongArray = Arrays.copyOfRange(RANGE_RECIP_PARAMS, mOffset, mOffset + mDwordLen)
                val mStr = mLongArray.contentToString()
                if (verbose)
                    println("($q, $k) qMin:$qMin qMax:$qMax k:$k2 prodDwordLen:$prodDwordLen S:$S mDwordLen:$mDwordLen M:$mStr")
            }
        }
    }

    @Test
    fun testRun() {
        initialize()
        println("iRRP:$iRRP")
        dumpDeltas()
        dumpTable()
        dumpOffsets()
        dumpSerialized()
    }

    @Test
    fun testProblemChild() {
        calcPowTables()
        val te = computeMSIfValid(30, 29, 138)
        println(te)
    }

    fun rangeDivPow10(z: U256, x: U256, pow10: Int): Residue {
        assert(pow10 >= K_MIN)
        initialize()
        return _divPow10(z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10)
    }

    private fun _divPow10(
        z: U256, q: Int, x3: Long, x2: Long, x1: Long, x0: Long, k: Int): Residue {
        require(q in Q_MIN..<Q_MAXX)
        require(k in K_MIN..<K_MAXX)
        // clear coeff without worrying about aliasing
        z.u256EnableIndexSetAndZeroOut()

        val paramsIndex = OFFSETS[offsetIndex(q, k)].toInt()
        val descriptor = RANGE_RECIP_PARAMS[paramsIndex]
        assert(q in unpackQMin(descriptor)..unpackQMax(descriptor))
        assert(k == unpackK(descriptor))
        val prodDwordLen = unpackProdDwordLen(descriptor)
        val mDwordCount = unpackMDwordLen(descriptor)
        val shift = unpackS(descriptor)
        val fractionBitLen = shift + 1

        val dividendShiftRight = k - 1
        assert(dividendShiftRight in 1..<64)
        val stickyBitsAlfa = x0 and ((1L shl dividendShiftRight) - 1)

        val dividendShiftLeft = 64 - dividendShiftRight
        val d0 = (x1 shl dividendShiftLeft) or (x0 ushr dividendShiftRight)
        val d1 = (x2 shl dividendShiftLeft) or (x1 ushr dividendShiftRight)
        val d2 = (x3 shl dividendShiftLeft) or (x2 ushr dividendShiftRight)
        val d3 = (x3 ushr dividendShiftRight)

        val residue = when {
            (d3 != 0L) ->
                u256RecipMul4(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d3, d2, d1, d0, fractionBitLen, stickyBitsAlfa
                )

            (d2 != 0L) ->
                u256RecipMul3(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d2, d1, d0, fractionBitLen, stickyBitsAlfa
                )

            (d1 != 0L) ->
                u256RecipMul2(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d1, d0, fractionBitLen, stickyBitsAlfa
                )

            (d0 != 0L) ->
                u256RecipMul1(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d0, fractionBitLen, stickyBitsAlfa
                )

            else -> throw RuntimeException("why am I here?")
        }

        z.u256DisableIndexSetAndUpdateLengths()
        return residue
    }

    class TC(val strDividend: String, val pow10: Int, val strExpected: String, val expectedResidue: Int) {
    }

    val tcs = arrayOf(
        TC("12345678900000000000", 11, "123456789", EXACT),
        TC("12345678901234567890", 10, "1234567890", LT_HALF),
        TC("12345000000000000000", 15, "12345", EXACT),
        TC("12345000000000000000", 16, "1234", HALF),
        TC("12345678901234567890", 12, "12345678", GT_HALF),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val dividend = U256(tc.strDividend)
        val k = tc.pow10
        val z = U256()
        val residue = rangeDivPow10(z, dividend, k)
        val expected = U256(tc.strExpected)
        if (verbose)
            println("$dividend / 10**$k => $expected (${tc.expectedResidue})")
        assertEquals(expected, z)
        assertEquals(tc.expectedResidue, residue.value)
    }

}