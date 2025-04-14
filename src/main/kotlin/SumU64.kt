package com.decimal128
import java.lang.Long.compareUnsigned

fun sumU64(dwA: Long, dwB: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()
    return carryAB to sumAB
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumABC = sumAB + dwC
    val carryABC = carryAB + (compareUnsigned(sumABC, sumAB) ushr 31).toLong()
    return carryABC to sumABC
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()
    return carryABCD to sumABCD
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()

    val sumABCDE = sumABCD + dwE
    val carryABCDE = carryABCD + (compareUnsigned(sumABCDE, sumABCD) ushr 31).toLong()
    return carryABCDE to sumABCDE
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()

    val sumEF = dwE + dwF
    val carryEF = (compareUnsigned(sumEF, dwE) ushr 31).toLong()

    val sumABCDEF = sumABCD + sumEF
    val carryABCDEF = carryABCD + carryEF + (compareUnsigned(sumABCDEF, sumABCD) ushr 31).toLong()
    return carryABCDEF to sumABCDEF
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()

    val sumEF = dwE + dwF
    val carryEF = (compareUnsigned(sumEF, dwE) ushr 31).toLong()

    val sumEFG = sumEF + dwG
    val carryEFG = carryEF + (compareUnsigned(sumEFG, dwG) ushr 31).toLong()

    val sumABCDEFG = sumABCD + sumEFG
    val carryABCDEFG = carryABCD + carryEFG + (compareUnsigned(sumABCDEFG, sumABCD) ushr 31).toLong()
    return carryABCDEFG to sumABCDEFG
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()

    val sumEF = dwE + dwF
    val carryEF = (compareUnsigned(sumEF, dwE) ushr 31).toLong()

    val sumGH = dwG + dwH
    val carryGH = (compareUnsigned(sumGH, dwG) ushr 31).toLong()

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + (compareUnsigned(sumEFGH, sumEF) ushr 31).toLong()

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + (compareUnsigned(sumABCDEFGH, sumABCD) ushr 31).toLong()
    return carryABCDEFGH to sumABCDEFGH
}

fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long, dwI: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = (compareUnsigned(sumAB, dwA) ushr 31).toLong()

    val sumCD = dwC + dwD
    val carryCD = (compareUnsigned(sumCD, dwC) ushr 31).toLong()

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + (compareUnsigned(sumABCD, sumAB) ushr 31).toLong()

    val sumEF = dwE + dwF
    val carryEF = (compareUnsigned(sumEF, dwE) ushr 31).toLong()

    val sumGH = dwG + dwH
    val carryGH = (compareUnsigned(sumGH, dwG) ushr 31).toLong()

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + (compareUnsigned(sumEFGH, sumEF) ushr 31).toLong()

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + (compareUnsigned(sumABCDEFGH, sumABCD) ushr 31).toLong()

    val sumABCDEFGHI = sumABCDEFGH + dwI
    val carryABCDEFGHI = carryABCDEFGH + (compareUnsigned(sumABCDEFGHI, dwI) ushr 31).toLong()

    return carryABCDEFGHI to sumABCDEFGHI
}

fun mul1x1(xDw0: Long, yDw0: Long) : Pair<Long, Long> {
    val pDw0 = xDw0 * yDw0
    val pDw1Signed = Math.multiplyHigh(xDw0, yDw0)
    val unsignedCorrection = ((xDw0 shr 63) and yDw0) + ((yDw0 shr 63) and xDw0)
    val pDw1 = pDw1Signed + unsignedCorrection
    return pDw1 to pDw0
}

fun multu64u64u128(a: Long, b: Long): Pair<Long, Long> {
    val hiSigned = Math.multiplyHigh(a, b)
    val lo = a * b
    val unsignedCorrection = ((a shr 63) and b) + ((b shr 63) and a)
    val hiUnsigned = hiSigned + unsignedCorrection
    return hiUnsigned to lo
}
