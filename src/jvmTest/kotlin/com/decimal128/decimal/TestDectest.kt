package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.io.File
import java.util.EnumSet
import kotlin.test.assertEquals

class TestDectest {

    private val veryVerbose = true
    private val verbose = true

    private val prefix = "src/jvmTest/resources/dectest/"

    private var precision = 34
    private var rounding = "half_even"
    private var maxExponent = 6144
    private var minExponent = -6143
    private var version = ""
    private var extended = 0
    private var clamp = 0

    private val validRoundingStrings = arrayOf(
        "half_even",
        "half_up",
        "down",
        "ceiling",
        "floor",
        "up",
        "05up",
        "half_down",
    )

    private val decRoundings = arrayOf(
        DecRounding.ROUND_TIES_TO_EVEN,
        DecRounding.ROUND_TIES_TO_AWAY,
        DecRounding.ROUND_TOWARD_ZERO,
        DecRounding.ROUND_TOWARD_POSITIVE,
        DecRounding.ROUND_TOWARD_NEGATIVE,
    )

    private val dectestFiles = arrayOf(
        "dqMax.decTest",

        "dqAbs.decTest",
        "dqAdd.decTest",
        "dqBase.decTest",
        "dqCanonical.decTest",
        "dqCompare.decTest",
        "dqCompareSig.decTest",
        "dqCompareTotal.decTest",
        "dqCompareTotalMag.decTest",
        "dqCopy.decTest",
        "dqCopyAbs.decTest",
        "dqCopyNegate.decTest",
        "dqCopySign.decTest",
        "dqDivide.decTest",
        // dqDivideInt.decTest is not supported
        "dqEncode.decTest",
        "dqFMA.decTest",
        "dqLogB.decTest",

        "dqMinus.decTest",
        "dqMultiply.decTest",
        "dqSubtract.decTest",

        //"dqRemainder.decTest",
    )

    val ignoredCases = arrayOf(

        "dqabs900 abs  # -> NaN Invalid_operation", // IEEE says to ignore abs sign change
        "dqabs526 abs  -NaN22  -> -NaN22",
        "dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation",
        "dqabs523 abs  sNaN    ->  NaN   Invalid_operation",
        "dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation",
        "dqmns021 minus         NaN  -> NaN",
        "dqmns022 minus        -NaN  -> -NaN",
        "dqmns023 minus        sNaN  -> NaN  Invalid_operation",
        "dqmns024 minus       -sNaN  -> -NaN Invalid_operation",
        "dqmns031 minus       NaN13  -> NaN13",
        "dqmns032 minus      -NaN13  -> -NaN13",
        "dqmns033 minus      sNaN13  -> NaN13   Invalid_operation",
        "dqmns034 minus     -sNaN13  -> -NaN13  Invalid_operation",
        "dqmns035 minus       NaN70  -> NaN70",
        "dqmns036 minus      -NaN70  -> -NaN70",
        "dqmns037 minus      sNaN101 -> NaN101  Invalid_operation",
        "dqmns038 minus     -sNaN101 -> -NaN101 Invalid_operation",
        "dqmns111 minus          0   -> 0",
        "dqmns113 minus       0E+4   -> 0E+4",
        "dqmns115 minus     0.0000   -> 0.0000",
        "dqmns117 minus      0E-141  -> 0E-141",
        "dqcot9990 comparetotal 10  # -> NaN Invalid_operation",
        "dqcot9991 comparetotal  # 10 -> NaN Invalid_operation",
        "dqctm9990 comparetotmag 10  # -> NaN Invalid_operation",
        "dqctm9991 comparetotmag  # 10 -> NaN Invalid_operation",
        "dqfma2990 fma  10  #   0e+6144  -> NaN Invalid_operation",// # is null ... so Invalid
        "dqfma2991 fma   # 10   0e+6144  -> NaN Invalid_operation",// # is null ... so Invalid
        "dqlogb900  logb #   -> NaN Invalid_operation",
        // dectest was done before ieee754-2019, which redefined this
        // note that Colishaw decTest is using "max" for "maximumNumber"
        "dqmax161 max  sNaN -Inf   ->  NaN  Invalid_operation",
        "dqmax162 max  sNaN -1000  ->  NaN  Invalid_operation",
        "dqmax163 max  sNaN -1     ->  NaN  Invalid_operation",
        "dqmax164 max  sNaN -0     ->  NaN  Invalid_operation",
        "dqmax165 max  sNaN  0     ->  NaN  Invalid_operation",
        "dqmax166 max  sNaN  1     ->  NaN  Invalid_operation",
        "dqmax167 max  sNaN  1000  ->  NaN  Invalid_operation",
        "dqmax171 max -Inf  sNaN   ->  NaN  Invalid_operation",
        "dqmax172 max -1000 sNaN   ->  NaN  Invalid_operation",
        "dqmax173 max -1    sNaN   ->  NaN  Invalid_operation",
        "dqmax174 max -0    sNaN   ->  NaN  Invalid_operation",
        "dqmax175 max  0    sNaN   ->  NaN  Invalid_operation",
        "dqmax176 max  1    sNaN   ->  NaN  Invalid_operation",
        "dqmax177 max  1000 sNaN   ->  NaN  Invalid_operation",
        "dqmax178 max  Inf  sNaN   ->  NaN  Invalid_operation",
        "dqmax191 max  sNaN99 -Inf    ->  NaN99 Invalid_operation",
        "dqmax192 max  sNaN98 -1      ->  NaN98 Invalid_operation",
        "dqmax196 max -Inf    sNaN92  ->  NaN92 Invalid_operation",
        "dqmax197 max  0      sNaN91  ->  NaN91 Invalid_operation",
        "dqmax198 max  Inf   -sNaN90  -> -NaN90 Invalid_operation",
        "dqmax900 max 10  #  -> NaN Invalid_operation",
        "dqmax901 max  # 10  -> NaN Invalid_operation",

    )

    // Colishaw GDAS says that NaN triggers INVALID
    // in more operations than IEEE.
    // We will run those tests, but ignore the INVALID flag
    val ignoreInvalidOperationCases = arrayOf(
        "dqmul9990 multiply 10  # -> NaN Invalid_operation",
        "dqmul9991 multiply  # 10 -> NaN Invalid_operation",
        "dqsub9990 subtract 10  # -> NaN Invalid_operation",
        "dqsub9991 subtract  # 10 -> NaN Invalid_operation",
        "dqdiv9998 divide 10  # -> NaN Invalid_operation",
        "dqdiv9999 divide  # 10 -> NaN Invalid_operation",
        "dqadd9990 add 10  # -> NaN Invalid_operation",
        "dqadd9991 add  # 10 -> NaN Invalid_operation",
        "dqcom990 compare 10  # -> NaN Invalid_operation",
        "dqcom991 compare  # 10 -> NaN Invalid_operation",
        "dqcms821 comparesig  NaN -Inf    ->  NaN  Invalid_operation",
        "dqcms822 comparesig  NaN -1000   ->  NaN  Invalid_operation",
        "dqcms823 comparesig  NaN -1      ->  NaN  Invalid_operation",
        "dqcms824 comparesig  NaN -0      ->  NaN  Invalid_operation",
        "dqcms825 comparesig  NaN  0      ->  NaN  Invalid_operation",
        "dqcms826 comparesig  NaN  1      ->  NaN  Invalid_operation",
        "dqcms827 comparesig  NaN  1000   ->  NaN  Invalid_operation",
        "dqcms828 comparesig  NaN  Inf    ->  NaN  Invalid_operation",
        "dqcms829 comparesig  NaN  NaN    ->  NaN  Invalid_operation",
        "dqcms830 comparesig -Inf  NaN    ->  NaN  Invalid_operation",
        "dqcms831 comparesig -1000 NaN    ->  NaN  Invalid_operation",
        "dqcms832 comparesig -1    NaN    ->  NaN  Invalid_operation",
        "dqcms833 comparesig -0    NaN    ->  NaN  Invalid_operation",
        "dqcms834 comparesig  0    NaN    ->  NaN  Invalid_operation",
        "dqcms835 comparesig  1    NaN    ->  NaN  Invalid_operation",
        "dqcms836 comparesig  1000 NaN    ->  NaN  Invalid_operation",
        "dqcms837 comparesig  Inf  NaN    ->  NaN  Invalid_operation",
        "dqcms838 comparesig -NaN -NaN    -> -NaN  Invalid_operation",
        "dqcms839 comparesig +NaN -NaN    ->  NaN  Invalid_operation",
        "dqcms840 comparesig -NaN +NaN    -> -NaN  Invalid_operation",

        "dqcms860 comparesig  NaN9 -Inf   ->  NaN9    Invalid_operation",
        "dqcms861 comparesig  NaN8  999   ->  NaN8    Invalid_operation",
        "dqcms862 comparesig  NaN77 Inf   ->  NaN77   Invalid_operation",
        "dqcms863 comparesig -NaN67 NaN5  -> -NaN67   Invalid_operation",
        "dqcms864 comparesig -Inf  -NaN4  -> -NaN4    Invalid_operation",
        "dqcms865 comparesig -999  -NaN33 -> -NaN33   Invalid_operation",
        "dqcms866 comparesig  Inf   NaN2  ->  NaN2    Invalid_operation",
        "dqcms867 comparesig -NaN41 -NaN42 -> -NaN41  Invalid_operation",
        "dqcms868 comparesig +NaN41 -NaN42 ->  NaN41  Invalid_operation",
        "dqcms869 comparesig -NaN41 +NaN42 -> -NaN41  Invalid_operation",
        "dqcms870 comparesig +NaN41 +NaN42 ->  NaN41  Invalid_operation",
        "dqcms990 comparesig 10  # -> NaN Invalid_operation",
        "dqcms991 comparesig  # 10 -> NaN Invalid_operation",
        "dqcan244 comparesig  #7c400ff3ffff3fcff3fcff3fcff3fcff   -1000 -> #7c000ff3fcff3fcff3fcff3fcff3fcff Invalid_operation",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            processLine(tc)
    }

    val tcs = arrayOf(
        // these are adapted from Colishaw decTest
        // max == maximumNumber
        // behavior of sNaN treatment changed from
        // IEEE754-2008 => IEEE754-2019
        "dqmax161 max  sNaN -Inf   ->  -Inf  Invalid_operation",
        "dqmax162 max  sNaN -1000  ->  -1000  Invalid_operation",

        "dqadd36444 fma  1    1   -77e-36      ->  0.9999999999999999999999999999999999 Inexact Rounded",
        "dqadd3038 fma  1  '70000'  '10000e+34' -> '1.000000000000000000000000000000001E+38' Inexact Rounded",
        "dqfma2990 fma  10  #   0e+6144  -> NaN Invalid_operation",
        "dqfma2504  fma   0E-4260 1000E-4260    0e+6144  -> 0E-6176 Clamped",
        "dqfma0902 fma  0     0     NaN5   ->  NaN5",
        "dqfma0801 fma  Inf   Inf  -Inf    ->  NaN Invalid_operation",
        "dqfma0305  fma   1e-6176    0.1  0         -> 0E-6176 Underflow Subnormal Inexact Rounded Clamped",
        "dqfma0300  fma   9e+6144    10   0         -> Infinity  Overflow Inexact Rounded",
        "dqfma0268  fma          21590290365127685.3675       7853139227576541379426.8       -3275859437236180.761544  ->  1.695515562011520746125607502237558E+38 Inexact Rounded",
        "dqfma0258  fma        817941336593541742159684       733867339769310729266598      78563844650942419311830.8  ->  6.002604327732568490562249875306822E+47 Inexact Rounded",
        "dqfma0229  fma        2539892357016099706.4126      -996142232667504817717435       53682082598315949425.937  ->  -2.530094043253148806272276368579143E+42 Inexact Rounded",
        "dqfma0202  fma       68537985861355864457.5694      6565875762972086605.85969       35892634447236753.172812  ->  4.500119002100000209469729375698779E+38 Inexact Rounded",
        "dqcot850 comparetotal  sNaN  NaN   ->  -1",
        "dqcot101 comparetotal   7.0    7      -> -1",
        "dqadd9990 add 10  # -> NaN Invalid_operation",
        "dqadd9991 add  # 10 -> NaN Invalid_operation",
        "dqmul9991 multiply  # 10 -> NaN Invalid_operation",
        "dqmns117 minus      0E-141  -> 0E-141",
        "dqabs525 abs  sNaN33  ->  NaN33 Invalid_operation",
        "dqmul9990 multiply 10  # -> NaN Invalid_operation",
        "dqabs527 abs -sNaN33  -> -NaN33 Invalid_operation",
        "dqabs900 abs  # -> NaN Invalid_operation",
        "dqbas906 toSci '99e999999999'       -> Infinity Overflow  Inexact Rounded",
        "dqbas610 toSci  .0               -> 0.0",
        "dqbas519 toSci ''                -> NaN Conversion_syntax",
        "dqbas510 toSci ' +1'             -> NaN Conversion_syntax",

        "dqbas450  toSci 10000000000000000000000000000000009    -> 1.000000000000000000000000000000001E+34   Rounded Inexact",
        "dqbas444  toSci 10000000000000000000000000000000003    -> 1.000000000000000000000000000000000E+34   Rounded Inexact",

        "dqbas035 toSci '0.000000123456789'   -> '1.23456789E-7'",

        "rounding: half_even",
        "dqadd6445 add   1   -77e-37      ->  1.000000000000000000000000000000000 Inexact Rounded",

        "rounding: half_even",
        "dqmul767 multiply 1e-6069 1e-108 -> 0E-6176 Underflow Subnormal Inexact Rounded Clamped",

        "rounding:half_up",
        "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded",

        "rounding:    floor",
        "dqadd71720 add  0        0E-19  ->  0E-19",

        "dqabs526 abs  -NaN22  -> -NaN22",
        "dqabs523 abs  sNaN    ->  NaN   Invalid_operation",
        "dqmul699 multiply -NaN    -sNaN89 -> -NaN89 Invalid_operation",

        "rounding: half_even",
        "dqdiv788 divide -1000  Inf   -> -0E-6176 Clamped",
        "dqmul770 multiply 1e+40 1e+6101 -> 1.000000000000000000000000000000E+6141 Clamped",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "dqadd7882 add -NaN26    NaN28 -> -NaN26",
        "dqadd7841 add  sNaN -Inf   ->  NaN  Invalid_operation",
        "dqadd7861 add  NaN1   -Inf    ->  NaN1",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "dqadd7735 add -0    -0     -> -0     -- IEEE 754 special case",
        "dqadd7728 add -00.00 0E+3  -> 0.00",
        "rounding:half_up",
        "dqadd172 add '4.444444444444444444444444444444444'  '0.5555555555555555555555555555555565' -> '5.000000000000000000000000000000001' Inexact Rounded",
    )

    @Test
    fun testReadDectestFiles() {
        for (dectestFile in dectestFiles)
            read1(prefix + dectestFile)
    }

    fun read1(dectestFileName: String) {
        if (verbose)
            println("dectestFileName: $dectestFileName")
        val file = File(dectestFileName).bufferedReader()
        for (line in file.readLines())
            processLine(line)
    }

    fun processLine(line: String) {
        if (veryVerbose)
            println("line:$line")
        val commentIndex = line.indexOf("--")
        val trimmed = (if (commentIndex >= 0) line.substring(0, commentIndex) else line).trim()
        when {
            trimmed.length == 0 -> {}
            ignoredCases.contains(trimmed) -> {
                if (veryVerbose)
                    println("<<<ignored>>>")
            }
            processDirective(trimmed) -> {}
            processTest(trimmed) -> {}
            else -> {
                println("UNRECOGNIZED: $trimmed")
            }
        }
    }

    fun processDirective(line: String): Boolean {
        val colonIndex = line.indexOf(':')
        if (colonIndex < 0)
            return false
        if (veryVerbose)
            println(line)
        val directive = line.substring(0, colonIndex).trim().lowercase()
        val value = line.substring(colonIndex + 1).trim().lowercase()
        when (directive) {
            "precision" -> {
                val p = value.toInt()
                require (p >= 3)
                require (p <= 38)
                precision = p
            }
            "rounding" -> {
                require (value in validRoundingStrings)
                rounding = value
            }
            "maxexponent" -> {
                val e = value.toInt()
                require (e >= 0)
                require (e <= 999999999)
                maxExponent = e
            }
            "minexponent" -> {
                val e = value.toInt()
                require (e <= 0)
                require (e >= -999999999)
                minExponent = e
            }
            "version" -> version = value
            "extended" -> {
                val v = value.toInt()
                require (v == 0 || v == 1)
                extended = v
            }
            "clamp" -> {
                val v = value.toInt()
                require (v == 0 || v == 1)
                clamp = v
            }
            else -> throw RuntimeException("unrecognized directive: $line")
        }
        return true
    }

    fun splitTestTokens(s: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            when {
                s[i].isWhitespace() -> i++
                s[i] == '\'' -> {
                    // Parse quoted string
                    val start = i
                    i++ // skip opening quote
                    while (i < s.length && s[i] != '\'') i++
                    i++ // skip closing quote
                    tokens.add(s.substring(start, i))
                }
                else -> {
                    // Parse unquoted token
                    val start = i
                    while (i < s.length && !s[i].isWhitespace()) i++
                    tokens.add(s.substring(start, i))
                }
            }
        }
        return tokens
    }

    fun processTest(line: String): Boolean {
        val arrowIndex = line.indexOf("->")
        if (arrowIndex < 0)
            return false
        val lhs = line.substring(0, arrowIndex).trim()
        val rhs = line.substring(arrowIndex + 2).trim()

        val lhsTokens = splitTestTokens(lhs)
        val rhsTokens = splitTestTokens(rhs)

        if (lhsTokens.size < 3) {
            println("Invalid LHS format")
            return false
        }

        val id = lhsTokens[0]
        val op = lhsTokens[1]
        val operand1 = lhsTokens.getOrElse(2) { "" }
        val operand2 = lhsTokens.getOrElse(3) { "" }
        val operand3 = lhsTokens.getOrElse(4) { "" }

        val result = rhsTokens.getOrElse(0) { "" }
        val conditions = rhsTokens.drop(1).toTypedArray()

        val dectest = Dectest(line, id, op, operand1, operand2, operand3, result, conditions)

        if (verbose)
            println(dectest)
        dectest.eval()
        return true
    }

    private val MY_NAN = MutDec().set("NaN")

    inner class Dectest(val line: String, val id: String, val op: String,
                        val operand1: String, val operand2: String, val operand3: String,
                        val result: String, val conditions: Array<String>) {
        val op1 = parseOperand(operand1)
        val op2 = if (operand2 == "") MY_NAN else parseOperand(operand2)
        val op3 = if (operand3 == "") MY_NAN else parseOperand(operand3)
        val res = parseOperand(result)
        val exceptionSet: Set<DecException> = captureExceptionSet(conditions)

        override fun toString(): String {
            val sb = StringBuilder("test id:$id op:$op op1:$op1")
            if (op2 !== MY_NAN) {
                sb.append(" op2:$op2")
                if (op3 !== MY_NAN)
                    sb.append(" op3:$op3")
            }
            sb.append(" ==> res:$res")
            if (conditions.isNotEmpty())
                sb.append(conditions.contentToString())
            return sb.toString()
        }

        fun captureExceptionSet(conditions: Array<String>): Set<DecException> {
            val exceptionSet = EnumSet.noneOf(DecException::class.java)
            for (cond in conditions) {
                when (cond.lowercase()) {
                    "clamped" -> {}
                    "conversion_syntax" -> {}
                    "division_by_zero" -> exceptionSet.add(DecException.DIV_BY_ZERO)
                    "division_impossible" -> exceptionSet.add(DecException.INVALID_OPERATION)
                    "division_undefined" -> exceptionSet.add(DecException.INVALID_OPERATION)
                    "inexact" -> exceptionSet.add(DecException.INEXACT)
                    "insufficient_storage" -> exceptionSet.add(DecException.INVALID_OPERATION)
                    "invalid_context" -> exceptionSet.add(DecException.INVALID_OPERATION)
                    "invalid_operation" -> {
                        if (!ignoreInvalidOperationCases.contains(line))
                            exceptionSet.add(DecException.INVALID_OPERATION)
                    }
                    "lost_digits" -> {}
                    "overflow" -> exceptionSet.add(DecException.OVERFLOW)
                    "rounded" -> {}
                    "subnormal" -> {}
                    "underflow" -> exceptionSet.add(DecException.UNDERFLOW)
                    else -> throw RuntimeException("unrecognized condition:$cond")
                }
            }
            return exceptionSet
        }

        fun eval() {
            val env = buildenv()
            if (env == null)
                return
            if (verbose)
                println("op:$op op1:$op1 op2:$op2 ==> res:$res")
            val observed = when (op) {
                "abs" -> MutDec().setAbs(op1)
                "add" -> MutDec().setAdd(op1, op2, env)
                "fma" -> MutDec().setFma(op1, op2, op3, env)
                "subtract" -> MutDec().setSub(op1, op2, env)
                "minus" -> MutDec().setNegate(op1)
                "multiply" -> MutDec().setMul(op1, op2, env)
                "divide" -> MutDec().setDiv(op1, op2, env)
                "toSci" -> {
                    val parseResult = parseOperand(operand1, env)
                    parseResult
                }
                //"remainder" -> Decimal.newMod(op1, op2, ctx)
                "compare" -> op1.partialCompareTo(op2, env)
                "comparesig" -> op1.partialCompareTo(op2, env)
                "comparetotal" -> MutDec().set(op1.totalCompareTo(op2))
                "comparetotmag" -> MutDec().set(op1.magnitudeTotalCompareTo(op2))
                "copy" -> MutDec().set(op1)
                "copyabs" -> MutDec().setAbs(op1)
                "copynegate" -> MutDec().setNegate(op1)
                "copysign" -> MutDec().set(op1, op2.sign)
                "apply" -> MutDec().set(op1)
                "logb" -> MutDec().setLogB(op1, env)
                "max" -> MutDec().setMaximumNumber(op1, op2, env)
                else -> return
            }
            if (verbose)
                println("    observed:$observed")
            if (! res.exactlyEQ(observed)) {
                println("snafu!")
                val parseResult = parseOperand(operand1, env)
                println("res:$res observed:$observed")
                val eq = res.exactlyEQ(observed)
                println("eq:$eq")
                println(":(")
            }
            require (res.exactlyEQ(observed))

            val observedExceptions = env.decFlags.getSetExceptions()
            assertEquals(this.exceptionSet, observedExceptions)
        }
    }

    fun parseOperand(str: String): MutDec {
        if (str.length == 0)
            return MY_NAN
        var t = str
        if (t[0] == '\'' && t[t.lastIndex] == '\'') {
            t = t.substring(1, t.lastIndex).replace("''", "'")
        } else if (t[0] == '\"' && t[t.lastIndex] == '\"') {
            t = t.substring(1, t.lastIndex).replace("\"\"", "\"")
        }
        if (t.length == 0)
            return MY_NAN
        if (t == "#")
            return MY_NAN
        if (t.startsWith('#')) {
            require(t.length == 33)
            val hi = hexStringToLong(t.substring(1, 17))
            val lo = hexStringToLong(t.substring(17, 33))
            val dpd = MutDec().setDpd128(hi, lo)
            return dpd
        }
        val d = MutDec().set(t)
        return d
    }

    fun hexStringToLong(hex: String): Long {
        require(hex.length == 16)
        return hex.toULong(16).toLong()
    }

    fun parseOperand(str: String, env: DecEnv): MutDec {
        if (str.length == 0)
            return MY_NAN
        var t = str
        if (t[0] == '\'' && t[t.lastIndex] == '\'') {
            t = t.substring(1, t.lastIndex).replace("''", "'")
        } else if (t[0] == '\"' && t[t.lastIndex] == '\"') {
            t = t.substring(1, t.lastIndex).replace("\"\"", "\"")
        }
        if (t.length == 0)
            return MY_NAN
        if (t == "#")
            return MY_NAN
        if (t.startsWith('#')) {
            println("octothorpe not fully implemented")
            return MY_NAN
        }
        val d = MutDec().set(t, env)
        return d
    }

    fun buildenv(): DecEnv? {
        // relax this requirement
        // require (minExponent == -(maxExponent - 1))
        val roundingIndex = validRoundingStrings.indexOf(rounding)
        if (roundingIndex < 0 || roundingIndex >= decRoundings.size)
            return null
        val fmt = DecFormat(precision, maxExponent)
        val env = DecEnv().with(fmt).with(decRoundings[roundingIndex])
        return env
    }

}