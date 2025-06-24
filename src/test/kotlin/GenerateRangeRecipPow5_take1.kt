package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.math.min

object GenerateRangeRecipPow5_take1 {

    private val ONE = BigInteger.ONE
    private val TEN = BigInteger.TEN

    private const val Q_MIN = 11
    private const val Q_MAXX = 79
    private const val K_MIN = 10
    private const val K_MAXX = 44

    private const val EXACT = 0
    private const val LT_HALF = 1
    private const val HALF = 2
    private const val GT_HALF = 3

    private fun getResidue(remainder: BigInteger, denominator: BigInteger): Int {
        if (remainder.bitLength() == 0)
            return EXACT
        val cmp = remainder.shiftLeft(1).compareTo(denominator)
        return when {
            cmp < 0 -> LT_HALF
            cmp == 0 -> HALF
            else -> GT_HALF
        }
    }

    private var POW_10 = Array<BigInteger>(Q_MAXX) { ONE }
    private var POW_5 = Array<BigInteger>(K_MAXX) { ONE }

    fun calcPowTables() {
        for (i in 1..<Q_MAXX)
            POW_10[i] = POW_10[i-1].multiply(TEN)
        for (i in 1..<K_MAXX)
            POW_5[i] = POW_10[i].shiftRight(i)
    }

    data class TableEntry(val maxQ: Int, val k: Int, val bitLen: Int, val M: BigInteger, val S: Int) {
        var minQ = maxQ
        fun dwordLen() = (bitLen + 0x3f) ushr 6

        override fun toString() = if (maxQ == -1) "[NULL]" else "[q:$minQ-$maxQ k:$k bitLen:$bitLen]"
    }

    val NULL_TABLE_ENTRY = TableEntry(-1, -1, -1, ONE, -1)

    val recipTable : Array<Array<TableEntry>> = Array(Q_MAXX) { Array<TableEntry>(K_MAXX) { NULL_TABLE_ENTRY} }

    fun populateTable() {
        for (j in Q_MIN..<Q_MAXX) {
            var prev = recipTable[j-1][K_MIN]
            for (k in K_MIN..<min(j, K_MAXX)) {
                val yPrev = if (prev != NULL_TABLE_ENTRY) prev.S else 8
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

    fun findTableEntry(q: Int, k: Int, yStart: Int): TableEntry? {
        var y = yStart
        var te = computeMSIfValid(q, k, y)
        if (te != null) {
            while (true) {
                --y
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
            }
        }
    }

    fun computeMSIfValid(q: Int, k: Int, y: Int): TableEntry? {
        val twoPowY = ONE.shiftLeft(y)
        val fivePowK = POW_5[k]
        val tenPowK = POW_10[k]
        val tenPowQ = POW_10[q]
        val M = twoPowY.add(fivePowK).subtract(ONE).divide(fivePowK)
        val S = y

        val C_max = tenPowQ // for me C_max == 10**q
        val maxProd = C_max.shiftRight(k - 1).multiply(M)

        if (!isValid(tenPowQ, k, tenPowK, twoPowY, M, S))
            return null
        val half = tenPowK.shiftRight(1)
        if (!isValid(half, k, tenPowK, twoPowY, M, S))
            return null
        val nines5 = tenPowQ.subtract(half)
        if (!isValid(nines5, k, tenPowK, twoPowY, M, S))
            return null
        val nines5down = nines5.subtract(ONE)
        if (!isValid(nines5down, k, tenPowK, twoPowY, M, S))
            return null
        val nines5up = nines5.add(ONE)
        if (!isValid(nines5up, k, tenPowK, twoPowY, M, S))
            return null

        val bitLen = maxProd.bitLength()
        return TableEntry(q, k, bitLen, M, y)
    }

    fun isValid(dividend: BigInteger, k: Int, tenPowK: BigInteger, twoPowY: BigInteger, M: BigInteger, S: Int): Boolean {
        val quotRem = dividend.divideAndRemainder(TEN.pow(k))
        val expectedQuot = quotRem[0]
        val expectedResidue = getResidue(quotRem[1], tenPowK)

        val dividendAlfa = dividend.shiftRight(k - 1)
        val maskAlfa = ONE.shiftLeft(k - 1) - ONE
        val fracAlfa = dividend.and(maskAlfa)
        val stickyAlfa = fracAlfa.bitLength() > 0

        val prod = dividendAlfa.multiply(M)
        val quotPlusRound = prod.shiftRight(S)
        val quot = quotPlusRound.shiftRight(1)
        val round = quotPlusRound.and(ONE).bitLength() > 0
        if (expectedQuot != quot)
            return false
        val fracBeta = prod.subtract(quotPlusRound.shiftLeft(S))
        val stickyBeta = fracBeta >= M

        val sticky = stickyAlfa or stickyBeta

        val residue = when {
            round && sticky -> GT_HALF
            round -> HALF
            sticky -> LT_HALF
            else -> EXACT
        }

        return expectedResidue == residue
    }

    fun tableMerge() {
        for (k in K_MIN..<K_MAXX) {
            var tePrev = recipTable[Q_MAXX-1][k]
            for (q in Q_MAXX-2 downTo Q_MIN) {
                val te = recipTable[q][k]
                if (tePrev != NULL_TABLE_ENTRY && te.dwordLen() == tePrev.dwordLen()) {
                    tePrev.minQ = q
                    recipTable[q][k] = tePrev
                    //println("merge($q, $k)")
                } else {
                    tePrev = te
                }
            }
        }
    }

    fun dumpTable() {
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

    fun dumpDeltas() {
        var maxIntraRowDelta = 0
        var maxInterRowDelta = 0
        var maxBitLen = IntArray(Q_MAXX)
        for (j in Q_MIN..<Q_MAXX) {
            if (j > Q_MIN) {
                val prevRow = recipTable[j-1][K_MIN]
                val thisRow = recipTable[j][K_MIN]
                if (prevRow != NULL_TABLE_ENTRY && thisRow != NULL_TABLE_ENTRY) {
                    val interRowDelta = thisRow.S - prevRow.S
                    maxInterRowDelta = kotlin.math.max(maxInterRowDelta, interRowDelta)
                }
            }
            var intraRowSum = 0
            var prevTE: TableEntry = NULL_TABLE_ENTRY
            for (k in K_MIN + 1..<K_MAXX) {
                val te = recipTable[j][k]
                if (te != NULL_TABLE_ENTRY && prevTE != NULL_TABLE_ENTRY) {
                    val intraRowDelta = te.S - prevTE.S
                    maxIntraRowDelta = kotlin.math.max(maxIntraRowDelta, intraRowDelta)
                    intraRowSum += intraRowDelta
                    maxBitLen[j] = kotlin.math.max(maxBitLen[j], te.bitLen)
                }
                prevTE = te
            }
            val intraRowCount = min(K_MAXX, j) - K_MIN - 1
            if (intraRowCount > 0) {
                val avg = intraRowSum.toDouble() / intraRowCount
                println("j:$j intraRowCount:$intraRowCount avg:$avg maxBitLen:${maxBitLen[j]}")
            }
        }
        println("maxIntraRowDelta:$maxIntraRowDelta")
        println("maxInterRowDelta:$maxInterRowDelta")
    }

    @Test
    fun testRun() {
        calcPowTables()
        populateTable()
        tableMerge()
        dumpDeltas()
        dumpTable()
    }

}