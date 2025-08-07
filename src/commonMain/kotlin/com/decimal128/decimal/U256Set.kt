package com.decimal128.decimal

@Suppress("NOTHING_TO_INLINE")
object U256Set {

    fun u256SetZero(u: U256) {
        u.dw3 = 0L; u.dw2 = 0L; u.dw1 = 0L; u.dw0 = 0L;
        u.bitLen = 0; u.digitLen = 0
    }

    fun u256SetOne(z: U256) {
        z.dw3 = 0L; z.dw2 = 0L; z.dw1 = 0L; z.dw0 = 1L;
        z.bitLen = 1; z.digitLen = 1
    }

    fun u256Set64(u: U256, d0: Long) {
        u.dw3 = 0L; u.dw2 = 0L; u.dw1 = 0L
        u.dw0 = d0
        u.bitLen = calcBitLen64(d0)
        u.digitLen = U256Pow10.calcDigitLen64(u.bitLen, d0)
    }

    fun u256Set128(u: U256, d1: Long, d0: Long) {
        u.dw3 = 0L; u.dw2 = 0L
        u.dw1 = d1; u.dw0 = d0
        u.bitLen = calcBitLen128(d1, d0)
        u.digitLen = U256Pow10.calcDigitLen128(u.bitLen, d1, d0)
    }

    fun u256Set192(u: U256, d2: Long, d1: Long, d0: Long) {
        u.dw3 = 0L
        u.dw2 = d2; u.dw1 = d1; u.dw0 = d0
        u.bitLen = calcBitLen192(d2, d1, d0)
        u.digitLen = U256Pow10.calcDigitLen192(u.bitLen, d2, d1, d0)
    }


    fun u256Set256(u: U256, d3: Long, d2: Long, d1: Long, d0: Long) {
        u.dw3 = d3; u.dw2 = d2; u.dw1 = d1; u.dw0 = d0
        u.bitLen = calcBitLen256(d3, d2, d1, d0)
        u.digitLen = U256Pow10.calcDigitLen256(u.bitLen, d3, d2, d1, d0)
    }

    fun u256Set(u: U256, x: U256) {
        u.dw3 = x.dw3; u.dw2 = x.dw2; u.dw1 = x.dw1; u.dw0 = x.dw0
        u.bitLen = x.bitLen; u.digitLen = x.digitLen
    }

    fun u256Set(u: U256, car: IntArray)  = u256Set(u, car, Car.nonZeroLimbLen(car))

    fun u256Set(u: U256, car: IntArray, carLen: Int) {
        val carSize = car.size

        val lo0 = if (carSize > 0) car[0].toLong() else 0L
        val hi0 = if (carSize > 1) car[1].toLong() else 0L
        val dw0 = (hi0 shl 32) or (lo0 and MASK32)
        if (carLen <= 2) {
            u.u256Set64(dw0)
            return
        }

        val lo1 = if (carSize > 2) car[2].toLong() else 0L
        val hi1 = if (carSize > 3) car[3].toLong() else 0L
        val dw1 = (hi1 shl 32) or (lo1 and MASK32)
        if (carLen <= 4) {
            u.u256Set128(dw1, dw0)
            return
        }

        val lo2 = if (carSize > 4) car[4].toLong() else 0L
        val hi2 = if (carSize > 5) car[5].toLong() else 0L
        val dw2 = (hi2 shl 32) or (lo2 and MASK32)
        if (carLen <= 6) {
            u.u256Set192(dw2, dw1, dw0)
            return
        }

        val lo3 = if (carSize > 6) car[6].toLong() else 0L
        val hi3 = if (carSize > 7) car[7].toLong() else 0L
        val dw3 = (hi3 shl 32) or (lo3 and MASK32)
        if (carLen <= 8) {
            u.u256Set256(dw3, dw2, dw1, dw0)
            return
        }

        throw RuntimeException("u256 overflow")
    }

    fun u256Set(u: U256, x: LongArray, xOff: Int, xLen: Int) {
        u.u256SetZero()
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
                u.u256Set64(c0)
            }

            1 -> {
                val c0 = x[xOff + 0]
                val c1 = nonZeroVal
                u.u256Set128(c1, c0)
            }

            2 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = nonZeroVal
                u.u256Set192(c2, c1, c0)
            }

            3 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = x[xOff + 2]
                val c3 = nonZeroVal
                u.u256Set256(c3, c2, c1, c0)
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

    inline fun u256SetShiftLeft(z: U256, x: U256, s: Int) = u256SetShiftLeftOr(z, x, s, 0L)

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

}
