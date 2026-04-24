// SPDX-License-Identifier: MIT

@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal const val VERIFY_ENABLED: Boolean = false

internal inline fun verify(block: () -> Boolean) {
    if (VERIFY_ENABLED)
        check(block())
}