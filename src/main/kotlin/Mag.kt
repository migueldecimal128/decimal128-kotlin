package com.decimal128

import kotlin.math.max
import kotlin.math.min

const val MIN_SPECIAL_VALUE = 1000000000
const val NON_FINITE_INF = 1000000000
const val NON_FINITE_QNAN = 1000000001
const val NON_FINITE_SNAN = 1000000002

const val CAPPED_EXP_MIN = -25000
const val CAPPED_EXP_MAX = 25000

@Suppress("NOTHING_TO_INLINE")
inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

open class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) : Coeff() {
    var qExp = 0

    fun magMutateScaleUpPow10(pow10: Int, sign: Int, ctx: DecimalContext): Residue {
        val headroom = PRECISION_34 - digitLen
        val scaleUp = min(headroom, pow10)
        this.coeffSetScaleUpPow10(this, scaleUp)
        qExp += pow10 - scaleUp
        return Residue.EXACT
    }

    fun magMutateScaleDownPow10(pow10: Int, sign: Int, ctx: DecimalContext): Residue {
        var residue = Residue.EXACT
        if (! coeffIsZero()) {
            if (pow10 >= digitLen) {
                residue = (
                        if (pow10 == digitLen)
                            Residue.residueFrom(this)
                        else
                            Residue.LT_HALF
                        )
                coeffSetZero()
            }
        } else {
            residue = this.coeffSetScaleDownPow10(this, pow10)
        }
        qExp -= pow10
        return residue
    }

}