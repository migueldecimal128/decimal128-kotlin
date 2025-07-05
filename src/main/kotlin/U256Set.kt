package com.decimal128

object U256Set {

    fun u256Set(c: U256, x: LongArray, xOff: Int, xLen: Int) {
        c.u256SetZero()
        if (xLen == 0)
            return
        var nonZeroIndex = xLen
        var nonZeroVal = 0L
        while (nonZeroVal == 0L && --nonZeroIndex >= 0) {
            nonZeroVal = x[xOff + nonZeroIndex]
        }
        when (nonZeroIndex) {
            -1 -> {}
            0 -> {
                val c0 = nonZeroVal
                c.u256Set64(c0)
            }

            1 -> {
                val c0 = x[xOff + 0]
                val c1 = nonZeroVal
                c.u256Set128(c1, c0)
            }

            2 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = nonZeroVal
                c.u256Set192(c2, c1, c0)
            }

            3 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = x[xOff + 2]
                val c3 = nonZeroVal
                c.u256Set256(c3, c2, c1, c0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun u256Set(c: U256, x: IntArray, xLen: Int) {
        c.u256SetZero()
        if (xLen == 0)
            return
        var nonZeroIndex2 = (xLen + 1) ushr 1
        var nonZeroVal = if ((xLen and 1) != 0) (x[xLen - 1].toLong() and MASK32) else 0L
        while (nonZeroVal == 0L && --nonZeroIndex2 >= 0) {
            nonZeroVal = (x[nonZeroIndex2*2 + 1].toLong() shl 32) or (x[nonZeroIndex2*2].toLong() and MASK32)
        }
        when (nonZeroIndex2) {
            -1 -> {}
            0 -> {
                val c0 = nonZeroVal
                c.u256Set64(c0)
            }

            1 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32)
                val c1 = nonZeroVal
                c.u256Set128(c1, c0)
            }

            2 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32)
                val c1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32)
                val c2 = nonZeroVal
                c.u256Set192(c2, c1, c0)
            }

            3 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                val c1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                val c2 = (x[5].toLong() shl 32) or (x[4].toLong() and MASK32);
                val c3 = nonZeroVal;
                c.u256Set256(c3, c2, c1, c0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun u256SetShiftRight(z: U256, x: U256, bitShift: Int) {
        if (x.bitLen <= 64) {
            val le63Mask = if (bitShift <= 63) -1L else 0L
            val r = (x.dw0 ushr bitShift) and le63Mask
            z.u256Set64(r)
            return
        }
        val wholeDwordCount = bitShift ushr 6
        val innerShift = bitShift and 0x3F
        val nonZeroMask = -innerShift.toLong() shr 63
        when (wholeDwordCount) {
            0 -> {
                val z0 = (nonZeroMask and (x.dw1 shl -innerShift)) or (x.dw0 ushr innerShift)
                val z1 = (nonZeroMask and (x.dw2 shl -innerShift)) or (x.dw1 ushr innerShift)
                val z2 = (nonZeroMask and (x.dw3 shl -innerShift)) or (x.dw2 ushr innerShift)
                val z3 =                                            (x.dw3 ushr innerShift)
                z.u256Set256(z3, z2, z1, z0)
            }

            1 -> {
                val z0 = (nonZeroMask and (x.dw2 shl -innerShift)) or (x.dw1 ushr innerShift)
                val z1 = (nonZeroMask and (x.dw3 shl -innerShift)) or (x.dw2 ushr innerShift)
                val z2 = x.dw3 ushr innerShift
                z.u256Set192(z2, z1, z0)
            }

            2 -> {
                val z0 = (nonZeroMask and (x.dw3 shl -innerShift)) or (x.dw2 ushr innerShift)
                val z1 = x.dw3 ushr innerShift
                z.u256Set128(z1, z0)
            }

            3 -> {
                val z0 = x.dw3 ushr innerShift
                z.u256Set64(z0)
            }

            else -> z.u256SetZero()
        }
    }

    fun u256SetShiftLeftOr(z: U256, x: U256, s: Int, d0: Long) {
        val wholeDwordCount = s ushr 6
        val innerL = s and 0x3F
        val nonZeroMask = -innerL.toLong() shr 63
        val innerR = -innerL
        val topBitsMask = nonZeroMask shl innerR
        //FIXME need to check for non-zero bit overflow out the top
        var z3 = 0L
        var z2 = 0L
        var z1 = 0L
        var z0 = d0
        coefficient_overflow@
        do {
            when (wholeDwordCount) {
                0 -> {
                    if (x.dw3 and topBitsMask != 0L)
                        break@coefficient_overflow
                    z3 = (x.dw3 shl innerL) or ((x.dw2 ushr innerR) and nonZeroMask)
                    z2 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                    z1 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                    z0 = (x.dw0 shl innerL) or d0
                }

                1 -> {
                    if ((x.dw3 or (x.dw2 and topBitsMask)) != 0L)
                        break@coefficient_overflow
                    z3 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                    z2 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                    z1 = (x.dw0 shl innerL)
                }

                2 -> {
                    if ((x.dw3 or x.dw2 or (x.dw1 and topBitsMask)) != 0L)
                        break@coefficient_overflow
                    z3 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                    z2 = (x.dw0 shl innerL)
                }

                3 -> {
                    if ((x.dw3 or x.dw2 or x.dw1 or (x.dw0 and topBitsMask)) != 0L)
                        break@coefficient_overflow
                    z3 = (x.dw0 shl innerL)
                }

                else -> {
                    if (x.bitLen > 0)
                        break@coefficient_overflow
                }
            }
            z.u256Set256(z3, z2, z1, z0)
            return
        } while (false)
        throw RuntimeException("coefficientOverflow")
    }

    fun u256SetShiftRight(z: U256, x: LongArray, xOff: Int, xLen: Int, bitCount: Int) {

        z.u256SetZero()
        // strip leading zeros from x
        var nonZeroLen = xLen
        while (nonZeroLen > 0 && x[xOff + nonZeroLen - 1] == 0L)
            --nonZeroLen

        val dwordShift = bitCount ushr 6
        val innerShift = bitCount and 0x3F
        val innerShiftNonZeroMask = -innerShift.toLong() shr 63
        val newLen = nonZeroLen - dwordShift
        val shiftOff = xOff + dwordShift
        when (newLen) {
            0 -> {}
            1 -> {
                val z0 = x[shiftOff + 0] ushr innerShift
                z.u256Set64(z0)
            }

            2 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl -innerShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = x[shiftOff + 1] ushr innerShift
                z.u256Set128(z1, z0)
            }

            3 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl -innerShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl -innerShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = x[shiftOff + 2] ushr innerShift
                z.u256Set192(z2, z1, z0)
            }

            4 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl -innerShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl -innerShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl -innerShift)) or (x[shiftOff + 2] ushr innerShift)
                val z3 = x[shiftOff + 3] ushr innerShift
                z.u256Set256(z3, z2, z1, z0)
            }

            5 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl -innerShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl -innerShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl -innerShift)) or (x[shiftOff + 2] ushr innerShift)
                val z3 = (innerShiftNonZeroMask and (x[shiftOff + 4] shl -innerShift)) or (x[shiftOff + 3] ushr innerShift)
                val z4 = x[shiftOff + 4] ushr innerShift
                if (z4 != 0L)
                    throw RuntimeException("overflow")
                z.u256Set256(z3, z2, z1, z0)
            }

            else -> {
                throw RuntimeException("overflow")
            }
        }
    }

    fun u256SetShiftRight(z: U256, x: IntArray, xLen: Int, s: Int) {
        assert(s < 32)
        if (s == 0) {
            u256Set(z, x, xLen)
            return
        }
        z.u256SetZero()
        if (xLen == 0)
            return
        var nonZeroIndex2 = (xLen + 1) ushr 1
        var nonZeroVal = if ((xLen and 1) != 0) ((x[xLen - 1].toLong() and MASK32) shr s) else 0L
        while (nonZeroVal == 0L && --nonZeroIndex2 >= 0) {
            nonZeroVal = ((x[nonZeroIndex2*2 + 1].toLong() shl 32) or (x[nonZeroIndex2*2].toLong() and MASK32)) shr s
        }
        when (nonZeroIndex2) {
            -1 -> {}
            0 -> {
                val z0 = nonZeroVal
                z.u256Set64(z0)
            }

            1 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = nonZeroVal
                z.u256Set128(z1, z0)
            }

            2 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                val z2 = nonZeroVal
                z.u256Set192(z2, z1, z0)
            }

            3 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                val z2 = ((x[6] shl -s).toLong() shl 32) or (((x[5].toLong() shl 32) or (x[4].toLong() and MASK32)) shr s)
                val z3 = nonZeroVal;
                z.u256Set256(z3, z2, z1, z0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

}
