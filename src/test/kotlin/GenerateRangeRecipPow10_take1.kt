package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.math.min

private val ONE = BigInteger.ONE
private val TEN = BigInteger.TEN

private val EXACT = 0
private val LT_HALF = 1
private val HALF = 2
private val GT_HALF = 3

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


class GenerateRangeRecipPow10_take1 {

    val q_min = 2
    val q_maxx = 79
    val k_min = 1
    val k_maxx = 44

    data class TableEntry(val q: Int, val k: Int, val bitLen: Int, val M: BigInteger, val S: Int)

    val recipTable : Array<Array<TableEntry?>> = Array(q_maxx) { arrayOfNulls<TableEntry>(k_maxx)}

    fun populateTable() {
        for (j in q_min..<q_maxx) {
            var prev = recipTable[j-1][k_min]
            for (k in k_min..<min(j, k_maxx)) {
                val yPrev = if (prev != null) prev.S else 8
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
        val tenPowK = TEN.pow(k)
        val tenPowQ = TEN.pow(q)
        val M = twoPowY.add(tenPowK).subtract(ONE).divide(tenPowK)
        val S = y

        val prod = tenPowQ.multiply(M)
        val C_max = tenPowQ.subtract(ONE)

        if (!isValid(tenPowQ, tenPowK, twoPowY, M, S))
            return null
        val half = tenPowK.shiftRight(1)
        if (!isValid(half, tenPowK, twoPowY, M, S))
            return null
        val nines5 = tenPowQ.subtract(half)
        if (!isValid(nines5, tenPowK, twoPowY, M, S))
            return null
        val down = nines5.subtract(ONE)
        if (!isValid(down, tenPowK, twoPowY, M, S))
            return null
        val up = nines5.add(ONE)
        if (!isValid(up, tenPowK, twoPowY, M, S))
            return null

        val bitLen = prod.bitLength()
        return TableEntry(q, k, bitLen, M, y)
    }

    fun isValid(dividend: BigInteger, tenPowK: BigInteger, twoPowY: BigInteger, M: BigInteger, S: Int): Boolean {
        val quotRem = dividend.divideAndRemainder(tenPowK)
        val expectedQuot = quotRem[0]
        val expectedResidue = getResidue(quotRem[1], tenPowK)

        val prod = dividend.multiply(M)
        val quot = prod.shiftRight(S)
        if (expectedQuot != quot)
            return false
        val mask = twoPowY.subtract(ONE)
        val frac = prod.and(mask)
        if (frac < M)
            return expectedResidue == EXACT
        val halfTwoPowY = twoPowY.shiftRight(1)
        if (frac < halfTwoPowY)
            return expectedResidue == LT_HALF
        if (frac < halfTwoPowY.add(M))
            return expectedResidue == HALF
        else
            return expectedResidue == GT_HALF
    }

    fun dumpTable() {
        print("        ")
        for (k in k_min..<k_maxx) {
            print("$k  ")
        }
        println()
        for (j in q_min..<q_maxx) {
            print("$j : ")
            for (k in k_min..<k_maxx) {
                val te = recipTable[j][k]
                print("$te ")
            }
            println()
        }
    }

    fun dumpDeltas() {
        var maxIntraRowDelta = 0
        var maxInterRowDelta = 0
        var maxBitLen = IntArray(q_maxx)
        for (j in q_min..<q_maxx) {
            if (j > q_min) {
                val prevRow = recipTable[j-1][k_min]
                val thisRow = recipTable[j][k_min]
                if (prevRow != null && thisRow != null) {
                    val interRowDelta = thisRow.S - prevRow.S
                    maxInterRowDelta = kotlin.math.max(maxInterRowDelta, interRowDelta)
                }
            }
            var intraRowSum = 0
            var prevTE: TableEntry? = null
            for (k in k_min + 1..<k_maxx) {
                val te = recipTable[j][k]
                if (te != null && prevTE != null) {
                    val intraRowDelta = te.S - prevTE.S
                    maxIntraRowDelta = kotlin.math.max(maxIntraRowDelta, intraRowDelta)
                    intraRowSum += intraRowDelta
                    maxBitLen[j] = kotlin.math.max(maxBitLen[j], te.bitLen)
                }
                prevTE = te
            }
            val intraRowCount = min(k_maxx, j) - k_min - 1
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
        populateTable()
        dumpDeltas()
        dumpTable()
    }

}
