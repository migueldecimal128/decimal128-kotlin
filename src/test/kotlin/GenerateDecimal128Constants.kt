package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.*
import kotlin.math.ceil

@OptIn(ExperimentalStdlibApi::class)
class GenerateDecimal128Constants {
    data class Constant(val rootName: String, val dwordCount: Int, val bi: BigInteger)

    val constants = arrayOf(
        Constant("Automatically generated", -1, ZERO),
        Constant("MIN_POW10_DIGIT_LEN_128", 0, BigInteger("20")),
        Constant("MIN_POW10_DIGIT_LEN_192", 0, BigInteger("39")),
        Constant("MIN_POW10_DIGIT_LEN_256", 0, BigInteger("58")),
        Constant("MAX_DIGIT_LEN", 0, BigInteger("78")),
        Constant("ONE_E19", 1, TEN.pow(19)),
        Constant("FIVE_E19", 2, TEN.pow(20).shiftRight(1)),
        Constant("ONE_E20", 2, TEN.pow(20)),
        Constant("ONE_E38", 3, TEN.pow(38)),
        Constant("FIVE_E38", 3, TEN.pow(39).shiftRight(1)),
        Constant("FIVE_E57", 3, TEN.pow(58).shiftRight(1)),
        Constant("ONE_E58", 4, TEN.pow(58)),
        Constant("ONE_E77", 4, TEN.pow(77)),
    )
    @Test
    fun generateConstants() {
        for (constant in constants) {
            if (constant != null)
                generateHexConstant(constant)
            else
                println()
        }
    }

    fun generateHexConstant(constant: Constant) {
        when (constant.dwordCount) {
            -1 -> dumpComment(constant.rootName)
            0 -> dumpInt(constant.rootName, constant.bi)
            1 -> dump64(constant.rootName, constant.bi)
            2 -> dump128(constant.rootName, constant.bi)
            3 -> dump192(constant.rootName, constant.bi)
            4 -> dump256(constant.rootName, constant.bi)
            }
    }

    fun dumpComment(str: String) {
        println("// $str")
    }

    fun dumpInt(name: String, bi: BigInteger) {
        val bdStr = BigDecimal(bi).stripTrailingZeros()
        val w0 = bi.toInt()
        val s0 = String.format("%-11s", w0)
        val hex0 = w0.toHexString(HexFormat.UpperCase)
        println("""
            |
            |const val ${name} = $s0 // 0x$hex0 $bdStr
        """.trimMargin())
    }

    fun dump64(name: String, bi: BigInteger) {
        val bdStr = BigDecimal(bi).stripTrailingZeros()
        val dw0 = bi.toLong()
        val s0 = String.format("%20sL", dw0)
        val hex0 = dw0.toHexString(HexFormat.UpperCase)
        println("""
            |
            |// $bdStr
            |const val ${name}_dw0 = $s0 // 0x$hex0
        """.trimMargin())
    }

    fun dump128(name: String, bi: BigInteger) {
        val bdStr = BigDecimal(bi).stripTrailingZeros()
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val s0 = String.format("%20sL", dw0)
        val s1 = String.format("%20sL", dw1)
        val hex0 = dw0.toHexString(HexFormat.UpperCase)
        val hex1 = dw1.toHexString(HexFormat.UpperCase)
        println("""
            |
            |// $bdStr
            |const val ${name}_dw1 = $s1 // 0x$hex1
            |const val ${name}_dw0 = $s0 // 0x$hex0
        """.trimMargin())
    }

    fun dump192(name: String, bi: BigInteger) {
        val bdStr = BigDecimal(bi).stripTrailingZeros()
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val s0 = String.format("%20sL", dw0)
        val s1 = String.format("%20sL", dw1)
        val s2 = String.format("%20sL", dw2)
        val hex0 = dw0.toHexString(HexFormat.UpperCase)
        val hex1 = dw1.toHexString(HexFormat.UpperCase)
        val hex2 = dw2.toHexString(HexFormat.UpperCase)
        println("""
            |
            |// $bdStr
            |const val ${name}_dw2 = $s2 // 0x$hex2
            |const val ${name}_dw1 = $s1 // 0x$hex1
            |const val ${name}_dw0 = $s0 // 0x$hex0
        """.trimMargin())
    }

    fun dump256(name: String, bi: BigInteger) {
        val bdStr = BigDecimal(bi).stripTrailingZeros()
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong()
        val s0 = String.format("%20sL", dw0)
        val s1 = String.format("%20sL", dw1)
        val s2 = String.format("%20sL", dw2)
        val s3 = String.format("%20sL", dw3)
        val hex0 = dw0.toHexString(HexFormat.UpperCase)
        val hex1 = dw1.toHexString(HexFormat.UpperCase)
        val hex2 = dw2.toHexString(HexFormat.UpperCase)
        val hex3 = dw3.toHexString(HexFormat.UpperCase)
        println("""
            |
            |// $bdStr
            |const val ${name}_dw3 = $s3 // 0x$hex3
            |const val ${name}_dw2 = $s2 // 0x$hex2
            |const val ${name}_dw1 = $s1 // 0x$hex1
            |const val ${name}_dw0 = $s0 // 0x$hex0
        """.trimMargin())
    }

}

