package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.pow10BitLen
import com.decimal128.decimal.U256Pow10.pow10Offset
import com.decimal128.decimal.U256Pow10.POW10

object U256Compare {

    fun u256IsZero(x: C256): Boolean {
        return x.bitLen == 0
    }

    fun u256IsNotZero(x: C256): Boolean {
        return x.bitLen != 0
    }

    fun u256IsOne(x: C256): Boolean {
        return x.bitLen == 1
    }

    fun u256UnscaledCompare(x:C256, y:C256) : Int {
        if (x.bitLen != y.bitLen)
            return x.bitLen.compareTo(y.bitLen)
        val cmp0 = unsignedCmp(x.dw0, y.dw0)
        val cmp1 = unsignedCmp(x.dw1, y.dw1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        if (x.bitLen <= 128)
            return cmp10
        val cmp2 = unsignedCmp(x.dw2, y.dw2)
        val cmp3 = unsignedCmp(x.dw3, y.dw3)
        val cmp32 = if (cmp3 != 0) cmp3 else cmp2
        val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
        return cmp3210
    }

    fun u256UnscaledCompare(x: C256, y: IntArray): Int {
        require(y.size >= 8)
        val y3 = (y[7].toLong() shl 32) or (y[6].toLong() and MASK32)
        if (x.dw3 != y3)
            return unsignedCmp(x.dw3, y3)
        val y2 = (y[5].toLong() shl 32) or (y[4].toLong() and MASK32)
        if (x.dw2 != y2)
            return unsignedCmp(x.dw2, y2)
        val y1 = (y[3].toLong() shl 32) or (y[2].toLong() and MASK32)
        if (x.dw1 != y1)
            return unsignedCmp(x.dw1, y1)
        val y0 = (y[1].toLong() shl 32) or (y[0].toLong() and MASK32)
        return unsignedCmp(x.dw0, y0)
    }

    fun u256UnscaledEQ(x:C256, y:C256) : Boolean {
        return ((x.bitLen - y.bitLen).toLong() or
                (x.dw0 - y.dw0) or (x.dw1 - y.dw1) or
                (x.dw2 - y.dw2) or (x.dw3 - y.dw3)) == 0L
    }

    fun u256GTOne(x: C256) = x.bitLen > 1

    fun u256ScaledCompare(x:C256, y:C256, pow10Delta: Int) : Int {
        val pow10BitLen = pow10BitLen(pow10Delta)
        val yBitLen = y.bitLen
        require(yBitLen <= 128)
        val xBitLen = x.bitLen
        val minYBitLen = yBitLen + pow10BitLen - 1
        val maxYBitLen = yBitLen + pow10BitLen(pow10Delta + 1)
        if (xBitLen < minYBitLen)
            return -1
        if (xBitLen > maxYBitLen)
            return 1
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val pow10Offset = pow10Offset(pow10Delta)
        val p0 = POW10[(pow10Offset + 0) and 0x3F]
        val p1 = POW10[(pow10Offset + 1) and 0x3F]
        if (xBitLen <= 128) {
            val ret = when {
                yBitLen <= 64 && pow10BitLen <= 64 ->
                    _cmp128x64x64(x1, x0, y0, p0)
                yBitLen <= 64 && pow10BitLen <= 128 ->
                    _cmp128x128x64(x1, x0, p1, p0, y0)
                pow10BitLen <= 64 ->
                    _cmp128x128x64(x1, x0, y1, y0, p0)
                else -> throw RuntimeException()
            }
            return ret
        }
        val x2 = x.dw2
        val p2 = POW10[(pow10Offset + 2) and 0x3F]
        if (x.bitLen <= 192) {
            return when {
                yBitLen <= 64 -> {
                    // Small y: compute pow10 × y
                    when {
                        pow10BitLen <= 64 -> throw IllegalStateException()
                        pow10BitLen <= 128 ->
                            _cmp192x128x64(x2, x1, x0, p1, p0, y0)  // 128×64
                        else -> {
                            check(pow10BitLen <= 192)
                            _cmp192x192x64(x2, x1, x0, p2, p1, p0, y0)  // 192×64
                        }
                    }
                }
                else -> {
                    // Large y: compute y × pow10
                    when {
                        pow10BitLen <= 64 ->
                            _cmp192x128x64(x2, x1, x0, y1, y0, p0)  // 128×64
                        pow10BitLen <= 128 ->
                            _cmp256x128x128(0L, x2, x1, x0, y1, y0, p1, p0)  // 128×128
                        else -> {
                            check(pow10BitLen <= 192)
                            _cmp256x192x128(0L, x2, x1, x0, p2, p1, p0, y1, y0)  // 128×192
                        }
                    }
                }
            }
        }
        val x3 = x.dw3
        val p3 = POW10[(pow10Offset + 3) and 0x3F]
        if (x.bitLen <= 256) {
            check(pow10BitLen > 64)
            val ret = when {
                pow10BitLen <= 128 ->
                    _cmp256x128x128(x3, x2, x1, x0, y1, y0, p1, p0)
                pow10BitLen <= 192 ->
                    _cmp256x192x128(x3, x2, x1, x0, p2, p1, p0, y1, y0)
                else -> { // pow10BitLen <= 256
                    check(yBitLen <= 64)
                    _cmp256x256x64(x3, x2, x1, x0, p3, p2, p1, p0, y0)
                }
            }
            return ret
        }
        throw RuntimeException()
    }

    private fun _cmp128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Int {
        val p1 = unsignedMulHi(y0, pow10)
        val p0 = y0 * pow10

        val cmp1 = unsignedCmp(x1, p1)
        val cmp0 = unsignedCmp(x0, p0)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        return cmp10
    }

    private fun _cmp128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Int {
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = unsignedCmp(x1, p1)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        val p2 = carry1 + pp10Hi
        val cmp210 = if (p2 != 0L) -1 else cmp10
        return cmp210
    }

    private fun _cmp192x128x64(
        x2: Long, x1: Long, x0: Long,  // 192-bit x (3 limbs)
        m1: Long, m0: Long,             // 128-bit m (2 limbs)
        n0: Long                        // 64-bit n (1 limb)
    ) : Int {
        // Compute m × n (128 × 64 = 192 bits max)

        // Limb 0
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        // Limb 1
        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = unsignedCmp(x1, p1)

        // Limb 2
        val p2 = carry1 + pp10Hi
        val cmp2 = unsignedCmp(x2, p2)

        // Compare from high to low
        return when {
            cmp2 != 0 -> cmp2        // limb 2 differs
            cmp1 != 0 -> cmp1        // limb 1 differs
            else -> cmp0              // check limb 0
        }
    }

    private fun _cmp192x192x64(
        x2: Long, x1: Long, x0: Long,  // 192-bit x (3 limbs)
        m2: Long, m1: Long, m0: Long,  // 192-bit m (3 limbs)
        n0: Long                        // 64-bit n (1 limb)
    ) : Int {
        // Compute m × n (192 × 64 = 256 bits max)

        // Limb 0: m0 × n0
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        // Limb 1: m1 × n0 + carry
        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = unsignedCmp(x1, p1)

        // Limb 2: m2 × n0 + carry
        val pp20Hi = unsignedMulHi(m2, n0)
        val pp20Lo = m2 * n0
        val (carry2, p2) = sumU64(pp10Hi, pp20Lo, carry1)
        val cmp2 = unsignedCmp(x2, p2)

        // Limb 3 (overflow check): final carry
        val p3 = carry2 + pp20Hi

        // Compare from high to low
        return when {
            p3 != 0L -> -1           // product > x (overflow beyond 192 bits)
            cmp2 != 0 -> cmp2        // limb 2 differs
            cmp1 != 0 -> cmp1        // limb 1 differs
            else -> cmp0              // check limb 0
        }
    }

    private fun _cmp256x128x128(
        x3: Long, x2: Long, x1: Long, x0: Long,  // 256-bit x (4 limbs)
        m1: Long, m0: Long,                       // 128-bit m (2 limbs)
        n1: Long, n0: Long                        // 128-bit n (2 limbs)
    ) : Int {
        // Compute m × n (128 × 128 = 256 bits)

        // Limb 0: m0 × n0
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        // Limb 1: m1 × n0 + m0 × n1 + carry
        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val pp01Hi = unsignedMulHi(m0, n1)
        val pp01Lo = m0 * n1
        val (carry1a, sum1a) = sumU64(pp00Hi, pp10Lo)
        val (carry1b, p1) = sumU64(sum1a, pp01Lo)
        val carry1 = carry1a + carry1b
        val cmp1 = unsignedCmp(x1, p1)

        // Limb 2: m1 × n1 + carries from limb 1
        val pp11Hi = unsignedMulHi(m1, n1)
        val pp11Lo = m1 * n1
        val (carry2a, sum2a) = sumU64(pp10Hi, pp01Hi, carry1)
        val (carry2b, p2) = sumU64(sum2a, pp11Lo)
        val carry2 = carry2a + carry2b
        val cmp2 = unsignedCmp(x2, p2)

        // Limb 3: final carry + high part of m1 × n1
        val p3 = carry2 + pp11Hi
        val cmp3 = unsignedCmp(x3, p3)

        // Compare from high to low
        return when {
            cmp3 != 0 -> cmp3        // limb 3 differs
            cmp2 != 0 -> cmp2        // limb 2 differs
            cmp1 != 0 -> cmp1        // limb 1 differs
            else -> cmp0              // check limb 0
        }
    }

    private fun _cmp256x192x128(
        x3: Long, x2: Long, x1: Long, x0: Long,  // 256-bit x (4 limbs)
        m2: Long, m1: Long, m0: Long,             // 192-bit m (3 limbs)
        n1: Long, n0: Long                        // 128-bit n (2 limbs)
    ) : Int {
        // Compute m × n (192 × 128 = 320 bits max, but we only compare 256 bits)

        // Limb 0: m0 × n0
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        // Limb 1: m1 × n0 + m0 × n1 + carry
        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val pp01Hi = unsignedMulHi(m0, n1)
        val pp01Lo = m0 * n1
        val (carry1a, sum1a) = sumU64(pp00Hi, pp10Lo)
        val (carry1b, p1) = sumU64(sum1a, pp01Lo)
        val carry1 = carry1a + carry1b
        val cmp1 = unsignedCmp(x1, p1)

        // Limb 2: m2 × n0 + m1 × n1 + m0 × n2(=0) + carries
        val pp20Hi = unsignedMulHi(m2, n0)
        val pp20Lo = m2 * n0
        val pp11Hi = unsignedMulHi(m1, n1)
        val pp11Lo = m1 * n1
        val (carry2a, sum2a) = sumU64(pp10Hi, pp01Hi, carry1)
        val (carry2b, sum2b) = sumU64(sum2a, pp20Lo)
        val (carry2c, p2) = sumU64(sum2b, pp11Lo)
        val carry2 = carry2a + carry2b + carry2c
        val cmp2 = unsignedCmp(x2, p2)

        // Limb 3: m2 × n1 + carries from limb 2
        val pp21Hi = unsignedMulHi(m2, n1)
        val pp21Lo = m2 * n1
        val (carry3a, sum3a) = sumU64(pp20Hi, pp11Hi, carry2)
        val (carry3b, p3) = sumU64(sum3a, pp21Lo)
        val carry3 = carry3a + carry3b
        val cmp3 = unsignedCmp(x3, p3)

        // Limb 4 (overflow check): final carry + pp21Hi
        val p4 = carry3 + pp21Hi

        // Compare from high to low
        return when {
            p4 != 0L -> -1           // product > x (overflow beyond 256 bits)
            cmp3 != 0 -> cmp3        // limb 3 differs
            cmp2 != 0 -> cmp2        // limb 2 differs
            cmp1 != 0 -> cmp1        // limb 1 differs
            else -> cmp0              // check limb 0
        }
    }

    private fun _cmp256x256x64(
        x3: Long, x2: Long, x1: Long, x0: Long,  // 256-bit x (4 limbs)
        m3: Long, m2: Long, m1: Long, m0: Long,  // 256-bit m (4 limbs)
        n0: Long                                  // 64-bit n (1 limb)
    ) : Int {
        // Compute m × n (256 × 64 = 320 bits max)

        // Limb 0: m0 × n0
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo
        val cmp0 = unsignedCmp(x0, p0)

        // Limb 1: m1 × n0 + carry
        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
        val cmp1 = unsignedCmp(x1, p1)

        // Limb 2: m2 × n0 + carry
        val pp20Hi = unsignedMulHi(m2, n0)
        val pp20Lo = m2 * n0
        val (carry2, p2) = sumU64(pp10Hi, pp20Lo, carry1)
        val cmp2 = unsignedCmp(x2, p2)

        // Limb 3: m3 × n0 + carry
        val pp30Hi = unsignedMulHi(m3, n0)
        val pp30Lo = m3 * n0
        val (carry3, p3) = sumU64(pp20Hi, pp30Lo, carry2)
        val cmp3 = unsignedCmp(x3, p3)

        // Limb 4 (overflow check): final carry
        val p4 = carry3 + pp30Hi

        // Compare from high to low
        return when {
            p4 != 0L -> -1           // product > x (overflow beyond 256 bits)
            cmp3 != 0 -> cmp3        // limb 3 differs
            cmp2 != 0 -> cmp2        // limb 2 differs
            cmp1 != 0 -> cmp1        // limb 1 differs
            else -> cmp0              // check limb 0
        }
    }

    fun u256ScaledEQ(x:C256, y:C256, pow10Delta: Int) : Boolean {
        val pow10BitLen = pow10BitLen(pow10Delta)
        val minYBitLen = y.bitLen + pow10BitLen - 1
        val maxYBitLen = y.bitLen + pow10BitLen(pow10Delta + 1)
        if (x.bitLen < minYBitLen || x.bitLen > maxYBitLen)
            return false
        val x0 = x.dw0
        val x1 = x.dw1
        val y0 = y.dw0
        val y1 = y.dw1
        val pow10Offset = pow10Offset(pow10Delta)
        val pow10dw0 = POW10[(pow10Offset + 0) and 0x3F]
        val pow10dw1 = POW10[(pow10Offset + 1) and 0x3F]
        if (x.bitLen <= 128) {
            val ret = when {
                y.bitLen <= 64 && pow10BitLen <= 64 ->
                    _EQ128x64x64(x1, x0, y0, pow10dw0)
                y.bitLen <= 64 && pow10BitLen <= 128 ->
                    _EQ128x128x64(x1, x0, pow10dw1, pow10dw0, y0)
                y.bitLen <= 128 && pow10BitLen <= 64 ->
                    _EQ128x128x64(x1, x0, y1, y0, pow10dw0)
                else -> throw RuntimeException()
            }
            return ret
        }
        throw RuntimeException("?que? EQ should be <= 128 bits")
    }

    private fun _EQ128x64x64(x1: Long, x0: Long, y0: Long, pow10: Long) : Boolean {
        val p1 = unsignedMulHi(y0, pow10)
        val p0 = y0 * pow10

        return ((x1 - p1) or (x0 - p0)) == 0L
    }

    private fun _EQ128x128x64(x1: Long, x0: Long, m1: Long, m0: Long, n0: Long) : Boolean {
        val pp00Hi = unsignedMulHi(m0, n0)
        val pp00Lo = m0 * n0
        val p0 = pp00Lo

        val pp10Hi = unsignedMulHi(m1, n0)
        val pp10Lo = m1 * n0
        val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

        val p2 = carry1 + pp10Hi
        return ((x0 - p0) or (x1 - p1) or p2) == 0L
    }


}