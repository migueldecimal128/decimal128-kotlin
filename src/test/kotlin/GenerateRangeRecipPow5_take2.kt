package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.math.min

object GenerateRangeRecipPow5_take2 {

    private const val Q_MIN = 10
    private const val Q_MAXX = 79
    private const val K_MIN = 1
    private const val K_MAXX = 44

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

    private var ONE = intArrayOf(1)
    private var POW_10 = Array<IntArray>(Q_MAXX) { ONE }
    private var POW_5 = Array<IntArray>(K_MAXX) { ONE }

    fun calcPowTables() {
        for (i in 1..<Q_MAXX)
            POW_10[i] = Car.newMul(POW_10[i-1], 10)
        for (i in 1..<K_MAXX)
            POW_5[i] = Car.newMul(POW_5[i-1], 5)
    }

    data class TableEntry(val maxQ: Int, val k: Int, val bitLen: Int, val M: IntArray, val S: Int) {
        var minQ = maxQ
        fun dwordLen() = (bitLen + 0x3f) ushr 6

        override fun toString() = if (maxQ == -1) "[NULL]" else "[q:$minQ-$maxQ k:$k bitLen:$bitLen M:${Car.toString(M)} S:$S]"
    }

    val NULL_TABLE_ENTRY = TableEntry(-1, -1, -1, ONE, -1)

    val recipTable : Array<Array<TableEntry>> = Array(Q_MAXX) { Array<TableEntry>(K_MAXX) { NULL_TABLE_ENTRY} }

    fun populateTable() {
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

    fun findTableEntry(q: Int, k: Int, yStart: Int): TableEntry? {
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

    fun computeMSIfValid(q: Int, k: Int, y: Int): TableEntry? {
        val twoPowY = Car.newShiftLeft(ONE, y)
        val fivePowK = POW_5[k]
        val tenPowK = POW_10[k]
        val tenPowQ = POW_10[q]
        val x = Car.newAdd(twoPowY, fivePowK)
        Car.mutateSub(x, 1)
        val M = Car.newDiv(x, fivePowK)
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

    fun isValid(dividend: IntArray, k: Int, M: IntArray, S: Int): Boolean {
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
        var uniqueEntryCount = 0
        var nonNullEntryCount = 0
        val maxBitLen = IntArray(Q_MAXX)
        for (q in Q_MIN..<Q_MAXX) {
            if (q > Q_MIN) {
                val prevRow = recipTable[q-1][K_MIN]
                val thisRow = recipTable[q][K_MIN]
                if (prevRow != NULL_TABLE_ENTRY && thisRow != NULL_TABLE_ENTRY) {
                    val interRowDelta = thisRow.S - prevRow.S
                    maxInterRowDelta = kotlin.math.max(maxInterRowDelta, interRowDelta)
                }
            }
            var intraRowSum = 0
            var prevTE: TableEntry = NULL_TABLE_ENTRY
            for (k in K_MIN + 1..<K_MAXX) {
                val te = recipTable[q][k]
                if (te != NULL_TABLE_ENTRY && prevTE != NULL_TABLE_ENTRY) {
                    val intraRowDelta = te.S - prevTE.S
                    maxIntraRowDelta = kotlin.math.max(maxIntraRowDelta, intraRowDelta)
                    intraRowSum += intraRowDelta
                    maxBitLen[q] = kotlin.math.max(maxBitLen[q], te.bitLen)
                }
                if (te != NULL_TABLE_ENTRY) {
                    ++nonNullEntryCount
                    if (q == te.minQ)
                        ++uniqueEntryCount
                }
                prevTE = te
            }
            val intraRowCount = min(K_MAXX, q) - K_MIN - 1
            if (intraRowCount > 0) {
                val avg = intraRowSum.toDouble() / intraRowCount
                println("j:$q intraRowCount:$intraRowCount avg:$avg maxBitLen:${maxBitLen[q]}")
            }
        }
        println("maxIntraRowDelta:$maxIntraRowDelta")
        println("maxInterRowDelta:$maxInterRowDelta")
        println("nonNullEntryCount:$nonNullEntryCount")
        println("uniqueEntryCount:$uniqueEntryCount")
    }

    @Test
    fun testRun() {
        calcPowTables()
        populateTable()
        tableMerge()
        dumpDeltas()
        dumpTable()
    }

    @Test
    fun testProblemChild() {
        calcPowTables()
        val te = computeMSIfValid(30, 29, 138)
        println(te)
    }

}