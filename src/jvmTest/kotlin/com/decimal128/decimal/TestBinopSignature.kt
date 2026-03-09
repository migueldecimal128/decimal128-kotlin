package com.decimal128.decimal

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
        val zer_zer = binopSignatureOf(ZERO.steal, ZERO.steal)
        assertEquals(ZER_ZER, zer_zer)
        val zer_fnz = binopSignatureOf(ZERO.steal, ONE.steal)
        assertEquals(ZER_FNZ, zer_fnz)
        val zer_inf = binopSignatureOf(ZERO.steal, INFINITY.steal)
        assertEquals(ZER_INF, zer_inf)

        val fnz_zer = binopSignatureOf(ONE.steal, ZERO.steal)
        assertEquals(FNZ_ZER, fnz_zer)
        val fnz_fnz = binopSignatureOf(ONE.steal, ONE.steal)
        assertEquals(FNZ_FNZ, fnz_fnz)
        val fnz_inf = binopSignatureOf(ONE.steal, INFINITY.steal)
        assertEquals(FNZ_INF, fnz_inf)

        val inf_zer = binopSignatureOf(INFINITY.steal, ZERO.steal)
        assertEquals(INF_ZER, inf_zer)
        val inf_fnz = binopSignatureOf(INFINITY.steal, ONE.steal)
        assertEquals(INF_FNZ, inf_fnz)
        val inf_inf = binopSignatureOf(INFINITY.steal, INFINITY.steal)
        assertEquals(INF_INF, inf_inf)

        val nan_zer = binopSignatureOf(NaN.steal, ZERO.steal)
        assertEquals(NAN_ZER, nan_zer)
        val fnz_nan =binopSignatureOf(ONE.steal, sNaN.steal)
        assertEquals(FNZ_NAN, fnz_nan)
    }

}
