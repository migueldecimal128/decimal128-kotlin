package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.INFINITY
import com.decimal128.decimal.Decimal.Companion.NaN
import com.decimal128.decimal.Decimal.Companion.ONE
import com.decimal128.decimal.Decimal.Companion.ZERO
import com.decimal128.decimal.Decimal.Companion.sNaN
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestBinopSignature {


    @Test
    fun test() {
        val zer_zer = binopSignatureOf(ZERO, ZERO)
        assertEquals(ZER_ZER, zer_zer)
        val zer_fnz = binopSignatureOf(ZERO, ONE)
        assertEquals(ZER_FNZ, zer_fnz)
        val zer_inf = binopSignatureOf(ZERO, INFINITY)
        assertEquals(ZER_INF, zer_inf)

        val fnz_zer = binopSignatureOf(ONE, ZERO)
        assertEquals(FNZ_ZER, fnz_zer)
        val fnz_fnz = binopSignatureOf(ONE, ONE)
        assertEquals(FNZ_FNZ, fnz_fnz)
        val fnz_inf = binopSignatureOf(ONE, INFINITY)
        assertEquals(FNZ_INF, fnz_inf)

        val inf_zer = binopSignatureOf(INFINITY, ZERO)
        assertEquals(INF_ZER, inf_zer)
        val inf_fnz = binopSignatureOf(INFINITY, ONE)
        assertEquals(INF_FNZ, inf_fnz)
        val inf_inf = binopSignatureOf(INFINITY, INFINITY)
        assertEquals(INF_INF, inf_inf)

        val qNaN1 = binopSignatureOf(NaN, ZERO)
        assertEquals(NAN_FOUND, qNaN1)
        val qNaN2 =binopSignatureOf(ONE, sNaN)
        assertEquals(NAN_FOUND, qNaN2)
    }

}
