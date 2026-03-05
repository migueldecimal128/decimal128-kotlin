package com.decimal128.decimal

class DwQuad {
    var dw0 = 0L
    var dw1 = 0L
    var dw2 = 0L
    var dw3 = 0L

    override fun toString(): String {
        return "[${dw3.toULong()},${dw2.toULong()},${dw1.toULong()},${dw0.toULong()}]"
    }
}