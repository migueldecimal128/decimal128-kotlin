package com.decimal128.decimal

internal class DecTmps {
    val mdecResult = MutDec()

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    val mdecBridge1 = MutDec()
    val mdecBridge2 = MutDec()
    val mdecBridge3 = MutDec()

    // use of these tmps should be internal to the MutDec layer
    val mdecArg1 = MutDec()
    val mdecDiv = MutDec()

    val mdecFusedProduct = MutDec()

    val mdecTrans1 = MutDec()
    val mdecTrans2 = MutDec()
    val mdecTrans3 = MutDec()
    val mdecTrans4 = MutDec()

    //
    val pentad1 = Pentad()
    val pentad2 = Pentad()

    val c256 = C256()

    val knuthD = IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.
    val c256PrintOnly = C256()
    val bytesPrintOnly = ByteArray(MAX_DEC38_CHAR_LEN)
}