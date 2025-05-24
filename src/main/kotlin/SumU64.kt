package com.decimal128
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA:Long, dwB:Long) :Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L
    return carryAB to sumAB
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA:Long, dwB:Long, dwC: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumABC = sumAB + dwC
    val carryABC = carryAB + if (compareUnsigned(sumABC, sumAB) < 0) 1L else 0L
    return carryABC to sumABC
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L
    return carryABCD to sumABCD
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L

    val sumABCDE = sumABCD + dwE
    val carryABCDE = carryABCD + if (compareUnsigned(sumABCDE, sumABCD) < 0) 1L else 0L
    return carryABCDE to sumABCDE
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (compareUnsigned(sumEF, dwE) < 0) 1L else 0L

    val sumABCDEF = sumABCD + sumEF
    val carryABCDEF = carryABCD + carryEF + if (compareUnsigned(sumABCDEF, sumABCD) < 0) 1L else 0L
    return carryABCDEF to sumABCDEF
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (compareUnsigned(sumEF, dwE) < 0) 1L else 0L

    val sumEFG = sumEF + dwG
    val carryEFG = carryEF + if (compareUnsigned(sumEFG, dwG) < 0) 1L else 0L

    val sumABCDEFG = sumABCD + sumEFG
    val carryABCDEFG = carryABCD + carryEFG + if (compareUnsigned(sumABCDEFG, sumABCD) < 0) 1L else 0L
    return carryABCDEFG to sumABCDEFG
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (compareUnsigned(sumEF, dwE) < 0) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (compareUnsigned(sumGH, dwG) < 0) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (compareUnsigned(sumEFGH, sumEF) < 0) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (compareUnsigned(sumABCDEFGH, sumABCD) < 0) 1L else 0L
    return carryABCDEFGH to sumABCDEFGH
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long, dwI: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (compareUnsigned(sumAB, dwA) < 0) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (compareUnsigned(sumCD, dwC) < 0) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (compareUnsigned(sumABCD, sumAB) < 0) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (compareUnsigned(sumEF, dwE) < 0) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (compareUnsigned(sumGH, dwG) < 0) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (compareUnsigned(sumEFGH, sumEF) < 0) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (compareUnsigned(sumABCDEFGH, sumABCD) < 0) 1L else 0L

    val sumABCDEFGHI = sumABCDEFGH + dwI
    val carryABCDEFGHI = carryABCDEFGH + if (compareUnsigned(sumABCDEFGHI, dwI) < 0) 1L else 0L

    return carryABCDEFGHI to sumABCDEFGHI
}

@Suppress("NOTHING_TO_INLINE")
// returns borrow 0 or 1
inline fun diffU64(dwA:Long, dwZ:Long) :Pair<Long, Long> {
    val diffAZ = dwA - dwZ
    val borrowAZ = if (compareUnsigned(diffAZ, dwA) > 0) 1L else 0L
    return borrowAZ to diffAZ
}

@Suppress("NOTHING_TO_INLINE")
inline fun diffU64withBorrow(dwA:Long, dwB: Long, borrowIn: Long): Pair<Long, Long> {
    assert(borrowIn in 0..1)
    // First subtract b from a:
    val diffAB = dwA - dwB
    val borrow1 = if (compareUnsigned(dwA, dwB) < 0) 1L else 0L
    val totalDiff = diffAB - borrowIn
    val totalBorrow = if (compareUnsigned(diffAB, borrowIn) < 0) 1L else borrow1
    return totalBorrow to totalDiff
}

@Suppress("NOTHING_TO_INLINE")
inline fun sumU128(x1: Long, x0: Long, y1: Long, y0: Long) : Pair<Long, Long> {
    val s0 = x0 + y0
    val carry0 = if (compareUnsigned(s0, x0) < 0) 1L else 0L
    val s1 = carry0 + x1 + y1
    return s1 to s0
}

@Suppress("NOTHING_TO_INLINE")
inline fun diffU128(x1: Long, x0: Long, y1: Long, y0: Long) : Pair<Long, Long> {
    val d0 = x0 - y0
    val borrow0 = if (compareUnsigned(x0, y0) < 0) 1L else 0L
    val d1 = x1 - y1 - borrow0
    return d1 to d0
}

@Suppress("NOTHING_TO_INLINE")
inline fun umul128x128to128(x1: Long, x0: Long, y1: Long, y0: Long): Pair<Long, Long> {
    val pp00Hi = unsignedMultiplyHigh(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0
    val pp01Lo = x0 * y1

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo + pp01Lo
    return p1 to p0
}

@Suppress("NOTHING_TO_INLINE")
inline fun umul128x64to128(x1: Long, x0: Long, y0: Long): Pair<Long, Long> {
    val pp00Hi = unsignedMultiplyHigh(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo
    return p1 to p0
}

@Suppress("NOTHING_TO_INLINE")
inline fun umul128x64to192_not_worth_it(x1: Long, x0: Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMultiplyHigh(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMultiplyHigh(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

    val p2 = carry1 + pp10Hi

    return Triple(p2, p1, p0)
}

@Suppress("NOTHING_TO_INLINE")
inline fun umul192x64to192(x2: Long, x1: Long, x0: Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMultiplyHigh(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMultiplyHigh(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

    val pp20Lo = x2 * y0
    val p2 = carry1 + pp10Hi + pp20Lo

    return Triple(p2, p1, p0)
}

@Suppress("NOTHING_TO_INLINE")
inline fun umul128x128to192(x1: Long, x0: Long, y1:Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMultiplyHigh(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp01Hi = unsignedMultiplyHigh(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMultiplyHigh(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

    val pp11Lo = x1 * y1
    val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo

    return Triple(p2, p1, p0)
}


