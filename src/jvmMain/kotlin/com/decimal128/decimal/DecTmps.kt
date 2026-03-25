package com.decimal128.decimal

actual open class DecTmps {
    @JvmField
    internal actual val mdecResult: MutDec = MutDec()

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
    internal actual val mdecArg1: MutDec = MutDec()
    @JvmField
    internal actual val mdecDiv: MutDec = MutDec()

    @JvmField
    internal actual val mdecFusedProduct: MutDec = MutDec()

    @JvmField
    internal actual val mdecTrans1: MutDec = MutDec()
    @JvmField
    internal actual val mdecTrans2: MutDec = MutDec()
    @JvmField
    internal actual val mdecTrans3: MutDec = MutDec()
    @JvmField
    internal actual val mdecTrans4: MutDec = MutDec()

    //
    @JvmField
    internal actual val pentad1: Pentad = Pentad()
    @JvmField
    internal actual val pentad2: Pentad = Pentad()

    @JvmField
    internal actual val c256: C256 = C256()

    @JvmField
    internal actual val knuthD: IntArray = IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.
    @JvmField
    internal actual val c256PrintOnly: C256 = C256()
    // perhaps this should be 76 in order to support 253-bit coefficients
    // in flight in the debugger
    @JvmField
    internal actual val bytesPrintOnly: ByteArray = ByteArray(MAX_DEC38_CHAR_LEN)
}