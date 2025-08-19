@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.hugeint.Car
import com.decimal128.hugeint.HugeInt
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul4
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul3
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul2
import com.decimal128.decimal.U256RecipMulPow5.u256RecipMul1
import kotlin.math.min

const val Q_MIN = POW10_64_COUNT
const val Q_MAXX = 79 // exclusive
const val K_MIN = BARRETT_POW10_MAXX
const val K_MAXX = Q_MAXX - 34

object DivRangeRecipMulPow10 {

    private var EMPTY: Array<IntArray> = emptyArray()
    private var ONE = intArrayOf(1)
    private var POW_10 = Array<IntArray>(Q_MAXX) { ONE }
    private var POW_5 = Array<IntArray>(K_MAXX) { ONE }

    private var POW_10_HI = Array<HugeInt>(Q_MAXX) { HugeInt.ONE }
    private var POW_5_HI = Array<HugeInt>(K_MAXX) { HugeInt.ONE }

    private fun calcPowTables() {
        for (i in 1..<Q_MAXX) {
            POW_10[i] = Car.newMul(POW_10[i - 1], 10)
            POW_10_HI[i] = POW_10_HI[i - 1] * 10
        }
        for (i in 1..<K_MAXX) {
            POW_5[i] = Car.newMul(POW_5[i - 1], 5)
            POW_5_HI[i] = POW_5_HI[i - 1] * 5
        }
    }

    private data class TableEntry(val qMax: Int, val k: Int, val prodBitLen: Int, val M: HugeInt, val S: Int) {
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

    private val NULL_TABLE_ENTRY = TableEntry(-1, -1, -1, HugeInt.ONE, -1)

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
        val twoPowY_hi = HugeInt.ONE shl y
        val fivePowK_hi = POW_5_HI[k]
        val tenPowK_hi = POW_10_HI[k]
        val tenPowQ_hi = POW_10_HI[q]
        val x_hi = twoPowY_hi + fivePowK_hi - 1
        val M_hi = x_hi / fivePowK_hi

        val S = y

        val C_max_hi = tenPowQ_hi
        val C_max_prime_hi = C_max_hi shr (k - 1)
        val maxProd_hi = C_max_prime_hi * M_hi

        if (!isValid(tenPowQ_hi, k, M_hi, S))
            return null
        //println("tenPowQ is valid")
        val half_hi = tenPowK_hi shr 1

        if (!isValid(half_hi, k, M_hi, S))
            return null
        //println("half is valid")
        val nines5_hi = tenPowQ_hi - half_hi

        if (!isValid(nines5_hi, k, M_hi, S))
            return null
        //println("nines5 is valid")
        val nines5down_hi = nines5_hi - 1

        if (!isValid(nines5down_hi, k, M_hi, S))
            return null
        //println("nines5down is valid")
        val nines5up_hi = nines5_hi + 1

        if (!isValid(nines5up_hi, k, M_hi, S))
            return null
        //println("nines5up is valid")

        val bitLen = maxProd_hi.magnitudeBitLen()
        return TableEntry(q, k, bitLen, M_hi, y)
    }

    private fun isValid(dividend_hi: HugeInt, k: Int, M_hi: HugeInt, S: Int): Boolean {
        val dividend = dividend_hi.toLittleEndianIntArray()
        val M = M_hi.toLittleEndianIntArray()
        val tenPowK = POW_10[k]
        val quotRem = Car.newDivMod(dividend, tenPowK)
        val expectedQuot = quotRem[0]
        val expectedResidue = Residue.residueFromRemainderDivisor(quotRem[1], tenPowK)

        val tenPowK_hi = POW_10_HI[k]
        val (expectedQuot_hi, expectedRem_hi) = dividend_hi.divMod(tenPowK_hi)
        val expectedResidue_hi = Residue.residueFromRemainderDivisor(expectedRem_hi, tenPowK_hi)
        require (expectedQuot_hi EQ expectedQuot)
        require (expectedRem_hi EQ quotRem[1])
        require (expectedResidue_hi == expectedResidue)

        val dividendAlfa = if (k == 1) dividend else Car.newShiftRight(dividend, k - 1)
        val maskAlfa = Car.mutateSub(Car.newShiftLeft(ONE, k - 1), 1)
        val fracAlfa = Car.newAnd(dividend, maskAlfa)
        val stickyAlfa = Car.bitLen(fracAlfa) > 0
        //println("dividendAlfa:${Car.toString(dividendAlfa)} fracAlfa:${Car.toString(fracAlfa)} stickyAlfa:$stickyAlfa")

        val dividendAlfa_hi = dividend_hi shr (k - 1)
        require (dividendAlfa_hi EQ dividendAlfa)
        val maskAlfa_hi = HugeInt.withBitMask(k - 1)
        require (maskAlfa_hi EQ maskAlfa)
        val fracAlfa_hi = dividend_hi and maskAlfa_hi
        require (fracAlfa_hi EQ fracAlfa)
        val stickyAlfa_hi = fracAlfa_hi.isNotZero()
        if (stickyAlfa_hi != stickyAlfa) {
            println(
                "dividend_hi:$dividend_hi maskAlfa_hi:$maskAlfa_hi fracAlfa_hi:$fracAlfa_hi stickyAlfa_hi:$stickyAlfa_hi stickyAlfa:$stickyAlfa"
            )
            println("foo!")
        }
        require (stickyAlfa_hi == stickyAlfa)

        val prod = Car.newMul(dividendAlfa, M)
        val quotPlusRound = Car.newShiftRight(prod, S)
        val quot = Car.newShiftRight(quotPlusRound, 1)
        val round = (quotPlusRound[0] and 1) > 0

        val prod_hi = dividendAlfa_hi * M_hi
        val quotPlusRound_hi = prod_hi ushr S
        val quot_hi = quotPlusRound_hi ushr 1
        val round_hi = quotPlusRound_hi.testBit(0)

        require (prod_hi EQ prod)
        require (quotPlusRound_hi EQ quotPlusRound)
        require (quot_hi EQ quot)
        require (round_hi == round)

        //println("prod:${Car.toString(prod)} quotPlusRound:${Car.toString(quotPlusRound)} quot:${Car.toString(quot)} round:$round")
        if (! Car.EQ(expectedQuot, quot)) {
            require (expectedQuot_hi NE quot_hi)
            return false
        }
        val fracBeta = Car.newSub(prod, Car.newShiftLeft(quotPlusRound, S))
        val stickyBeta = Car.compare(fracBeta, M) >= 0

        val fracBeta_hi = prod_hi - (quotPlusRound_hi shl S)
        val stickyBeta_hi = fracBeta_hi >= M_hi

        require (fracBeta_hi EQ fracBeta)
        require (stickyBeta_hi == stickyBeta)

        val sticky = stickyAlfa or stickyBeta

        val sticky_hi = stickyAlfa_hi or stickyBeta_hi

        val residue = when {
            round && sticky -> Residue.GT_HALF
            round -> Residue.HALF
            sticky -> Residue.LT_HALF
            else -> Residue.EXACT
        }

        val residue_hi = when {
            round_hi and sticky_hi -> Residue.GT_HALF
            round_hi -> Residue.HALF
            sticky_hi -> Residue.LT_HALF
            else -> Residue.EXACT
        }

        require (residue_hi == residue)

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
        check(digitCount in Q_MIN..<Q_MAXX)
        check(pow10 in K_MIN..<K_MAXX)
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
        val mLimbs = te.M.toLittleEndianLongArray()
        for (mLimb in mLimbs)
            appendLong(mLimb)
        /*
        val M = te.M
        for (i in 0..<(M.size and 0x7FFF_FFFE) step 2) {
            val mLimb = (M[i + 1].toLong() shl 32) + (M[i].toLong() and 0xFFFF_FFFFL)
        }
        if ((M.size and 1) != 0) {
            val mLimb = M[M.size - 1].toLong() and 0xFFFF_FFFFL
            appendLong(mLimb)
        }
         */
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

        POW_5_HI = emptyArray()
        POW_10_HI = POW_5_HI

        recipTable = emptyArray()
    }

    private var initialized = false
    fun initialize() {
        if (! initialized) {
            calcPowTables()
            populateTable()
            tableMerge()
            serializeTable()
            releaseTemporaryStorage()
            initialized = true
        }
    }

    fun divModPow10(dividendDigitCount: Int, d0: Long, pow10: Int): Pair<Long, Long> {
        throw RuntimeException("not impl")
    }

    fun divModPow10(q: U256, r: U256, d: U256, pow10: Int) {
        throw RuntimeException("not impl")
    }

    fun rangeDivPow10(z: U256, x: U256, pow10: Int): Residue {
        check(pow10 >= K_MIN)
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
        check(q in unpackQMin(descriptor)..unpackQMax(descriptor))
        check(k == unpackK(descriptor))
        val prodDwordLen = unpackProdDwordLen(descriptor)
        val mDwordCount = unpackMDwordLen(descriptor)
        val shift = unpackS(descriptor)
        val fractionBitLen = shift + 1

        val dividendShiftRight = k - 1
        check(dividendShiftRight in 1..<64)
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

}
