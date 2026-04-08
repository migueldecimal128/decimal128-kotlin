package com.decimal128.decimal

actual open class DecTmps {
    @JvmField
    internal actual val mdecBridgeResult: MutDec = MutDec()

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    @JvmField
    internal actual val mdecBridge1: MutDec = MutDec()
    @JvmField
    internal actual val mdecBridge2: MutDec = MutDec()
    @JvmField
    internal actual val mdecBridge3: MutDec = MutDec()

    // use of these tmps should be internal to the MutDec layer
    @JvmField
    internal actual val mdecFmaParseConvert: MutDec = MutDec()
    @JvmField
    internal actual val mdecDivRemPowCtzd: MutDec = MutDec()

    @JvmField
    internal actual val mdecTrans1: MutDec = MutDec()
    @JvmField
    internal actual val mdecTrans2: MutDec = MutDec()
    @JvmField
    internal actual val mdecTrans3: MutDec = MutDec()

    //
    @JvmField
    internal actual val pentad: Pentad = Pentad()

    @JvmField
    internal actual val c256: C256 = C256()

    @JvmField
    internal actual val knuthD: IntArray = IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.

    // perhaps this should be 76 in order to support 253-bit coefficients
    // in flight in the debugger
    @JvmField
    internal actual val utf8BytesPrintOnly: ByteArray = ByteArray(MAX_DEC77_CHAR_LEN)

    @JvmField
    internal actual val parseStringLatin1Iterator: StringLatin1Iterator = StringLatin1Iterator("")
}