package com.decimal128.decimal

expect open class DecTmps() {
    internal val mdecResult: MutDec

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    internal val mdecBridge1: MutDec
    internal val mdecBridge2: MutDec
    internal val mdecBridge3: MutDec

    // use of these tmps should be internal to the MutDec layer
    internal val mdecArg1: MutDec
    internal val mdecDiv: MutDec

    internal val mdecFusedProduct: MutDec

    internal val mdecTrans1: MutDec
    internal val mdecTrans2: MutDec
    internal val mdecTrans3: MutDec
    internal val mdecTrans4: MutDec

    //
    internal val pentad1: Pentad
    internal val pentad2: Pentad

    internal val c256: C256


    internal val knuthD: IntArray // IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.
    internal val c256PrintOnly: C256
    internal val utf8BytesPrintOnly: ByteArray
    internal val parseStringLatin1Iterator: StringLatin1Iterator
}

