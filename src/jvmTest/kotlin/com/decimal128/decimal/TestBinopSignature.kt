package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.DecOld.Companion.INFINITY
import com.decimal128.decimal.DecOld.Companion.NaN
import com.decimal128.decimal.DecOld.Companion.ONE
import com.decimal128.decimal.DecOld.Companion.ZERO
import com.decimal128.decimal.DecOld.Companion.sNaN
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestBinopSignature {


    @Test
    fun test() {
        val zer_zer = BinopSignature.of(ZERO, ZERO)
        assertEquals(ZER_ZER, zer_zer)
        val zer_fnz = BinopSignature.of(ZERO, ONE)
        assertEquals(ZER_FNZ, zer_fnz)
        val zer_inf = BinopSignature.of(ZERO, INFINITY)
        assertEquals(ZER_INF, zer_inf)

        val fnz_zer = BinopSignature.of(ONE, ZERO)
        assertEquals(FNZ_ZER, fnz_zer)
        val fnz_fnz = BinopSignature.of(ONE, ONE)
        assertEquals(FNZ_FNZ, fnz_fnz)
        val fnz_inf = BinopSignature.of(ONE, INFINITY)
        assertEquals(FNZ_INF, fnz_inf)

        val inf_zer = BinopSignature.of(INFINITY, ZERO)
        assertEquals(INF_ZER, inf_zer)
        val inf_fnz = BinopSignature.of(INFINITY, ONE)
        assertEquals(INF_FNZ, inf_fnz)
        val inf_inf = BinopSignature.of(INFINITY, INFINITY)
        assertEquals(INF_INF, inf_inf)

        val qNaN1 = BinopSignature.of(NaN, ZERO)
        assertEquals(NAN_FOUND, qNaN1)
        val qNaN2 =BinopSignature.of(ONE, sNaN)
        assertEquals(NAN_FOUND, qNaN2)
    }

}
