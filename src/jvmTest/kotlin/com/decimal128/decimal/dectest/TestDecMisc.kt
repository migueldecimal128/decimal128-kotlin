package com.decimal128.decimal.dectest

import com.decimal128.decimal.Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runBinaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runMethod_IntCtx_Decimal
import com.decimal128.decimal.dectest.DectestRunner1.runTernaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalCtxOp
import com.decimal128.decimal.dectest.DectestRunner1.runUnaryDecimalOp
import org.junit.jupiter.api.Test
import com.decimal128.decimal.fmaImpl

class TestDecMisc {

    val verbose = true

    @Test
    fun testLogB() = runUnaryDecimalCtxOp(
        "dqLogB.decTest",
        "logb",
        Decimal::logB,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testScaleB(): Unit = runMethod_IntCtx_Decimal(
        "dqScaleB.decTest",
        "scaleb",
        Decimal::scaleB,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
            "dqscb018 scaleb  10  Infinity -> NaN Invalid_operation",
            "dqscb019 scaleb  10 -Infinity -> NaN Invalid_operation",
            "dqscb025 scaleb    4    NaN    -> NaN",
            "dqscb026 scaleb -Inf   -NaN    -> -NaN",
            "dqscb027 scaleb    4   sNaN    -> NaN Invalid_operation",
            "dqscb028 scaleb  Inf  -sNaN    -> -NaN Invalid_operation",
            "dqscb031 scaleb  1.23    1.00 ->  NaN Invalid_operation",
            "dqscb032 scaleb  1.23    1.1  ->  NaN Invalid_operation",
            "dqscb033 scaleb  1.23    1.01 ->  NaN Invalid_operation",
            "dqscb034 scaleb  1.23    0.01 ->  NaN Invalid_operation",
            "dqscb035 scaleb  1.23    0.11 ->  NaN Invalid_operation",
            "dqscb036 scaleb  1.23    0.999999999 ->  NaN Invalid_operation",
            "dqscb0614 scaleb  1.23   -1.00 ->  NaN Invalid_operation",
            "dqscb039 scaleb  1.23   -1.1  ->  NaN Invalid_operation",
            "dqscb040 scaleb  1.23   -1.01 ->  NaN Invalid_operation",
            "dqscb041 scaleb  1.23   -0.01 ->  NaN Invalid_operation",
            "dqscb042 scaleb  1.23   -0.11 ->  NaN Invalid_operation",
            "dqscb043 scaleb  1.23   -0.999999999 ->  NaN Invalid_operation",
            "dqscb044 scaleb  1.23    0.1         ->  NaN Invalid_operation",
            "dqscb045 scaleb  1.23    1E+1        ->  NaN Invalid_operation",
            "dqscb046 scaleb  1.23    1.1234E+6   ->  NaN Invalid_operation",
            "dqscb047 scaleb  1.23    1.123E+4    ->  NaN Invalid_operation",
            "dqscb122 scaleb  1.23    12357       ->  NaN Invalid_operation",
            "dqscb123 scaleb  1.23    12358       ->  NaN Invalid_operation",
            "dqscb126 scaleb  1.23   -12357       ->  NaN Invalid_operation",
            "dqscb127 scaleb  1.23   -12358       ->  NaN Invalid_operation",
            "dqscb861 scaleb  NaN01   -Inf     ->  NaN1",
            "dqscb864 scaleb  NaN04    Inf     ->  NaN4",
            "dqscb865 scaleb  NaN05    NaN61   ->  NaN5",
            "dqscb866 scaleb -Inf     -NaN71   -> -NaN71",
            "dqscb867 scaleb -1000     NaN81   ->  NaN81",
            "dqscb868 scaleb  1000     NaN91   ->  NaN91",
            "dqscb869 scaleb  Inf      NaN101  ->  NaN101",
            "dqscb871 scaleb  sNaN011  -Inf    ->  NaN11  Invalid_operation",
            "dqscb874 scaleb  sNaN014   NaN171 ->  NaN14  Invalid_operation",
            "dqscb875 scaleb  sNaN015  sNaN181 ->  NaN15  Invalid_operation",
            "qscb876 scaleb  NaN016   sNaN191 ->  NaN191 Invalid_operation",
            "dqscb876 scaleb  NaN016   sNaN191 ->  NaN191 Invalid_operation",
            "dqscb877 scaleb -Inf      sNaN201 ->  NaN201 Invalid_operation",
            "dqscb878 scaleb -1000     sNaN211 ->  NaN211 Invalid_operation",
            "dqscb879 scaleb  1000    -sNaN221 -> -NaN221 Invalid_operation",
            "dqscb880 scaleb  Inf      sNaN231 ->  NaN231 Invalid_operation",
            "dqscb881 scaleb  NaN025   sNaN241 ->  NaN241 Invalid_operation",

        )
    )

    @Test
    fun testNextUp() = runUnaryDecimalCtxOp(
        "dqNextPlus.decTest",
        "nextplus",
        Decimal::nextUp,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testNextUpCases() = runUnaryDecimalCtxOp(
        Decimal::nextUp,
        verbose = verbose,
        cases = arrayOf(
            "dqnextp151 nextplus  -Inf    -> -9.999999999999999999999999999999999E+6144",
            "dqnextp104 nextplus  0E+30000    ->  1E-6176",
            "dqnextp026 nextplus -1.000000000000000000000000000000000  ->  -0.9999999999999999999999999999999999",
        )
    )

    @Test
    fun testNextDown() = runUnaryDecimalCtxOp(
        "dqNextMinus.decTest",
        "nextminus",
        Decimal::nextDown,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testReduce() = runUnaryDecimalCtxOp(
        "dqReduce.decTest",
        "reduce",
        Decimal::stripTrailingZeros,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testReduceCases() = runUnaryDecimalCtxOp(
        Decimal::stripTrailingZeros,
        verbose = verbose,
        cases = arrayOf(
            "dqred003 reduce '1.00'   -> '1'",
        )
    )

    @Test
    fun testToIntegralExact() = runUnaryDecimalCtxOp(
        "dqToIntegral.decTest",
        "tointegralx",
        Decimal::roundToIntegralExact,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testToIntegralExact2() = runUnaryDecimalCtxOp(
        "dqCanonical.decTest",
        "tointegralx",
        Decimal::roundToIntegralExact,
        verbose = verbose,
        skip = true,
        skipCases = arrayOf(
        )
    )

    @Test
    fun testToIntegralExactCases(): Unit = runUnaryDecimalCtxOp(
        Decimal::roundToIntegralExact,
        verbose = verbose,
        cases = arrayOf(
            "dqcan616 tointegralx  #7e010ff3fcff3fcff3fcff3fcff3fcff  -> #7c000ff3fcff3fcff3fcff3fcff3fcff  Invalid_operation",
            "dqintx074 tointegralx '1.23E+6144'  -> #47ffd300000000000000000000000000 Clamped",
        )
    )

}

