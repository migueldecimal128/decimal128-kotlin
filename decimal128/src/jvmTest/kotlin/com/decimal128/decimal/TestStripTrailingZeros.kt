package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import com.decimal128.bigint.Magia
import kotlin.test.assertEquals
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test

private const val M_U64_DIV_1E16 = 0x39A5652FB1137857uL
private const val S_U64_DIV_1E16 = 51
private const val oneE16 = 1_0000_0000_0000_0000uL

private const val M_U64_DIV_1E8 = 0xABCC77118461CEFDuL
private const val S_U64_DIV_1E8 = 26
private const val oneE8 = 1_0000_0000uL

private const val M_1E9_DIV_1E4 = 879_609_303uL
private const val S_1E9_DIV_1E4 = 43
private const val oneE4 = 1_0000uL

private const val M_U32_DIV_1E1 = 0xCCCCCCCDuL
private const val S_U32_DIV_1E1 = 35


class TestStripTrailingZeros {

    val verbose = false


    @Test
    fun testStrip64() {
        test1(1234567890uL)
        test1(10000000000uL)
        test1(10000000000uL)
    }

    val TEST_COUNT = 1_000

    @Test
    fun testRandom64() {
        for (i in 0..<TEST_COUNT) {
            val rnd = Random.nextULong()
            if (rnd == 0uL)
                continue
            test1(Random.nextULong())
        }
        for (i in 0..<TEST_COUNT) {
            val base = Random.nextULong() and 0x0FFF_FFFFuL
            if (base == 0uL)
                continue
            val pow = pow10(Random.nextInt(10))
            test1(base * pow)
        }
        for (i in 0..<TEST_COUNT) {
            val base = Random.nextULong() and 0x0FFF_FFFFuL
            if (base == 0uL)
                continue
            val pow = pow10(Random.nextInt(10))
            test1(base * pow)
        }
    }

    @Test
    fun testProblemChild() {
        test1(BigInt.from("980000000000000000000"))
    }

    @Test
    fun testRandom128() {
        for (i in 0..<TEST_COUNT) {
            val hi = BigInt.fromRandom(Random.nextInt(129))
            if (hi.isZero())
                continue
            test1(hi)
        }
        for (i in 0..<TEST_COUNT) {
            val hi = BigInt.from(Random.nextInt(100)) * BigInt.from(10).pow(Random.nextInt(20))
            if (hi.magnitudeBitLen() > 128 || hi.isZero())
                continue
            test1(hi)

        }
    }

    fun test1(hi: BigInt) {
        if (verbose)
            println("hi:$hi")
        val dw0 = hi.toULong()
        val dw1 = hi.extractULongAtBitIndex(64)

        val ntzHi = ntzBigInt(hi)
        val (q1:ULong, q0:ULong, ntz: Int) = ntzdU128(dw1, dw0)

        assertEquals(ntzHi, ntz)

        val hiReduced = hi / BigInt.from(10).pow(ntz)
        val r0 = hiReduced.toULong()
        val r1 = hiReduced.extractULongAtBitIndex(64)

        assertEquals(r1, q1)
        assertEquals(r0, q0)
    }

    fun ntzBigInt(hi: BigInt): Int {
        val s = "$hi"
        var ntz = 0
        for (i in s.lastIndex downTo 0)
            if (s[i] == '0')
                ++ntz
            else
                return ntz
        return 0
    }

    fun test1(v: ULong) {
        if (verbose)
            println("v:$v")

        val (q1, tzCount1) = stripTrailingZeros64_1(v)
        if (verbose)
            println(" q1:$q1 tzCount1:$tzCount1")

        val (qNtz, tzCountNtz) = ntzdU64(v)
        if (verbose)
            println(" qNtz:$qNtz tzCountNtz:$tzCountNtz")

        assertEquals(q1, qNtz)
        assertEquals(tzCount1, tzCountNtz)

    }

    // 0xABCC77118461CEFD s = 26

    fun stripTrailingZeros64_1(dw: ULong): Pair<ULong, Int> {
        check (dw != 0uL)
        var t = dw
        var ntz = 0
        while ((t % 10000uL) == 0uL) {
            t /= 10000uL
            ntz += 4
        }
        while ((t % 10uL) == 0uL) {
            t /= 10uL
            ntz += 1
        }
        return Pair(t, ntz)
    }

    fun pow10(n: Int): ULong {
        var p = 1uL
        for (i in 1..n)
            p *= 10uL
        return p
    }

    fun ntzdU32(r0: UInt): Pair<UInt, Int> {
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

    fun ntzdU64(r0: ULong): Pair<ULong, Int> {
        var r = r0
        var ntz = 0

        if (r >= 1_0000_0000_0000_0000uL && r % 1_0000_0000_0000_0000uL == 0uL) {
            r /= 1_0000_0000_0000_0000uL
            ntz += 16
        }
        if (r >= 1_0000_0000uL && r % 1_0000_0000uL == 0uL) {
            r /= 1_0000_0000uL
            ntz += 8
        }
        if (r >= 1_0000uL && r % 1_0000uL == 0uL) {
            r /= 1_0000u
            ntz += 4
        }
        if (r >= 100uL && r % 100uL == 0uL) {
            r /= 100uL
            ntz += 2
        }
        if (r >= 10uL && r % 10uL == 0uL) {
            r /= 10u
            ntz += 1
        }

        return r to ntz
    }

    fun ntzdU128_0(r1: ULong, r0: ULong): Triple<ULong, ULong, Int> {
        if (r1 == 0uL) {
            val (q0, ntz) = ntzdU64(r0)
            return Triple(0uL, q0, ntz)
        }
        var ntz = 0
        val tmp = intArrayOf(r0.toInt(), (r0 shr 32).toInt(), r1.toInt(), (r1 shr 32).toInt())
        var tmpLen = if (tmp[3] == 0) 3 else 4
        do {
            val packed = Magia.mutateBarrettDivBy1e9(tmp, tmpLen)
            val r = packed.toUInt()
            tmpLen = (packed shr 32).toInt()
            if (r != 0u) {
                val (rPrime, ntzTail) = ntzdU32(r)
                ntz += ntzTail
                if (ntz == 0)
                    return Triple(r1, r0, 0)
                Magia.mutateFmaPow10(tmp, 9 - ntzTail, rPrime)
                val r1Prime = (tmp[3].toULong() shl 32) or (tmp[2].toUInt().toULong())
                val r0Prime = (tmp[1].toULong() shl 32) or (tmp[0].toUInt().toULong())
                return Triple(r1Prime, r0Prime, ntz)
            }
            ntz += 9
        } while (tmpLen > 2)
        val r0Prime = (tmp[1].toULong() shl 32) or (tmp[0].toUInt().toULong())
        val (rPrime, ntzTail) = ntzdU64(r0Prime)
        ntz += ntzTail
        return Triple(0uL, rPrime, ntz)
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