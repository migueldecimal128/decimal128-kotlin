// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

private inline fun d128Encode(d: Decimal, isDpd: Boolean, out: Pentad) {
    if (isDpd)
        dpd128Encode(d, out)
    else
        bid128Encode(d, out)
}

private inline fun d128Decode(isDpd: Boolean, dec128Hi: Long, dec128Lo: Long): Decimal =
    if (isDpd)
        dpd128Decode(dec128Hi, dec128Lo)
    else
        bid128Decode(dec128Hi, dec128Lo)

/**
 * Encodes a [Decimal] value into a [LongArray] as a 128-bit decimal (DPD or BID format).
 *
 * @param d The decimal value to encode.
 * @param isDpd `true` to encode in DPD format, `false` for BID format.
 * @param longs The destination array. Must have at least [offset] + 2 elements.
 * @param offset Index of the first (least significant, if little-endian) long to write.
 * @param isLittleEndian `true` if the least significant long is stored at [offset].
 * @param pentad Scratch buffer used during encoding.
 */
internal fun d128Encode(d: Decimal, isDpd: Boolean, longs: LongArray, offset: Int,
                        isLittleEndian: Boolean, pentad: Pentad) {
    require (offset >= 0 && offset + 2 <= longs.size)
    d128Encode(d, isDpd, pentad)
    val iLS = offset + if (isLittleEndian) 0 else 1
    val iMS = offset + if (isLittleEndian) 1 else 0
    longs[iLS] = pentad.dw0
    longs[iMS] = pentad.dw1
}

/**
 * Encodes a [Decimal] value into a [ByteArray] as a 128-bit decimal (DPD or BID format).
 *
 * @param d The decimal value to encode.
 * @param isDpd `true` to encode in DPD format, `false` for BID format.
 * @param bytes The destination array. Must have at least [offset] + 16 elements.
 * @param offset Index of the first byte to write.
 * @param isLittleEndian `true` if the least significant byte is stored at [offset].
 * @param pentad Scratch buffer used during encoding.
 */
internal fun d128Encode(d: Decimal, isDpd: Boolean, bytes: ByteArray, offset: Int,
                        isLittleEndian: Boolean, pentad: Pentad) {
    require (offset >= 0 && offset + 16 <= bytes.size)
    d128Encode(d, isDpd, pentad)

    val lo: Long
    val hi: Long
    val shiftStep: Int
    var shift: Int
    if (isLittleEndian) {
        lo = pentad.dw0
        hi = pentad.dw1
        shiftStep = 8
        shift = 0
    } else {
        lo = pentad.dw1
        hi = pentad.dw0
        shiftStep = -8
        shift = 56
    }
    for (i in offset..<offset + 8) {
        bytes[i    ] = (lo shr shift).toByte()
        bytes[i + 8] = (hi shr shift).toByte()
        shift += shiftStep
    }
}

/**
 * Decodes a 128-bit decimal value (DPD or BID format) from a [LongArray].
 *
 * @param isDpd `true` to decode as DPD format, `false` for BID format.
 * @param longs The source array. Must have at least [offset] + 2 elements.
 * @param offset Index of the first (least significant, if little-endian) long to read.
 * @param isLittleEndian `true` if the least significant long is stored at [offset].
 * @return The decoded [Decimal] value.
 */
internal fun d128Decode(isDpd: Boolean, longs: LongArray, offset: Int,
                        isLittleEndian: Boolean): Decimal {
    require (offset >= 0 && offset + 2 <= longs.size)

    val iLS = offset + if (isLittleEndian) 0 else 1
    val iMS = offset + if (isLittleEndian) 1 else 0
    val dec128Hi = longs[iMS]
    val dec128Lo = longs[iLS]
    return d128Decode(isDpd, dec128Hi, dec128Lo)
}

/**
 * Decodes a 128-bit decimal value (DPD or BID format) from a [ByteArray].
 *
 * @param isDpd `true` to decode as DPD format, `false` for BID format.
 * @param bytes The source array. Must have at least [offset] + 16 elements.
 * @param offset Index of the first byte to read.
 * @param isLittleEndian `true` if the least significant byte is stored at [offset].
 * @return The decoded [Decimal] value.
 */
internal fun d128Decode(isDpd: Boolean, bytes: ByteArray, offset: Int,
                        isLittleEndian: Boolean): Decimal {
    require (offset >= 0 && offset + 16 <= bytes.size)

    var lo = 0L
    var hi = 0L
    val shiftStep = if (isLittleEndian) 8 else -8
    var shift = if (isLittleEndian) 0 else 56
    for (i in offset..<offset + 8) {
        lo = lo or ((bytes[i    ].toLong() and 0xFFL) shl shift)
        hi = hi or ((bytes[i + 8].toLong() and 0xFFL) shl shift)
        shift += shiftStep
    }
    val dec128Hi = if (isLittleEndian) hi else lo
    val dec128Lo = if (isLittleEndian) lo else hi
    return d128Decode(isDpd, dec128Hi, dec128Lo)
}

