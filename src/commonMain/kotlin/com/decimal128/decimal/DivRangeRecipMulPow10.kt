@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import kotlin.math.min

const val Q_MIN = POW10_64_COUNT
const val Q_MAXX = 79 // exclusive
const val K_MIN = BARRETT_POW10_MAXX
const val K_MAXX = Q_MAXX - 34

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

private var recipTable: Array<Array<TableEntry>> = Array(Q_MAXX) { Array<TableEntry>(K_MAXX) { NULL_TABLE_ENTRY } }

private fun populateTable() {
    for (j in Q_MIN..<Q_MAXX) {
        var prev = recipTable[j - 1][K_MIN]
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
    for (k in K_MIN..<K_MAXX) {
        var tePrev = recipTable[Q_MAXX - 1][k]
        for (q in Q_MAXX - 2 downTo Q_MIN) {
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

private fun paramsIndex(digitCount: Int, pow10: Int): Int {
    return OFFSETS[offsetIndex(digitCount, pow10)].toInt()
}

private fun storeParamsIndex(digitCount: Int, pow10: Int, paramsIndex: Int) {
    val offsetIndex = offsetIndex(digitCount, pow10)
    OFFSETS[offsetIndex] = paramsIndex.toShort()
}

private var iRRP = 1
private val RANGE_RECIP_PARAMS = LongArray(709)

private fun serializeTable() {
    for (q in Q_MIN..<Q_MAXX) {
        for (k in K_MIN..<min(q, K_MAXX)) {
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
        POW10[RANGE_RECIP_PARAMS_BASE + i] = RANGE_RECIP_PARAMS[i]

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

    println("kilroy was here!")
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

private fun releaseTemporaryStorage() {
    POW_5 = emptyArray()
    POW_10 = POW_5

    recipTable = emptyArray()
}

private var initialized = false
private fun initialize() {
    if (!initialized) {
        calcPowTables()
        populateTable()
        tableMerge()
        serializeTable()
        releaseTemporaryStorage()
        initialized = true
    }
}

internal fun divRangeRecipMulPow10(z: C256, x: C256, pow10: Int): Residue {
    verify { pow10 >= K_MIN }
    initialize()
    return _divPow10(z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10)
}

private fun _divPow10(
    z: C256, qDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long, kPow10: Int
): Residue {
    require(qDigitCount in Q_MIN..<Q_MAXX)
    require(kPow10 in K_MIN..<K_MAXX)
    // clear coeff without worrying about aliasing
    // since we have passed in x3 x2 x1 x0
    z.c256EnableIndexSetAndZeroOut()

    val paramsIndex = paramsIndex(qDigitCount, kPow10) + RANGE_RECIP_PARAMS_BASE
    val descriptor = POW10[paramsIndex and POW10_BCE]
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

    val modulusBitLen = fractionBitLen and 0x3F
    val shiftRightNonZeroMask = -modulusBitLen.toLong() shr 63
    val shiftRight = modulusBitLen
    val shiftLeft = -shiftRight
    val halfUlpBitIndex = (fractionBitLen - 1) and 0x3F
    val halfUlpBitMask = 1L shl halfUlpBitIndex
    val fractionTailMask = halfUlpBitMask - 1

    val residue = when {
        (d3 != 0L) ->
            c256RecipMul256(
                z, paramsIndex + 1, mDwordCount,
                d3, d2, d1, d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask
                )

        (d2 != 0L) ->
            c256RecipMul192(
                z, paramsIndex + 1, mDwordCount,
                d2, d1, d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask
            )

        (d1 != 0L) ->
            c256RecipMul128(
                z, paramsIndex + 1, mDwordCount,
                d1, d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask
            )

        (d0 != 0L) ->
            c256RecipMul64(
                z, paramsIndex + 1, mDwordCount,
                d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask
            )

        else -> throw RuntimeException("why am I here?")
    }

    z.c256DisableIndexSetAndUpdateLengths()
    return residue
}


private inline fun c256RecipMul256(
    quotient: C256,
    mOff: Int, mLen: Int,
    n3: Long, n2: Long, n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long,

    shiftRightNonZeroMask: Long,
    shiftRight: Int,        // == modulusBitLen
    shiftLeft: Int,
    halfUlpBitMask: Long,
    fractionTailMask: Long,
): Residue {
    var isolatedRoundBit = 0L
    var quotientIndex = 0


    var fractionBitsRemaining = fractionBitLen
    var stickyBitsFracCompare = 0
    verify { (fractionBitLen + 63) ushr 6 >= mLen }

    var z_1 = 0L

    var pp31_4 = 0L
    var pp31_3 = 0L
    var pp31_2 = 0L
    var pp31_1 = 0L

    var pp30_3 = 0L
    var pp30_2 = 0L
    var pp30_1 = 0L

    var pp21_3 = 0L
    var pp21_2 = 0L
    var pp21_1 = 0L

    var pp20_2 = 0L
    var pp20_1 = 0L

    var pp11_2 = 0L
    var pp11_1 = 0L

    var pp10_1 = 0L

    var pp01_1 = 0L

    var carry_1 = 0L
    var mI = 0
    while (mI < mLen) {
        val mX = POW10[(mOff + mI) and POW10_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        val pp21_0 = unsignedMulHi(mX, n2)
        val pp20_0 = mX * n2

        val pp31_0 = unsignedMulHi(mX, n3)
        val pp30_0 = mX * n3

        val (carry_0, z_0) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        if (fractionBitsRemaining > 0) {
            fractionBitsRemaining -= 64
            val mask =
                if (fractionBitsRemaining <= 0) {
                    isolatedRoundBit = z_0 and halfUlpBitMask
                    fractionTailMask
                } else {
                    -1L
                }
            if (mask != 0L) {
                val cmp = unsignedCmp((z_0 and mask), mX)
                stickyBitsFracCompare = if (cmp != 0) cmp else stickyBitsFracCompare
            }
        } else {
            val q0 = (z_0 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
            quotient[quotientIndex++] = q0
        }

        z_1 = z_0

        pp31_4 = pp31_3
        pp31_3 = pp31_2
        pp31_2 = pp31_1
        pp31_1 = pp31_0

        pp30_3 = pp30_2
        pp30_2 = pp30_1
        pp30_1 = pp30_0

        pp21_3 = pp21_2
        pp21_2 = pp21_1
        pp21_1 = pp21_0

        pp20_2 = pp20_1
        pp20_1 = pp20_0

        pp11_2 = pp11_1
        pp11_1 = pp11_0

        pp10_1 = pp10_0

        pp01_1 = pp01_0

        carry_1 = carry_0
        ++mI
    }
    val (carry1, z1) = sumU64(pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_1)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z1 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z1 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z1 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    val (carry2, z2) = sumU64(pp31_3, pp30_2, pp21_2, pp20_1, pp11_1, carry1)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z2 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z2 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z2 shl shiftLeft) or ((z1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    val (carry3, z3) = sumU64(pp31_2, pp30_1, pp21_1, carry2)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z3 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z3 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z3 shl shiftLeft) or ((z2 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    //val (carry4, z4) = sumU64(pp31_1, carry3)
    //require(carry4 == 0L)
    val z4 = pp31_1 + carry3
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z4 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z4 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z4 shl shiftLeft) or ((z3 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }

    verify { fractionBitsRemaining <= 0 }
    val q4 = ((z4 ushr shiftRight) and shiftRightNonZeroMask)
    if (q4 != 0L)
        quotient[quotientIndex] = q4

    val residue = Residue.fromRoundBitStickyBitsStickyBits(isolatedRoundBit, stickyBitsFracCompare, stickyBitsPow2)
    return residue
}

private inline fun c256RecipMul192(
    quotient: C256,
    mOff: Int, mLen: Int,
    n2: Long, n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long,

    shiftRightNonZeroMask: Long,
    shiftRight: Int,        // == modulusBitLen
    shiftLeft: Int,
    halfUlpBitMask: Long,
    fractionTailMask: Long,
): Residue {
    var isolatedRoundBit = 0L
    var quotientIndex = 0


    var fractionBitsRemaining = fractionBitLen
    var stickyBitsFracCompare = 0
    verify { (fractionBitLen + 63) ushr 6 >= mLen }

    var z_1 = 0L

    var pp21_3 = 0L
    var pp21_2 = 0L
    var pp21_1 = 0L

    var pp20_2 = 0L
    var pp20_1 = 0L

    var pp11_2 = 0L
    var pp11_1 = 0L

    var pp10_1 = 0L

    var pp01_1 = 0L

    var carry_1 = 0L
    var mI = 0
    while (mI < mLen) {
        val mX = POW10[(mOff + mI) and POW10_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        val pp21_0 = unsignedMulHi(mX, n2)
        val pp20_0 = mX * n2

        val (carry_0, z_0) = sumU64(pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        if (fractionBitsRemaining > 0) {
            fractionBitsRemaining -= 64
            val mask =
                if (fractionBitsRemaining <= 0) {
                    isolatedRoundBit = z_0 and halfUlpBitMask
                    fractionTailMask
                } else {
                    -1L
                }
            if (mask != 0L) {
                val cmp = unsignedCmp((z_0 and mask), mX)
                stickyBitsFracCompare = if (cmp != 0) cmp else stickyBitsFracCompare
            }
        } else {
            val q0 = (z_0 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
            quotient[quotientIndex++] = q0
        }

        z_1 = z_0

        pp21_3 = pp21_2
        pp21_2 = pp21_1
        pp21_1 = pp21_0

        pp20_2 = pp20_1
        pp20_1 = pp20_0

        pp11_2 = pp11_1
        pp11_1 = pp11_0

        pp10_1 = pp10_0

        pp01_1 = pp01_0

        carry_1 = carry_0
        ++mI
    }
    val (carry1, z1) = sumU64(pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_1)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z1 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z1 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z1 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    val (carry2, z2) = sumU64(pp21_2, pp20_1, pp11_1, carry1)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z2 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z2 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z2 shl shiftLeft) or ((z1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    val z3 = pp21_1 + carry2
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z3 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z3 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z3 shl shiftLeft) or ((z2 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    verify { fractionBitsRemaining <= 0 }
    val q3 = ((z3 ushr shiftRight) and shiftRightNonZeroMask)
    if (q3 != 0L)
        quotient[quotientIndex] = q3

    val residue = Residue.fromRoundBitStickyBitsStickyBits(isolatedRoundBit, stickyBitsFracCompare, stickyBitsPow2)
    return residue
}

private inline fun c256RecipMul128(
    quotient: C256,
    mOff: Int, mLen: Int,
    n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long,

    shiftRightNonZeroMask: Long,
    shiftRight: Int,        // == modulusBitLen
    shiftLeft: Int,
    halfUlpBitMask: Long,
    fractionTailMask: Long,
): Residue {
    var isolatedRoundBit = 0L
    var quotientIndex = 0


    var fractionBitsRemaining = fractionBitLen
    var stickyBitsFracCompare = 0
    verify { (fractionBitLen + 63) ushr 6 >= mLen }

    var z_1 = 0L

    var pp11_2 = 0L
    var pp11_1 = 0L

    var pp10_1 = 0L

    var pp01_1 = 0L

    var carry_1 = 0L
    var mI = 0
    while (mI < mLen) {
        val mX = POW10[(mOff + mI) and POW10_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        val (carry_0, z_0) = sumU64(pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        if (fractionBitsRemaining > 0) {
            fractionBitsRemaining -= 64
            val mask =
                if (fractionBitsRemaining <= 0) {
                    isolatedRoundBit = z_0 and halfUlpBitMask
                    fractionTailMask
                } else {
                    -1L
                }
            if (mask != 0L) {
                val cmp = unsignedCmp((z_0 and mask), mX)
                stickyBitsFracCompare = if (cmp != 0) cmp else stickyBitsFracCompare
            }
        } else {
            val q0 = (z_0 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
            quotient[quotientIndex++] = q0
        }

        z_1 = z_0

        pp11_2 = pp11_1
        pp11_1 = pp11_0

        pp10_1 = pp10_0

        pp01_1 = pp01_0

        carry_1 = carry_0
        ++mI
    }
    val (carry1, z1) = sumU64(pp11_2, pp10_1, pp01_1, carry_1)
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z1 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z1 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z1 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    val z2 = pp11_1 + carry1
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z2 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z2 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z2 shl shiftLeft) or ((z1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    verify { fractionBitsRemaining <= 0 }
    val q2 = ((z2 ushr shiftRight) and shiftRightNonZeroMask)
    if (q2 != 0L)
        quotient[quotientIndex] = q2

    val residue = Residue.fromRoundBitStickyBitsStickyBits(isolatedRoundBit, stickyBitsFracCompare, stickyBitsPow2)
    return residue
}

private inline fun c256RecipMul64(
    quotient: C256,
    mOff: Int, mLen: Int,
    n0: Long, fractionBitLen: Int, stickyBitsPow2: Long,

    shiftRightNonZeroMask: Long,
    shiftRight: Int,        // == modulusBitLen
    shiftLeft: Int,
    halfUlpBitMask: Long,
    fractionTailMask: Long,
): Residue {
    var isolatedRoundBit = 0L
    var quotientIndex = 0


    var fractionBitsRemaining = fractionBitLen
    var stickyBitsFracCompare = 0
    verify { (fractionBitLen + 63) ushr 6 >= mLen }

    var z_1 = 0L

    var pp01_1 = 0L

    var carry_1 = 0L
    var mI = 0
    while (mI < mLen) {
        val mX = POW10[(mOff + mI) and POW10_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val (carry_0, z_0) = sumU64(pp01_1, pp00_0, carry_1)
        if (fractionBitsRemaining > 0) {
            fractionBitsRemaining -= 64
            val mask =
                if (fractionBitsRemaining <= 0) {
                    isolatedRoundBit = z_0 and halfUlpBitMask
                    fractionTailMask
                } else {
                    -1L
                }
            if (mask != 0L) {
                val cmp = unsignedCmp((z_0 and mask), mX)
                stickyBitsFracCompare = if (cmp != 0) cmp else stickyBitsFracCompare
            }
        } else {
            val q0 = (z_0 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
            quotient[quotientIndex++] = q0
        }

        z_1 = z_0

        pp01_1 = pp01_0

        carry_1 = carry_0
        ++mI
    }
    val z1 = pp01_1 + carry_1
    if (fractionBitsRemaining > 0) {
        fractionBitsRemaining -= 64
        val mask =
            if (fractionBitsRemaining <= 0) {
                isolatedRoundBit = z1 and halfUlpBitMask
                fractionTailMask
            } else {
                -1L
            }
        if (mask != 0L) {
            stickyBitsFracCompare = if ((z1 and mask) != 0L) 1 else stickyBitsFracCompare
        }
    } else {
        val q0 = (z1 shl shiftLeft) or ((z_1 ushr shiftRight) and shiftRightNonZeroMask)
        quotient[quotientIndex++] = q0
    }
    verify { fractionBitsRemaining <= 0 }
    val q1 = ((z1 ushr shiftRight) and shiftRightNonZeroMask)
    if (q1 != 0L)
        quotient[quotientIndex] = q1

    val residue = Residue.fromRoundBitStickyBitsStickyBits(isolatedRoundBit, stickyBitsFracCompare, stickyBitsPow2)
    return residue
}

