@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import kotlin.math.max


private const val BASE_POW10_192 = (POW10_64_COUNT + POW10_128_COUNT) * 2
private const val BASE_POW10_256 = BASE_POW10_192 + POW10_192_COUNT * 3

// POW10_DWORD_COUNT = 215
private const val POW10_DWORD_COUNT = (BASE_POW10_256 + POW10_256_COUNT * 4)

internal const val BARRETT_POW10_MU_OFFSET = POW10_DWORD_COUNT
internal const val BARRETT_POW10_MAXX = 10

internal const val POW5_64_OFFSET = BARRETT_POW10_MU_OFFSET + BARRETT_POW10_MAXX
internal const val POW5_64_MAXX = 28

internal const val BARRETT_POW5_MU_OFFSET = POW5_64_OFFSET + POW5_64_MAXX
internal const val BARRETT_POW5_MAXX = 14

internal const val MAGIC_POW10_M_OFFSET = BARRETT_POW5_MU_OFFSET + BARRETT_POW5_MAXX
internal const val MAGIC_POW10_MAXX = 20

private const val TOTAL_ALLOCATION = MAGIC_POW10_M_OFFSET + MAGIC_POW10_MAXX

private const val POW10_BCE = 0xFF // POW10 Bounds Check Elimination

internal val POW10 = LongArray(TOTAL_ALLOCATION)
private val POW10_BIT_LEN_MINUS_1 = ByteArray(MAXX_DIGIT_LEN)

/*
// minBitCount:0  maxBitCount:64
1L, // 1 (0)
0x000000000000000AuL.toLong(), // 10 (10**1)
0x0000000000000064uL.toLong(), // 100 (10**2)
0x00000000000003E8uL.toLong(), // 1000 (10**3)
0x0000000000002710uL.toLong(), // 10000 (10**4)
0x00000000000186A0uL.toLong(), // 100000 (10**5)
0x00000000000F4240uL.toLong(), // 1000000 (10**6)
0x0000000000989680uL.toLong(), // 10000000 (10**7)
0x0000000005F5E100uL.toLong(), // 100000000 (10**8)
0x000000003B9ACA00uL.toLong(), // 1000000000 (10**9)
0x00000002540BE400uL.toLong(), // 10000000000 (10**10)
0x000000174876E800uL.toLong(), // 100000000000 (10**11)
0x000000E8D4A51000uL.toLong(), // 1000000000000 (10**12)
0x000009184E72A000uL.toLong(), // 10000000000000 (10**13)
0x00005AF3107A4000uL.toLong(), // 100000000000000 (10**14)
0x00038D7EA4C68000uL.toLong(), // 1000000000000000 (10**15)
0x002386F26FC10000uL.toLong(), // 10000000000000000 (10**16)
0x016345785D8A0000uL.toLong(), // 100000000000000000 (10**17)
0x0DE0B6B3A7640000uL.toLong(), // 1000000000000000000 (10**18)
0x8AC7230489E80000uL.toLong(), // 10000000000000000000 (10**19)
// minBitCount:64  maxBitCount:128
0x6BC75E2D63100000uL.toLong(), 0x0000000000000005uL.toLong(), // 100000000000000000000 (10**20)
0x35C9ADC5DEA00000uL.toLong(), 0x0000000000000036uL.toLong(), // 1000000000000000000000 (10**21)
0x19E0C9BAB2400000uL.toLong(), 0x000000000000021EuL.toLong(), // 10000000000000000000000 (10**22)
0x02C7E14AF6800000uL.toLong(), 0x000000000000152DuL.toLong(), // 100000000000000000000000 (10**23)
0x1BCECCEDA1000000uL.toLong(), 0x000000000000D3C2uL.toLong(), // 1000000000000000000000000 (10**24)
0x161401484A000000uL.toLong(), 0x0000000000084595uL.toLong(), // 10000000000000000000000000 (10**25)
0xDCC80CD2E4000000uL.toLong(), 0x000000000052B7D2uL.toLong(), // 100000000000000000000000000 (10**26)
0x9FD0803CE8000000uL.toLong(), 0x00000000033B2E3CuL.toLong(), // 1000000000000000000000000000 (10**27)
0x3E25026110000000uL.toLong(), 0x00000000204FCE5EuL.toLong(), // 10000000000000000000000000000 (10**28)
0x6D7217CAA0000000uL.toLong(), 0x00000001431E0FAEuL.toLong(), // 100000000000000000000000000000 (10**29)
0x4674EDEA40000000uL.toLong(), 0x0000000C9F2C9CD0uL.toLong(), // 1000000000000000000000000000000 (10**30)
0xC0914B2680000000uL.toLong(), 0x0000007E37BE2022uL.toLong(), // 10000000000000000000000000000000 (10**31)
0x85ACEF8100000000uL.toLong(), 0x000004EE2D6D415BuL.toLong(), // 100000000000000000000000000000000 (10**32)
0x38C15B0A00000000uL.toLong(), 0x0000314DC6448D93uL.toLong(), // 1000000000000000000000000000000000 (10**33)
0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(), // 10000000000000000000000000000000000 (10**34)
0x2B878FE800000000uL.toLong(), 0x0013426172C74D82uL.toLong(), // 100000000000000000000000000000000000 (10**35)
0xB34B9F1000000000uL.toLong(), 0x00C097CE7BC90715uL.toLong(), // 1000000000000000000000000000000000000 (10**36)
0x00F436A000000000uL.toLong(), 0x0785EE10D5DA46D9uL.toLong(), // 10000000000000000000000000000000000000 (10**37)
0x098A224000000000uL.toLong(),
0x4B3B4CA85A86C47AuL.toLong(), // 100000000000000000000000000000000000000 (10**38)
// minBitCount:128  maxBitCount:192
0x5F65568000000000uL.toLong(), 0xF050FE938943ACC4uL.toLong(),
0x0000000000000002uL.toLong(), // 1000000000000000000000000000000000000000 (10**39)
0xB9F5610000000000uL.toLong(), 0x6329F1C35CA4BFABuL.toLong(),
0x000000000000001DuL.toLong(), // 10000000000000000000000000000000000000000 (10**40)
0x4395CA0000000000uL.toLong(), 0xDFA371A19E6F7CB5uL.toLong(),
0x0000000000000125uL.toLong(), // 100000000000000000000000000000000000000000 (10**41)
0xA3D9E40000000000uL.toLong(), 0xBC627050305ADF14uL.toLong(),
0x0000000000000B7AuL.toLong(), // 1000000000000000000000000000000000000000000 (10**42)
0x6682E80000000000uL.toLong(), 0x5BD86321E38CB6CEuL.toLong(),
0x00000000000072CBuL.toLong(), // 10000000000000000000000000000000000000000000 (10**43)
0x011D100000000000uL.toLong(), 0x9673DF52E37F2410uL.toLong(),
0x0000000000047BF1uL.toLong(), // 100000000000000000000000000000000000000000000 (10**44)
0x0B22A00000000000uL.toLong(), 0xE086B93CE2F768A0uL.toLong(),
0x00000000002CD76FuL.toLong(), // 1000000000000000000000000000000000000000000000 (10**45)
0x6F5A400000000000uL.toLong(), 0xC5433C60DDAA1640uL.toLong(),
0x0000000001C06A5EuL.toLong(), // 10000000000000000000000000000000000000000000000 (10**46)
0x5986800000000000uL.toLong(), 0xB4A05BC8A8A4DE84uL.toLong(),
0x00000000118427B3uL.toLong(), // 100000000000000000000000000000000000000000000000 (10**47)
0x7F41000000000000uL.toLong(), 0x0E4395D69670B12BuL.toLong(),
0x00000000AF298D05uL.toLong(), // 1000000000000000000000000000000000000000000000000 (10**48)
0xF88A000000000000uL.toLong(), 0x8EA3DA61E066EBB2uL.toLong(),
0x00000006D79F8232uL.toLong(), // 10000000000000000000000000000000000000000000000000 (10**49)
0xB564000000000000uL.toLong(), 0x926687D2C40534FDuL.toLong(),
0x000000446C3B15F9uL.toLong(), // 100000000000000000000000000000000000000000000000000 (10**50)
0x15E8000000000000uL.toLong(), 0xB8014E3BA83411E9uL.toLong(),
0x000002AC3A4EDBBFuL.toLong(), // 1000000000000000000000000000000000000000000000000000 (10**51)
0xDB10000000000000uL.toLong(), 0x300D0E549208B31AuL.toLong(),
0x00001ABA4714957DuL.toLong(), // 10000000000000000000000000000000000000000000000000000 (10**52)
0x8EA0000000000000uL.toLong(), 0xE0828F4DB456FF0CuL.toLong(),
0x00010B46C6CDD6E3uL.toLong(), // 100000000000000000000000000000000000000000000000000000 (10**53)
0x9240000000000000uL.toLong(), 0xC51999090B65F67DuL.toLong(),
0x000A70C3C40A64E6uL.toLong(), // 1000000000000000000000000000000000000000000000000000000 (10**54)
0xB680000000000000uL.toLong(), 0xB2FFFA5A71FBA0E7uL.toLong(),
0x006867A5A867F103uL.toLong(), // 10000000000000000000000000000000000000000000000000000000 (10**55)
0x2100000000000000uL.toLong(), 0xFDFFC78873D4490DuL.toLong(),
0x04140C78940F6A24uL.toLong(), // 100000000000000000000000000000000000000000000000000000000 (10**56)
0x4A00000000000000uL.toLong(), 0xEBFDCB54864ADA83uL.toLong(),
0x28C87CB5C89A2571uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000 (10**57)
// minBitCount:192  maxBitCount:256
0xE400000000000000uL.toLong(), 0x37E9F14D3EEC8920uL.toLong(), 0x97D4DF19D6057673uL.toLong(),
0x0000000000000001uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000 (10**58)
0xE800000000000000uL.toLong(), 0x2F236D04753D5B48uL.toLong(), 0xEE50B7025C36A080uL.toLong(),
0x000000000000000FuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000 (10**59)
0x1000000000000000uL.toLong(), 0xD762422C946590D9uL.toLong(), 0x4F2726179A224501uL.toLong(),
0x000000000000009FuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000 (10**60)
0xA000000000000000uL.toLong(), 0x69D695BDCBF7A87AuL.toLong(), 0x17877CEC0556B212uL.toLong(),
0x0000000000000639uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000 (10**61)
0x4000000000000000uL.toLong(), 0x2261D969F7AC94CAuL.toLong(), 0xEB4AE1383562F4B8uL.toLong(),
0x0000000000003E3AuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000 (10**62)
0x8000000000000000uL.toLong(), 0x57D27E23ACBDCFE6uL.toLong(), 0x30ECCC3215DD8F31uL.toLong(),
0x0000000000026E4DuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000 (10**63)
0x0000000000000000uL.toLong(), 0x6E38ED64BF6A1F01uL.toLong(), 0xE93FF9F4DAA797EDuL.toLong(),
0x0000000000184F03uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000 (10**64)
0x0000000000000000uL.toLong(), 0x4E3945EF7A25360AuL.toLong(), 0x1C7FC3908A8BEF46uL.toLong(),
0x0000000000F31627uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000 (10**65)
0x0000000000000000uL.toLong(), 0x0E3CBB5AC5741C64uL.toLong(), 0x1CFDA3A5697758BFuL.toLong(),
0x00000000097EDD87uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000 (10**66)
0x0000000000000000uL.toLong(), 0x8E5F518BB6891BE8uL.toLong(), 0x21E864761EA97776uL.toLong(),
0x000000005EF4A747uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000 (10**67)
0x0000000000000000uL.toLong(), 0x8FB92F75215B1710uL.toLong(), 0x5313EC9D329EAAA1uL.toLong(),
0x00000003B58E88C7uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000 (10**68)
0x0000000000000000uL.toLong(), 0x9D3BDA934D8EE6A0uL.toLong(), 0x3EC73E23FA32AA4FuL.toLong(),
0x00000025179157C9uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000 (10**69)
0x0000000000000000uL.toLong(), 0x245689C107950240uL.toLong(), 0x73C86D67C5FAA71CuL.toLong(),
0x00000172EBAD6DDCuL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000 (10**70)
0x0000000000000000uL.toLong(), 0x6B61618A4BD21680uL.toLong(), 0x85D4460DBBCA8719uL.toLong(),
0x00000E7D34C64A9CuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000 (10**71)
0x0000000000000000uL.toLong(), 0x31CDCF66F634E100uL.toLong(), 0x3A4ABC8955E946FEuL.toLong(),
0x000090E40FBEEA1DuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000000 (10**72)
0x0000000000000000uL.toLong(), 0xF20A1A059E10CA00uL.toLong(), 0x46EB5D5D5B1CC5EDuL.toLong(),
0x0005A8E89D752524uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000000 (10**73)
0x0000000000000000uL.toLong(), 0x746504382CA7E400uL.toLong(), 0xC531A5A58F1FBB4BuL.toLong(),
0x003899162693736AuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000000 (10**74)
0x0000000000000000uL.toLong(), 0x8BF22A31BE8EE800uL.toLong(), 0xB3F07877973D50F2uL.toLong(),
0x0235FADD81C2822BuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000000000 (10**75)
0x0000000000000000uL.toLong(), 0x7775A5F171951000uL.toLong(), 0x0764B4ABE8652979uL.toLong(),
0x161BCCA7119915B5uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000000000 (10**76)
0x0000000000000000uL.toLong(), 0xAA987B6E6FD2A000uL.toLong(), 0x49EF0EB713F39EBEuL.toLong(),
0xDD15FE86AFFAD912uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000000000 (10**77)
// we can represent all 77 decimal digit numbers
*/

private fun initTables() {
    var hiPow10 = BigInt.ONE
    var j = 0
    for (i in 0..<MAXX_DIGIT_LEN) {
        POW10_BIT_LEN_MINUS_1[i] = (hiPow10.magnitudeBitLen() - 1).toByte()
        for (dw in hiPow10.magnitudeToLittleEndianLongArray())
            POW10[j++] = dw
        if (i < MIN_POW10_DIGIT_LEN_128)
            ++j
        hiPow10 *= 10
    }

    // initialize powers of 5 that fit in 64 bits
    POW10[POW5_64_OFFSET] = 1L
    for (i in 1..<POW5_64_MAXX)
        POW10[POW5_64_OFFSET + i] = POW10[POW5_64_OFFSET + i - 1] * 5L

    // initialize Barrett division
    val hiTwoPow64 = BigInt.ONE.shl(64)
    hiPow10 = BigInt.ONE

    // mu for 10**0 == 0 ... used for checking div by 1 case
    for (i in 1..<BARRETT_POW10_MAXX) {
        hiPow10 *= 10
        val mu10 = hiTwoPow64 / hiPow10
        POW10[BARRETT_POW10_MU_OFFSET + i] = mu10.toLong()

        val pow5 = hiPow10 ushr i
        val mu5 = hiTwoPow64 / pow5
        POW10[BARRETT_POW5_MU_OFFSET + i] = mu5.toLong()
    }
    for (i in 1..<BARRETT_POW5_MAXX) {
        val t = BigInt.fromUnsigned(POW10[POW5_64_OFFSET + i])
        val mu = hiTwoPow64 / t
        POW10[BARRETT_POW5_MU_OFFSET + i] = mu.toLong()
    }
    // initialization of Magic multipliers M is in DivMagic
}

private val _nada = initTables()

internal fun pow10BitLen(pow10: Int): Int {
    return (POW10_BIT_LEN_MINUS_1[pow10].toInt() and 0xFF) + 1
}

internal fun pow10Offset(pow10: Int): Int {
    /*
    val p = pow10 - 1
    val t = (p * 431) ushr 13
    val i = p - 19 * t
    val offset = 1 + 19 * (t * (t + 1) / 2) + i * (t + 1)
    val mask = -pow10 shr 31
    return offset and mask
     */
    return POW10_BCE and when {
        pow10 < MIN_POW10_DIGIT_LEN_192 -> 2 * pow10
        pow10 < MIN_POW10_DIGIT_LEN_256 -> BASE_POW10_192 + 3 * (pow10 - MIN_POW10_DIGIT_LEN_192)
        else -> BASE_POW10_256 + 4 * (pow10 - MIN_POW10_DIGIT_LEN_256)
    }
}

internal fun pow10_64(pow10: Int): Long {
    verify { pow10 < MIN_POW10_DIGIT_LEN_128 }
    return POW10[(2*pow10) and POW10_BCE]
}

internal inline fun pow10_128_dw0(pow10: Int): Long {
    return POW10[(2*pow10) and POW10_BCE]
}

internal inline fun pow10_128_dw1(pow10: Int): Long {
    return POW10[(2*pow10 + 1) and POW10_BCE]
}

internal inline fun pow10_128(pow10: Int): Pair<Long, Long> {
    verify { pow10 < MIN_POW10_DIGIT_LEN_192 }
    val offset = (2*pow10) and POW10_BCE
    return POW10[offset + 1] to POW10[offset]
}


internal fun calcMinDigitLenForBitLen(bitLen: Int): Int {
    return (((bitLen - 1) * 1233) ushr 12) + 1
}

internal fun calcMaxDigitLenForBitLen(bitLen: Int): Int {
    return ((bitLen * 19729) ushr 16) + 1
}

/**
 * Calcs the number of decimal digits in the unsigned long.
 * note that the value 0L returns 0
 */

internal fun calcDigitLen64(dw0: Long): Int {
    val bitLen = 64 - dw0.countLeadingZeroBits()
    return calcDigitLen64(bitLen, dw0)
}

internal fun calcDigitLen64(bitLen: Int, dw0: Long): Int {
    // this formula of
    // ((bitLen * 1233) ushr 12)
    // usually underestimates by 1
    // it is simpler than (((bitLen - 1) * 1233) ushr 12) + 1
    // more importantly, it avoids boundary condition issues
    // for 128 and 192 bits where they cross from 2->3->4 limbs
    val digitCountEstimate = (bitLen * 1233) ushr 12
    val p0 = POW10[(digitCountEstimate shl 1) and POW10_BCE]
    return digitCountEstimate + 1 - (unsignedCmp(dw0, p0) ushr 31)
}

internal fun calcDigitLen128(dw1: Long, dw0: Long): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    return calcDigitLen128(bitLen, dw1, dw0)
}

internal fun calcStealPackedLengths128(dw1: Long, dw0: Long): Int {
    val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
    val nlz1 = dw1.countLeadingZeroBits()
    val nlz0 = dw0.countLeadingZeroBits()
    val bitLen = 128 - nlz1 - (nlz0 and dw1IsZeroMask)

    val digitCountEstimate = (bitLen * 1233) ushr 12
    val dw1T = dw1 xor Long.MIN_VALUE
    val dw0T = dw0 xor Long.MIN_VALUE
    val pow10Offset = digitCountEstimate shl 1
    val p1 = POW10[(pow10Offset + 1) and POW10_BCE] xor Long.MIN_VALUE
    val p0 = POW10[(pow10Offset + 0) and POW10_BCE] xor Long.MIN_VALUE

    val digitLen = digitCountEstimate +
            if ((dw1T > p1) or ((dw1T == p1) and (dw0T >= p0))) 1 else 0

    return (digitLen shl STEAL_DIGITLEN_SHIFT) or (bitLen shl STEAL_BITLEN_SHIFT)
}

internal fun calcDigitLen128(bitLen: Int, dw1: Long, dw0: Long): Int {
    val digitCountEstimate = (bitLen * 1233) ushr 12
    val dw1T = dw1 xor Long.MIN_VALUE
    val dw0T = dw0 xor Long.MIN_VALUE
    val pow10Offset = digitCountEstimate shl 1
    val p1 = POW10[(pow10Offset + 1) and POW10_BCE] xor Long.MIN_VALUE
    val p0 = POW10[(pow10Offset + 0) and POW10_BCE] xor Long.MIN_VALUE

    val ret2 = digitCountEstimate +
            if ((dw1T > p1) or ((dw1T == p1) and (dw0T >= p0))) 1 else 0
    return ret2
}

internal fun calcDigitLen192(bitLen: Int, dw2: Long, dw1: Long, dw0: Long): Int {
    return when {
        bitLen > 128 -> {
            val loDigitCount = max((bitLen * 1233) ushr 12, MIN_POW10_DIGIT_LEN_192)
            val hiDigitCount = loDigitCount + 1
            val pow10Offset = pow10Offset(loDigitCount) and POW10_BCE
            val p2 = POW10[pow10Offset + 2]
            val p1 = POW10[pow10Offset + 1]
            val p0 = POW10[pow10Offset + 0]
            val cmp2 = unsignedCmp(dw2, p2)
            val cmp1 = unsignedCmp(dw1, p1)
            val cmp0 = unsignedCmp(dw0, p0)
            val cmp10 = if (cmp1 != 0) cmp1 else cmp0
            val cmp210 = if (cmp2 != 0) cmp2 else cmp10
            val ret = if (cmp210 < 0) loDigitCount else hiDigitCount
            return ret
        }

        bitLen > 64 -> calcDigitLen128(bitLen, dw1, dw0)
        else -> calcDigitLen64(bitLen, dw0)
    }
}

internal fun calcDigitLen256(bitLen: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): Int {
    return when {
        bitLen > 192 -> {
            val loDigitCount = max((bitLen * 1233) ushr 12, MIN_POW10_DIGIT_LEN_256)
            val hiDigitCount = loDigitCount + 1
            val pow10Offset = pow10Offset(loDigitCount) and POW10_BCE
            val p3 = POW10[pow10Offset + 3]
            val p2 = POW10[pow10Offset + 2]
            val p1 = POW10[pow10Offset + 1]
            val p0 = POW10[pow10Offset + 0]
            val cmp3 = unsignedCmp(dw3, p3)
            val cmp2 = unsignedCmp(dw2, p2)
            val cmp1 = unsignedCmp(dw1, p1)
            val cmp0 = unsignedCmp(dw0, p0)
            val cmp32 = if (cmp3 != 0) cmp3 else cmp2
            val cmp10 = if (cmp1 != 0) cmp1 else cmp0
            val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
            val ret = if (cmp3210 < 0) loDigitCount else hiDigitCount
            return ret
        }

        bitLen > 128 -> calcDigitLen192(bitLen, dw2, dw1, dw0)
        bitLen > 64 -> calcDigitLen128(bitLen, dw1, dw0)
        else -> calcDigitLen64(bitLen, dw0)
    }
}

internal fun compareWithHalfPow10_1(dw0: Long, pow10: Int): Int {
    verify { pow10 >= 0 && pow10 < MIN_POW10_DIGIT_LEN_128 }
    val pow10Dw0 = pow10_64(pow10)
    val halfPow10Dw0 = pow10Dw0 ushr 1
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    return cmp0
}

internal fun compareWithHalfPow10_2(dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= 0 && pow10 < MIN_POW10_DIGIT_LEN_192 }
    val pow10Dw1 = pow10_128_dw1(pow10)
    val halfPow10Dw1 = pow10Dw1 ushr 1
    val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
    if (cmp1 != 0)
        return cmp1
    val pow10Dw0 = pow10_128_dw0(pow10)
    val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    return cmp0
}

internal fun compareWithHalfPow10_3(dw2: Long, dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= MIN_POW10_DIGIT_LEN_192 && pow10 < MIN_POW10_DIGIT_LEN_256 }
    val pow10Offset = pow10Offset(pow10) and POW10_BCE
    val pow10Dw0 = POW10[pow10Offset + 0]
    val pow10Dw1 = POW10[pow10Offset + 1]
    val pow10Dw2 = POW10[pow10Offset + 2]
    val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
    val halfPow10Dw1 = (pow10Dw2 shl -1) or (pow10Dw1 ushr 1)
    val halfPow10Dw2 = pow10Dw2 ushr 1
    val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
    val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
    val cmp2 = unsignedCmp(dw2, halfPow10Dw2)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    val cmp210 = if (cmp2 != 0) cmp2 else cmp10
    return cmp210
}

internal fun compareWithHalfPow10_4(dw3: Long, dw2: Long, dw1: Long, dw0: Long, pow10: Int): Int {
    verify { pow10 >= MIN_POW10_DIGIT_LEN_256 && pow10 <= MAXX_DIGIT_LEN }
    if (pow10 < MAXX_DIGIT_LEN) {
        val pow10Offset = pow10Offset(pow10) and POW10_BCE
        val pow10Dw0 = POW10[pow10Offset + 0]
        val pow10Dw1 = POW10[pow10Offset + 1]
        val pow10Dw2 = POW10[pow10Offset + 2]
        val pow10Dw3 = POW10[pow10Offset + 3]
        val halfPow10Dw0 = (pow10Dw1 shl -1) or (pow10Dw0 ushr 1)
        val halfPow10Dw1 = (pow10Dw2 shl -1) or (pow10Dw1 ushr 1)
        val halfPow10Dw2 = (pow10Dw3 shl -1) or (pow10Dw2 ushr 1)
        val halfPow10Dw3 = pow10Dw3 ushr 1
        val cmp0 = unsignedCmp(dw0, halfPow10Dw0)
        val cmp1 = unsignedCmp(dw1, halfPow10Dw1)
        val cmp2 = unsignedCmp(dw2, halfPow10Dw2)
        val cmp3 = unsignedCmp(dw3, halfPow10Dw3)
        val cmp10 = if (cmp1 != 0) cmp1 else cmp0
        val cmp32 = if (cmp3 != 0) cmp3 else cmp2
        val cmp3210 = if (cmp32 != 0) cmp32 else cmp10
        return cmp3210
    } else {
        // when there are 78 digits then we are always LT_HALF
        return -1
    }
}

internal fun c256IsPowerOf10(x: C256): Boolean {
    val xBitLen = x.bitLen
    val xDigitLen = x.digitLen
    if (xDigitLen > 0) {
        val pow10Offset = pow10Offset(xDigitLen - 1) and POW10_BCE
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - xBitLen) shr 31).toLong()
        val p2 = POW10[pow10Offset + 2] and ((128 - xBitLen) shr 31).toLong()
        val p3 = POW10[pow10Offset + 3] and ((192 - xBitLen) shr 31).toLong()
        return (p0 == x.dw0) and (p1 == x.dw1) and (p2 == x.dw2) and (p3 == x.dw3)
    }
    return false
}

internal fun c256SetPow10(z: C256, pow10: Int) {
    if (pow10 >= 0) {
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10) and POW10_BCE
        val p0 = POW10[pow10Offset + 0]
        val p1 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p2 = POW10[pow10Offset + 2] and ((128 - pow10BitLen) shr 31).toLong()
        val p3 = POW10[pow10Offset + 3] and ((192 - pow10BitLen) shr 31).toLong()
        z.c256Set256(p3, p2, p1, p0)
    } else {
        z.c256SetZero()
    }
}







