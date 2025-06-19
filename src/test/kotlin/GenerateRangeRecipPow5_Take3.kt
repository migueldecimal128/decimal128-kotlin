package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.math.min

class GenerateRangeRecipPow5_Take3 {

    val j_min = 2
    val j_maxx = 79
    val k_min = 1
    val k_maxx = 44

    val ONE = BigInteger.ONE
    val TWO = BigInteger.TWO
    val FIVE = BigInteger.valueOf(5)
    val TEN = BigInteger.TEN

    data class TableEntry(val r: Int, val x: Int, val bitLen: Int, val M: BigInteger, val S: Int)

    val recipTable : Array<Array<TableEntry?>> = Array(j_maxx) { arrayOfNulls<TableEntry>(k_maxx)}

    fun populateTable() {
        for (j in j_min..<j_maxx) {
            var prev = recipTable[j-1][k_min]
            for (k in k_min..<min(j, k_maxx)) {
                val yPrev = if (prev != null) prev.S else 4
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

    fun findTableEntry(r: Int, x: Int, yStart: Int): TableEntry? {
        var y = yStart
        var te = computeMSIfValid(r, x, y)
        if (te != null) {
            while (true) {
                --y
                val teDown = computeMSIfValid(r, x, y) ?: return te
                te = teDown
            }
        } else {
            var yUp = yStart + 1
            while (true) {
                val teUp = computeMSIfValid(r, x, yUp)
                if (teUp != null)
                    return teUp
                ++yUp
            }
        }
    }

    fun computeMSIfValid(r: Int, x: Int, y: Int): TableEntry? {
        val divisor = FIVE.pow(x)                   // 5^x
        val maxC    = FIVE.pow(r).subtract(ONE)     // 5^r - 1
        val H       = maxC.divide(divisor)          // floor((5^r - 1) / 5^x)

        // truncation multiplier: ceil(2^y / 5^x)
        val twoY   = ONE.shiftLeft(y)
        val Mtrunc = twoY.add(divisor).subtract(ONE).divide(divisor)

        // anchors
        val C1     = H.multiply(divisor)
        val halfUp = divisor.add(ONE).shiftRight(1)   // ceil(5^x/2)
        val C2     = C1.add(halfUp).subtract(ONE)
        val C3     = C1.add(halfUp)
        val C4     = C1.add(divisor).subtract(ONE)

        fun rem(v: BigInteger) = v.multiply(Mtrunc).and(twoY.subtract(ONE))
        val r1 = rem(C1); val r2 = rem(C2); val r3 = rem(C3); val r4 = rem(C4)

        val lower1 = ONE
        val upper1 = Mtrunc

        val half    = ONE.shiftLeft(y - 1)
        val halfPlus= half.add(Mtrunc)

        val lower2 = Mtrunc.add(ONE); val upper2 = half
        val lower3 = half.add(ONE);   val upper3 = halfPlus
        val lower4 = halfPlus.add(ONE); val upper4 = twoY

        // strict checks via compareTo()
        if (r1.compareTo(lower1) >= 0 && r1.compareTo(upper1) < 0 &&
            r2.compareTo(lower2) >= 0 && r2.compareTo(upper2) < 0 &&
            r3.compareTo(lower3) >= 0 && r3.compareTo(upper3) < 0 &&
            r4.compareTo(lower4) >= 0 && r4.compareTo(upper4) < 0) {
            // bump for guard bit
            val yRound = y + 1
            val twoYr  = ONE.shiftLeft(yRound)
            val Mround = twoYr.add(divisor).subtract(ONE).divide(divisor)
            val Sround = y
            val maxBi = maxOf(C1, C2, C3, C4, H, maxC, Mtrunc, Mround, r1, r2, r3, r4)
            return TableEntry(r, x, maxBi.bitLength(), Mround, Sround)
        }
        return null
    }


    fun dumpTable() {
        print("        ")
        for (k in k_min..<k_maxx) {
            print("$k  ")
        }
        println()
        for (j in j_min..<j_maxx) {
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
        var maxBitLen = 0
        for (j in j_min..<j_maxx) {
            if (j > j_min) {
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
                    maxBitLen = kotlin.math.max(maxBitLen, te.bitLen)
                }
                prevTE = te
            }
            val intraRowCount = min(k_maxx, j) - k_min - 1
            if (intraRowCount > 0) {
                val avg = intraRowSum.toDouble() / intraRowCount
                println("j:$j intraRowCount:$intraRowCount avg:$avg")
            }
        }
        println("maxIntraRowDelta:$maxIntraRowDelta")
        println("maxInterRowDelta:$maxInterRowDelta")
        println("maxBitLen:$maxBitLen")
    }

    @Test
    fun testRun() {
        populateTable()
        dumpDeltas()
        //dumpTable()
    }

}
