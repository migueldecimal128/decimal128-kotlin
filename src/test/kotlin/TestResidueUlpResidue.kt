package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.decimal128.Residue.Companion.HALF
import com.decimal128.Residue.Companion.LT_HALF
import com.decimal128.Residue.Companion.GT_HALF
import com.decimal128.Residue.Companion.EXACT

import java.math.BigInteger


class TestResidueUlpResidue {

    val verbose = false

    class UlpResidueCase(val bi: BigInteger) {
        constructor(str: String) : this(BigInteger(str))

        val coeff = Coeff(bi)
        val pow10 = BigInteger.TEN.pow(coeff.digitLen)
        val bix2 = bi.shiftLeft(1)
        val cmp = bix2.compareTo(pow10)
        val expectedResidue = (
                if (coeff.digitLen == 0)
                    EXACT
                else if (cmp < 0)
                    LT_HALF
                else if (cmp == 0)
                    HALF
                else
                    GT_HALF
                )

        override fun toString() : String = "$bi (${coeff.digitLen}) $expectedResidue"
    }

    val residueCases = arrayOf(
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("9999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("5000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("4999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("100000000000000000001"),


        UlpResidueCase("1"),
        UlpResidueCase("4"),
        UlpResidueCase("5"),
        UlpResidueCase("6"),

        UlpResidueCase("11"),
        UlpResidueCase("49"),
        UlpResidueCase("50"),
        UlpResidueCase("51"),
        UlpResidueCase("99"),

        UlpResidueCase("101"),
        UlpResidueCase("499"),
        UlpResidueCase("500"),
        UlpResidueCase("501"),
        UlpResidueCase("999"),

        UlpResidueCase("1001"),
        UlpResidueCase("4999"),
        UlpResidueCase("5000"),
        UlpResidueCase("5001"),
        UlpResidueCase("9999"),

        // 18 digits
        UlpResidueCase("100000000000000001"),
        UlpResidueCase("499999999999999999"),
        UlpResidueCase("500000000000000000"),
        UlpResidueCase("500000000000000001"),
        UlpResidueCase("999999999999999999"),

        // 19 digits
        UlpResidueCase("1000000000000000001"),
        UlpResidueCase("4999999999999999999"),
        UlpResidueCase("5000000000000000000"),
        UlpResidueCase("5000000000000000001"),
        UlpResidueCase("9999999999999999999"),

        // 20 digits
        UlpResidueCase("10000000000000000001"),
        UlpResidueCase("49999999999999999999"),
        UlpResidueCase("50000000000000000000"),
        UlpResidueCase("50000000000000000001"),
        UlpResidueCase("99999999999999999999"),

        // 21 digits
        UlpResidueCase("100000000000000000001"),
        UlpResidueCase("499999999999999999999"),
        UlpResidueCase("500000000000000000000"),
        UlpResidueCase("500000000000000000001"),
        UlpResidueCase("999999999999999999999"),

        // 37 digits
        UlpResidueCase("1000000000000000000000000000000000001"),
        UlpResidueCase("4999999999999999999999999999999999999"),
        UlpResidueCase("5000000000000000000000000000000000000"),
        UlpResidueCase("5000000000000000000000000000000000001"),
        UlpResidueCase("9999999999999999999999999999999999999"),

        // 38 digits
        UlpResidueCase("10000000000000000000000000000000000001"),
        UlpResidueCase("49999999999999999999999999999999999999"),
        UlpResidueCase("50000000000000000000000000000000000000"),
        UlpResidueCase("50000000000000000000000000000000000001"),
        UlpResidueCase("99999999999999999999999999999999999999"),

        // 39 digits
        UlpResidueCase("100000000000000000000000000000000000001"),
        UlpResidueCase("499999999999999999999999999999999999999"),
        UlpResidueCase("500000000000000000000000000000000000000"),
        UlpResidueCase("500000000000000000000000000000000000001"),
        UlpResidueCase("999999999999999999999999999999999999999"),

        // 40 digits
        UlpResidueCase("1000000000000000000000000000000000000001"),
        UlpResidueCase("4999999999999999999999999999999999999999"),
        UlpResidueCase("5000000000000000000000000000000000000000"),
        UlpResidueCase("5000000000000000000000000000000000000001"),
        UlpResidueCase("9999999999999999999999999999999999999999"),

        // 56 digits
        UlpResidueCase("10000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("49999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("99999999999999999999999999999999999999999999999999999999"),

        // 57 digits
        UlpResidueCase("100000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("499999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("500000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("500000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("999999999999999999999999999999999999999999999999999999999"),

        // 58 digits
        UlpResidueCase("1000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("4999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("5000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("5000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("9999999999999999999999999999999999999999999999999999999999"),

        // 59 digits
        UlpResidueCase("10000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("49999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("99999999999999999999999999999999999999999999999999999999999"),

        // 76 digits
        UlpResidueCase("1000000000000000000000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("4999999999999999999999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("5000000000000000000000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("5000000000000000000000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("9999999999999999999999999999999999999999999999999999999999999999999999999999"),

        // 77 digits
        UlpResidueCase("10000000000000000000000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("49999999999999999999999999999999999999999999999999999999999999999999999999999"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000000000000000000000000"),
        UlpResidueCase("50000000000000000000000000000000000000000000000000000000000000000000000000001"),
        UlpResidueCase("99999999999999999999999999999999999999999999999999999999999999999999999999999"),

        )

    @Test
    fun test() {
        for (residueCase in residueCases)
            test1(residueCase)
    }

    fun test1(tc: UlpResidueCase) {
        if (verbose)
            println("$tc")
        val observedResidue = Residue.residueFrom(tc.coeff)
        assertEquals(tc.expectedResidue, observedResidue)
    }

}

