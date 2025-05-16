@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.lang.Math.unsignedMultiplyHigh

object DivBarrett {

    fun barrettDivPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        val remainder = barrettDivModPow10(z, x, pow10)
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)
        return residue
    }

    fun barrettDivModPow10(z: Coeff, x: Coeff, pow10: Int): Long {
        assert(pow10 < BARRETT_POW10_MAX)
        val denom = POW10[pow10]
        val mu = POW10[BARRETT_POW10_MU_OFFSET + pow10]
        val xBitLen = x.bitLen
        val remainder = when {
            pow10 <= 3 -> when {
                (pow10 <= 0) -> {
                    assert(pow10 == 0)
                    z.coeffSet(x)
                    return 0L
                }

                xBitLen <= 64 ->
                    barrettDivMod_64(z, x.dw0, denom, mu)

                xBitLen <= 118 ->
                    barrettDivMod_54_118(z, x, denom, mu)

                xBitLen <= 172 ->
                    barrettDivMod_54_172(z, x, denom, mu)

                xBitLen <= 226 ->
                    barrettDivMod_54_226(z, x, denom, mu)

                else ->
                    barrettDivMod_54_256(z, x, denom, mu)
            }

            xBitLen <= 64 ->
                barrettDivMod_64(z, x.dw0, denom, mu)

            xBitLen <= 128 ->
                barrettDivMod_32_128(z, x, denom, mu)

            xBitLen <= 192 ->
                barrettDivMod_32_192(z, x, denom, mu)

            else ->
                barrettDivMod_32_256(z, x, denom, mu)
        }
        return remainder
    }

    fun barrettDivModPow5(z: Coeff, x: Coeff, pow5: Int): Long {
        assert(pow5 < BARRETT_POW5_MAX)
        val denom = POW10[POW5_64_OFFSET + pow5]
        val mu = POW10[BARRETT_POW5_MU_OFFSET + pow5]
        val xBitLen = x.bitLen
        val remainder = when {
            pow5 <= 6 -> when {
                (pow5 <= 0) -> {
                    assert(pow5 == 0)
                    z.coeffSet(x)
                    return 0L
                }

                xBitLen <= 64 ->
                    barrettDivMod_64(z, x.dw0, denom, mu)

                xBitLen <= 118 ->
                    barrettDivMod_54_118(z, x, denom, mu)

                xBitLen <= 172 ->
                    barrettDivMod_54_172(z, x, denom, mu)

                xBitLen <= 226 ->
                    barrettDivMod_54_226(z, x, denom, mu)

                else ->
                    barrettDivMod_54_256(z, x, denom, mu)
            }

            xBitLen <= 64 ->
                barrettDivMod_64(z, x.dw0, denom, mu)

            xBitLen <= 128 ->
                barrettDivMod_32_128(z, x, denom, mu)

            xBitLen <= 192 ->
                barrettDivMod_32_192(z, x, denom, mu)

            else ->
                barrettDivMod_32_256(z, x, denom, mu)
        }
        return remainder
    }


    private fun barrettDivMod_32_256(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwC = dw1 and 0xFFFF_FFFFL
        val dwD = dw1 ushr 32
        val dwE = dw2 and 0xFFFF_FFFFL
        val dwF = dw2 ushr 32
        val dwG = dw3

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppF = (rG shl 32) or dwF
        val qFhat = unsignedMultiplyHigh(ppF, mu)
        val rFhat = ppF - (qFhat * denom)
        val adjustF = rFhat >= denom
        val qF = qFhat + if (adjustF) 1L else 0L
        val rF = rFhat - if (adjustF) denom else 0L

        val ppE = (rF shl 32) or dwE
        val qEhat = unsignedMultiplyHigh(ppE, mu)
        val rEhat = ppE - (qEhat * denom)
        val adjustE = rEhat >= denom
        val qE = qEhat + if (adjustE) 1L else 0L
        val rE = rEhat - if (adjustE) denom else 0L

        val ppD = (rE shl 32) or dwD
        val qDhat = unsignedMultiplyHigh(ppD, mu)
        val rDhat = ppD - (qDhat * denom)
        val adjustD = rDhat >= denom
        val qD = qDhat + if (adjustD) 1L else 0L
        val rD = rDhat - if (adjustD) denom else 0L

        val ppC = (rD shl 32) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 32) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 32) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q3 = qG
        val q2 = (qF shl 32) or qE
        val q1 = (qD shl 32) or qC
        val q0 = (qB shl 32) or qA
        q.coeffSet256(q3, q2, q1, q0)
        return rA
    }

    private fun barrettDivMod_32_192(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwC = dw1 and 0xFFFF_FFFFL
        val dwD = dw1 ushr 32
        val dwG = dw2

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppD = (rG shl 32) or dwD
        val qDhat = unsignedMultiplyHigh(ppD, mu)
        val rDhat = ppD - (qDhat * denom)
        val adjustD = rDhat >= denom
        val qD = qDhat + if (adjustD) 1L else 0L
        val rD = rDhat - if (adjustD) denom else 0L

        val ppC = (rD shl 32) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 32) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 32) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q2 = qG
        val q1 = (qD shl 32) or qC
        val q0 = (qB shl 32) or qA
        q.coeffSet192(q2, q1, q0)
        return remainder
    }

    private fun barrettDivMod_32_128(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwG = dw1

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppB = (rG shl 32) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 32) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q1 = qG
        val q0 = (qB shl 32) or qA
        q.coeffSet128(q1, q0)
        return remainder
    }

    private fun barrettDivMod_64(q: Coeff, dw0: Long, denom: Long, mu: Long): Long {

        val dwG = dw0

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val remainder = rG

        val q0 = qG
        q.coeffSet64(q0)
        return remainder
    }

    private fun barrettDivMod_54_256(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
        val dwC = ((dw2 shl 20) or (dw1 ushr 44)) and 0x003F_FFFF_FFFF_FFFFL
        val dwD = ((dw3 shl 30) or (dw2 ushr 34)) and 0x003F_FFFF_FFFF_FFFFL
        val dwG =                  (dw3 ushr 24)

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppD = (rG shl 54) or dwD
        val qDhat = unsignedMultiplyHigh(ppD, mu)
        val rDhat = ppD - (qDhat * denom)
        val adjustD = rDhat >= denom
        val qD = qDhat + if (adjustD) 1L else 0L
        val rD = rDhat - if (adjustD) denom else 0L

        val ppC = (rD shl 54) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 54) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 54) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q3 = (qG shl 24) or (qD ushr 30)
        val q2 = (qD shl 34) or (qC ushr 20)
        val q1 = (qC shl 44) or (qB ushr 10)
        val q0 = (qB shl 54) or (qA ushr  0)
        q.coeffSet256(q3, q2, q1, q0)
        return remainder
    }

    private fun barrettDivMod_54_226(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
        val dwC = ((dw2 shl 20) or (dw1 ushr 44)) and 0x003F_FFFF_FFFF_FFFFL
        val dwG =  (dw3 shl 30) or (dw2 ushr 34)

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppC = (rG shl 54) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 54) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 54) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q3 =                (qG ushr 30)
        val q2 = (qG shl 34) or (qC ushr 20)
        val q1 = (qC shl 44) or (qB ushr 10)
        val q0 = (qB shl 54) or (qA ushr  0)
        q.coeffSet256(q3, q2, q1, q0)
        return remainder
    }

    private fun barrettDivMod_54_172(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
        val dwG =  (dw2 shl 20) or (dw1 ushr 44)

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppB = (rG shl 54) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 54) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q2 =                (qG ushr 20)
        val q1 = (qG shl 44) or (qB ushr 10)
        val q0 = (qB shl 54) or (qA ushr  0)
        q.coeffSet192(q2, q1, q0)
        return remainder
    }

    private fun barrettDivMod_54_118(q: Coeff, x: Coeff, denom: Long, mu: Long): Long {

        val dw0 = x.dw0; val dw1 = x.dw1

        val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
        val dwG =  (dw1 shl 10) or (dw0 ushr 54)

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppA = (rG shl 54) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q1 =                (qG ushr 10)
        val q0 = (qG shl 54) or (qA ushr  0)
        q.coeffSet128(q1, q0)
        return remainder
    }

}
