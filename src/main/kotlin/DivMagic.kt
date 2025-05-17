@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128

import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger

const val MAGIC_MAX_DIVISOR_BITLEN = 64

object DivMagic {

    data class Magic(val m: Long, val add: Boolean, val s: Int)

    /**
     * Compute magic number and shift for unsigned division by d (any 1 ≤ d < 2^64).
     * Returns:
     *   m   = low 64 bits of the “true” multiplier,
     *   add = true if the true multiplier had bit-64 set (i.e. needed 65 bits),
     *   s   = the right-shift amount beyond 64.
     */
    fun magicu64(d: Long): Magic {
        require(d != 0L) { "divisor must be nonzero" }
        val N = 64
        val twoPowN = BigInteger.ONE.shiftLeft(N)           // 2^64
        // Reconstruct the unsigned 64-bit divisor in a BigInteger:
        val hi32 = d ushr 32                             // logical shift, bits 63–32
        val lo32 = d and 0xFFFFFFFFL                     // bits 31–0
        val biD = BigInteger.valueOf(hi32)
            .shiftLeft(32)
            .or(BigInteger.valueOf(lo32))
        // anc = 2^N - 1 - ((2^N - 1) mod d)
        val biN1   = twoPowN - BigInteger.ONE
        val anc    = biN1 - biN1.mod(biD)

        val twoPowNminus1 = BigInteger.ONE.shiftLeft(N - 1)       // 2^(N-1)
        var p       = (N - 1).toLong()
        var q1      = twoPowNminus1.divide(anc)
        var r1      = twoPowNminus1.remainder(anc)
        var q2      = twoPowNminus1.divide(biD)
        var r2      = twoPowNminus1.remainder(biD)
        var delta: BigInteger

        do {
            p += 1
            q1 = q1.shiftLeft(1); r1 = r1.shiftLeft(1)
            if (r1 >= anc) { q1 += BigInteger.ONE; r1 -= anc }
            q2 = q2.shiftLeft(1); r2 = r2.shiftLeft(1)
            if (r2 >= biD)  { q2 += BigInteger.ONE; r2 -= biD }
            delta = biD - r2
        } while (q1 < delta || (q1 == delta && r1 == BigInteger.ZERO))

        val Mtrue   = q2 + BigInteger.ONE
        val addFlag = Mtrue.testBit(N)                    // was bit-64 set?
        val m_mod   = Mtrue.clearBit(N).toLong()           // low 64 bits as signed Long
        val s        = (p - N).toInt()

        return Magic(m_mod, addFlag, s)
    }

    private var initialized = false
    val MAGIC_FLAG_AND_SHIFT_POW10 = ByteArray(MAGIC_POW10_MAX)

    fun initializeMagicPow10_64() {
        if (initialized)
            return
        initialized = true
        POW10[MAGIC_POW10_M_OFFSET + 0] = 1
        MAGIC_FLAG_AND_SHIFT_POW10[0] = Byte.MIN_VALUE
        for (k in 1..<MAGIC_POW10_MAX) {
            val d     = POW10[k]
            val magic = magicu64(d)
            POW10[MAGIC_POW10_M_OFFSET + k]   = magic.m
            MAGIC_FLAG_AND_SHIFT_POW10[k] =
                (if (magic.add) 0x80 or magic.s else magic.s).toByte()
            if (magic.add)
                println("Kilroy was here! ... and so was a magic flag for k:$k")
        }
    }

    init {
        initializeMagicPow10_64()
    }


    fun magicDivPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        assert(pow10 in 0..<MAGIC_POW10_MAX)
        assert(initialized)
        val remainder = magicDivModPow10(z, x, pow10)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)
        return residue
    }

    fun magicDivModPow10(z: Coeff, x: Coeff, pow10: Int): Long {
        when {
            pow10 > 0 && pow10 < MAGIC_POW10_MAX -> {
                val m = POW10[MAGIC_POW10_M_OFFSET + pow10]
                val flagAndShift = MAGIC_FLAG_AND_SHIFT_POW10[pow10].toLong()
                val xBitLen = x.bitLen
                val remainder = when {
                    xBitLen <= 64 ->
                        magicDivModPow10_64(z, x.dw0, pow10, m, flagAndShift)

                    xBitLen <= 128 ->
                        magicDivModPow10_128(z, x, pow10, m, flagAndShift)

                    xBitLen <= 192 ->
                        magicDivModPow10_192(z, x, pow10, m, flagAndShift)

                    else ->
                        magicDivModPow10_256(z, x, pow10, m, flagAndShift)
                }
                return remainder
            }
            pow10 == 0 -> {
                z.coeffSet(x)
                return 0L
            }
            else ->
                throw RuntimeException()
        }
    }

    private fun magicDivModPow10_256(
        q: Coeff,
        x: Coeff,
        pow10: Int,
        m: Long,
        flagAndShift: Long
    ): Long {
        throw RuntimeException("not impl")
    }

    private fun magicDivModPow10_192(
        q: Coeff,
        x: Coeff,
        pow10: Int,
        m: Long,
        flagAndShift: Long
    ): Long {
        throw RuntimeException("not impl")
    }

    private fun magicDivModPow10_128(
        q: Coeff,
        x: Coeff,
        pow10: Int,
        m: Long,
        flagAndShift: Long
    ): Long {
        throw RuntimeException("not impl")
    }

    // Pow10 is in the name because it should
    // only be used for powers of 10
    // otherwise, there is risk of
    private fun magicDivModPow10_64(
        q: Coeff,
        x0: Long,
        pow10: Int,
        m: Long,
        flagAndShift: Long
    ): Long {
        //val biX0 = BigInteger.valueOf(x0 ushr 32).shiftLeft(32).or(BigInteger.valueOf(x0 and 0xFFFFFFFFL))
        val denom = POW10[pow10]
        val s = flagAndShift.toInt() and 0x3F
        val qPotentialCarry = 1L shl -s
        val addMask = flagAndShift shr 63
        val pHiUncorrected = unsignedMultiplyHigh(x0, m)
        val pLo = x0 * m
        val pHiCorrected = pHiUncorrected + (x0 and addMask)
        val qCarryAdd = if (compareUnsigned(pHiCorrected, pHiUncorrected) < 0) qPotentialCarry else 0L
        val qHat = pHiCorrected ushr s
        val q0 = qCarryAdd + qHat

        // NOTE ...
        // this multiply will overflow only when Q0 is very large and denom is very small
        // for denom = 10**1 the magic correction flag is not set, the multiply cannot overflow
        // for denom = 10**2 the magic correction flag is set.
        // with x0==2**64-1 and denom==100 the multiply stays in 64 bits
        // therefore correction is not ever needed ...
        // ... AS LONG AS THIS IS NEVER USED FOR ANYTHING SMALLER THAN 10**2 == 100
        //val reconstructedHi = unsignedMultiplyHigh(q0, denom)
        val reconstructedLo = q0 * denom
        val rHat = x0 - reconstructedLo
        val r = rHat // + (-reconstructedHi and x0)

        q.coeffSet64(q0)
        return r
    }

}
