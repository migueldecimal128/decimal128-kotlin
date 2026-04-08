package com.decimal128.decimal

actual open class DecTmps {
    internal actual val mdecResult: MutDec = MutDec()

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    internal actual val mdecBridge1: MutDec = MutDec()
    internal actual val mdecBridge2: MutDec = MutDec()
    internal actual val mdecBridge3: MutDec = MutDec()

    // use of these tmps should be internal to the MutDec layer
    internal actual val mdecArg1: MutDec = MutDec()
    internal actual val mdecDivRemPow: MutDec = MutDec()

    internal actual val mdecTrans1: MutDec = MutDec()
    internal actual val mdecTrans2: MutDec = MutDec()
    internal actual val mdecTrans3: MutDec = MutDec()

    // used at the lowest level C256 operations for
    // unsigned sums, diffs, etc.
    internal actual val pentad: Pentad = Pentad()

    internal actual val c256: C256 = C256()

    internal actual val knuthD: IntArray = IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.

    // perhaps this should be 76 in order to support 253-bit coefficients
    // in flight in the debugger
    internal actual val utf8BytesPrintOnly: ByteArray = ByteArray(MAX_DEC77_CHAR_LEN)

    internal actual val parseStringLatin1Iterator: StringLatin1Iterator = StringLatin1Iterator("")
}