package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestBinopSignature {


    @Test
    fun test() {
        val zer_zer = binopSignatureOf(Decimal.Companion.ZERO.steal, Decimal.Companion.ZERO.steal)
        assertEquals(ZER_ZER, zer_zer)
        val zer_fnz = binopSignatureOf(Decimal.Companion.ZERO.steal, Decimal.Companion.ONE.steal)
        assertEquals(ZER_FNZ, zer_fnz)
        val zer_inf = binopSignatureOf(Decimal.Companion.ZERO.steal, Decimal.Companion.INFINITY.steal)
        assertEquals(ZER_INF, zer_inf)

        val fnz_zer = binopSignatureOf(Decimal.Companion.ONE.steal, Decimal.Companion.ZERO.steal)
        assertEquals(FNZ_ZER, fnz_zer)
        val fnz_fnz = binopSignatureOf(Decimal.Companion.ONE.steal, Decimal.Companion.ONE.steal)
        assertEquals(FNZ_FNZ, fnz_fnz)
        val fnz_inf = binopSignatureOf(Decimal.Companion.ONE.steal, Decimal.Companion.INFINITY.steal)
        assertEquals(FNZ_INF, fnz_inf)

        val inf_zer = binopSignatureOf(Decimal.Companion.INFINITY.steal, Decimal.Companion.ZERO.steal)
        assertEquals(INF_ZER, inf_zer)
        val inf_fnz = binopSignatureOf(Decimal.Companion.INFINITY.steal, Decimal.Companion.ONE.steal)
        assertEquals(INF_FNZ, inf_fnz)
        val inf_inf = binopSignatureOf(Decimal.Companion.INFINITY.steal, Decimal.Companion.INFINITY.steal)
        assertEquals(INF_INF, inf_inf)

        val nan_zer = binopSignatureOf(Decimal.Companion.NaN.steal, Decimal.Companion.ZERO.steal)
        assertEquals(NAN_ZER, nan_zer)
        val fnz_nan =binopSignatureOf(Decimal.Companion.ONE.steal, Decimal.Companion.sNaN.steal)
        assertEquals(FNZ_NAN, fnz_nan)
    }

}