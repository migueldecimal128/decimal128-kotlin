package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.MAGIC_POW10_MAXX
import com.decimal128.decimal.U256Pow10.MAGIC_POW10_M_OFFSET
import com.decimal128.bigint.BigInt
import com.decimal128.decimal.U256Pow10.POW10

object DivMagic {

    /**
     * magic division of a ULong by powers of 10 using
     * unsignedMulHi() that do *not* need correction
     *
     * 1e1 0xCCCCCCCCCCCCCCCD s=3
     * 1e4 0x346DC5D63886594B s=11
     * 1e6 0x431BDE82D7B634DB s = 18
     * 1e7 0xD6BF94D5E57A42BD s = 23
     * 1e8 0xABCC77118461CEFD s = 26
     * 1e10 0xDBE6FECEBDEDD5BF	s = 33
     * 1e11 0xAFEBFF0BCB24AAFF	s = 36
     * 1e12 0x232F33025BD42233	s = 37
     * 1e13 0x384B84D092ED0385	s = 41
     * 1e14 0xB424DC35095CD81 s = 42
     * 1e16 0x39A5652FB1137857	s = 51
     * 1e19 0x760F253EDB4AB0D3	s = 62
     *
     */

    data class Magic(val m: Long, val add: Boolean, val s: Int)

    /**
     * Compute magic number and shift for unsigned division by d (1 ≤ d < 2^64),
     * using BigInt instead of BigInteger.
     */
    fun magicu64(d: Long): Magic {
        require(d != 0L) { "divisor must be nonzero" }
        val N = 64

        // 1) build BigInt version of 2^N and of the unsigned divisor
        val hiD        = BigInt.fromUnsigned(d)

        // 2) anc = (2^N − 1) − ((2^N − 1) mod d)
        val nBitMask   = BigInt.withBitMask(N)
        val anc      = nBitMask - (nBitMask % hiD)

        // 3) initialize p, q1/r1 for anc and q2/r2 for d
        var p          = (N - 1).toLong()
        val twoPowN1 = BigInt.withSetBit(N - 1)         // 2^(N−1)

        //var (q1, r1) = twoPowN1.divMod(anc)
        var q1 = twoPowN1 / anc
        var r1 = twoPowN1 % anc
        //var (q2, r2) = twoPowN1.divMod(hiD)
        var q2 = twoPowN1 / hiD
        var r2 = twoPowN1 % hiD
        lateinit var delta: BigInt

        do {
            p += 1

            // double q1/r1 mod anc
            q1 = q1 shl 1
            r1 = r1 shl 1
            if (r1 >= anc) {
                q1 += 1
                r1 -= anc
            }

            // double q2/r2 mod biD
            q2 = q2 shl 1
            r2 = r2 shl 1
            if (r2 >= hiD) {
                q2 += 1
                r2 -= hiD
            }

            delta = hiD - r2
        } while (q1 < delta || q1 == delta && r1.isZero())

        // 5) extract the “true” multiplier and shift
        val Mtrue   = q2 + 1
        val addFlag = Mtrue.isBitSet(N)            // bit-64 set?
        val m_mod   = Mtrue.toLong()           // low 64 bits
        val s       = (p - N).toInt()

        return Magic(m_mod, addFlag, s)
    }


    private var initialized = false
    val MAGIC_FLAG_AND_SHIFT_POW10 = ByteArray(MAGIC_POW10_MAXX)

    fun initializeMagicPow10_64() {
        if (initialized)
            return
        initialized = true
        POW10[MAGIC_POW10_M_OFFSET + 0] = 1
        MAGIC_FLAG_AND_SHIFT_POW10[0] = Byte.MIN_VALUE
        for (k in 1..<MAGIC_POW10_MAXX) {
            val d     = POW10[k]
            val magic = magicu64(d)
            POW10[MAGIC_POW10_M_OFFSET + k]   = magic.m
            MAGIC_FLAG_AND_SHIFT_POW10[k] =
                (if (magic.add) 0x80 or magic.s else magic.s).toByte()
        }
    }

    init {
        //initializeMagicPow10_64()
    }


    // Magic division allows a 64-bit dividend and a 64-bit divisor
    // However, on a 64-bit machine it cannot be used for multi-word
    // division beyond 32-bit limbs because a 32-bit remainder has to
    // get shifted up.
    // 64-bit limbs would require a 128-bit divide operation
    // Therefore, with a restriction of 32-bit limbs, Barrett Reduction
    // is smaller, faster, cleaner than Magic division
    //
    // Magic is still the right thing to do for small divisors, since
    // it has the 64-bit range in the divisor ... compared with Barrett
    // 32-bit divisor

    fun magicDivPow10_64(z: C256, x0: Long, pow10: Int): Residue {
        verify { pow10 in 0..<MAGIC_POW10_MAXX }
        initializeMagicPow10_64()
        verify { initialized }
        val remainder = magicDivModPow10_64(z, x0, pow10)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)
        return residue
    }

    fun magicDivPow10_64(x0: ULong, pow10: Int): ULong {
        initializeMagicPow10_64()
        verify { initialized }
        when {
            pow10 > 0 && pow10 < MAGIC_POW10_MAXX -> {
                val m = POW10[MAGIC_POW10_M_OFFSET + pow10].toULong()
                val flagAndShift = MAGIC_FLAG_AND_SHIFT_POW10[pow10].toInt()
                val denom = POW10[pow10].toULong()
                val s = flagAndShift and 0x3F
                val correctionMask = (flagAndShift shr 31).toLong().toULong()

                val carryAmount = 1uL shl -s // 1uL shl (64 - s)
                val pHiUncorrected = unsignedMulHi(x0, m)
                val pHiCorrected = pHiUncorrected + (x0 and correctionMask)
                val carry = if (pHiCorrected < pHiUncorrected) carryAmount else 0uL
                val qHat = pHiCorrected shr s
                val q0 = carry + qHat

                return q0
            }

            pow10 == 0 -> return x0
            else -> throw IllegalArgumentException()
        }
    }

    private fun magicDivModPow10_64(z: C256, x0: Long, pow10: Int): Long {
        when {
            pow10 > 0 && pow10 < MAGIC_POW10_MAXX -> {
                val m = POW10[MAGIC_POW10_M_OFFSET + pow10]
                val flagAndShift = MAGIC_FLAG_AND_SHIFT_POW10[pow10].toInt()
                val denom = POW10[pow10]
                val s = flagAndShift and 0x3F
                val correctionMask = (flagAndShift shr 31).toLong()

                val carryAmount = 1L shl -s
                val pHiUncorrected = unsignedMulHi(x0, m)
                val pHiCorrected = pHiUncorrected + (x0 and correctionMask)
                val carry = if (unsignedLT(pHiCorrected, pHiUncorrected)) carryAmount else 0L
                val qHat = pHiCorrected ushr s
                val q0 = carry + qHat

                val reconstructedHi = unsignedMulHi(q0, denom)
                val reconstructedLo = q0 * denom
                val rHat = x0 - reconstructedLo
                val remainder = rHat + (-reconstructedHi and denom)

                z.c256Set64(q0)
                return remainder
            }
            pow10 == 0 -> {
                z.c256Set64(x0)
                return 0L
            }
            else ->
                throw IllegalArgumentException()

        }
    }

}
