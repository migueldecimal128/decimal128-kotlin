package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestLogExp2 {

    data class TC(val function: String, val x: String, val expected: String)

    val tcs = arrayOf(
        TC("ln", "2.5", "0.9162907318741550651835272117680111"),
        TC("ln", "3", "1.098612288668109691395245236922526"),
        TC("ln", "2", "0.6931471805599453094172321214581765681"),
        TC("ln", "10", "2.302585092994045684017991454684364"),
        TC("ln", "2.718281828459045235360287471352662", "0.9999999999999999999999999999999998"),
        // e was rounded up in the last digit ... 3 instead of 2
        TC("ln", "2.718281828459045235360287471352663", "1.000000000000000000000000000000000"),
        TC("ln", "1.5", "0.4054651081081643819780131154643661"),

        TC("exp10", "-7000", "0E-6176"),

        TC("log10", "2", "0.3010299956639811952137388947244930268"),
        TC("log10", "3", "0.4771212547196624372950279032551153092"),
        TC("log10", "7", "0.8450980400142568307122162585926361935"),

        TC("log10", "10", "1"),
        TC("log10", "100", "2"),
        TC("log10", "1000", "3"),
        TC("log10", "0.5", "-0.3010299956639811952137388947244930268"),
        TC("log10", "0.1", "-1"),
        TC("log10", "0.01", "-2"),
        TC("log10", "1.5", "0.1760912590556812420812890085306296"),
        TC("log10", "2.5", "0.3979400086720376095725222105510139465"),
        TC("log10", "9.9", "0.9956351945975499153402557777532548601"),
        TC("log10", "123456789.123456789", "8.091514977603564929204438209903815091"),

        TC("exp10", "0", "1"),
        TC("exp10", "1", "1E1"),
        TC("exp10", "-1", "0.1"),
        TC("exp10", "2", "1E2"),
        TC("exp10", "-2", "0.01"),
        TC("exp10", "6", "1E6"),
        TC("exp10", "-6", "0.000001"),
        TC("exp10", "7", "1E7"),
        TC("exp10", "-7", "1E-7"),
        TC("exp10", "700", "1E700"),
        TC("exp10", "-700", "1E-700"),
        TC("exp10", "7000", "Infinity"),
        TC("exp10", "-7000", "0E-6176"),
        TC("exp10", "0.5", "3.162277660168379331998893544432718534"),
        TC("exp10", "-0.5", "0.3162277660168379331998893544432718534"),
        TC("exp10", "0.1", "1.258925411794167210423954106395800606"),
        TC("exp10", "-0.1", "0.7943282347242815020659182828363879326"),
        TC("exp10", "1.5", "31.62277660168379331998893544432718534"),
        TC("exp10", "-1.5", "0.03162277660168379331998893544432718534"),
        TC("exp10", "0.30102999566398119521373889472449302677", "2.000000000000000000000000000000000"),

        // ln(10)+.1
        TC("exp", "2.4025850929940456840179914546843642076", "11.05170918075647624811707826490246"),
        TC("exp", "1", "2.7182818284590452353602874713526624982"),
        // ln(100)
        // FIXME ... looks like this answer is too far off.
        TC("exp", "4.6051701859880913680359828938172284152", "99.99999999999999999999999844484996"),
        // ln(10)
        TC("exp", "2.302585092994045684017991454684364", "9.999999999999999999999999999999998"),

    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        when (tc.function) {
            "ln" -> assertLnCorrect(tc.x, tc.expected)
            "exp" -> assertExpCorrect(tc.x, tc.expected)
            "log10" -> assertLog10Correct(tc.x, tc.expected)
            "exp10" -> assertExp10Correct(tc.x, tc.expected)
        }
    }

    private fun assertLnCorrect(input: String, expected: String) {
        val ctx = DecContext.decimal128IEEE()
        val x = MutDec().set(input, ctx)
        val result = MutDec().setLn(x, ctx)
        val exp = MutDec().set(expected, ctx)
        assertTrue(result.bitwiseEQ(exp), "ln($input) = $result, expected $exp")
    }

    private fun assertLog10Correct(input: String, expected: String) {
        val ctx = DecContext.decimal128IEEE()
        val x = MutDec().set(input, ctx)
        val result = MutDec().setLog10(x, ctx)
        val exp = MutDec().set(expected, ctx)
        assertTrue(result.bitwiseEQ(exp), "log10($input) = $result, expected $exp")
    }

    private fun assertExpCorrect(input: String, expected: String) {
        val ctx = DecContext.decimal128IEEE()
        val x = MutDec().set(input, ctx)
        val result = MutDec().setExp(x, ctx)
        val exp = MutDec().set(expected, ctx)
        assertTrue(result.bitwiseEQ(exp), "exp($input) = $result, expected $exp")
    }

    private fun assertExp10Correct(input: String, expected: String) {
        val ctx = DecContext.decimal128IEEE()
        val x = MutDec().set(input, ctx)
        val result = MutDec().setExp10(x, ctx)
        val exp = MutDec().set(expected, ctx)
        assertTrue(result.bitwiseEQ(exp), "exp10($input) = $result, expected $exp")
    }
}