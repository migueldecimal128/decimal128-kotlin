package com.decimal128.decimal

internal const val MASK16L = 0x0000_0000_0000_FFFFL
internal const val MASK32L = 0x0000_0000_FFFF_FFFFL
internal const val MASK48L = 0x0000_FFFF_FFFF_FFFFL

internal const val MAX_DEC34_CHAR_LEN = 1 /*sign*/ + 34 /*coefficient*/ + 1 /*dot*/ + 1 /*E*/ + 1 /*sign*/+ 4 /*exp*/
internal const val MAX_DEC38_CHAR_LEN = 1 /*sign*/ + 38 /*coefficient*/ + 1 /*dot*/ + 1 /*E*/ + 1 /*sign*/+ 4 /*exp*/
internal const val MAX_DEC77_CHAR_LEN = 1 /*sign*/ + 77 /*coefficient*/ + 1 /*dot*/ + 1 /*E*/ + 1 /*sign*/+ 4 /*exp*/

internal const val Q_MAX = 6111
internal const val Q_TINY = -6176
internal const val E_MAX = 6144
internal const val E_MIN = -6143
internal const val NAN_PAYLOAD_PRECISION_33 = 33

/*
    0 1 2 3 4 5 6 7 8 9 A B C D E F
2  SP ! " # $ % & ' ( ) * + , - . /
3   0 1 2 3 4 5 6 7 8 9 : ; < = > ?
4   @ A B C D E F G H I J K L M N O
5   P Q R S T U V W X Y Z [ \ ] ^ _
6   ` a b c d e f g h i j k l m n o
7   p q r s t u v w x y z { | } ~ DEL
*/

internal const val ascii_minus: Byte = 0x2D
internal const val ascii_plus: Byte = 0x2B
internal const val ascii_dot: Byte = 0x2E
internal const val ascii_underbar: Byte = 0x5F
internal const val ascii_lParen: Byte = 0x28
internal const val ascii_rParen: Byte = 0x29
internal const val ascii_lSquare: Byte = 0x5B
internal const val ascii_rSquare: Byte = 0x5D
internal const val ascii_lCurly: Byte = 0x7B
internal const val ascii_rCurly: Byte = 0x7D
internal const val ascii_lAngle: Byte = 0x3C
internal const val ascii_rAngle: Byte = 0x3E

internal const val ascii_0: Byte = 0x30
internal const val ascii_1: Byte = 0x31
internal const val ascii_2: Byte = 0x32
internal const val ascii_3: Byte = 0x33
internal const val ascii_4: Byte = 0x34
internal const val ascii_5: Byte = 0x35
internal const val ascii_6: Byte = 0x36
internal const val ascii_7: Byte = 0x37
internal const val ascii_8: Byte = 0x38
internal const val ascii_9: Byte = 0x39

internal const val ascii_A: Byte = 0x41
internal const val ascii_B: Byte = 0x42
internal const val ascii_C: Byte = 0x43
internal const val ascii_D: Byte = 0x44
internal const val ascii_E: Byte = 0x45
internal const val ascii_F: Byte = 0x46
internal const val ascii_G: Byte = 0x47
internal const val ascii_H: Byte = 0x48
internal const val ascii_I: Byte = 0x49
internal const val ascii_J: Byte = 0x4A
internal const val ascii_K: Byte = 0x4B
internal const val ascii_L: Byte = 0x4C
internal const val ascii_M: Byte = 0x4D
internal const val ascii_N: Byte = 0x4E
internal const val ascii_O: Byte = 0x4F
internal const val ascii_P: Byte = 0x50
internal const val ascii_Q: Byte = 0x51
internal const val ascii_R: Byte = 0x52
internal const val ascii_S: Byte = 0x53
internal const val ascii_T: Byte = 0x54
internal const val ascii_U: Byte = 0x55
internal const val ascii_V: Byte = 0x56
internal const val ascii_W: Byte = 0x57
internal const val ascii_X: Byte = 0x58
internal const val ascii_Y: Byte = 0x59
internal const val ascii_Z: Byte = 0x5A

internal const val ascii_a: Byte = 0x61
internal const val ascii_b: Byte = 0x62
internal const val ascii_c: Byte = 0x63
internal const val ascii_d: Byte = 0x64
internal const val ascii_e: Byte = 0x65
internal const val ascii_f: Byte = 0x66
internal const val ascii_g: Byte = 0x67
internal const val ascii_h: Byte = 0x68
internal const val ascii_i: Byte = 0x69
internal const val ascii_j: Byte = 0x6A
internal const val ascii_k: Byte = 0x6B
internal const val ascii_l: Byte = 0x6C
internal const val ascii_m: Byte = 0x6D
internal const val ascii_n: Byte = 0x6E
internal const val ascii_o: Byte = 0x6F
internal const val ascii_p: Byte = 0x70
internal const val ascii_q: Byte = 0x71
internal const val ascii_r: Byte = 0x72
internal const val ascii_s: Byte = 0x73
internal const val ascii_t: Byte = 0x74
internal const val ascii_u: Byte = 0x75
internal const val ascii_v: Byte = 0x76
internal const val ascii_w: Byte = 0x77
internal const val ascii_x: Byte = 0x78
internal const val ascii_y: Byte = 0x79
internal const val ascii_z: Byte = 0x7A


// Automatically generated ... but not any more

internal const val POW10_64_COUNT = 20          // 0x00000014 2E+1

internal const val POW10_128_COUNT = 19          // 0x00000013 19

internal const val POW10_192_COUNT = 19          // 0x00000013 19

internal const val POW10_256_COUNT = 20          // 0x00000014 2E+1

internal const val MIN_POW10_DIGIT_LEN_128 = 20          // 0x00000014 2E+1

internal const val MIN_POW10_DIGIT_LEN_192 = 39          // 0x00000027 39

internal const val MIN_POW10_DIGIT_LEN_256 = 58          // 0x0000003A 58

// 256-bit coefficient handles all 77 digit integers
internal const val MAXX_DIGIT_LEN = 78          // 0x0000004E 78


/**
 * DWORD_TABLES stores multiple tables in a single LongArray.
 * Total size of the table is 1024 Long.
 *
 * Tables stored are:
 * - Powers of 10 up thru 256 bits
 * - Powers of 5 that fit in 64 bits
 * - Barrett mu values for multi-limb reciprocal division by powers of 5 that fit in 32 bits
 * - Magic m values for division of 64-bit values by powers of 10 that fit in 64 bits
 * - Params for general range reciprocal division with sufficient precision for rounding
 *
 */
internal const val DWORD_TABLES_SIZE_POW_2 = 1024
internal expect val DWORD_TABLES: LongArray

/**
 * Bounds Check Elimination BCE is performed by masking the array
 * index with DWORD_TABLES_BCE == 1023 == 0x3FF
 *
 * There are some entries avail at the end that are wasted, but eliminating
 * bounds checking through masking is a net win for size and performance.
 */
internal const val DWORD_TABLES_BCE = DWORD_TABLES_SIZE_POW_2 - 1

/**
 * POW10 table is the primary and first entry in DWORD_TABLES.
 * Contains powers of 10 up thru 256 bits 10**77
 *
 * Powers of 10 that require 64 and 128 bits consume 2 entries each.
 * That is, up thru 10**19 they consume 2 dwords with the hi dword always 0L.
 *
 * Powers of 10 that require 192 and 256 bits consume 4 entries each.
 * Powers of 10 that would fit in 3 entries are zero-padded to consume 4.
 *
 * So, there are 2 tiers ... 2 entries and 4 entries.
 */
internal expect val POW10: LongArray
internal const val POW10_BCE = 0xFF // POW10_DWORD_COUNT is 234, so this covers all POW10

internal const val POW10_192_BASE = (POW10_64_COUNT + POW10_128_COUNT) * 2
internal const val POW10_256_BASE = POW10_192_BASE + POW10_192_COUNT * 4
// POW10_DWORD_COUNT = 234
internal const val POW10_DWORD_COUNT = (POW10_256_BASE + POW10_256_COUNT * 4)

/**
 * All powers of 5 that fit in 64-bits stored in DWORD_TABLES
 * at POW5_64_BASE.
 */
internal const val POW5_64_BASE = POW10_DWORD_COUNT
internal const val POW5_64_MAXX = 28

/**
 * Barrett mu parameter for powers of 5 that fit in 32 bits,
 * stored in DWORD_TABLES at BARRETT_POW5_MU_BASE
 *
 * When combined with removal of powers of 2 by shifting low bits
 * this enables multi-limb division by powers of 10 up thru 10**13.
 */
internal const val BARRETT_POW5_MU_BASE = POW5_64_BASE + POW5_64_MAXX
internal const val BARRETT_POW5_MU_MAXX = 14

/**
 * Magic division m parameter for powers of 10 that fit in 64 bits,
 * stored in DWORD_TABLES at MAGIC_POW10_M_BASE.
 *
 * Provides division of 64-bit dividend by power of 10 divisor up thru 10**19
 */
internal const val MAGIC_POW10_M_BASE = BARRETT_POW5_MU_BASE + BARRETT_POW5_MU_MAXX
internal const val MAGIC_POW10_M_MAXX = 20

/**
 * Parameters for distinct ranges of reciprocal division by powers of 5 stored in
 * DWORD_TABLES at RANGE_RECIP_MUL_PARAMS_BASE.
 *
 * Specific params are calculated for each combination of dividend digit count
 * q in [20,77] and divisor power of 10 k in [14,43]. Adjoining entries are
 * then merged to reduce table size when doing so would not increase
 * the number of run-time operations ... would not decrease performance.
 */
internal const val RANGE_RECIP_MUL_PARAMS_BASE = MAGIC_POW10_M_BASE + MAGIC_POW10_M_MAXX
internal const val RANGE_RECIP_MUL_PARAMS_MAXX = 678

internal const val DWORD_TABLES_COUNT = RANGE_RECIP_MUL_PARAMS_BASE + RANGE_RECIP_MUL_PARAMS_MAXX

private val checkSize_DWORD_TABLES = run {
    //println("DWORD_TABLES_COUNT:$DWORD_TABLES_COUNT EXPECTED_DWORD_TABLES_COUNT:$EXPECTED_DWORD_TABLES_COUNT")
    check(DWORD_TABLES_COUNT == EXPECTED_DWORD_TABLES_COUNT)
}

// barrett division thru by 10**13 by shifting out powers of 2 and using and pow5
internal const val BARRETT_POW10_MAXX = BARRETT_POW5_MU_MAXX
internal const val BARRETT_POW10_MAX = BARRETT_POW10_MAXX - 1

/**
 * BYTE_TABLES is a single allocated array of 2k bytes that is used to store
 * multiple single-byte tables.
 *
 * - POW10_BITLEN is a direct lookup of the number of bits required
 *   to store a power of 10. 10**0 requires 1 bit. 10**76 requires 253 bits.
 *
 * - MAGIC_FLAG_AND_SHIFT
 */
internal const val BYTE_TABLES_SIZE_POW_2 = 2048
expect internal val BYTE_TABLES: ByteArray
internal const val BYTE_TABLES_BCE = BYTE_TABLES_SIZE_POW_2 - 1 // 0x7FF

internal val POW10_BITLEN = BYTE_TABLES
internal const val POW10_BITLEN_BCE = BYTE_TABLES_BCE

internal const val POW10_BITLEN_COUNT = MAXX_DIGIT_LEN

/**
 * Magic division lookup table containing the "additional bit" flag
 * and the shr shift right value.
 */
internal const val MAGIC_FLAG_AND_SHIFT_BASE = POW10_BITLEN_COUNT
internal const val MAGIC_FLAG_AND_SHIFT_MAXX = MAGIC_POW10_M_MAXX

// RRMP10 == Range Recip Mul Pow10

// if the dividend has less than 20 digits then division can be handled by other means
internal const val RRMP10_Q_MIN = POW10_64_COUNT
// 256-bit coefficient supports all 77 digit numbers and some 78 digit numbers.
// We need to support 77 digit numbers because division may require
// (38 + 38) + 1
internal const val RRMP10_Q_MAXX = 78 // exclusive
internal const val RRMP10_K_MIN = BARRETT_POW10_MAXX
internal const val RRMP10_K_MAXX = RRMP10_Q_MAXX - 34

internal const val RRMP10_ENCODE_BASE_INTERCEPT = 768
internal const val RRMP10_ENCODE_BLOCK_MULTIPLIER = 52
internal const val RRMP10_LOOKUP_ROW_SIZE = 32 // K_MAXX - K_MIN
internal const val RRMP10_LOOKUP_SHIFT = 5
internal const val RRMP10_LOOKUP_TABLE_SIZE = (RRMP10_Q_MAXX - RRMP10_Q_MIN) shl RRMP10_LOOKUP_SHIFT


internal const val RESOURCE_TABLE_PATHNAME = "/com/decimal128/decimal/decimal128_tables.bin"
internal const val EXPECTED_TABLE_VERSION = 0x0008_D128
internal const val EXPECTED_DWORD_TABLES_COUNT = 974
internal const val EXPECTED_BYTE_TABLES_COUNT = 1955 // BYTE_TABLES_SIZE
internal const val EXPECTED_DWORD_TABLES_FNV1A = 2073989853
internal const val EXPECTED_BYTE_TABLES_FNV1A = -409221829

/**
 * DivRangeRecipMulPow10 needs a 2-dimensional array for
 * digitLenDividend by powerOfTenDivisor ... q * k.
 *
 * This is used as an index into RANGE_RECIP_MUL_PARAMS where
 * the actual params are stored.
 *
 * The index is stored with some complicated encoding so that
 * the entries will fit in a single byte.
 */
internal const val RRMP10_LOOKUP_BASE = MAGIC_FLAG_AND_SHIFT_BASE + MAGIC_FLAG_AND_SHIFT_MAXX
internal const val RRMP10_LOOKUP_MAXX = RRMP10_LOOKUP_TABLE_SIZE

internal const val BYTES_TABLE_TERMINATOR = 1
internal const val BYTE_TABLES_COUNT = RRMP10_LOOKUP_BASE + RRMP10_LOOKUP_MAXX + BYTES_TABLE_TERMINATOR

private val checkSize_BYTE_TABLES = check(BYTE_TABLES_COUNT == EXPECTED_BYTE_TABLES_COUNT)

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
