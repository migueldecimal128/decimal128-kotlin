@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal inline fun sumU64(dwA:Long, dwB:Long) :Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L
    return carryAB to sumAB
}

internal inline fun sumU64(sum: DwQuad, dwA:Long, dwB:Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    sum.dw0 = sumAB
    sum.dw1 = carryAB
}

internal inline fun sumU64(dwA:ULong, dwB:ULong) :Pair<ULong, ULong> {
    val sumAB = dwA + dwB
    val carryAB = if (sumAB < dwA) 1uL else 0uL
    return carryAB to sumAB
}

internal inline fun sumU64(dwA:Long, dwB:Long, dwC: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumABC = sumAB + dwC
    val carryABC = carryAB + if (unsignedLT(sumABC, sumAB)) 1L else 0L
    return carryABC to sumABC
}

internal inline fun sumU64(sum: DwQuad, dwA:Long, dwB:Long, dwC: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumABC = sumAB + dwC
    val carryABC = carryAB + if (unsignedLT(sumABC, sumAB)) 1L else 0L

    sum.dw0 = sumABC
    sum.dw1 = carryABC
}

internal inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L
    return carryABCD to sumABCD
}

internal inline fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    sum.dw0 = sumABCD
    sum.dw1 = carryABCD
}

internal inline fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumABCDE = sumABCD + dwE
    val carryABCDE = carryABCD + if (unsignedLT(sumABCDE, sumABCD)) 1L else 0L
    return carryABCDE to sumABCDE
}

internal inline fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumABCDE = sumABCD + dwE
    val carryABCDE = carryABCD + if (unsignedLT(sumABCDE, sumABCD)) 1L else 0L

    sum.dw0 = sumABCDE
    sum.dw1 = carryABCDE
}


internal /*inline*/ fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumABCDEF = sumABCD + sumEF
    val carryABCDEF = carryABCD + carryEF + if (unsignedLT(sumABCDEF, sumABCD)) 1L else 0L
    return carryABCDEF to sumABCDEF
}

internal /*inline*/ fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumABCDEF = sumABCD + sumEF
    val carryABCDEF = carryABCD + carryEF + if (unsignedLT(sumABCDEF, sumABCD)) 1L else 0L

    sum.dw0 = sumABCDEF
    sum.dw1 = carryABCDEF
}


internal /*inline*/ fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumEFG = sumEF + dwG
    val carryEFG = carryEF + if (unsignedLT(sumEFG, dwG)) 1L else 0L

    val sumABCDEFG = sumABCD + sumEFG
    val carryABCDEFG = carryABCD + carryEFG + if (unsignedLT(sumABCDEFG, sumABCD)) 1L else 0L
    return carryABCDEFG to sumABCDEFG
}

internal /*inline*/ fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumEFG = sumEF + dwG
    val carryEFG = carryEF + if (unsignedLT(sumEFG, dwG)) 1L else 0L

    val sumABCDEFG = sumABCD + sumEFG
    val carryABCDEFG = carryABCD + carryEFG + if (unsignedLT(sumABCDEFG, sumABCD)) 1L else 0L

    sum.dw0 = sumABCDEFG
    sum.dw1 = carryABCDEFG
}


internal /*inline*/ fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (unsignedLT(sumGH, dwG)) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (unsignedLT(sumEFGH, sumEF)) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (unsignedLT(sumABCDEFGH, sumABCD)) 1L else 0L
    return carryABCDEFGH to sumABCDEFGH
}

internal /*inline*/ fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (unsignedLT(sumGH, dwG)) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (unsignedLT(sumEFGH, sumEF)) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (unsignedLT(sumABCDEFGH, sumABCD)) 1L else 0L

    sum.dw0 = sumABCDEFGH
    sum.dw1 = carryABCDEFGH
}


internal /*inline*/ fun sumU64(dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long, dwI: Long) : Pair<Long, Long> {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (unsignedLT(sumGH, dwG)) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (unsignedLT(sumEFGH, sumEF)) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (unsignedLT(sumABCDEFGH, sumABCD)) 1L else 0L

    val sumABCDEFGHI = sumABCDEFGH + dwI
    val carryABCDEFGHI = carryABCDEFGH + if (unsignedLT(sumABCDEFGHI, dwI)) 1L else 0L

    return carryABCDEFGHI to sumABCDEFGHI
}

internal /*inline*/ fun sumU64(sum: DwQuad, dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long, dwF: Long, dwG: Long, dwH: Long, dwI: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    val sumEF = dwE + dwF
    val carryEF = if (unsignedLT(sumEF, dwE)) 1L else 0L

    val sumGH = dwG + dwH
    val carryGH = if (unsignedLT(sumGH, dwG)) 1L else 0L

    val sumEFGH = sumEF + sumGH
    val carryEFGH = carryEF + carryGH + if (unsignedLT(sumEFGH, sumEF)) 1L else 0L

    val sumABCDEFGH = sumABCD + sumEFGH
    val carryABCDEFGH = carryABCD + carryEFGH + if (unsignedLT(sumABCDEFGH, sumABCD)) 1L else 0L

    val sumABCDEFGHI = sumABCDEFGH + dwI
    val carryABCDEFGHI = carryABCDEFGH + if (unsignedLT(sumABCDEFGHI, dwI)) 1L else 0L

    sum.dw0 = sumABCDEFGHI
    sum.dw1 = carryABCDEFGHI
}


// returns borrow 0 or 1
internal inline fun diffU64(dwA:Long, dwZ:Long) :Pair<Long, Long> {
    val diffAZ = dwA - dwZ
    val borrowAZ = if (unsignedCmp(diffAZ, dwA) > 0) 1L else 0L
    return borrowAZ to diffAZ
}


internal inline fun diffU64withBorrow(dwA:Long, dwB: Long, borrowIn: Long): Pair<Long, Long> {
    verify { borrowIn in 0..1 }
    // First subtract b from a:
    val diffAB = dwA - dwB
    val borrow1 = if (unsignedLT(dwA, dwB)) 1L else 0L
    val totalDiff = diffAB - borrowIn
    val totalBorrow = if (unsignedLT(diffAB, borrowIn)) 1L else borrow1
    return totalBorrow to totalDiff
}


internal inline fun sumU128(x1: Long, x0: Long, y1: Long, y0: Long) : Pair<Long, Long> {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1 + y1
    return s1 to s0
}

internal inline fun sumU128(sum: DwQuad, x1: Long, x0: Long, y1: Long, y0: Long) {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1 + y1

    sum.dw0 = s0
    sum.dw1 = s1
}


internal inline fun sumU128U64(x1: Long, x0: Long, y0: Long) : Pair<Long, Long> {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1
    return s1 to s0
}

internal inline fun sumU128U64(sum: DwQuad, x1: Long, x0: Long, y0: Long) {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1
    sum.dw0 = s0
    sum.dw1 = s1
}


internal inline fun diffU128(x1: Long, x0: Long, y1: Long, y0: Long) : Pair<Long, Long> {
    val d0 = x0 - y0
    val borrow0 = if (unsignedLT(x0, y0)) 1L else 0L
    val d1 = x1 - y1 - borrow0
    return d1 to d0
}


internal fun umul64x64to128(x0: Long, y0: Long): Pair<Long, Long> {
    val p0 = x0 * y0
    val p1 = unsignedMulHi(x0, y0)
    return p1 to p0
}

internal inline fun umul128x64to128(x1: Long, x0: Long, y0: Long): Pair<Long, Long> {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo
    return p1 to p0
}

internal inline fun umul128x128to128(x1: Long, x0: Long, y1: Long, y0: Long): Pair<Long, Long> {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0
    val pp01Lo = x0 * y1

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo + pp01Lo
    return p1 to p0
}

internal inline fun umul128x128to128(prod: DwQuad, x1: Long, x0: Long, y1: Long, y0: Long) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0
    val pp01Lo = x0 * y1

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo + pp01Lo

    prod.dw0 = p0
    prod.dw1 = p1
}

internal fun umul128xPow10to128(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
    val (pow10Dw1, pow10Dw0) = pow10_128(pow10)
    return umul128x128to128(dw1, dw0, pow10Dw1, pow10Dw0)
}

internal /*inline*/ fun umul128x64to192(x1: Long, x0: Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

    val p2 = carry1 + pp10Hi

    return Triple(p2, p1, p0)
}


internal /*inline*/ fun umul192x64to192(x2: Long, x1: Long, x0: Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

    val pp20Lo = x2 * y0
    val p2 = carry1 + pp10Hi + pp20Lo

    return Triple(p2, p1, p0)
}


internal /*inline*/ fun umul128x128to192(x1: Long, x0: Long, y1:Long, y0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

    val pp11Lo = x1 * y1
    val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo

    return Triple(p2, p1, p0)
}


internal /*inline*/ fun usqr96to192(x1: Long, x0: Long): Triple<Long, Long, Long> {
    val pp00Hi = unsignedMulHi(x0, x0)
    val pp00Lo = x0 * x0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, x1)
    val pp01Lo = x0 * x1
    val pp10Hi = pp01Hi
    val pp10Lo = pp01Lo
    val (carry1, p1) = sumU64(pp00Hi, pp01Lo, pp10Lo)

    val pp11Lo = x1 * x1
    val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo

    return Triple(p2, p1, p0)
}


/**
 * Divide the 128-bit unsigned (x1<<64 | x0) by the 64-bit unsigned y0,
 * returning Triple(quotientHigh64, quotientLow64, remainder).
 */
internal fun udivMod128x64to128(x1: Long, x0: Long, y0: Long) : Triple<Long, Long, Long> {

    // 1) special-case “pure 64-bit” dividend
    if (x1 == 0L) {
        val q0 = unsignedDiv(x0, y0)
        val r0  = unsignedMod(x0, y0)
        return Triple(0L, q0, r0)
    } else {
        return udivMod128x64to128_stage2(x1, x0, y0)
    }
}

internal fun udivMod128x64to128_stage2(x1: Long, x0: Long, y0: Long) : Triple<Long, Long, Long> {
    require(y0 != 0L) { "division by zero" }
    val v1 = y0 ushr 32
    val v0 = y0 and MASK32L
    if (v1 == 0L) {
        val u3 = x1 ushr 32
        val u2 = x1 and MASK32L
        val u1 = x0 ushr 32
        val u0 = x0 and MASK32L

        val t3 = u3
        val q3 = unsignedDiv(t3, v0); val r3 = unsignedMod(t3, v0)
        val t2 = (r3 shl 32) or u2
        val q2 = unsignedDiv(t2, v0); val r2 = unsignedMod(t2, v0)
        val t1 = (r2 shl 32) or u1
        val q1 = unsignedDiv(t1, v0); val r1 = unsignedMod(t1, v0)
        val t0 = (r1 shl 32) or u0
        val q0 = unsignedDiv(t0, v0); val r0 = unsignedMod(t0, v0)

        val qDw1 = (q3 shl 32) or q2
        val qDw0 = (q1 shl 32) or q0
        val rDw0 = r0
        return Triple(qDw1, qDw0, rDw0)
    }

    return knuthUdiv128x64to128(x1, x0, y0)
}

internal fun udivMod128x64to128(x1: ULong, x0: ULong, y0: ULong) : Triple<ULong, ULong, ULong> {

    // 1) special-case “pure 64-bit” dividend
    if (x1 == 0uL) {
        val q0 = x0 / y0
        val r0  = x0 % y0
        return Triple(0uL, q0, r0)
    } else {
        return udivMod128x64to128_stage2(x1, x0, y0)
    }
}

internal fun udivMod128x64to128_stage2(x1: ULong, x0: ULong, y0: ULong) : Triple<ULong, ULong, ULong> {
    require(y0 != 0uL) { "division by zero" }
    val v1 = y0 shr 32
    val v0 = y0 and 0xFFFF_FFFFuL
    if (v1 == 0uL) {
        val u3 = x1 shr 32
        val u2 = x1 and 0xFFFF_FFFFuL
        val u1 = x0 shr 32
        val u0 = x0 and 0xFFFF_FFFFuL

        val t3 = u3
        val q3 = t3 / v0; val r3 = t3 % v0
        val t2 = (r3 shl 32) or u2
        val q2 = t2 / v0; val r2 = t2 % v0
        val t1 = (r2 shl 32) or u1
        val q1 = t1 / v0; val r1 = t1 % v0
        val t0 = (r1 shl 32) or u0
        val q0 = t0 / v0; val r0 = t0 % v0

        val qDw1 = (q3 shl 32) or q2
        val qDw0 = (q1 shl 32) or q0
        val rDw0 = r0
        return Triple(qDw1, qDw0, rDw0)
    }

    val (d1, d0, e0) = knuthUdiv128x64to128(x1.toLong(), x0.toLong(), y0.toLong())
    return Triple(d1.toULong(), d0.toULong(), e0.toULong())
}

/**
 * Divide the 128-bit unsigned (xHigh<<64 | xLow) by the 64-bit unsigned y,
 * returning (quotientHigh64, quotientLow64, remainder64).
 */
internal fun knuthUdiv128x64to128(xHigh: Long, xLow: Long, y: Long): Triple<Long,Long,Long> {
    require(y != 0L) { "division by zero" }
    require((y ushr 32) != 0L)

    // Base and normalization shift
    val B = 1L shl 32
    val s = y.countLeadingZeroBits()

    // Normalize divisor
    val yNorm = y shl s
    val vn1   = (yNorm ushr 32) and MASK32L  // top 32 bits (MSB=1)
    val vn0   =  yNorm         and MASK32L  // low 32 bits

    // Normalize dividend (128-bit << s)
    val carryHigh = if (s == 0) 0L else xHigh ushr (64 - s)
    val hiNorm    = if (s == 0) xHigh else (xHigh shl s) or (xLow ushr (64 - s))
    val loNorm    = xLow  shl s

    // Carve out five 32-bit limbs u4…u0
    var un4 = carryHigh             and MASK32L
    var un3 = (hiNorm ushr 32)      and MASK32L
    var un2 =  hiNorm               and MASK32L
    var un1 = (loNorm ushr 32)      and MASK32L
    var un0 =  loNorm               and MASK32L


    // --- Estimate q2, q1, q0 ---
    var q2: Long
    run {
        val u32 = (un4 shl 32) or un3
        var qhat = unsignedDiv(u32, vn1)
        var rhat = unsignedMod(u32, vn1)
        if (unsignedCmp(qhat, B) >= 0 ||
            unsignedCmp(qhat * vn0, (rhat shl 32) + un2) > 0
        ) {
            qhat -= 1L; rhat += vn1
            if (unsignedLT(rhat, B) &&
                (unsignedCmp(qhat, B) >= 0 ||
                        unsignedCmp(qhat * vn0, (rhat shl 32) + un2) > 0)
            ) qhat -= 1L
        }
        // Multiply & subtract with full carry handling
        var borrow = 0L
        val p0 = qhat * vn0
        val p1 = qhat * vn1
        val p0_lo = p0 and MASK32L
        val p0_hi = p0 ushr 32
        val p1_lo_raw = (p1 and MASK32L) + p0_hi
        val carry1 = if (unsignedCmp(p1_lo_raw, B) >= 0) 1L else 0L
        val p1_mid = p1_lo_raw and MASK32L
        val p1_hi = (p1 ushr 32) + carry1
        // subtract at un2
        var t = un2 - p0_lo
        borrow = if (unsignedCmp(t, un2) > 0) 1L else 0L; un2 = t and MASK32L
        // subtract at un3
        t = un3 - p1_mid - borrow
        borrow = if (unsignedCmp(t, un3) > 0) 1L else 0L; un3 = t and MASK32L
        // subtract at un4
        t = un4 - p1_hi - borrow
        borrow = if (unsignedCmp(t, un4) > 0) 1L else 0L; un4 = t and MASK32L
        if (borrow != 0L) {
            qhat -= 1L
            // add back V aligned
            var carry = 0L
            t = un2 + vn0; carry = if (unsignedLT(t, un2)) 1L else 0L; un2 = t and MASK32L
            t = un3 + vn1 + carry; carry = if (unsignedLT(t, un3) || (carry == 1L && t == un3)) 1L else 0L; un3 = t and MASK32L
            un4 = (un4 + carry) and MASK32L
        }
        q2 = qhat
    }

    var q1: Long
    run {
        val u21 = (un3 shl 32) or un2
        var qhat = unsignedDiv(u21, vn1)
        var rhat = unsignedMod(u21, vn1)
        if (unsignedCmp(qhat, B) >= 0 ||
            unsignedCmp(qhat * vn0, (rhat shl 32) + un1) > 0
        ) {
            qhat -= 1L; rhat += vn1
            if (unsignedLT(rhat, B) &&
                (unsignedCmp(qhat, B) >= 0 ||
                        unsignedCmp(qhat * vn0, (rhat shl 32) + un1) > 0)
            ) qhat -= 1L
        }
        var borrow = 0L
        val p0 = qhat * vn0
        val p1 = qhat * vn1
        val p0_lo = p0 and MASK32L
        val p0_hi = p0 ushr 32
        val p1_lo_raw = (p1 and MASK32L) + p0_hi
        val carry1 = if (unsignedCmp(p1_lo_raw, B) >= 0) 1L else 0L
        val p1_mid = p1_lo_raw and MASK32L
        val p1_hi = (p1 ushr 32) + carry1
        var t = un1 - p0_lo
        borrow = if (unsignedCmp(t, un1) > 0) 1L else 0L; un1 = t and MASK32L
        t = un2 - p1_mid - borrow
        borrow = if (unsignedCmp(t, un2) > 0) 1L else 0L; un2 = t and MASK32L
        t = un3 - p1_hi - borrow
        borrow = if (unsignedCmp(t, un3) > 0) 1L else 0L; un3 = t and MASK32L
        if (borrow != 0L) {
            qhat -= 1L
            var carry = 0L
            t = un1 + vn0; carry = if (unsignedLT(t, un1)) 1L else 0L; un1 = t and MASK32L
            t = un2 + vn1 + carry; carry = if (unsignedLT(t, un2) || (carry == 1L && t == un2)) 1L else 0L; un2 = t and MASK32L
            un3 = (un3 + carry) and MASK32L
        }
        q1 = qhat
    }

    var q0: Long
    run {
        val u10 = (un2 shl 32) or un1
        var qhat = unsignedDiv(u10, vn1)
        var rhat = unsignedMod(u10, vn1)
        if (unsignedCmp(qhat, B) >= 0 ||
            unsignedCmp(qhat * vn0, (rhat shl 32) + un0) > 0
        ) {
            qhat -= 1L; rhat += vn1
            if (unsignedLT(rhat, B) &&
                (unsignedCmp(qhat, B) >= 0 ||
                        unsignedCmp(qhat * vn0, (rhat shl 32) + un0) > 0)
            ) qhat -= 1L
        }
        var borrow = 0L
        val p0 = qhat * vn0
        val p1 = qhat * vn1
        val p0_lo = p0 and MASK32L
        val p0_hi = p0 ushr 32
        val p1_lo_raw = (p1 and MASK32L) + p0_hi
        val carry1 = if (unsignedCmp(p1_lo_raw, B) >= 0) 1L else 0L
        val p1_mid = p1_lo_raw and MASK32L
        val p1_hi = (p1 ushr 32) + carry1
        var t = un0 - p0_lo
        borrow = if (unsignedCmp(t, un0) > 0) 1L else 0L; un0 = t and MASK32L
        t = un1 - p1_mid - borrow
        borrow = if (unsignedCmp(t, un1) > 0) 1L else 0L; un1 = t and MASK32L
        t = un2 - p1_hi - borrow
        borrow = if (unsignedCmp(t, un2) > 0) 1L else 0L; un2 = t and MASK32L
        if (borrow != 0L) {
            qhat -= 1L
            var carry = 0L
            t = un0 + vn0; carry = if (unsignedLT(t, un0)) 1L else 0L; un0 = t and MASK32L
            t = un1 + vn1 + carry; carry = if (unsignedLT(t, un1) || (carry == 1L && t == un1)) 1L else 0L; un1 = t and MASK32L
            un2 = (un2 + carry) and MASK32L
        }
        q0 = qhat
    }


    val qDw1 = q2
    val qDw0 = (q1 shl 32) or q0
    val rDw0 = ((un1 shl 32) or un0) ushr s
    return Triple(qDw1, qDw0, rDw0)
}

internal fun ucmp128(dw1X: Long, dw0X: Long, dw1Y: Long, dw0Y: Long): Int {
    val cmpDw1 = unsignedCmp(dw1X, dw1Y)
    if (cmpDw1 != 0)
        return cmpDw1
    return unsignedCmp(dw0X, dw0Y)
}

internal inline fun cmp32(x: Int, y: Int): Int {
    val d = x - y
    val lt = d ushr 31
    val gt = -d ushr 31
    return gt - lt
}

internal fun ucmp128ScalePow10(x1: Long, x0: Long, y1: Long, y0: Long, pow10: Int): Int {
    verify { pow10 in 1..<MIN_POW10_DIGIT_LEN_192 }
    val (p1, p0) = pow10_128(pow10)
    if (pow10 < MIN_POW10_DIGIT_LEN_128)
        return ucmp128_128x64(x1, x0, y1, y0, p0)
    verify { y1 == 0L }
    return ucmp128_128x64(x1, x0, p1, p0, y0)
}

internal fun ucmp128_64x64(x1: Long, x0: Long, y0: Long, z0: Long) : Int {
    val p1 = unsignedMulHi(y0, z0)
    val p0 = y0 * z0

    val cmp1 = unsignedCmp(x1, p1)
    val cmp0 = unsignedCmp(x0, p0)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    return cmp10
}

internal fun ucmp128_128x64(x1: Long, x0: Long, y1: Long, y0: Long, z0: Long) : Int {
    val pp00Hi = unsignedMulHi(y0, z0)
    val pp00Lo = y0 * z0
    val p0 = pp00Lo
    val cmp0 = unsignedCmp(x0, p0)

    val pp10Hi = unsignedMulHi(y1, z0)
    val pp10Lo = y1 * z0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)
    val cmp1 = unsignedCmp(x1, p1)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    val p2 = carry1 + pp10Hi
    val cmp210 = if (p2 != 0L) -1 else cmp10
    return cmp210
}

internal fun EQ128_64x64(x1: Long, x0: Long, y0: Long, z0: Long) : Boolean {
    val p1 = unsignedMulHi(y0, z0)
    val p0 = y0 * z0

    return ((x1 - p1) or (x0 - p0)) == 0L
}

internal fun EQ128_128x64(x1: Long, x0: Long, y1: Long, y0: Long, z0: Long) : Boolean {
    val pp00Hi = unsignedMulHi(y0, z0)
    val pp00Lo = y0 * z0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(y1, z0)
    val pp10Lo = y1 * z0
    val (carry1, p1) = sumU64(pp00Hi, pp10Lo)

    val p2 = carry1 + pp10Hi
    return ((x0 - p0) or (x1 - p1) or p2) == 0L
}

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U32_DIV_1E4 = 0x346DC5D7L
private const val S_U32_DIV_1E4 = 43


// FIXME
//  A binary int with z lo zero bits can have no more than z
//  trailing Zeros.
//  The cost of a short division DivDirect.divModX32 is the
//  same, regardless  of divisor size.
//  Consider whether or not this should be used to short-circuit
//  ... messing up the pipeline.

internal fun countTrailingZeroDigits32(n: Int): Int {
    verify { n > 0 }
    var d = n.toLong()
    var ntzd = 0

    var q: Long
    var r: Long
    var mask: Long

    q = (d * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
    r = d - (q * 1_0000)
    mask = ((d - 10000) or (-r)) shr 63
    d = (d and mask) or (q and mask.inv())
    ntzd += 4 and (mask.inv()).toInt()

    q = (d * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
    r = d - (q * 1_0000)
    mask = ((d - 10000) or (-r)) shr 63
    d = (d and mask) or (q and mask.inv())
    ntzd += 4 and (mask.inv()).toInt()

    q = (d * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
    r = d - (q * 100)
    mask = ((d - 100) or (-r)) shr 63
    d = (d and mask) or (q and mask.inv())
    ntzd += 2 and (mask.inv()).toInt()

    q = (d * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
    r = d - (q * 10)
    mask = ((d - 10) or (-r)) shr 63
    ntzd += 1 and (mask.inv()).toInt()

    return ntzd
}
