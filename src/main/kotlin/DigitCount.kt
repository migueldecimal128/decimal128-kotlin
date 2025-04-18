package com.decimal128
import java.lang.Long.compareUnsigned
import java.lang.Long.numberOfLeadingZeros

const val POW10_64_OFFSET = 0
const val POW10_64_DWORD_INDEX = 0

const val POW10_128_OFFSET = 20
const val POW10_64_COUNT = POW10_128_OFFSET - POW10_64_OFFSET
const val POW10_128_DWORD_INDEX = POW10_64_DWORD_INDEX + (1 * POW10_64_COUNT)

const val POW10_192_OFFSET = 39
const val POW10_128_COUNT = POW10_192_OFFSET - POW10_128_OFFSET
const val POW10_192_DWORD_INDEX = POW10_128_DWORD_INDEX + (2 * POW10_128_COUNT)

const val POW10_256_OFFSET = 58
const val POW10_192_COUNT = POW10_256_OFFSET - POW10_192_OFFSET
const val POW10_256_DWORD_INDEX = POW10_192_DWORD_INDEX + (3 * POW10_192_COUNT)

const val POW10_MAX_OFFSET = 78
const val POW10_256_COUNT = POW10_MAX_OFFSET - POW10_256_OFFSET
const val POW10_MAX_DWORD_INDEX = POW10_256_DWORD_INDEX + (4 * POW10_256_COUNT)

const val MAX_COEFF_DIGIT_COUNT = POW10_MAX_OFFSET

val POW10 = longArrayOf(
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
    0x098A224000000000uL.toLong(), 0x4B3B4CA85A86C47AuL.toLong(), // 100000000000000000000000000000000000000 (10**38)
    // minBitCount:128  maxBitCount:192
    0x5F65568000000000uL.toLong(), 0xF050FE938943ACC4uL.toLong(), 0x0000000000000002uL.toLong(), // 1000000000000000000000000000000000000000 (10**39)
    0xB9F5610000000000uL.toLong(), 0x6329F1C35CA4BFABuL.toLong(), 0x000000000000001DuL.toLong(), // 10000000000000000000000000000000000000000 (10**40)
    0x4395CA0000000000uL.toLong(), 0xDFA371A19E6F7CB5uL.toLong(), 0x0000000000000125uL.toLong(), // 100000000000000000000000000000000000000000 (10**41)
    0xA3D9E40000000000uL.toLong(), 0xBC627050305ADF14uL.toLong(), 0x0000000000000B7AuL.toLong(), // 1000000000000000000000000000000000000000000 (10**42)
    0x6682E80000000000uL.toLong(), 0x5BD86321E38CB6CEuL.toLong(), 0x00000000000072CBuL.toLong(), // 10000000000000000000000000000000000000000000 (10**43)
    0x011D100000000000uL.toLong(), 0x9673DF52E37F2410uL.toLong(), 0x0000000000047BF1uL.toLong(), // 100000000000000000000000000000000000000000000 (10**44)
    0x0B22A00000000000uL.toLong(), 0xE086B93CE2F768A0uL.toLong(), 0x00000000002CD76FuL.toLong(), // 1000000000000000000000000000000000000000000000 (10**45)
    0x6F5A400000000000uL.toLong(), 0xC5433C60DDAA1640uL.toLong(), 0x0000000001C06A5EuL.toLong(), // 10000000000000000000000000000000000000000000000 (10**46)
    0x5986800000000000uL.toLong(), 0xB4A05BC8A8A4DE84uL.toLong(), 0x00000000118427B3uL.toLong(), // 100000000000000000000000000000000000000000000000 (10**47)
    0x7F41000000000000uL.toLong(), 0x0E4395D69670B12BuL.toLong(), 0x00000000AF298D05uL.toLong(), // 1000000000000000000000000000000000000000000000000 (10**48)
    0xF88A000000000000uL.toLong(), 0x8EA3DA61E066EBB2uL.toLong(), 0x00000006D79F8232uL.toLong(), // 10000000000000000000000000000000000000000000000000 (10**49)
    0xB564000000000000uL.toLong(), 0x926687D2C40534FDuL.toLong(), 0x000000446C3B15F9uL.toLong(), // 100000000000000000000000000000000000000000000000000 (10**50)
    0x15E8000000000000uL.toLong(), 0xB8014E3BA83411E9uL.toLong(), 0x000002AC3A4EDBBFuL.toLong(), // 1000000000000000000000000000000000000000000000000000 (10**51)
    0xDB10000000000000uL.toLong(), 0x300D0E549208B31AuL.toLong(), 0x00001ABA4714957DuL.toLong(), // 10000000000000000000000000000000000000000000000000000 (10**52)
    0x8EA0000000000000uL.toLong(), 0xE0828F4DB456FF0CuL.toLong(), 0x00010B46C6CDD6E3uL.toLong(), // 100000000000000000000000000000000000000000000000000000 (10**53)
    0x9240000000000000uL.toLong(), 0xC51999090B65F67DuL.toLong(), 0x000A70C3C40A64E6uL.toLong(), // 1000000000000000000000000000000000000000000000000000000 (10**54)
    0xB680000000000000uL.toLong(), 0xB2FFFA5A71FBA0E7uL.toLong(), 0x006867A5A867F103uL.toLong(), // 10000000000000000000000000000000000000000000000000000000 (10**55)
    0x2100000000000000uL.toLong(), 0xFDFFC78873D4490DuL.toLong(), 0x04140C78940F6A24uL.toLong(), // 100000000000000000000000000000000000000000000000000000000 (10**56)
    0x4A00000000000000uL.toLong(), 0xEBFDCB54864ADA83uL.toLong(), 0x28C87CB5C89A2571uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000 (10**57)
    // minBitCount:192  maxBitCount:256
    0xE400000000000000uL.toLong(), 0x37E9F14D3EEC8920uL.toLong(), 0x97D4DF19D6057673uL.toLong(), 0x0000000000000001uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000 (10**58)
    0xE800000000000000uL.toLong(), 0x2F236D04753D5B48uL.toLong(), 0xEE50B7025C36A080uL.toLong(), 0x000000000000000FuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000 (10**59)
    0x1000000000000000uL.toLong(), 0xD762422C946590D9uL.toLong(), 0x4F2726179A224501uL.toLong(), 0x000000000000009FuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000 (10**60)
    0xA000000000000000uL.toLong(), 0x69D695BDCBF7A87AuL.toLong(), 0x17877CEC0556B212uL.toLong(), 0x0000000000000639uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000 (10**61)
    0x4000000000000000uL.toLong(), 0x2261D969F7AC94CAuL.toLong(), 0xEB4AE1383562F4B8uL.toLong(), 0x0000000000003E3AuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000 (10**62)
    0x8000000000000000uL.toLong(), 0x57D27E23ACBDCFE6uL.toLong(), 0x30ECCC3215DD8F31uL.toLong(), 0x0000000000026E4DuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000 (10**63)
    0x0000000000000000uL.toLong(), 0x6E38ED64BF6A1F01uL.toLong(), 0xE93FF9F4DAA797EDuL.toLong(), 0x0000000000184F03uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000 (10**64)
    0x0000000000000000uL.toLong(), 0x4E3945EF7A25360AuL.toLong(), 0x1C7FC3908A8BEF46uL.toLong(), 0x0000000000F31627uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000 (10**65)
    0x0000000000000000uL.toLong(), 0x0E3CBB5AC5741C64uL.toLong(), 0x1CFDA3A5697758BFuL.toLong(), 0x00000000097EDD87uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000 (10**66)
    0x0000000000000000uL.toLong(), 0x8E5F518BB6891BE8uL.toLong(), 0x21E864761EA97776uL.toLong(), 0x000000005EF4A747uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000 (10**67)
    0x0000000000000000uL.toLong(), 0x8FB92F75215B1710uL.toLong(), 0x5313EC9D329EAAA1uL.toLong(), 0x00000003B58E88C7uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000 (10**68)
    0x0000000000000000uL.toLong(), 0x9D3BDA934D8EE6A0uL.toLong(), 0x3EC73E23FA32AA4FuL.toLong(), 0x00000025179157C9uL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000 (10**69)
    0x0000000000000000uL.toLong(), 0x245689C107950240uL.toLong(), 0x73C86D67C5FAA71CuL.toLong(), 0x00000172EBAD6DDCuL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000 (10**70)
    0x0000000000000000uL.toLong(), 0x6B61618A4BD21680uL.toLong(), 0x85D4460DBBCA8719uL.toLong(), 0x00000E7D34C64A9CuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000 (10**71)
    0x0000000000000000uL.toLong(), 0x31CDCF66F634E100uL.toLong(), 0x3A4ABC8955E946FEuL.toLong(), 0x000090E40FBEEA1DuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000000 (10**72)
    0x0000000000000000uL.toLong(), 0xF20A1A059E10CA00uL.toLong(), 0x46EB5D5D5B1CC5EDuL.toLong(), 0x0005A8E89D752524uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000000 (10**73)
    0x0000000000000000uL.toLong(), 0x746504382CA7E400uL.toLong(), 0xC531A5A58F1FBB4BuL.toLong(), 0x003899162693736AuL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000000 (10**74)
    0x0000000000000000uL.toLong(), 0x8BF22A31BE8EE800uL.toLong(), 0xB3F07877973D50F2uL.toLong(), 0x0235FADD81C2822BuL.toLong(), // 1000000000000000000000000000000000000000000000000000000000000000000000000000 (10**75)
    0x0000000000000000uL.toLong(), 0x7775A5F171951000uL.toLong(), 0x0764B4ABE8652979uL.toLong(), 0x161BCCA7119915B5uL.toLong(), // 10000000000000000000000000000000000000000000000000000000000000000000000000000 (10**76)
    0x0000000000000000uL.toLong(), 0xAA987B6E6FD2A000uL.toLong(), 0x49EF0EB713F39EBEuL.toLong(), 0xDD15FE86AFFAD912uL.toLong(), // 100000000000000000000000000000000000000000000000000000000000000000000000000000 (10**77)
    // we can represent all 77 decimal digit numbers
    )

private val validatePow10Size = run { assert(POW10.size == 195 && POW10.size == POW10_MAX_DWORD_INDEX) ; true}


fun calcDigitCount64_nlz(dw0:Long) : Int {
    val nlz = numberOfLeadingZeros(dw0)
    val bitLength = 64 - nlz
    if (bitLength == 0)
        return 0;
    val loDigitCount = (bitLength * 1233) shr 12
    val hiDigitCount = loDigitCount + 1
    val m0 = POW10[loDigitCount]
    val digitCount =
        if (compareUnsigned(dw0, m0) < 0)
            loDigitCount
        else
            hiDigitCount
    return digitCount
}

fun calcDigitCount128_nlz(dw1:Long, dw0:Long) : Int {
    val nlz = numberOfLeadingZeros(dw1)
    val bitLength = 128 - nlz
    val loDigitCount = (bitLength * 1233) shr 12
    if (loDigitCount < POW10_128_OFFSET)
        return POW10_128_OFFSET
    val hiDigitCount = loDigitCount + 1
    val i = loDigitCount - POW10_128_OFFSET
    val index = i*2 + POW10_128_DWORD_INDEX
    val m1 = POW10[index + 1]
    if (dw1 != m1)
        return if (compareUnsigned(dw1, m1) < 0) loDigitCount else hiDigitCount
    val m0 = POW10[index + 0]
    return if (compareUnsigned(dw0, m0) < 0) loDigitCount else hiDigitCount
}

fun calcDigitCount192_nlz(dw2:Long, dw1:Long, dw0:Long) : Int {
    val nlz = numberOfLeadingZeros(dw2)
    val bitLength = 192 - nlz
    val loDigitCount = (bitLength * 1233) shr 12
    if (loDigitCount < POW10_192_OFFSET)
        return POW10_192_OFFSET
    val hiDigitCount = loDigitCount + 1
    val i = loDigitCount - POW10_192_OFFSET
    val index = i*3 + POW10_192_DWORD_INDEX
    val m2 = POW10[index + 2]
    if (dw2 != m2)
        return if (compareUnsigned(dw2, m2) < 0) loDigitCount else hiDigitCount
    val m1 = POW10[index + 1]
    if (dw1 != m1)
        return if (compareUnsigned(dw1, m1) < 0) loDigitCount else hiDigitCount
    val m0 = POW10[index + 0]
    return if (compareUnsigned(dw0, m0) < 0) loDigitCount else hiDigitCount
}

fun calcDigitCount256_nlz(dw3:Long, dw2:Long, dw1:Long, dw0:Long) : Int {
    val nlz = numberOfLeadingZeros(dw3)
    val bitLength = 256 - nlz
    val loDigitCount = (bitLength * 1233) shr 12
    if (loDigitCount < POW10_256_OFFSET)
        return POW10_256_OFFSET
    val hiDigitCount = loDigitCount + 1
    val i = loDigitCount - POW10_256_OFFSET
    val index = i*4 + POW10_256_DWORD_INDEX
    val m3 = POW10[index + 3]
    if (dw3 != m3)
        return if (compareUnsigned(dw3, m3) < 0) loDigitCount else hiDigitCount
    val m2 = POW10[index + 2]
    if (dw2 != m2)
        return if (compareUnsigned(dw2, m2) < 0) loDigitCount else hiDigitCount
    val m1 = POW10[index + 1]
    if (dw1 != m1)
        return if (compareUnsigned(dw1, m1) < 0) loDigitCount else hiDigitCount
    val m0 = POW10[index + 0]
    return if (compareUnsigned(dw0, m0) < 0) loDigitCount else hiDigitCount
}

fun calcDigitCount64_binarySearch(dw0: Long) : Int {
    var lo = 0
    var hi = POW10_64_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid + POW10_64_DWORD_INDEX
        val m0 = POW10[index + 0]
        if (compareUnsigned(dw0, m0) >= 0)
            lo = mid + 1
        else
            hi = mid
    } while (lo < hi)
    val digitCount = POW10_64_OFFSET + lo
    return digitCount
}

fun calcDigitCount128_binarySearch(dw1:Long, dw0:Long) : Int {
    var lo = 0
    var hi = POW10_128_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*2 + POW10_128_DWORD_INDEX
        val m1 = POW10[index + 1]
        if (compareUnsigned(dw1, m1) > 0) {
            lo = mid + 1
        } else if (dw1 != m1) {
            hi = mid
        } else {
            val m0 = POW10[index + 0]
            if (compareUnsigned(dw0, m0) >= 0)
                lo = mid + 1
            else
                hi = mid
        }
    } while (lo < hi)
    val digitCount = POW10_128_OFFSET + lo
    return digitCount
}

fun calcDigitCount192_binarySearch(dw2: Long, dw1: Long, dw0: Long) : Int {
    var lo = 0
    var hi = POW10_192_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*3 + POW10_192_DWORD_INDEX
        val m2 = POW10[index + 2]
        if (compareUnsigned(dw2, m2) > 0) {
            lo = mid + 1
        } else if (dw2 != m2) {
            hi = mid
        } else {
            val m1 = POW10[index + 1]
            if (compareUnsigned(dw1, m1) > 0) {
                lo = mid + 1
            } else if (dw1 != m1) {
                hi = mid
            } else {
                val m0 = POW10[index + 0]
                if (compareUnsigned(dw0, m0) >= 0)
                    lo = mid + 1
                else
                    hi = mid
            }
        }
    } while (lo < hi)
    val digitCount = POW10_192_OFFSET + lo
    return digitCount
}

fun calcDigitCount256_binarySearch(dw3: Long, dw2: Long, dw1: Long, dw0: Long) : Int {
    var lo = 0
    var hi = POW10_256_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*4 + POW10_256_DWORD_INDEX
        val m3 = POW10[index + 3]
        if (compareUnsigned(dw3, m3) > 0) {
            lo = mid + 1
        } else if (dw3 != m3) {
            hi = mid
        } else {
            val m2 = POW10[index + 2]
            if (compareUnsigned(dw2, m2) > 0) {
                lo = mid + 1
            } else if (dw2 != m2) {
                hi = mid
            } else {
                val m1 = POW10[index + 1]
                if (compareUnsigned(dw1, m1) > 0) {
                    lo = mid + 1
                } else if (dw1 != m1) {
                    hi = mid
                } else {
                    val m0 = POW10[index + 0]
                    if (compareUnsigned(dw0, m0) >= 0)
                        lo = mid + 1
                    else
                        hi = mid
                }
            }
        }
    } while (lo < hi)
    val digitCount = POW10_256_OFFSET + lo
    return digitCount
}

fun calcDigitCount64(dw0:Long) = calcDigitCount64_nlz(dw0)

fun calcDigitCount64(dw0:ULong) = calcDigitCount64(dw0.toLong())

fun calcDigitCount128(dw1:Long, dw0:Long) = calcDigitCount128_nlz(dw1, dw0)

fun calcDigitCount192(dw2:Long, dw1:Long, dw0:Long) = calcDigitCount192_nlz(dw2, dw1, dw0)

fun calcDigitCount256(dw3:Long, dw2:Long, dw1:Long, dw0:Long) = calcDigitCount256_nlz(dw3, dw2, dw1, dw0)

fun calcDigitCount(dw1: Long, dw0: Long) : Int {
    return if (dw1 == 0L) calcDigitCount64(dw0) else calcDigitCount128(dw1, dw0)
}

fun calcDigitCount(dw3: Long, dw2: Long, dw1: Long, dw0: Long) : Int {
    if ((dw3 or dw2) == 0L)
        return if (dw1 == 0L) calcDigitCount64(dw0) else calcDigitCount128(dw1, dw0)
    else
        return if (dw3 == 0L) calcDigitCount192(dw2, dw1, dw0) else calcDigitCount256(dw3, dw2, dw1, dw0)
}

 fun recalcDigitCountOnly64(c: Coeff) {
    assert ((c.dw3 or c.dw2 or c.dw1) == 0L)
    var lo = 0
    var hi = POW10_64_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid + POW10_64_DWORD_INDEX
        val m0 = POW10[index + 0]
        if (compareUnsigned(c.dw0, m0) >= 0)
            lo = mid + 1
        else
            hi = mid
    } while (lo < hi)
    c.digitCount = POW10_64_OFFSET + lo
}

fun recalcDigitCountOnly128(c: Coeff) {
    assert ((c.dw3 or c.dw2) == 0L && c.dw1 != 0L)
    var lo = 0
    var hi = POW10_128_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*2 + POW10_128_DWORD_INDEX
        val m1 = POW10[index + 1]
        if (compareUnsigned(c.dw1, m1) > 0) {
            lo = mid + 1
        } else if (c.dw1 != m1) {
            hi = mid
        } else {
            val m0 = POW10[index + 0]
            if (compareUnsigned(c.dw0, m0) >= 0)
                lo = mid + 1
            else
                hi = mid
        }
    } while (lo < hi)
    c.digitCount = POW10_128_OFFSET + lo
}

fun recalcDigitCountOnly192(c: Coeff) {
    assert (c.dw3 == 0L && c.dw2 != 0L)
    var lo = 0
    var hi = POW10_192_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*3 + POW10_192_DWORD_INDEX
        val m2 = POW10[index + 2]
        if (compareUnsigned(c.dw2, m2) > 0) {
            lo = mid + 1
        } else if (c.dw2 != m2) {
            hi = mid
        } else {
            val m1 = POW10[index + 1]
            if (compareUnsigned(c.dw1, m1) > 0) {
                lo = mid + 1
            } else if (c.dw1 != m1) {
                hi = mid
            } else {
                val m0 = POW10[index + 0]
                if (compareUnsigned(c.dw0, m0) >= 0)
                    lo = mid + 1
                else
                    hi = mid
            }
        }
    } while (lo < hi)
    c.digitCount = POW10_192_OFFSET + lo
}

fun recalcDigitCountOnly256(c: Coeff) {
    assert (c.dw3 != 0L)
    var lo = 0
    var hi = POW10_256_COUNT
    do {
        val mid = (lo + hi) / 2
        val index = mid*4 + POW10_256_DWORD_INDEX
        val m3 = POW10[index + 3]
        if (compareUnsigned(c.dw3, m3) > 0) {
            lo = mid + 1
        } else if (c.dw3 != m3) {
            hi = mid
        } else {
            val m2 = POW10[index + 2]
            if (compareUnsigned(c.dw2, m2) > 0) {
                lo = mid + 1
            } else if (c.dw2 != m2) {
                hi = mid
            } else {
                val m1 = POW10[index + 1]
                if (compareUnsigned(c.dw1, m1) > 0) {
                    lo = mid + 1
                } else if (c.dw1 != m1) {
                    hi = mid
                } else {
                    val m0 = POW10[index + 0]
                    if (compareUnsigned(c.dw0, m0) >= 0)
                        lo = mid + 1
                    else
                        hi = mid
                }
            }
        }
    } while (lo < hi)
    c.digitCount = POW10_256_OFFSET + lo
}

fun tweakDigitCountOnly64(c: Coeff) {
    // recalcDigitCount256orLess()
    val loDigitCount = c.digitCount
    assert(loDigitCount >= 0 && loDigitCount <= POW10_128_OFFSET)
    if (loDigitCount < POW10_128_OFFSET) { // < 20 digits
        val hiDigitCount = loDigitCount + 1
        val i = loDigitCount - POW10_64_OFFSET
        val index = i * 1 + POW10_64_DWORD_INDEX
        val m0 = POW10[index + 0]
        c.digitCount = if (compareUnsigned(c.dw0, m0) < 0) loDigitCount else hiDigitCount
    } else {
        c.digitCount = POW10_128_OFFSET
    }

}

fun tweakDigitCountOnly128(c: Coeff) {
    // recalcDigitCount256orLess()
    val loDigitCount = c.digitCount
    assert(loDigitCount >= POW10_128_OFFSET-1 && loDigitCount <= POW10_192_OFFSET)
    assert(c.dw1 != 0L && (c.dw2 or c.dw3) == 0L)
    if (loDigitCount < POW10_128_OFFSET) {
        c.digitCount = POW10_128_OFFSET
    } else if (loDigitCount < POW10_192_OFFSET) {
        val hiDigitCount = loDigitCount + 1
        val i = loDigitCount - POW10_128_OFFSET
        val index = i*2 + POW10_128_DWORD_INDEX
        val m1 = POW10[index + 1]
        if (c.dw1 != m1) {
            c.digitCount = if (compareUnsigned(c.dw1, m1) < 0) loDigitCount else hiDigitCount
            return
        }
        val m0 = POW10[index + 0]
        c.digitCount = if (compareUnsigned(c.dw0, m0) < 0) loDigitCount else hiDigitCount
    } else {
        c.digitCount = POW10_192_OFFSET
    }
}

fun tweakDigitCountOnly192(c: Coeff) {
    // recalcDigitCount256orLess()
    val loDigitCount = c.digitCount
    assert(loDigitCount >= POW10_192_OFFSET-1 && loDigitCount <= POW10_256_OFFSET)
    assert(c.dw2 != 0L && c.dw3 == 0L)
    if (loDigitCount < POW10_192_OFFSET) {
        c.digitCount = POW10_192_OFFSET
    } else if (loDigitCount < POW10_256_OFFSET) {
        val hiDigitCount = loDigitCount + 1
        val i = loDigitCount - POW10_192_OFFSET
        val index = i * 3 + POW10_192_DWORD_INDEX
        val m2 = POW10[index + 2]
        if (c.dw2 != m2) {
            c.digitCount = if (compareUnsigned(c.dw2, m2) < 0) loDigitCount else hiDigitCount
            return
        }
        val m1 = POW10[index + 1]
        if (c.dw1 != m1) {
            c.digitCount = if (compareUnsigned(c.dw1, m1) < 0) loDigitCount else hiDigitCount
            return
        }
        val m0 = POW10[index + 0]
        c.digitCount = if (compareUnsigned(c.dw0, m0) < 0) loDigitCount else hiDigitCount
    } else {
        c.digitCount = POW10_256_OFFSET
    }
}

fun tweakDigitCountOnly256(c: Coeff) {
    // recalcDigitCount256orLess()
    val loDigitCount = c.digitCount
    if (!(loDigitCount >= POW10_256_OFFSET-1 && loDigitCount <= POW10_MAX_OFFSET))
        println("foo!")
    assert(loDigitCount >= POW10_256_OFFSET-1 && loDigitCount <= POW10_MAX_OFFSET)
    assert(c.dw3 != 0L)
    if (loDigitCount < POW10_256_OFFSET) {
        c.digitCount = POW10_256_OFFSET
    } else if (loDigitCount < POW10_MAX_OFFSET) {
        val hiDigitCount = loDigitCount + 1
        val i = loDigitCount - POW10_256_OFFSET
        val index = i * 4 + POW10_256_DWORD_INDEX
        val m3 = POW10[index + 3]
        if (c.dw3 != m3) {
            c.digitCount = if (compareUnsigned(c.dw3, m3) < 0) loDigitCount else hiDigitCount
            return
        }
        val m2 = POW10[index + 2]
        if (c.dw2 != m2) {
            c.digitCount = if (compareUnsigned(c.dw2, m2) < 0) loDigitCount else hiDigitCount
            return
        }
        val m1 = POW10[index + 1]
        if (c.dw1 != m1) {
            c.digitCount = if (compareUnsigned(c.dw1, m1) < 0) loDigitCount else hiDigitCount
            return
        }
        val m0 = POW10[index + 0]
        c.digitCount = if (compareUnsigned(c.dw0, m0) < 0) loDigitCount else hiDigitCount
    } else {
        c.digitCount = POW10_MAX_OFFSET
    }

}

fun tweakDigitCountPostRoundup(c: Coeff, ctx: Decimal128Context) {
    if ((c.dw3 or c.dw2) == 0L) {
        if (c.dw1 == 0L)
            tweakDigitCountOnly64(c)
        else
            tweakDigitCountOnly128(c)
    } else {
        if (c.dw3 == 0L)
            tweakDigitCountOnly192(c)
        else
            tweakDigitCountOnly256(c)
    }
}
