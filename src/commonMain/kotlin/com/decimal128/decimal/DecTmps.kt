package com.decimal128.decimal

expect open class DecTmps() {
    internal val mdecResult: MutDec

    // these tmps are for bridging from the 128-bit Decimal layer
    // to the 256-bit MutDec layer
    internal val mdecBridge1: MutDec
    internal val mdecBridge2: MutDec
    internal val mdecBridge3: MutDec

    // use of these tmps should be internal to the MutDec arithmetic layer
    internal val mdecArg1: MutDec
    internal val mdecDivRemPow: MutDec

    // use of these tmps is restricted to transcendental functions
    internal val mdecTrans1: MutDec
    internal val mdecTrans2: MutDec
    internal val mdecTrans3: MutDec

    // used at the lowest level C256 operations for
    // unsigned sums, diffs, etc.
    internal val pentad: Pentad

    internal val c256: C256


    internal val knuthD: IntArray // IntArray(32)

    // printing must have dedicated tmps because toString()
    // will be called during debugging and we don't want to
    // overwrite another tmp when stepping through code.
    internal val utf8BytesPrintOnly: ByteArray
    internal val parseStringLatin1Iterator: StringLatin1Iterator
}

