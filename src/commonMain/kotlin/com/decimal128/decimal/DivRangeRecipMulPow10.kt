@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

// qMin = 8 bits
// qMax = 8 bits
// k = 8 bits
// maxProdDwordLen = 8 bits
// S = 16 bits
// mDwordLen = 16 bits

private fun unpackQMin(d: Long) = (d ushr 56).toInt()
private fun unpackQMax(d: Long) = (d ushr 48).toInt() and 0xFF
private fun unpackK(d: Long) = (d ushr 40).toInt() and 0xFF
private fun unpackProdDwordLen(d: Long) = (d ushr 32).toInt() and 0xFF
private fun unpackS(d: Long) = (d ushr 16).toInt() and 0xFFFF
private fun unpackMDwordLen(d: Long) = d.toInt() and 0xFFFF



// I tried setting this up to use triangle indexing
// it only saved 15% of the table size and required
// more calculation to find the offsetIndex, esp because
// of the upper triangle vs the lower rectangle

private fun offsetIndex(digitCount: Int, pow10: Int): Int {
    verify { digitCount in RRMP10_Q_MIN..<RRMP10_Q_MAXX }
    verify { pow10 in RRMP10_K_MIN..<RRMP10_K_MAXX }
    val index = ((digitCount - RRMP10_Q_MIN) shl RRMP10_LOOKUP_SHIFT) + (pow10 - RRMP10_K_MIN)
    return index
}

private fun paramsIndex(digitCount: Int, pow10: Int): Int {
    val offsetIndex = offsetIndex(digitCount, pow10)
    val encodedIndex =
        BYTE_TABLES[(RRMP10_LOOKUP_BASE + offsetIndex) and BYTE_TABLES_BCE].toInt() and 0xFF
    val baseMask = (RRMP10_ENCODE_BASE_INTERCEPT - offsetIndex) shr 31
    val block = (offsetIndex - (RRMP10_ENCODE_BASE_INTERCEPT - 128)) ushr 7
    val base = (block shl 6) - (block shl 3)  // base = block * 56
    val effectiveBase = base and baseMask
    return effectiveBase + encodedIndex
}

internal fun divRangeRecipMulPow10(z: C256, x: C256, pow10: Int, pentad: Pentad): Residue {
    verify { pow10 >= RRMP10_K_MIN }
    return _divPow10(z, x.digitLen, x.dw3, x.dw2, x.dw1, x.dw0, pow10, pentad)
}

private fun _divPow10(
    z: C256, qDigitCount: Int, x3: Long, x2: Long, x1: Long, x0: Long, kPow10: Int,
    pentad: Pentad
): Residue {
    require(qDigitCount in RRMP10_Q_MIN..<RRMP10_Q_MAXX)
    require(kPow10 in RRMP10_K_MIN..<RRMP10_K_MAXX)
    // clear coeff without worrying about aliasing
    // since we have passed in x3 x2 x1 x0
    z.c256EnableIndexSetAndZeroOut()

    val paramsIndex = paramsIndex(qDigitCount, kPow10) + RANGE_RECIP_MUL_PARAMS_BASE
    val descriptor = DWORD_TABLES[paramsIndex and DWORD_TABLES_BCE]
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
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask,
                pentad
                )

        (d2 != 0L) ->
            c256RecipMul192(
                z, paramsIndex + 1, mDwordCount,
                d2, d1, d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask,
                pentad
            )

        (d1 != 0L) ->
            c256RecipMul128(
                z, paramsIndex + 1, mDwordCount,
                d1, d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask,
                pentad
            )

        (d0 != 0L) ->
            c256RecipMul64(
                z, paramsIndex + 1, mDwordCount,
                d0,
                fractionBitLen, stickyBitsPow2,
                shiftRightNonZeroMask, shiftRight, shiftLeft, halfUlpBitMask, fractionTailMask,
                pentad
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
    pentad: Pentad
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
        val mX = DWORD_TABLES[(mOff + mI) and DWORD_TABLES_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        val pp21_0 = unsignedMulHi(mX, n2)
        val pp20_0 = mX * n2

        val pp31_0 = unsignedMulHi(mX, n3)
        val pp30_0 = mX * n3

        sumU64(pentad, pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        val carry_0 = pentad.dw1
        val z_0 = pentad.dw0
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
    sumU64(pentad, pp31_4, pp30_3, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_1)
    val carry1 = pentad.dw1
    val z1 = pentad.dw0
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
    sumU64(pentad, pp31_3, pp30_2, pp21_2, pp20_1, pp11_1, carry1)
    val carry2 = pentad.dw1
    val z2 = pentad.dw0
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
    sumU64(pentad, pp31_2, pp30_1, pp21_1, carry2)
    val carry3 = pentad.dw1
    val z3 = pentad.dw0
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
    pentad: Pentad
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
        val mX = DWORD_TABLES[(mOff + mI) and DWORD_TABLES_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        val pp21_0 = unsignedMulHi(mX, n2)
        val pp20_0 = mX * n2

        sumU64(pentad, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        val carry_0 = pentad.dw1
        val z_0 = pentad.dw0
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
    sumU64(pentad, pp21_3, pp20_2, pp11_2, pp10_1, pp01_1, carry_1)
    val carry1 = pentad.dw1
    val z1 = pentad.dw0
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
    sumU64(pentad, pp21_2, pp20_1, pp11_1, carry1)
    val carry2 = pentad.dw1
    val z2 = pentad.dw0
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
    pentad: Pentad
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
        val mX = DWORD_TABLES[(mOff + mI) and DWORD_TABLES_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        val pp11_0 = unsignedMulHi(mX, n1)
        val pp10_0 = mX * n1

        sumU64(pentad, pp11_2, pp10_1, pp01_1, pp00_0, carry_1)
        val carry_0 = pentad.dw1
        val z_0 = pentad.dw0
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
    sumU64(pentad, pp11_2, pp10_1, pp01_1, carry_1)
    val carry1 = pentad.dw1
    val z1 = pentad.dw0
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
    pentad: Pentad
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
        val mX = DWORD_TABLES[(mOff + mI) and DWORD_TABLES_BCE]

        val pp01_0 = unsignedMulHi(mX, n0)
        val pp00_0 = mX * n0

        sumU64(pentad, pp01_1, pp00_0, carry_1)
        val carry_0 = pentad.dw1
        val z_0 = pentad.dw0
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

