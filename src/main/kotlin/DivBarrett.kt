@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.lang.Math.unsignedMultiplyHigh

object DivBarrett {

    fun barrettDivPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        assert(pow10 < BARRETT_POW10_MAX)
        val xBitLen = x.bitLen
        val remainder = when {
            pow10 < 4 -> when {
                (pow10 <= 0) -> {
                    assert(pow10 == 0)
                    z.coeffSet(x)
                    return EXACT
                }

                xBitLen <= 64 ->
                    barrettDivModPow10_64(z, x.dw0, pow10)

                xBitLen <= 114 ->
                    barrettDivModPow10_50_114(z, x, pow10)

                xBitLen <= 164 ->
                    barrettDivModPow10_50_164(z, x, pow10)

                xBitLen <= 214 ->
                    barrettDivModPow10_50_214(z, x, pow10)

                else ->
                    barrettDivModPow10_50_256(z, x, pow10)
            }

            xBitLen <= 64 ->
                barrettDivModPow10_64(z, x.dw0, pow10)

            xBitLen <= 128 ->
                barrettDivModPow10_32_128(z, x, pow10)

            xBitLen <= 192 ->
                barrettDivModPow10_32_192(z, x, pow10)

            else ->
                barrettDivModPow10_32_256(z, x, pow10)
        }
        val residue = Residue.residueFromRemainderPow10(remainder, pow10)
        return residue
    }


    fun barrettDivModPow10_32_256(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..9)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwC = dw1 and 0xFFFF_FFFFL
        val dwD = dw1 ushr 32
        val dwE = dw2 and 0xFFFF_FFFFL
        val dwF = dw2 ushr 32
        val dwG = dw3

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

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

    fun barrettDivModPow10_32_192(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..9)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwC = dw1 and 0xFFFF_FFFFL
        val dwD = dw1 ushr 32
        val dwG = dw2

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

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

    fun barrettDivModPow10_32_128(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..9)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwG = dw1

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

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

    fun barrettDivModPow10_64(q: Coeff, dw0: Long, pow10: Int): Long {
        require(pow10 in 1..9)

        val dwG = dw0

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

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

    fun barrettDivModPow10_50_256(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..4)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0x3_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 14) or (dw0 ushr 50)) and 0x3_FFFF_FFFF_FFFFL
        val dwC = ((dw2 shl 28) or (dw1 ushr 36)) and 0x3_FFFF_FFFF_FFFFL
        val dwD = ((dw3 shl 42) or (dw2 ushr 22)) and 0x3_FFFF_FFFF_FFFFL
        val dwG = (dw3 ushr 8)

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppD = (rG shl 50) or dwD
        val qDhat = unsignedMultiplyHigh(ppD, mu)
        val rDhat = ppD - (qDhat * denom)
        val adjustD = rDhat >= denom
        val qD = qDhat + if (adjustD) 1L else 0L
        val rD = rDhat - if (adjustD) denom else 0L

        val ppC = (rD shl 50) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 50) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 50) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q3 = (qG shl  8) or (qD ushr 42)
        val q2 = (qD shl 22) or (qC ushr 28)
        val q1 = (qC shl 36) or (qB ushr 14)
        val q0 = (qB shl 50) or (qA ushr  0)
        q.coeffSet256(q3, q2, q1, q0)
        return remainder
    }

    fun barrettDivModPow10_50_214(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..4)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2; val dw3 = x.dw3

        val dwA = dw0 and 0x3_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 14) or (dw0 ushr 50)) and 0x3_FFFF_FFFF_FFFFL
        val dwC = ((dw2 shl 28) or (dw1 ushr 36)) and 0x3_FFFF_FFFF_FFFFL
        val dwG = (dw3 shl 42) or (dw2 ushr 22)

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppC = (rG shl 50) or dwC
        val qChat = unsignedMultiplyHigh(ppC, mu)
        val rChat = ppC - (qChat * denom)
        val adjustC = rChat >= denom
        val qC = qChat + if (adjustC) 1L else 0L
        val rC = rChat - if (adjustC) denom else 0L

        val ppB = (rC shl 50) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 50) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q3 =                (qG ushr 42)
        val q2 = (qG shl 22) or (qC ushr 28)
        val q1 = (qC shl 36) or (qB ushr 14)
        val q0 = (qB shl 50) or (qA ushr  0)
        q.coeffSet256(q3, q2, q1, q0)
        return remainder
    }

    fun barrettDivModPow10_50_164(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..4)

        val dw0 = x.dw0; val dw1 = x.dw1; val dw2 = x.dw2

        val dwA = dw0 and 0x3_FFFF_FFFF_FFFFL
        val dwB = ((dw1 shl 14) or (dw0 ushr 50)) and 0x3_FFFF_FFFF_FFFFL
        val dwG = (dw2 shl 28) or (dw1 ushr 36)

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppB = (rG shl 50) or dwB
        val qBhat = unsignedMultiplyHigh(ppB, mu)
        val rBhat = ppB - (qBhat * denom)
        val adjustB = rBhat >= denom
        val qB = qBhat + if (adjustB) 1L else 0L
        val rB = rBhat - if (adjustB) denom else 0L

        val ppA = (rB shl 50) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q2 =                (qG ushr 28)
        val q1 = (qG shl 36) or (qB ushr 14)
        val q0 = (qB shl 50) or (qA ushr  0)
        q.coeffSet192(q2, q1, q0)
        return remainder
    }

    fun barrettDivModPow10_50_114(q: Coeff, x: Coeff, pow10: Int): Long {
        require(pow10 in 1..4)

        val dw0 = x.dw0; val dw1 = x.dw1

        val dwA = dw0 and 0x3_FFFF_FFFF_FFFFL
        val dwG = (dw1 shl 14) or (dw0 ushr 50)

        val denom = POW10[pow10]
        val mu = POW10[(BARRETT_POW10_MU_OFFSET + pow10) and 0xFF]

        val qGhat = unsignedMultiplyHigh(dwG, mu)
        val rGhat = dwG - (qGhat * denom)
        val adjustG = rGhat >= denom
        val qG = qGhat + if (adjustG) 1L else 0L
        val rG = rGhat - if (adjustG) denom else 0L

        val ppA = (rG shl 50) or dwA
        val qAhat = unsignedMultiplyHigh(ppA, mu)
        val rAhat = ppA - (qAhat * denom)
        val adjustA = rAhat >= denom
        val qA = qAhat + if (adjustA) 1L else 0L
        val rA = rAhat - if (adjustA) denom else 0L

        val remainder = rA

        val q1 = (qG ushr 14)
        val q0 = (qG shl 50) or qA
        q.coeffSet128(q1, q0)
        return remainder
    }

}
