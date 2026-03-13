package com.decimal128.decimal


/**
 * A Pentad is a 5-tuple containing a single Int word and 4x Long dwords
 * that is passed in as temporary to facilitate return of multiple
 * primitive integer values from operations without heap allocation of
 * Pair() or Triple().
 */
actual class Pentad {
    @JvmField
    actual var w = 0
    @JvmField
    actual var dw0 = 0L
    @JvmField
    actual var dw1 = 0L
    @JvmField
    actual var dw2 = 0L
    @JvmField
    actual var dw3 = 0L

    actual override fun toString(): String {
        return "[w:${w.toUInt()} dw0:${dw0.toULong()} dw1:${dw1.toULong()} dw2:${dw2.toULong()} dw3:${dw3.toULong()}]"
    }
}