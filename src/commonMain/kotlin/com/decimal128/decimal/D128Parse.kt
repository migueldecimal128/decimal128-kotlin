// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.InvalidOperationReason.PARSE_INVALID_UNDERSCORE_LOCATION
import com.decimal128.decimal.InvalidOperationReason.PARSE_DOUBLE_DOT
import com.decimal128.decimal.InvalidOperationReason.PARSE_EMPTY_STRING
import com.decimal128.decimal.InvalidOperationReason.PARSE_NO_EXPONENT_DIGIT
import com.decimal128.decimal.InvalidOperationReason.PARSE_UNEXPECTED_CHAR
import com.decimal128.decimal.InvalidOperationReason.PARSE_VALUE_OUT_OF_RANGE
import kotlin.math.max
import kotlin.math.min

object D128Parse {

    fun parseDecimal(str: String, ctx: DecContext = DecContext.current()): Decimal {
        val md = ctx.tmps.mdecArg1
        MutDecParse.parseDecimal(md, str, ctx)
        return Decimal.from(md)
    }

}

