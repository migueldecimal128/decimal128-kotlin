package com.decimal128.decimal


internal fun c256RecipMul256(
    quotient: C256,
    m: LongArray, mOff: Int, mLen: Int,
    n3: Long, n2: Long, n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long
): Residue {
    val modulusBitLen = fractionBitLen and 0x3F
    val shiftRightNonZeroMask = -modulusBitLen.toLong() shr 63
    val shiftRight = modulusBitLen
    val shiftLeft = -shiftRight
    val halfUlpBitIndex = (fractionBitLen - 1) and 0x3F
    val halfUlpBitMask = 1L shl halfUlpBitIndex
    val fractionTailMask = halfUlpBitMask - 1
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
        val mX = m[mOff + mI]

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

internal fun c256RecipMul192(
    quotient: C256,
    m: LongArray, mOff: Int, mLen: Int,
    n2: Long, n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long
): Residue {
    val modulusBitLen = fractionBitLen and 0x3F
    val shiftRightNonZeroMask = -modulusBitLen.toLong() shr 63
    val shiftRight = modulusBitLen
    val shiftLeft = -shiftRight
    val halfUlpBitIndex = (fractionBitLen - 1) and 0x3F
    val halfUlpBitMask = 1L shl halfUlpBitIndex
    val fractionTailMask = halfUlpBitMask - 1
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
        val mX = m[mOff + mI]

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

internal fun c256RecipMul128(
    quotient: C256,
    m: LongArray, mOff: Int, mLen: Int,
    n1: Long, n0: Long, fractionBitLen: Int, stickyBitsPow2: Long
): Residue {
    val modulusBitLen = fractionBitLen and 0x3F
    val shiftRightNonZeroMask = -modulusBitLen.toLong() shr 63
    val shiftRight = modulusBitLen
    val shiftLeft = -shiftRight
    val halfUlpBitIndex = (fractionBitLen - 1) and 0x3F
    val halfUlpBitMask = 1L shl halfUlpBitIndex
    val fractionTailMask = halfUlpBitMask - 1
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
        val mX = m[mOff + mI]

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

internal fun c256RecipMul64(
    quotient: C256,
    m: LongArray, mOff: Int, mLen: Int,
    n0: Long, fractionBitLen: Int, stickyBitsPow2: Long
): Residue {
    val modulusBitLen = fractionBitLen and 0x3F
    val shiftRightNonZeroMask = -modulusBitLen.toLong() shr 63
    val shiftRight = modulusBitLen
    val shiftLeft = -shiftRight // jvm spec looks only at the bottom bits
    val halfUlpBitIndex = (fractionBitLen - 1) and 0x3F
    val halfUlpBitMask = 1L shl halfUlpBitIndex
    val fractionTailMask = halfUlpBitMask - 1
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
        val mX = m[mOff + mI]

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
