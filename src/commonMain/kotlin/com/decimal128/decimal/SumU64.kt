@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal inline fun sumU64(sum: Pentad, dwA:Long, dwB:Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    sum.dw0 = sumAB
    sum.dw1 = carryAB
}

internal inline fun sumU64(sum: Pentad, dwA:Long, dwB:Long, dwC: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumABC = sumAB + dwC
    val carryABC = carryAB + if (unsignedLT(sumABC, sumAB)) 1L else 0L

    sum.dw0 = sumABC
    sum.dw1 = carryABC
}

internal inline fun sumU64(sum: Pentad,
                           dwA: Long, dwB: Long, dwC: Long, dwD: Long) {
    val sumAB = dwA + dwB
    val carryAB = if (unsignedLT(sumAB, dwA)) 1L else 0L

    val sumCD = dwC + dwD
    val carryCD = if (unsignedLT(sumCD, dwC)) 1L else 0L

    val sumABCD = sumAB + sumCD
    val carryABCD = carryAB + carryCD + if (unsignedLT(sumABCD, sumAB)) 1L else 0L

    sum.dw0 = sumABCD
    sum.dw1 = carryABCD
}

internal inline fun sumU64(sum: Pentad,
                           dwA: Long, dwB: Long, dwC: Long, dwD: Long, dwE: Long) {
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


internal /*inline*/ fun sumU64(sum: Pentad,
                               dwA: Long, dwB: Long, dwC: Long, dwD: Long,
                               dwE: Long, dwF: Long) {
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


internal /*inline*/ fun sumU64(sum: Pentad,
                               dwA: Long, dwB: Long, dwC: Long, dwD: Long,
                               dwE: Long, dwF: Long, dwG: Long) {
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


internal /*inline*/ fun sumU64(sum: Pentad,
                               dwA: Long, dwB: Long, dwC: Long, dwD: Long,
                               dwE: Long, dwF: Long, dwG: Long, dwH: Long) {
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


internal /*inline*/ fun sumU64(sum: Pentad,
                               dwA: Long, dwB: Long, dwC: Long, dwD: Long,
                               dwE: Long, dwF: Long, dwG: Long, dwH: Long, dwI: Long) {
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
internal /*inline*/ fun diffU64(pentad: Pentad, dwA:Long, dwZ:Long) {
    val diffAZ = dwA - dwZ
    val borrowAZ = if (unsignedCmp(diffAZ, dwA) > 0) 1L else 0L

    pentad.dw1 = borrowAZ
    pentad.dw0 = diffAZ
}


internal /*inline*/ fun diffU64withBorrow(pentad: Pentad, dwA: Long, dwB: Long, borrowIn: Long) {
    verify { borrowIn in 0..1 }
    // First subtract b from a:
    val diffAB = dwA - dwB
    val borrow1 = if (unsignedLT(dwA, dwB)) 1L else 0L
    val totalDiff = diffAB - borrowIn
    val totalBorrow = if (unsignedLT(diffAB, borrowIn)) 1L else borrow1

    pentad.dw1 = totalBorrow
    pentad.dw0 = totalDiff
}


internal inline fun sumU128(sum: Pentad, x1: Long, x0: Long, y1: Long, y0: Long): Pentad {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1 + y1

    sum.dw0 = s0
    sum.dw1 = s1
    return sum
}


internal inline fun sumU128U64(sum: Pentad,
                               x1: Long, x0: Long, y0: Long): Pentad {
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val s1 = carry0 + x1
    sum.dw0 = s0
    sum.dw1 = s1
    return sum
}

internal inline fun umul128x128to128(prod: Pentad,
                                     x1: Long, x0: Long, y1: Long, y0: Long) {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val pp10Lo = x1 * y0
    val pp01Lo = x0 * y1

    val p0 = pp00Lo
    val p1 = pp00Hi + pp10Lo + pp01Lo

    prod.dw0 = p0
    prod.dw1 = p1
}

internal fun umul128xPow10to128(pentad: Pentad,
                                dw1: Long, dw0: Long, pow10: Int) {
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val pow10Dw1 = POW10[pow10Offset + 1]
    val pow10Dw0 = POW10[pow10Offset    ]
    umul128x128to128(pentad, dw1, dw0, pow10Dw1, pow10Dw0)
}

internal /*inline*/ fun umul192x64to192(pentad: Pentad,
                                        x2: Long, x1: Long, x0: Long, y0: Long): Pentad {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0

    val pp20Lo = x2 * y0
    val p2 = carry1 + pp10Hi + pp20Lo

    pentad.dw0 = p0
    pentad.dw1 = p1
    pentad.dw2 = p2
    return pentad
}


internal /*inline*/ fun umul128x128to192(pentad: Pentad,
                                         x1: Long, x0: Long, y1:Long, y0: Long): Pentad {
    val pp00Hi = unsignedMulHi(x0, y0)
    val pp00Lo = x0 * y0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, y1)
    val pp01Lo = x0 * y1
    val pp10Hi = unsignedMulHi(x1, y0)
    val pp10Lo = x1 * y0
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0

    val pp11Lo = x1 * y1
    val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo

    pentad.dw0 = p0
    pentad.dw1 = p1
    pentad.dw2 = p2
    return pentad
}

internal /*inline*/ fun usqr96to192(pentad: Pentad, x1: Long, x0: Long) {
    val pp00Hi = unsignedMulHi(x0, x0)
    val pp00Lo = x0 * x0
    val p0 = pp00Lo

    val pp01Hi = unsignedMulHi(x0, x1)
    val pp01Lo = x0 * x1
    val pp10Hi = pp01Hi
    val pp10Lo = pp01Lo
    sumU64(pentad, pp00Hi, pp01Lo, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0

    val pp11Lo = x1 * x1
    val p2 = carry1 + pp01Hi + pp10Hi + pp11Lo

    pentad.dw2 = p2
    pentad.dw1 = p1
    pentad.dw0 = p0
}

internal fun ucmp128(dw1X: Long, dw0X: Long,
                     dw1Y: Long, dw0Y: Long): Int {
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

internal fun ucmp128ScalePow10(x1: Long, x0: Long,
                               y1: Long, y0: Long, pow10: Int, pentad: Pentad): Int {
    verify { pow10 in 1..<MIN_POW10_DIGIT_LEN_192 }
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val p1 = POW10[pow10Offset + 1]
    val p0 = POW10[pow10Offset    ]
    if (pow10 < MIN_POW10_DIGIT_LEN_128)
        return ucmp128_128x64(x1, x0, y1, y0, p0, pentad)
    verify { y1 == 0L }
    return ucmp128_128x64(x1, x0, p1, p0, y0, pentad)
}

internal fun ucmp128_128x64(x1: Long, x0: Long,
                            y1: Long, y0: Long, z0: Long, pentad: Pentad) : Int {
    val pp00Hi = unsignedMulHi(y0, z0)
    val pp00Lo = y0 * z0
    val p0 = pp00Lo
    val cmp0 = unsignedCmp(x0, p0)

    val pp10Hi = unsignedMulHi(y1, z0)
    val pp10Lo = y1 * z0
    sumU64(pentad, pp00Hi, pp10Lo)
    val carry1 = pentad.dw1
    val p1 = pentad.dw0

    val cmp1 = unsignedCmp(x1, p1)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    val p2 = carry1 + pp10Hi
    val cmp210 = if (p2 != 0L) -1 else cmp10
    return cmp210
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
