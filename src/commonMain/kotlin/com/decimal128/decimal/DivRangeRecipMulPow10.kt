@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import com.decimal128.decimal.C256RecipMulPow5.c256RecipMul256
import com.decimal128.decimal.C256RecipMulPow5.c256RecipMul192
import com.decimal128.decimal.C256RecipMulPow5.c256RecipMul128
import com.decimal128.decimal.C256RecipMulPow5.c256RecipMul64
import kotlin.math.min

const val Q_MIN = POW10_64_COUNT
const val Q_MAXX = 79 // exclusive
const val K_MIN = BARRETT_POW10_MAXX
const val K_MAXX = Q_MAXX - 34

object DivRangeRecipMulPow10 {

    private var POW_10 = Array<BigInt>(Q_MAXX) { BigInt.ONE }
    private var POW_5 = Array<BigInt>(K_MAXX) { BigInt.ONE }

    private fun calcPowTables() {
        for (i in 1..<Q_MAXX)
            POW_10[i] = POW_10[i - 1] * 10
        for (i in 1..<K_MAXX)
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
        val expectedResidue = Residue.residueFromRemainderDivisor(expectedRem, tenPowK)

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
        verify { digitCount in Q_MIN..<Q_MAXX }
        verify { pow10 in K_MIN..<K_MAXX }
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
        val mLimbs = te.M.magnitudeToLittleEndianLongArray()
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
        POW_5 = emptyArray()
        POW_10 = POW_5

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

    fun divModPow10(q: C256, r: C256, d: C256, pow10: Int) {
        throw RuntimeException("not impl")
    }

    fun rangeDivPow10(z: C256, x: C256, pow10: Int): Residue {
        verify { pow10 >= K_MIN }
        initialize()
        return _divPow10(z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10)
    }

    private fun _divPow10(
        z: C256, qDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long, kPow10: Int): Residue {
        require(qDigitCount in Q_MIN..<Q_MAXX)
        require(kPow10 in K_MIN..<K_MAXX)
        // clear coeff without worrying about aliasing
        // since we have passed in x3 x2 x1 x0
        z.c256EnableIndexSetAndZeroOut()

        val paramsIndex = OFFSETS[offsetIndex(qDigitCount, kPow10)].toInt()
        val descriptor = RANGE_RECIP_PARAMS[paramsIndex]
        verify { qDigitCount in unpackQMin(descriptor)..unpackQMax(descriptor) }
        verify { kPow10 == unpackK(descriptor) }
        val prodDwordLen = unpackProdDwordLen(descriptor)
        val mDwordCount = unpackMDwordLen(descriptor)
        val shift = unpackS(descriptor)
        val fractionBitLen = shift + 1

        val dividendShiftRight = kPow10 - 1
        verify { dividendShiftRight in 1..<64 }
        // We divide by 2**(kPow10-1) by shifting right.
        // This reduces the size of the dividend.
        // The lo bits that get shifted out are part of
        // the sticky bit residue calculation.
        val stickyBitsPow2 = x0 and ((1L shl dividendShiftRight) - 1)

        val dividendShiftLeft = 64 - dividendShiftRight
        val d0 = (x1 shl dividendShiftLeft) or (x0 ushr dividendShiftRight)
        val d1 = (x2 shl dividendShiftLeft) or (x1 ushr dividendShiftRight)
        val d2 = (x3 shl dividendShiftLeft) or (x2 ushr dividendShiftRight)
        val d3 = (x3 ushr dividendShiftRight)

        val residue = when {
            (d3 != 0L) ->
                c256RecipMul256(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d3, d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d2 != 0L) ->
                c256RecipMul192(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d2, d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d1 != 0L) ->
                c256RecipMul128(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d1, d0, fractionBitLen, stickyBitsPow2
                )

            (d0 != 0L) ->
                c256RecipMul64(
                    z, RANGE_RECIP_PARAMS, paramsIndex + 1, mDwordCount,
                    d0, fractionBitLen, stickyBitsPow2
                )

            else -> throw RuntimeException("why am I here?")
        }

        z.c256DisableIndexSetAndUpdateLengths()
        return residue
    }

}
