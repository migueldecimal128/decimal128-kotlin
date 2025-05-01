package com.decimal128

import com.decimal128.CoeffSet.coeffSetZero
import java.lang.Long.numberOfLeadingZeros


object CoeffBits {

    fun setLoBit(c: Coeff, dw0: Long) {
        val masked = dw0 and 1
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L
        c.dw0 = masked
        c.digitLen = masked.toInt()
    }

    fun bitLength(c: Coeff): Int {
        return when {
            (c.digitLen < 20 || c.digitLen == 20 && c.dw1 == 0L) ->
                64 - numberOfLeadingZeros(c.dw0)
            (c.digitLen < 39 || c.digitLen == 39 && c.dw2 == 0L) ->
                128 - numberOfLeadingZeros(c.dw1)
            (c.digitLen < 58 || c.digitLen == 58 && c.dw3 == 0L) ->
                192 - numberOfLeadingZeros(c.dw2)
            else ->
                256 - numberOfLeadingZeros(c.dw3)
        }
    }

}
