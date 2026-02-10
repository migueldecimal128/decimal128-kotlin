package com.decimal128.decimal

class DecTemps {
    val mdecResult = MutDec()

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    val mdecBridge1 = MutDec()
    val mdecBridge2 = MutDec()
    val mdecBridge3 = MutDec()

    // use of these tmps should be internal to the MutDec layer
    val mdecArg1 = MutDec()
    val mdecArg2 = MutDec()
    val mdecArg3 = MutDec()


    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.
    val c256Print = C256()
    val bytesPrint = ByteArray(MAX_DEC38_CHAR_LEN)
}