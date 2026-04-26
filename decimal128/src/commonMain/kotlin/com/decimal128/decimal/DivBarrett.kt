@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal fun barrettDivPow10(z: C256, x: C256, pow10: Int): Residue {
    verify { pow10 in 1..<BARRETT_POW10_MAXX }
    val remainder = barrettDivRemPow10(z, x, pow10)
    val residue = Residue.residueFromRemainderPow10(remainder, pow10)
    return residue
}

internal fun barrettDivRemPow10(z: C256, x: C256, pow10: Int): Long {
    verify { pow10 in 1..<BARRETT_POW10_MAXX }
    val loBits = x.dw0 and ((1L shl pow10) - 1)
    c256SetShiftRight(z, x, pow10)
    val remainder5 = barrettDivModPow5(z, z, pow10)
    val remainder10 = (remainder5 shl pow10) + loBits
    return remainder10
}

internal fun barrettDivModPow5(z: C256, x: C256, pow5: Int): Long {
    verify { pow5 in 1..<BARRETT_POW10_MAXX }

    val denom = POW10[POW5_64_BASE + pow5]
    val mu = POW10[BARRETT_POW5_MU_BASE + pow5]
    val xBitLen = x.bitLen
    val remainder = when {
        xBitLen <= 64 ->
            barrettDivMod_64(z, x.dw0, denom, mu)

        pow5 <= 4 -> when {

            xBitLen <= 118 ->
                barrettDivMod_54_118(z, x, denom, mu)

            xBitLen <= 172 ->
                barrettDivMod_54_172(z, x, denom, mu)

            xBitLen <= 226 ->
                barrettDivMod_54_226(z, x, denom, mu)

            else ->
                barrettDivMod_54_256(z, x, denom, mu)
        }

        xBitLen <= 128 ->
            barrettDivMod_32_128(z, x, denom, mu)

        xBitLen <= 192 ->
            barrettDivMod_32_192(z, x, denom, mu)

        else ->
            barrettDivMod_32_256(z, x, denom, mu)
    }
    return remainder
}

private fun barrettDivMod_64(q: C256, dw0: Long, denom: Long, mu: Long): Long {
    val dwG = dw0

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv() // adjust == -1 if adjustment needed else 0
    val qG = qHatG - adjustG // subtract -1 or zero ... subtract -1 == add 1
    val rG = rHatG - (adjustG and denom) // subtract denom or 0

    val remainder = rG

    val q0 = qG
    q.c256Set64(q0)
    return remainder
}

internal fun barrettDivMod_32_256(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1;
    val dw2 = x.dw2;
    val dw3 = x.dw3

    val dwA = dw0 and MASK32L
    val dwB = dw0 ushr 32
    val dwC = dw1 and MASK32L
    val dwD = dw1 ushr 32
    val dwE = dw2 and MASK32L
    val dwF = dw2 ushr 32
    val dwG = dw3

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppF = (rG shl 32) or dwF
    val qHatF = unsignedMulHi(ppF, mu)
    val rHatF = ppF - (qHatF * denom)
    val adjustF = ((rHatF - denom) shr 63).inv()
    val qF = qHatF - adjustF
    val rF = rHatF - (adjustF and denom)

    val ppE = (rF shl 32) or dwE
    val qHatE = unsignedMulHi(ppE, mu)
    val rHatE = ppE - (qHatE * denom)
    val adjustE = ((rHatE - denom) shr 63).inv()
    val qE = qHatE - adjustE
    val rE = rHatE - (adjustE and denom)

    val ppD = (rE shl 32) or dwD
    val qHatD = unsignedMulHi(ppD, mu)
    val rHatD = ppD - (qHatD * denom)
    val adjustD = ((rHatD - denom) shr 63).inv()
    val qD = qHatD - adjustD
    val rD = rHatD - (adjustD and denom)

    val ppC = (rD shl 32) or dwC
    val qHatC = unsignedMulHi(ppC, mu)
    val rHatC = ppC - (qHatC * denom)
    val adjustC = ((rHatC - denom) shr 63).inv()
    val qC = qHatC - adjustC
    val rC = rHatC - (adjustC and denom)

    val ppB = (rC shl 32) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 32) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q3 = qG
    val q2 = (qF shl 32) or qE
    val q1 = (qD shl 32) or qC
    val q0 = (qB shl 32) or qA
    q.c256Set256(q3, q2, q1, q0)
    return remainder
}

private fun barrettDivMod_32_192(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1;
    val dw2 = x.dw2

    val dwA = dw0 and MASK32L
    val dwB = dw0 ushr 32
    val dwC = dw1 and MASK32L
    val dwD = dw1 ushr 32
    val dwG = dw2

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppD = (rG shl 32) or dwD
    val qHatD = unsignedMulHi(ppD, mu)
    val rHatD = ppD - (qHatD * denom)
    val adjustD = ((rHatD - denom) shr 63).inv()
    val qD = qHatD - adjustD
    val rD = rHatD - (adjustD and denom)

    val ppC = (rD shl 32) or dwC
    val qHatC = unsignedMulHi(ppC, mu)
    val rHatC = ppC - (qHatC * denom)
    val adjustC = ((rHatC - denom) shr 63).inv()
    val qC = qHatC - adjustC
    val rC = rHatC - (adjustC and denom)

    val ppB = (rC shl 32) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 32) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q2 = qG
    val q1 = (qD shl 32) or qC
    val q0 = (qB shl 32) or qA
    q.c256Set192(q2, q1, q0)
    return remainder
}

internal fun barrettDivMod_32_128(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1

    val dwA = dw0 and MASK32L
    val dwB = dw0 ushr 32
    val dwG = dw1

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppB = (rG shl 32) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 32) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q1 = qG
    val q0 = (qB shl 32) or qA
    q.c256Set128(q1, q0)
    return remainder
}

private fun barrettDivMod_54_256(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1;
    val dw2 = x.dw2;
    val dw3 = x.dw3

    val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
    val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
    val dwC = ((dw2 shl 20) or (dw1 ushr 44)) and 0x003F_FFFF_FFFF_FFFFL
    val dwD = ((dw3 shl 30) or (dw2 ushr 34)) and 0x003F_FFFF_FFFF_FFFFL
    val dwG = (dw3 ushr 24)

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppD = (rG shl 54) or dwD
    val qHatD = unsignedMulHi(ppD, mu)
    val rHatD = ppD - (qHatD * denom)
    val adjustD = ((rHatD - denom) shr 63).inv()
    val qD = qHatD - adjustD
    val rD = rHatD - (adjustD and denom)

    val ppC = (rD shl 54) or dwC
    val qHatC = unsignedMulHi(ppC, mu)
    val rHatC = ppC - (qHatC * denom)
    val adjustC = ((rHatC - denom) shr 63).inv()
    val qC = qHatC - adjustC
    val rC = rHatC - (adjustC and denom)

    val ppB = (rC shl 54) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 54) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q3 = (qG shl 24) or (qD ushr 30)
    val q2 = (qD shl 34) or (qC ushr 20)
    val q1 = (qC shl 44) or (qB ushr 10)
    val q0 = (qB shl 54) or (qA ushr 0)
    q.c256Set256(q3, q2, q1, q0)
    return remainder
}

private fun barrettDivMod_54_226(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1;
    val dw2 = x.dw2;
    val dw3 = x.dw3

    val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
    val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
    val dwC = ((dw2 shl 20) or (dw1 ushr 44)) and 0x003F_FFFF_FFFF_FFFFL
    val dwG = (dw3 shl 30) or (dw2 ushr 34)

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppC = (rG shl 54) or dwC
    val qHatC = unsignedMulHi(ppC, mu)
    val rHatC = ppC - (qHatC * denom)
    val adjustC = ((rHatC - denom) shr 63).inv()
    val qC = qHatC - adjustC
    val rC = rHatC - (adjustC and denom)

    val ppB = (rC shl 54) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 54) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q3 = (qG ushr 30)
    val q2 = (qG shl 34) or (qC ushr 20)
    val q1 = (qC shl 44) or (qB ushr 10)
    val q0 = (qB shl 54) or (qA ushr 0)
    q.c256Set256(q3, q2, q1, q0)
    return remainder
}

private fun barrettDivMod_54_172(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1;
    val dw2 = x.dw2

    val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
    val dwB = ((dw1 shl 10) or (dw0 ushr 54)) and 0x003F_FFFF_FFFF_FFFFL
    val dwG = (dw2 shl 20) or (dw1 ushr 44)

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppB = (rG shl 54) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom)
    val adjustB = ((rHatB - denom) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom)

    val ppA = (rB shl 54) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q2 = (qG ushr 20)
    val q1 = (qG shl 44) or (qB ushr 10)
    val q0 = (qB shl 54) or (qA ushr 0)
    q.c256Set192(q2, q1, q0)
    return remainder
}

private fun barrettDivMod_54_118(q: C256, x: C256, denom: Long, mu: Long): Long {

    val dw0 = x.dw0;
    val dw1 = x.dw1

    val dwA = dw0 and 0x003F_FFFF_FFFF_FFFFL
    val dwG = (dw1 shl 10) or (dw0 ushr 54)

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom)
    val adjustG = ((rHatG - denom) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom)

    val ppA = (rG shl 54) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom)
    val adjustA = ((rHatA - denom) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom)

    val remainder = rA

    val q1 = (qG ushr 10)
    val q0 = (qG shl 54) or (qA ushr 0)
    q.c256Set128(q1, q0)
    return remainder
}

