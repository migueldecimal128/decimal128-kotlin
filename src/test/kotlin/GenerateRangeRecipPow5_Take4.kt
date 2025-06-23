package com.decimal128

import org.junit.jupiter.api.Test
import kotlin.math.min

class GenerateRangeRecipPow5_Take4 {

    val verbose = true

    val j_min = 2
    val j_maxx = 79
    val k_min = 1
    val k_maxx = 44

    private val POWERS_5 = Array<IntArray>(j_maxx) { Car.EMPTY_CAR}

    data class TableEntry(val r: Int, val x: Int, val M: IntArray, val S: Int)

    private val NULL_TE = TableEntry(-1, -1, Car.EMPTY_CAR, -1)
    var recipTable : Array<Array<TableEntry>> = Array(j_maxx) { Array(k_maxx) { NULL_TE } }
    var populated =false

    fun populatePowers5() {
        var carPow5 = intArrayOf(1)
        var i = 0
        while (true) {
            POWERS_5[i++] = carPow5
            if (i == j_maxx)
                break
            carPow5 = Car.newMul(carPow5, 5)
        }
/*
        for (j in 0..<j_maxx) {
            val car = POWERS_5_CAR[j]
            val str = CarTransducer.carToString(car)
            val bitLen = Car.bitLen(car)
            val carSize = car.size
            println("5**$j => $str bitLen:$bitLen carSize:$carSize")
            if (carSize != (bitLen + 0x1F) ushr 5)
                println("empty limb!")
        }
 */
    }



    fun populateTable() {
        if (populated)
            return
        for (j in j_min..<j_maxx) {
            var prev = recipTable[j-1][k_min]
            for (k in k_min..<min(j, k_maxx)) {
                val yPrev = if (prev.r != -1) prev.S else 4
                val te = findTableEntry(j, k, yPrev + 2)
                if (te != null) {
                    //println("($j, $k) => minimalY:${te.S}")
                    recipTable[j][k] = te
                    prev = te
                    //println(te)
                }
            }
        }
        populated = false
    }

    fun findTableEntry(r: Int, x: Int, yStart: Int): TableEntry? {
        var y = yStart
        //println("($r, $x) yStart:$yStart")
        var te = computeMSIfValid(r, x, y)
        val yDelta = if (te == null) 1 else -1
        while (true) {
            y += yDelta
            val teDelta = computeMSIfValid(r, x, y)
            if (te != null && teDelta == null)
                return te
            if (te == null && teDelta != null)
                return teDelta
            te = teDelta
        }
    }

    val ONE = intArrayOf(1)

    fun computeMSIfValid(r: Int, x: Int, y: Int): TableEntry? {
        val maxC    = Car.newSub(POWERS_5[r], 1)     // 5^r - 1
        val divisor = POWERS_5[x]                   // 5^x
        val H       = Car.newDivide(maxC, divisor)          // floor((5^r - 1) / 5^x)

        // truncation multiplier: ceil(2^y / 5^x)
        val twoPowY   = Car.newShiftLeft(ONE, y)
        val twoPowYMinus1 = Car.newSub(twoPowY, 1)
        val Mtrunc = Car.newDivide(Car.mutateSub(Car.newAdd(twoPowY, divisor), 1), divisor)

        // anchors
        val C1     = Car.newMul(H, divisor)
        val halfUp = Car.mutateShiftRight(Car.newAdd(divisor, 1), 1)   // ceil(5^x/2)
        val C3     = Car.newAdd(C1, halfUp)
        val C2     = Car.newSub(C3, 1)
        val C4     = Car.mutateSub(Car.newAdd(C1, divisor), 1)

        fun rem(v: IntArray) = Car.newAnd(Car.newMul(v, Mtrunc), twoPowYMinus1)

        val r1 = rem(C1)
        val r2 = rem(C2)
        val r3 = rem(C3)
        val r4 = rem(C4)

        val lower1 = ONE
        val upper1 = Mtrunc

        val half    = Car.newShiftLeft(ONE, y - 1)
        val halfPlus= Car.newAdd(half, Mtrunc)

        val lower2 = Car.newAdd(Mtrunc, 1); val upper2 = half
        val lower3 = Car.newAdd(half, 1);   val upper3 = halfPlus
        val lower4 = Car.newAdd(halfPlus, 1); val upper4 = twoPowY

        // strict checks via compareTo()
        if (Car.compare(r1, lower1) >= 0 && Car.compare(r1, upper1) < 0 &&
            Car.compare(r2, lower2) >= 0 && Car.compare(r2, upper2) < 0 &&
            Car.compare(r3, lower3) >= 0 && Car.compare(r3, upper3) < 0 &&
            Car.compare(r4, lower4) >= 0 && Car.compare(r4, upper4) < 0) {
            // bump for guard bit
            val yRound = y + 1
            val twoYr  = Car.newShiftLeft(ONE, yRound)
            val Mround = Car.newDivide(Car.mutateSub(Car.newAdd(twoYr, divisor), 1), divisor)
            val Sround = y
            return TableEntry(r, x, Mround, Sround)
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
                if (prevRow != NULL_TE && thisRow != NULL_TE) {
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
                    maxBitLen = kotlin.math.max(maxBitLen, Car.bitLen(te.M))
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
        populatePowers5()
        populateTable()
        dumpDeltas()
        //dumpTable()
    }

    class TC(val str: String, val pow10: Int) {
        val car = CarTransducer.carFromString(str)
    }

    val tcs = arrayOf(
        //TC("10", 1),
        //TC("100", 1),
        //TC("1000", 1),
        //TC("10000", 1),
        TC("100000", 1),
        TC("1000000", 1),
        TC("1000000000", 1),
        TC("10000000000000000000", 1),
        TC("10000000000000000000000000000000000000000000", 1),
        //TC("10000000000000000000000000000000000000000000", 20)
    )

    @Test
    fun testCases() {
        populatePowers5()
        populateTable()
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose) {
            val str2 = CarTransducer.carToString(tc.car)
            assert(str2 == tc.str)
            println("$str2 / 10**${tc.pow10}")
        }
        test1(tc.car, tc.pow10)
    }

    fun test1(car: IntArray, k: Int) {
        val denom = Car.newShiftLeft(POWERS_5[k], k)
        val carKnuth = Car.newDivide(car, denom)

        val carRecipMul = divideRecipMulPow5(car, k)
        assert(Car.EQ(carKnuth, carRecipMul))
    }

    fun divideRecipMulPow5(car: IntArray, pow5: Int): IntArray {
        val p = CarTransducer.calcDigitCount(car)
        val te = recipTable[p][pow5]
        assert(te.r == p)
        assert(te.x == pow5)

        val carT = Car.newShiftRight(car, pow5)
        val prod = Car.newMul(carT, te.M)
        val quotWithGuard = Car.newShiftRight(prod, te.S)
        val quot = Car.newShiftRight(quotWithGuard, 1)
        return quot
    }


}
