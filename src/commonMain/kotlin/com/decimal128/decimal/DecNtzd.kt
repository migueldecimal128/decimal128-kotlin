@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.Magia

/**
 * Number of Trailing Zero Digits
 */
object DecNtzd {

    private inline fun ntzdU32(r0: UInt): Pair<UInt, Int> {
        var r = r0
        var ntz = 0

        if (r >= 1_0000_0000u && r % 1_0000_0000u == 0u) {
            r /= 1_0000_0000u
            ntz += 8
        }
        if (r >= 1_0000u && r % 1_0000u == 0u) {
            r /= 1_0000u
            ntz += 4
        }
        if (r >= 100u && r % 100u == 0u) {
            r /= 100u
            ntz += 2
        }
        if (r >= 10u && r % 10u == 0u) {
            r /= 10u
            ntz += 1
        }

        return r to ntz
    }

    private inline fun ntzdU64(d0: ULong): Pair<ULong, Int> {
        var t: ULong = d0
        var ntzd = 0

        if (t >= 1_0000_0000_0000_0000uL && t % 1_0000_0000_0000_0000uL == 0uL) {
            t /= 1_0000_0000_0000_0000uL
            ntzd += 16
        }
        if (t >= 1_0000_0000uL && t % 1_0000_0000uL == 0uL) {
            t /= 1_0000_0000uL
            ntzd += 8
        }
        if (t >= 1_0000uL && t % 1_0000uL == 0uL) {
            t /= 1_0000u
            ntzd += 4
        }
        if (t >= 100uL && t % 100uL == 0uL) {
            t /= 100uL
            ntzd += 2
        }
        if (t >= 10uL && t % 10uL == 0uL) {
            t /= 10u
            ntzd += 1
        }

        return Pair(t, ntzd)
    }

    fun ntzdU128(d1: ULong, d0: ULong): Triple<ULong, ULong, Int> {
        var t1 = d1
        var t0 = d0
        var ntzd = 0
        retTriple@
        do {
            if (t1 != 0uL) {
                val tmp = intArrayOf(t0.toInt(), (t0 shr 32).toInt(), t1.toInt(), (t1 shr 32).toInt())
                var tmpLen = if (tmp[3] == 0) 3 else 4
                do {
                    val packed = Magia.mutateBarrettDivBy1e9(tmp, tmpLen)
                    val r = packed.toUInt()
                    tmpLen = (packed shr 32).toInt()
                    if (r != 0u) {
                        val (rPrime, ntzTail) = ntzdU32(r)
                        ntzd += ntzTail
                        if (ntzd == 0)
                            break@retTriple
                        Magia.mutateFmaPow10(tmp, 9 - ntzTail, rPrime)
                        t1 = (tmp[3].toULong() shl 32) or (tmp[2].toUInt().toULong())
                        t0 = (tmp[1].toULong() shl 32) or (tmp[0].toUInt().toULong())
                        break@retTriple
                    }
                    ntzd += 9
                } while (tmpLen > 2)
                t0 = (tmp[1].toULong() shl 32) or (tmp[0].toUInt().toULong())
                check(ntzd == 9 || ntzd == 18)
            }
            // arrive here when the high word is zero
            // either because it started off zero .OR.
            // because we reduced 9 or 18 zeros
            val (q0, ntzdTail) = ntzdU64(t0)
            t1 = 0uL
            t0 = q0
            ntzd += ntzdTail
        } while (false)
        return Triple(t1, t0, ntzd)
    }

}