package com.decimal128.decimal

class DwPair {
    var dw1 = 0L
    var dw0 = 0L

    override fun toString(): String {
        return "[${dw1.toULong()},${dw0.toULong()}]"
    }
}