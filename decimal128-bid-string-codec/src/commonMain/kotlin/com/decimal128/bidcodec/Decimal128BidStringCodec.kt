package com.decimal128.bidcodec

/**
 * Thin codec converting between BID128 binary representations and decimal
 * string form, per IEEE 754-2019.
 *
 * BID128 values are represented as a pair of 64-bit longs: `dw1` (high 64 bits)
 * and `dw0` (low 64 bits). This object provides no arithmetic, no field
 * extraction, and no value-type wrapper — just string conversion in both
 * directions.
 *
 * For the full decimal128 implementation (arithmetic, comparison, rounding,
 * etc.), see the `decimal128-kotlin` artifact.
 */
public object Decimal128BidStringCodec {

    /**
     * Convert a BID128 value to its decimal string representation.
     *
     * @param dw1 high 64 bits of the BID128 encoding
     * @param dw0 low 64 bits of the BID128 encoding
     * @return the decimal string form of the BID128 value
     */
    public fun toString(dw1: Long, dw0: Long): String =
        TODO("not yet implemented")

    /**
     * Parse a decimal string into a BID128 value, writing the result into [dest].
     *
     * On success: `dest[0]` is set to `dw1` (high 64 bits), `dest[1]` is set
     * to `dw0` (low 64 bits), and this method returns `null`.
     *
     * On failure: `dest[0]` and `dest[1]` are set to zero, and this method
     * returns a human-readable error message describing the failure. The
     * exact wording of error messages is not part of the API contract and
     * may change between versions.
     *
     * @param dest a `LongArray` of size at least 2, into which the parsed
     *             BID128 value is written on success
     * @param str  the decimal string to parse
     * @return `null` on success, or a human-readable error message on failure
     * @throws IllegalArgumentException if `dest.size < 2`
     */
    public fun parseReturnError(dest: LongArray, str: String): String? {
        require(dest.size >= 2) { "dest must have size >= 2, was ${dest.size}" }
        TODO("not yet implemented")
    }
}
