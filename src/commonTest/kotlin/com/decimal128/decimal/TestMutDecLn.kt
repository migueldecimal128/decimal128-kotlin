package com.decimal128.decimal

import kotlin.test.Test

class TestMutDecLn {

    @Test
    fun test1() {
        val x = MutDec().setOne()
        val z = MutDec()
        z.setLn(x)
    }
}