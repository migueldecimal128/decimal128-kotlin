package com.decimal128.decimal


/**
 * A Pentad is a 5-tuple containing a single Int word and 4x Long dwords
 * that is passed in as temporary to facilitate return of multiple
 * primitive integer values from operations without heap allocation of
 * Pair() or Triple().
 */
expect class Pentad() {
    var w: Int
    var dw0: Long
    var dw1: Long
    var dw2: Long
    var dw3: Long

    override fun toString(): String

}