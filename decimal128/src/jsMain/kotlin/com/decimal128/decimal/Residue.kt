// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.RoundingDirection.Companion.TIES_TO_AWAY
import com.decimal128.decimal.RoundingDirection.Companion.TIES_TO_EVEN
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_POSITIVE
import com.decimal128.decimal.RoundingDirection.Companion.TOWARD_ZERO

actual value class Residue internal constructor(val value:Int) {

    actual companion object {
        actual val EXACT = Residue(0)
        actual val LT_HALF = Residue(1)
        actual val HALF = Residue(2)
        actual val GT_HALF = Residue(3)

        actual operator fun invoke(res: Int) = Residue(res and 0x03)

    }

    override fun toString() = toDebugString()
}
