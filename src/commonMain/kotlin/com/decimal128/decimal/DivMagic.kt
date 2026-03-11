package com.decimal128.decimal

import com.decimal128.bigint.BigInt

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
        verify { pow10 in 0..<MAGIC_POW10_M_MAXX }
        verify { DWORD_TABLES[MAGIC_POW10_M_BASE + 1] == 0xCCCCCCCCCCCCCCCDuL.toLong() }
        val remainder = magicDivModPow10_64(z, x0, pow10)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)
        return residue
    }

    private fun magicDivModPow10_64(z: C256, x0: Long, pow10: Int): Long {
        when {
            pow10 > 0 && pow10 < MAGIC_POW10_M_MAXX -> {
                val m = DWORD_TABLES[(MAGIC_POW10_M_BASE + pow10) and DWORD_TABLES_BCE]
                val flagAndShift =
                    BYTE_TABLES[(MAGIC_FLAG_AND_SHIFT_BASE + pow10) and BYTE_TABLES_BCE].toInt()
                val denom = pow10_64(pow10)
                val s = flagAndShift and 0x3F
                val correctionMask = (flagAndShift shr 31).toLong()

                val carryAmount = 1L shl -s
                val pHiUncorrected = unsignedMulHi(x0, m)
                val pHiCorrected = pHiUncorrected + (x0 and correctionMask)
                val carry = if (unsignedLT(pHiCorrected, pHiUncorrected)) carryAmount else 0L
                val qHat = pHiCorrected ushr s
                val q0 = carry + qHat

                val reconstructedHi = unsignedMulHi(q0, denom)
                verify { reconstructedHi == 0L || reconstructedHi == 1L }
                val reconstructedLo = q0 * denom
                val rHat = x0 - reconstructedLo
                // add denom iff reconstructedHi == 1
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
