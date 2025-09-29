package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.*
import java.lang.Math.ceil

@OptIn(ExperimentalStdlibApi::class)
class GenerateMutDec128Constants {
    class Constant(val rootName: String, val dwordCount: Int, val bi: BigInteger) {
        constructor(rootName: String, n: Long) : this(rootName, 0, BigInteger.valueOf(n))
    }

    val constants = arrayOf(
        Constant("Automatically generated", -1, ZERO),
        Constant("POW10_64_COUNT", 20),
        Constant("POW10_128_COUNT", 19),
        Constant("POW10_192_COUNT", 19),
        Constant("POW10_256_COUNT", 20),
        Constant("MIN_POW10_DIGIT_LEN_128", 20),
        Constant("MIN_POW10_DIGIT_LEN_192", 39),
        Constant("MIN_POW10_DIGIT_LEN_256", 58),
        Constant("MAX_DIGIT_LEN", 78),
    )
    @Test
    fun generateConstants() {
        for (constant in constants) {
            generateHexConstant(constant)
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

