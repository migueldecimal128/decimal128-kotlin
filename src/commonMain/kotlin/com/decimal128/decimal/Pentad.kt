package com.decimal128.decimal


/**
 * A Pentad is a 5-tuple containing a single Int word and 4x Long dwords
 * that is passed in as temporary to facilitate return of multiple
 * primitive integer values from operations without heap allocation of
 * Pair() or Triple().
 */
class Pentad {
    var w = 0
    var dw0 = 0L
    var dw1 = 0L
    var dw2 = 0L
    var dw3 = 0L

    override fun toString(): String {
        return "[w:${w.toUInt()}${dw3.toULong()},${dw2.toULong()},${dw1.toULong()},${dw0.toULong()}]"
    }
}